package com.example.otomuzik.data

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.otomuzik.model.FolderItem
import com.example.otomuzik.model.Playlist
import com.example.otomuzik.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class MusicRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("oto_muzik_prefs", Context.MODE_PRIVATE)

    private val dbHelper = MusicDatabaseHelper(context)

    companion object {
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "wav", "flac", "ogg", "aac", "wma", "opus", "mp4")
        private const val KEY_FAVORITES = "fav_songs_paths"
        private const val KEY_LAST_PATH = "last_played_path"
        private const val KEY_LAST_POS = "last_played_pos"
        private const val KEY_LAST_SHUFFLE = "last_shuffle"
        private const val KEY_LAST_REPEAT = "last_repeat"
    }

    /**
     * Kütüphanedeki şarkıları döner. Önce SQLite veritabanındaki önbelleğe bakar;
     * eğer kütüphane boşsa otomatik olarak ilk taramayı yapar ve veritabanına kaydeder.
     * Böylece yüzlerce dosya içeren dizinler her seferinde yeniden taranmaz.
     */
    /**
     * Kütüphanedeki şarkıları doğrudan SQLite veritabanından çeker.
     * Disk veya dosya taraması kesinlikle yapmaz. Anında (milisaniyeler içinde) döner.
     */
    suspend fun getSongsFromCacheOnly(): List<Song> = withContext(Dispatchers.IO) {
        dbHelper.getAllSongs().filterNot { isSongHidden(it.path) }
    }

    suspend fun getAllSongs(forceRefresh: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = dbHelper.getAllSongs().filterNot { isSongHidden(it.path) }
            if (cached.isNotEmpty()) {
                return@withContext cached
            }
        }
        scanAndIndexLibrary()
    }

    /**
     * Cihazdaki (Dahili Hafıza ve tüm USB/SD depolama alanları) müzik dosyalarını tarar,
     * ID3 ve meta verilerini SQLite kütüphanesine kaydeder.
     */
    suspend fun scanAndIndexLibrary(onProgress: ((currentDir: String, foundCount: Int) -> Unit)? = null): List<Song> = withContext(Dispatchers.IO) {
        val foundSongs = mutableListOf<Song>()
        val seenPaths = mutableSetOf<String>()
        onProgress?.invoke("Android Medya Deposu taranıyor...", 0)

        // 1. Android MediaStore üzerinden hızlı tarama
        // API 30+ (Android 11) cihazlarda YEAR ve GENRE MediaStore'dan doğrudan alınabilir.
        val baseProjection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )
        // YEAR her API seviyesinde MediaStore.Audio.Media.YEAR üzerinden erişilebilir.
        baseProjection.add(MediaStore.Audio.Media.YEAR)
        // GENRE ise API 30 (Android 11) ve üzerinde MediaStore sütunu olarak mevcut.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            baseProjection.add(MediaStore.Audio.Media.GENRE)
        }
        val projection = baseProjection.toTypedArray()

        // Araç multimedya sistemlerinde kopyalanan şarkılarda IS_MUSIC 0 kalabilir veya DURATION henüz hesaplanmamış olabilir.
        // Bu yüzden uzantı tabanlı geniş filtreleme uygulanır.
        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DATA} LIKE '%.mp3' OR ${MediaStore.Audio.Media.DATA} LIKE '%.flac' OR ${MediaStore.Audio.Media.DATA} LIKE '%.m4a' OR ${MediaStore.Audio.Media.DATA} LIKE '%.wav' OR ${MediaStore.Audio.Media.DATA} LIKE '%.aac' OR ${MediaStore.Audio.Media.DATA} LIKE '%.ogg') AND (${MediaStore.Audio.Media.DURATION} >= 5000 OR ${MediaStore.Audio.Media.DURATION} IS NULL OR ${MediaStore.Audio.Media.DURATION} = 0)"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol       = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol    = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol   = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol    = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol     = it.getColumnIndex(MediaStore.Audio.Media.DATA)
                val albumIdCol  = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val yearCol     = it.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    it.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else -1

                val includedFolders = getIncludedFolders()

                while (it.moveToNext()) {
                    val path = if (dataCol >= 0) it.getString(dataCol) ?: "" else ""
                    if (path.isEmpty() || seenPaths.contains(path) || !isPathAllowed(path, includedFolders) || isSongHidden(path)) {
                        continue
                    }

                    val id          = it.getLong(idCol)
                    val rawTitle    = it.getString(titleCol) ?: "Bilinmeyen Parça"
                    val rawArtist   = it.getString(artistCol) ?: "Bilinmeyen Sanatçı"
                    val rawAlbum    = it.getString(albumCol) ?: "Bilinmeyen Albüm"
                    
                    val id3Info = com.example.otomuzik.util.Id3Helper.parseFile(path)
                    val title  = com.example.otomuzik.util.TurkishStringFixer.fix(id3Info.title.takeIf { it.isNotBlank() } ?: rawTitle)
                    val artist = com.example.otomuzik.util.TurkishStringFixer.fix(id3Info.artist.takeIf { it.isNotBlank() } ?: rawArtist)
                    val album  = com.example.otomuzik.util.TurkishStringFixer.fix(id3Info.album.takeIf { it.isNotBlank() } ?: rawAlbum)
                    
                    val duration = if (id3Info.durationMs > 0) id3Info.durationMs else it.getLong(durationCol)
                    val albumId  = it.getLong(albumIdCol)
                    val year = if (yearCol >= 0) {
                        val y = it.getInt(yearCol)
                        if (y > 0) y.toString() else ""
                    } else ""
                    val genre = if (genreCol >= 0) it.getString(genreCol) ?: "" else ""

                    seenPaths.add(path)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val file = File(path)
                    val parentFolder = file.parent ?: ""
                    val ext = file.extension.lowercase(Locale.ROOT)
                    val sizeMb = if (file.exists()) {
                        "%.1f MB".format(file.length() / (1024.0 * 1024.0))
                    } else ""

                    foundSongs.add(
                        Song(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>") "Bilinmeyen Sanatçı" else artist,
                            album  = if (album  == "<unknown>") "Bilinmeyen Albüm"   else album,
                            durationMs       = duration,
                            path             = path,
                            uriString        = contentUri,
                            albumId          = albumId,
                            folderPath       = parentFolder,
                            format           = ext,
                            fileSizeFormatted = sizeMb,
                            year             = year,
                            genre            = genre
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Araç USB / SD Kartlarını ve Kullanıcının Seçtiği Klasörleri doğrudan dosya sistemi olarak tara
        val activeIncludedFolders = getIncludedFolders()
        try {
            val roots = getStorageRoots()
            for (root in roots) {
                val rootFile = File(root.path)
                if (rootFile.exists() && rootFile.isDirectory) {
                    onProgress?.invoke(rootFile.name, foundSongs.size)
                    scanDirectoryForSongs(rootFile, seenPaths, foundSongs, 0, onProgress, activeIncludedFolders)
                }
            }
            // Dahil edilen özel klasörler varsa onları da doğrudan tara
            for (folderPath in activeIncludedFolders) {
                val folderFile = File(folderPath)
                if (folderFile.exists() && folderFile.isDirectory && !seenPaths.contains(folderPath)) {
                    onProgress?.invoke(folderFile.name, foundSongs.size)
                    scanDirectoryForSongs(folderFile, seenPaths, foundSongs, 0, onProgress, activeIncludedFolders)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Veritabanına toplu kaydet ve artık var olmayanları temizle
        onProgress?.invoke("Veritabanı güncelleniyor...", foundSongs.size)
        dbHelper.saveSongs(foundSongs)
        dbHelper.syncWithExistingPaths(seenPaths)

        dbHelper.getAllSongs().filterNot { isSongHidden(it.path) }
    }

    private fun scanDirectoryForSongs(
        currentDir: File,
        seenPaths: MutableSet<String>,
        outList: MutableList<Song>,
        depth: Int = 0,
        onProgress: ((currentDir: String, foundCount: Int) -> Unit)? = null,
        includedFolders: Set<String> = emptySet()
    ) {
        if (depth > 8) return // Derin döngüleri engelle
        val dirName = currentDir.name
        if (dirName.startsWith(".") || dirName.equals("Android", ignoreCase = true) || dirName.equals("lost+found", ignoreCase = true)) {
            return
        }

        onProgress?.invoke(dirName, outList.size)
        val files = currentDir.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) {
                if (!f.isHidden && !f.name.startsWith(".") && !f.name.equals("Android", ignoreCase = true)) {
                    scanDirectoryForSongs(f, seenPaths, outList, depth + 1, onProgress, includedFolders)
                }
            } else if (f.isFile && isAudioFile(f.name) && !seenPaths.contains(f.absolutePath) && isPathAllowed(f.absolutePath, includedFolders) && !isSongHidden(f.absolutePath)) {
                seenPaths.add(f.absolutePath)
                val name = f.nameWithoutExtension
                val ext = f.extension.uppercase(Locale.ROOT)
                val sizeMb = "%.1f MB".format(f.length() / (1024.0 * 1024.0))

                val id3Info = com.example.otomuzik.util.Id3Helper.parseFile(f.absolutePath)
                var songTitle = id3Info.title.takeIf { it.isNotBlank() } ?: name
                var songArtist = id3Info.artist.takeIf { it.isNotBlank() } ?: currentDir.name
                val songAlbum = id3Info.album.takeIf { it.isNotBlank() } ?: currentDir.name

                if (songTitle == name && name.contains(" - ")) {
                    songArtist = name.substringBefore(" - ").trim()
                    songTitle = name.substringAfter(" - ").trim()
                }

                outList.add(
                    Song(
                        id = f.absolutePath.hashCode().toLong(),
                        title = com.example.otomuzik.util.TurkishStringFixer.fix(songTitle),
                        artist = com.example.otomuzik.util.TurkishStringFixer.fix(songArtist),
                        album = com.example.otomuzik.util.TurkishStringFixer.fix(songAlbum),
                        durationMs = if (id3Info.durationMs > 0) id3Info.durationMs else 0L,
                        path = f.absolutePath,
                        uriString = Uri.fromFile(f).toString(),
                        folderPath = currentDir.absolutePath,
                        format = ext,
                        fileSizeFormatted = sizeMb
                    )
                )
            }
        }
    }

    /**
     * Araçtaki USB bellekler, SD kartlar ve Dahili Hafıza kök dizinlerini bulur.
     */
    suspend fun getStorageRoots(): List<FolderItem> = withContext(Dispatchers.IO) {
        val roots = mutableListOf<FolderItem>()
        val seenPaths = mutableSetOf<String>()

        // 1. Dahili Hafıza (Internal Storage)
        val internalStorage = Environment.getExternalStorageDirectory()
        if (internalStorage != null && internalStorage.exists()) {
            val musicDir = File(internalStorage, "Music")
            if (musicDir.exists()) {
                roots.add(
                    FolderItem(
                        name = "Dahili Hafıza (Music)",
                        path = musicDir.absolutePath,
                        isDirectory = true,
                        isStorageRoot = true,
                        songCount = countAudioFiles(musicDir)
                    )
                )
                seenPaths.add(musicDir.absolutePath)
            }
            val downloadDir = File(internalStorage, "Download")
            if (downloadDir.exists() && !seenPaths.contains(downloadDir.absolutePath)) {
                roots.add(
                    FolderItem(
                        name = "Dahili Hafıza (Download)",
                        path = downloadDir.absolutePath,
                        isDirectory = true,
                        isStorageRoot = true,
                        songCount = countAudioFiles(downloadDir)
                    )
                )
                seenPaths.add(downloadDir.absolutePath)
            }
            if (!seenPaths.contains(internalStorage.absolutePath)) {
                roots.add(
                    FolderItem(
                        name = "Dahili Hafıza (Tümü)",
                        path = internalStorage.absolutePath,
                        isDirectory = true,
                        isStorageRoot = true,
                        songCount = countAudioFiles(internalStorage)
                    )
                )
                seenPaths.add(internalStorage.absolutePath)
            }
        }

        // 2. Harici Depolama / USB Bellekler / SD Kartlar (ContextCompat getExternalFilesDirs)
        try {
            val extDirs = ContextCompat.getExternalFilesDirs(context, null)
            for (dir in extDirs) {
                if (dir != null) {
                    val fullPath = dir.absolutePath
                    val storageIdx = fullPath.indexOf("/storage/")
                    if (storageIdx != -1) {
                        val sub = fullPath.substring(storageIdx + "/storage/".length)
                        val slashIdx = sub.indexOf('/')
                        val volumeName = if (slashIdx != -1) sub.substring(0, slashIdx) else sub

                        if (volumeName != "emulated" && volumeName != "self" && volumeName != "knox") {
                            val rootPath = "/storage/$volumeName"
                            val rootFile = File(rootPath)
                            if (rootFile.exists() && !seenPaths.contains(rootPath)) {
                                roots.add(
                                    FolderItem(
                                        name = "USB Bellek ($volumeName)",
                                        path = rootPath,
                                        isDirectory = true,
                                        isStorageRoot = true,
                                        songCount = countAudioFiles(rootFile)
                                    )
                                )
                                seenPaths.add(rootPath)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. /storage klasörünü doğrudan tara (Android araç multimedyalarında yaygın)
        try {
            val storageFolder = File("/storage")
            if (storageFolder.exists() && storageFolder.isDirectory) {
                val list = storageFolder.listFiles()
                if (list != null) {
                    for (f in list) {
                        val name = f.name
                        if (f.isDirectory && name != "emulated" && name != "self" && name != "knox" && !seenPaths.contains(f.absolutePath)) {
                            val songCount = countAudioFiles(f)
                            roots.add(
                                FolderItem(
                                    name = "USB / Harici ($name)",
                                    path = f.absolutePath,
                                    isDirectory = true,
                                    isStorageRoot = true,
                                    songCount = songCount
                                )
                            )
                            seenPaths.add(f.absolutePath)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Araç multimedya USB montaj dizinlerini tara (/mnt/media_rw, /mnt/usb_storage vb.)
        val candidateMounts = listOf(
            "/mnt/media_rw",
            "/mnt/usb_storage",
            "/mnt/udisk",
            "/mnt/udisk1",
            "/mnt/udisk2",
            "/mnt/extsd",
            "/mnt/sdcard2"
        )
        for (mPath in candidateMounts) {
            try {
                val mFile = File(mPath)
                if (mFile.exists() && mFile.isDirectory) {
                    if (mPath == "/mnt/media_rw") {
                        mFile.listFiles()?.forEach { sub ->
                            if (sub.isDirectory && !seenPaths.contains(sub.absolutePath)) {
                                roots.add(
                                    FolderItem(
                                        name = "USB (${sub.name})",
                                        path = sub.absolutePath,
                                        isDirectory = true,
                                        isStorageRoot = true,
                                        songCount = countAudioFiles(sub)
                                    )
                                )
                                seenPaths.add(sub.absolutePath)
                            }
                        }
                    } else if (!seenPaths.contains(mFile.absolutePath)) {
                        roots.add(
                            FolderItem(
                                name = "USB (${mFile.name})",
                                path = mFile.absolutePath,
                                isDirectory = true,
                                isStorageRoot = true,
                                songCount = countAudioFiles(mFile)
                            )
                        )
                        seenPaths.add(mFile.absolutePath)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        roots
    }

    /**
     * Belirtilen klasörün içindeki alt klasörleri ve müzik dosyalarını listeler.
     */
    suspend fun getFolderContents(folderPath: String): List<FolderItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FolderItem>()
        val folder = File(folderPath)

        if (!folder.exists() || !folder.isDirectory) {
            return@withContext items
        }

        val files = folder.listFiles() ?: return@withContext items

        // Önce klasörler (alfabetik)
        val dirList = files.filter { it.isDirectory && !it.isHidden && it.canRead() }
            .sortedBy { it.name.lowercase(Locale.ROOT) }

        for (dir in dirList) {
            val count = countAudioFiles(dir)
            items.add(
                FolderItem(
                    name = dir.name,
                    path = dir.absolutePath,
                    isDirectory = true,
                    songCount = count
                )
            )
        }

        // Sonra müzik dosyaları
        val audioFiles = files.filter { it.isFile && isAudioFile(it.name) && !isSongHidden(it.absolutePath) }
            .sortedBy { it.name.lowercase(Locale.ROOT) }

        var dummyId = 1000000L
        for (file in audioFiles) {
            val name = file.nameWithoutExtension
            val ext = file.extension.uppercase(Locale.ROOT)
            val sizeMb = "%.1f MB".format(file.length() / (1024.0 * 1024.0))

            var songTitle = name
            var songArtist = folder.name
            if (name.contains(" - ")) {
                songArtist = name.substringBefore(" - ").trim()
                songTitle = name.substringAfter(" - ").trim()
            }

            val song = Song(
                id = dummyId++,
                title = com.example.otomuzik.util.TurkishStringFixer.fix(songTitle),
                artist = com.example.otomuzik.util.TurkishStringFixer.fix(songArtist),
                album = com.example.otomuzik.util.TurkishStringFixer.fix(folder.name),
                durationMs = 0L,
                path = file.absolutePath,
                uriString = Uri.fromFile(file).toString(),
                folderPath = folder.absolutePath,
                format = ext,
                fileSizeFormatted = sizeMb
            )
            items.add(
                FolderItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = false,
                    song = song
                )
            )
        }

        items
    }

    /**
     * Bir klasördeki ve alt klasörlerindeki tüm müzikleri toplar (Klasörü Çal ve Çalma Listesine Ekle için).
     * Önce SQLite kütüphanesine bakar (anında döner), yoksa diskten toplar.
     */
    suspend fun getAllSongsInDirectory(folderPath: String): List<Song> = withContext(Dispatchers.IO) {
        val cached = dbHelper.getSongsInFolder(folderPath)
        if (cached.isNotEmpty()) {
            return@withContext cached
        }

        val result = mutableListOf<Song>()
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) return@withContext result

        fun scanDir(current: File) {
            val files = current.listFiles() ?: return
            for (f in files) {
                if (f.isDirectory && !f.isHidden) {
                    scanDir(f)
                } else if (f.isFile && isAudioFile(f.name)) {
                    val name = f.nameWithoutExtension
                    val ext = f.extension.uppercase(Locale.ROOT)
                    val sizeMb = "%.1f MB".format(f.length() / (1024.0 * 1024.0))
                    var songTitle = name
                    var songArtist = current.name
                    if (name.contains(" - ")) {
                        songArtist = name.substringBefore(" - ").trim()
                        songTitle = name.substringAfter(" - ").trim()
                    }

                    result.add(
                        Song(
                            id = f.absolutePath.hashCode().toLong(),
                            title = com.example.otomuzik.util.TurkishStringFixer.fix(songTitle),
                            artist = com.example.otomuzik.util.TurkishStringFixer.fix(songArtist),
                            album = com.example.otomuzik.util.TurkishStringFixer.fix(current.name),
                            durationMs = 0L,
                            path = f.absolutePath,
                            uriString = Uri.fromFile(f).toString(),
                            folderPath = current.absolutePath,
                            format = ext,
                            fileSizeFormatted = sizeMb
                        )
                    )
                }
            }
        }

        scanDir(folder)
        val sorted = com.example.otomuzik.util.SongSortHelper.sortSongs(result)
        if (sorted.isNotEmpty()) {
            dbHelper.saveSongs(sorted)
        }
        sorted
    }

    private fun isAudioFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return AUDIO_EXTENSIONS.contains(ext)
    }

    /**
     * Klasördeki şarkı sayısını hesaplar.
     * Önce SQLite kütüphane indeksine bakar; bu sayede binlerce dosya olan dizinlerde
     * her seferinde disk taranmaz ve klasörler anında açılır.
     */
    private fun countAudioFiles(dir: File): Int {
        val dbCount = dbHelper.getSongCountInFolder(dir.absolutePath)
        if (dbCount > 0) {
            return dbCount
        }

        var count = 0
        try {
            val files = dir.listFiles() ?: return 0
            for (f in files) {
                if (f.isFile && isAudioFile(f.name)) {
                    count++
                } else if (f.isDirectory && !f.isHidden) {
                    val sub = f.listFiles() ?: continue
                    count += sub.count { it.isFile && isAudioFile(it.name) }
                }
            }
        } catch (e: Exception) {
            // Güvenlik ve izin kısıtlamalarını sessizce yakala
        }
        return count
    }

    // --- Çalma Listesi (Playlist) İşlemleri ---
    suspend fun getPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        dbHelper.getAllPlaylists()
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        dbHelper.createPlaylist(name)
    }

    suspend fun deletePlaylist(playlistId: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deletePlaylist(playlistId)
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songs: List<Song>): Int = withContext(Dispatchers.IO) {
        dbHelper.addSongsToPlaylist(playlistId, songs)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songPath: String): Boolean = withContext(Dispatchers.IO) {
        dbHelper.removeSongFromPlaylist(playlistId, songPath)
    }

    suspend fun getSongsForPlaylist(playlistId: Long): List<Song> = withContext(Dispatchers.IO) {
        when (playlistId) {
            -1L -> dbHelper.getMostPlayedSongs(50)
            -2L -> dbHelper.getRecentlyAddedSongs(50)
            else -> dbHelper.getSongsForPlaylist(playlistId)
        }
    }

    suspend fun incrementPlayCount(songPath: String) = withContext(Dispatchers.IO) {
        dbHelper.incrementPlayCount(songPath)
    }

    suspend fun clearPlayHistory() = withContext(Dispatchers.IO) {
        dbHelper.clearPlayHistory()
    }

    suspend fun getSmartPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val mostPlayed = dbHelper.getMostPlayedSongs(50)
        val recentlyAdded = dbHelper.getRecentlyAddedSongs(50)

        val smartLists = mutableListOf<Playlist>()
        if (mostPlayed.isNotEmpty()) {
            smartLists.add(Playlist(id = -1, name = "En Çok Dinlenenler", createdAt = 0, songCount = mostPlayed.size))
        }
        if (recentlyAdded.isNotEmpty()) {
            smartLists.add(Playlist(id = -2, name = "Son Eklenenler", createdAt = 0, songCount = recentlyAdded.size))
        }
        smartLists
    }

    // --- Favoriler (Preferences tabanlı, ultra hafif) ---
    fun getFavoritePaths(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun toggleFavorite(songPath: String): Boolean {
        val current = getFavoritePaths().toMutableSet()
        val isFav: Boolean
        if (current.contains(songPath)) {
            current.remove(songPath)
            isFav = false
        } else {
            current.add(songPath)
            isFav = true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        return isFav
    }

    suspend fun updateSongDetails(
        oldPath: String,
        newPath: String,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newGenre: String,
        newYear: String
    ) = withContext(Dispatchers.IO) {
        dbHelper.updateSongDetails(oldPath, newPath, newTitle, newArtist, newAlbum, newGenre, newYear)
        if (oldPath != newPath) {
            val favs = getFavoritePaths().toMutableSet()
            if (favs.contains(oldPath)) {
                favs.remove(oldPath)
                favs.add(newPath)
                prefs.edit().putStringSet(KEY_FAVORITES, favs).apply()
            }
            if (getLastPlayedPath() == oldPath) {
                prefs.edit().putString(KEY_LAST_PATH, newPath).apply()
            }
        }
    }

    suspend fun deleteSong(song: Song): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(song.path)
            if (file.exists()) {
                file.delete()
            }
            try {
                val parent = file.parentFile
                val nameNoExt = file.nameWithoutExtension
                if (parent != null) {
                    val lrc = File(parent, "$nameNoExt.lrc")
                    if (lrc.exists()) lrc.delete()
                    val txt = File(parent, "$nameNoExt.txt")
                    if (txt.exists()) txt.delete()
                }
            } catch (ignored: Exception) {}

            try {
                if (song.uriString.isNotEmpty()) {
                    context.contentResolver.delete(Uri.parse(song.uriString), null, null)
                }
            } catch (ignored: Exception) {}

            try {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(song.path),
                    null,
                    null
                )
            } catch (ignored: Exception) {}

            dbHelper.deleteSong(song.path)
            unhideSong(song.path)

            val favs = getFavoritePaths().toMutableSet()
            if (favs.contains(song.path)) {
                favs.remove(song.path)
                prefs.edit().putStringSet(KEY_FAVORITES, favs).apply()
            }
            if (getLastPlayedPath() == song.path) {
                prefs.edit().remove(KEY_LAST_PATH).apply()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun renameSongFile(song: Song, newNameWithoutExt: String): Song? = withContext(Dispatchers.IO) {
        try {
            val originalFile = File(song.path)
            if (!originalFile.exists()) return@withContext null
            val extension = originalFile.extension.ifEmpty { "mp3" }
            var cleanName = newNameWithoutExt.trim()
            if (cleanName.endsWith(".$extension", ignoreCase = true)) {
                cleanName = cleanName.substringBeforeLast(".$extension").trim()
            }
            cleanName = cleanName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
            if (cleanName.isBlank()) return@withContext null

            val parentDir = originalFile.parentFile ?: return@withContext null
            val targetFile = File(parentDir, "$cleanName.$extension")
            if (targetFile.exists() && targetFile.absolutePath != originalFile.absolutePath) {
                return@withContext null
            }

            val renamed = originalFile.renameTo(targetFile)
            if (renamed) {
                val effectivePath = targetFile.absolutePath
                val effectiveUri = Uri.fromFile(targetFile).toString()

                try {
                    val oldLrc = File(parentDir, "${originalFile.nameWithoutExtension}.lrc")
                    if (oldLrc.exists()) oldLrc.renameTo(File(parentDir, "$cleanName.lrc"))
                    val oldTxt = File(parentDir, "${originalFile.nameWithoutExtension}.txt")
                    if (oldTxt.exists()) oldTxt.renameTo(File(parentDir, "$cleanName.txt"))
                } catch (ignored: Exception) {}

                try {
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(originalFile.absolutePath, targetFile.absolutePath),
                        null,
                        null
                    )
                } catch (ignored: Exception) {}

                val newTitle = cleanName
                updateSongDetails(
                    oldPath = song.path,
                    newPath = effectivePath,
                    newTitle = newTitle,
                    newArtist = song.artist,
                    newAlbum = song.album,
                    newGenre = song.genre,
                    newYear = song.year
                )

                return@withContext song.copy(
                    path = effectivePath,
                    uriString = effectiveUri,
                    title = newTitle
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- Son Çalınan Durum (Araç kontağı kapandığında hatırlama) ---
    fun saveLastFolder(path: String) {
        prefs.edit().putString("last_folder_path", path).apply()
    }

    fun getLastFolder(): String? {
        return prefs.getString("last_folder_path", null)
    }

    fun saveLastPlaybackState(songPath: String, positionMs: Long, isShuffle: Boolean, repeatMode: String) {
        prefs.edit()
            .putString(KEY_LAST_PATH, songPath)
            .putLong(KEY_LAST_POS, positionMs)
            .putBoolean(KEY_LAST_SHUFFLE, isShuffle)
            .putString(KEY_LAST_REPEAT, repeatMode)
            .apply()
    }

    fun getLastPlayedPath(): String? = prefs.getString(KEY_LAST_PATH, null)
    fun getLastPlayedPosition(): Long = prefs.getLong(KEY_LAST_POS, 0L)
    fun getLastShuffle(): Boolean = prefs.getBoolean(KEY_LAST_SHUFFLE, false)
    fun getLastRepeat(): String = prefs.getString(KEY_LAST_REPEAT, "OFF") ?: "OFF"

    // --- Ekolayzer (EQ) Ayarları Hafızası ---
    fun saveEqSettings(enabled: Boolean, preset: String, bassBoost: Short, bandLevels: Map<Short, Short>) {
        val bandsStr = bandLevels.entries.joinToString(";") { "${it.key}:${it.value}" }
        prefs.edit()
            .putBoolean("eq_enabled", enabled)
            .putString("eq_preset", preset)
            .putInt("eq_bass_boost", bassBoost.toInt())
            .putString("eq_bands", bandsStr)
            .apply()
    }

    fun getEqEnabled(): Boolean = prefs.getBoolean("eq_enabled", true)
    fun getEqPreset(): String = prefs.getString("eq_preset", "Normal") ?: "Normal"
    fun getEqBassBoost(): Short = prefs.getInt("eq_bass_boost", 0).toShort()
    fun getEqLoudnessEnhancerEnabled(): Boolean = prefs.getBoolean("eq_loudness_enhancer", false)
    fun setEqLoudnessEnhancerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("eq_loudness_enhancer", enabled).apply()
    }
    fun getEqBandLevels(): Map<Short, Short> {
        val str = prefs.getString("eq_bands", null) ?: return emptyMap()
        return try {
            str.split(";").filter { it.contains(":") }.associate {
                val parts = it.split(":")
                parts[0].toShort() to parts[1].toShort()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // --- Gelişmiş Ayarlar ---
    fun setAutoPlayOnBluetooth(enabled: Boolean) = prefs.edit().putBoolean("autoPlayOnBluetooth", enabled).apply()
    fun getAutoPlayOnBluetooth(): Boolean = prefs.getBoolean("autoPlayOnBluetooth", false)

    fun setKeepScreenOn(enabled: Boolean) = prefs.edit().putBoolean("keepScreenOn", enabled).apply()
    fun getKeepScreenOn(): Boolean = prefs.getBoolean("keepScreenOn", false)

    fun setCrossfadeDuration(durationMs: Int) = prefs.edit().putInt("crossfadeDuration", durationMs).apply()
    fun getCrossfadeDuration(): Int {
        val duration = prefs.getInt("crossfadeDuration", 0)
        if (duration == 1500) {
            setCrossfadeDuration(0)
            return 0
        }
        return duration
    }

    fun setAutoPlayOnStart(enabled: Boolean) = prefs.edit().putBoolean("autoPlayOnStart", enabled).apply()
    fun getAutoPlayOnStart(): Boolean = prefs.getBoolean("autoPlayOnStart", false)

    fun setGaplessPlayback(enabled: Boolean) = prefs.edit().putBoolean("gaplessPlayback", enabled).apply()
    fun getGaplessPlayback(): Boolean = prefs.getBoolean("gaplessPlayback", false)

    fun setDefaultStartTab(tab: String) = prefs.edit().putString("defaultStartTab", tab).apply()
    fun getDefaultStartTab(): String = prefs.getString("defaultStartTab", "HOME") ?: "HOME"

    fun setShowSmartPlaylists(show: Boolean) = prefs.edit().putBoolean("showSmartPlaylists", show).apply()
    fun getShowSmartPlaylists(): Boolean = prefs.getBoolean("showSmartPlaylists", true)

    fun setShowBlurredBackground(show: Boolean) = prefs.edit().putBoolean("showBlurredBackground", show).apply()
    fun getShowBlurredBackground(): Boolean = prefs.getBoolean("showBlurredBackground", true)

    fun setShowWaveform(show: Boolean) = prefs.edit().putBoolean("showWaveform", show).apply()
    fun getShowWaveform(): Boolean = prefs.getBoolean("showWaveform", true)

    fun setIsAlbumArtCropped(cropped: Boolean) = prefs.edit().putBoolean("isAlbumArtCropped", cropped).apply()
    fun getIsAlbumArtCropped(): Boolean = prefs.getBoolean("isAlbumArtCropped", false)


    // --- Başlangıçta Otomatik Başlat (Boot Completed) ---
    fun setStartOnBoot(enabled: Boolean) = prefs.edit().putBoolean("startOnBoot", enabled).apply()
    fun getStartOnBoot(): Boolean = prefs.getBoolean("startOnBoot", false)

    // --- Kütüphaneden Gizlenen / Çıkarılan Şarkılar ---
    private val KEY_HIDDEN_SONGS = "hidden_songs_set"
    fun getHiddenSongPaths(): Set<String> = prefs.getStringSet(KEY_HIDDEN_SONGS, emptySet()) ?: emptySet()
    fun isSongHidden(path: String): Boolean = getHiddenSongPaths().contains(path)
    fun hideSongFromLibrary(path: String) {
        val current = getHiddenSongPaths().toMutableSet()
        current.add(path)
        prefs.edit().putStringSet(KEY_HIDDEN_SONGS, current).apply()
    }
    fun unhideSong(path: String) {
        val current = getHiddenSongPaths().toMutableSet()
        current.remove(path)
        prefs.edit().putStringSet(KEY_HIDDEN_SONGS, current).apply()
    }
    fun clearHiddenSongs() {
        prefs.edit().remove(KEY_HIDDEN_SONGS).apply()
    }
    fun getHiddenSongsCount(): Int = getHiddenSongPaths().size

    suspend fun hideSongFromLibrary(song: Song): Boolean = withContext(Dispatchers.IO) {
        try {
            hideSongFromLibrary(song.path)
            dbHelper.deleteSong(song.path)

            val favs = getFavoritePaths().toMutableSet()
            if (favs.contains(song.path)) {
                favs.remove(song.path)
                prefs.edit().putStringSet(KEY_FAVORITES, favs).apply()
            }
            if (getLastPlayedPath() == song.path) {
                prefs.edit().remove(KEY_LAST_PATH).apply()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun setStopOnTaskRemoved(stop: Boolean) = prefs.edit().putBoolean("stopOnTaskRemoved", stop).apply()
    fun getStopOnTaskRemoved(): Boolean = prefs.getBoolean("stopOnTaskRemoved", true)
    
    // --- Tema ve Görünüm Ayarları ---
    fun setThemeMode(mode: String) = prefs.edit().putString("themeMode", mode).apply()
    fun getThemeMode(): String = prefs.getString("themeMode", "DARK") ?: "DARK"
    
    fun setThemePalette(palette: String) = prefs.edit().putString("themePalette", palette).apply()
    fun getThemePalette(): String = prefs.getString("themePalette", "NEON_CYAN") ?: "NEON_CYAN"

    fun setAutoOpenQueueOnStart(enabled: Boolean) = prefs.edit().putBoolean("autoOpenQueueOnStart", enabled).apply()
    fun getAutoOpenQueueOnStart(): Boolean = prefs.getBoolean("autoOpenQueueOnStart", false)

    fun setShowCoverLyrics(enabled: Boolean) = prefs.edit().putBoolean("showCoverLyrics", enabled).apply()
    fun getShowCoverLyrics(): Boolean = prefs.getBoolean("showCoverLyrics", false)

    fun setLyricsFontSize(size: String) = prefs.edit().putString("lyricsFontSize", size).apply()
    fun getLyricsFontSize(): String = prefs.getString("lyricsFontSize", "LARGE") ?: "LARGE"

    fun setAutoSaveId3Lyrics(enabled: Boolean) = prefs.edit().putBoolean("auto_save_id3_lyrics", enabled).apply()
    fun getAutoSaveId3Lyrics(): Boolean = prefs.getBoolean("auto_save_id3_lyrics", true)

    // --- Şarkı Geçiş Kaydırma (Swipe) Hassasiyeti (VERY_HIGH, HIGH, NORMAL, LOW) ---
    fun setSwipeSensitivity(sensitivity: String) = prefs.edit().putString("swipeSensitivity", sensitivity).apply()
    fun getSwipeSensitivity(): String = prefs.getString("swipeSensitivity", "HIGH") ?: "HIGH"

    // --- Cihaz ve Arayüz Modu (AUTO: Otomatik Algıla, CAR: Araç/Tablet, PHONE: Cep Telefonu) ---
    fun setUiMode(mode: String) = prefs.edit().putString("uiMode", mode).apply()
    fun getUiMode(): String = prefs.getString("uiMode", "AUTO") ?: "AUTO"

    fun setIsUiModeConfigured(configured: Boolean) = prefs.edit().putBoolean("isUiModeConfigured", configured).apply()
    fun getIsUiModeConfigured(): Boolean = prefs.getBoolean("isUiModeConfigured", false)

    // --- Kütüphane Taranacak Klasör Yönetimi ---
    private val KEY_INCLUDED_FOLDERS = "included_library_folders"

    /**
     * Kullanıcının kütüphaneye dahil ettiği klasörleri döner.
     * Eğer henüz hiçbir seçim yapılmadıysa varsayılan olarak cihazın Music klasörünü döner.
     */
    fun getIncludedFolders(): Set<String> {
        val saved = prefs.getStringSet(KEY_INCLUDED_FOLDERS, null)
        if (saved != null && saved.isNotEmpty()) {
            return saved
        }
        return emptySet()
    }

    fun setIncludedFolders(folders: Set<String>) {
        prefs.edit().putStringSet(KEY_INCLUDED_FOLDERS, folders).apply()
    }

    fun addIncludedFolder(path: String) {
        val current = getIncludedFolders().toMutableSet()
        current.add(path)
        setIncludedFolders(current)
    }

    fun removeIncludedFolder(path: String) {
        val current = getIncludedFolders().toMutableSet()
        current.remove(path)
        setIncludedFolders(current)
    }

    fun isPathAllowed(filePath: String, includedFolders: Set<String>): Boolean {
        if (filePath.isBlank()) return false
        // Sistem/zil sesi/indirilenler gibi istenmeyen kalıntıları engelle
        val lower = filePath.lowercase(Locale.ROOT)
        if (lower.contains("/ringtones/") ||
            lower.contains("/notifications/") ||
            lower.contains("/alarms/") ||
            lower.contains("/recordings/") ||
            lower.contains("/callrecordings/")
        ) {
            return false
        }

        // Eğer kullanıcı henüz özel bir klasör seçmediyse veya liste boşsa varsayılan olarak kabul et
        if (includedFolders.isEmpty()) {
            return true
        }

        fun norm(p: String): String {
            var s = p.replace('\\', '/').trimEnd('/')
            if (s.startsWith("/sdcard", ignoreCase = true)) {
                s = "/storage/emulated/0" + s.substring("/sdcard".length)
            }
            return s
        }

        val normFile = norm(filePath)
        // Eğer dosya izin verilen kök/dizinlerden herhangi birinin altındaysa kabul et
        for (folder in includedFolders) {
            val normFolder = norm(folder)
            if (normFolder.isBlank()) continue
            if (normFile.startsWith(normFolder, ignoreCase = true)) {
                if (normFile.length == normFolder.length || normFile.getOrNull(normFolder.length) == '/') {
                    return true
                }
            }
            // Sadece göreceli klasör adıysa (içinde '/' yoksa) ve sistem kökü değilse
            if (!normFolder.contains('/')) {
                if (normFile.contains("/$normFolder/", ignoreCase = true) ||
                    normFile.endsWith("/$normFolder", ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }
}




