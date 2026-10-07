package com.example.otomuzik.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.otomuzik.model.Playlist
import com.example.otomuzik.model.Song

class MusicDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "oto_muzik_library.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_SONGS = "songs"
        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_ARTIST = "artist"
        private const val COL_ALBUM = "album"
        private const val COL_DURATION = "duration_ms"
        private const val COL_PATH = "path"
        private const val COL_URI = "uri_string"
        private const val COL_ALBUM_ID = "album_id"
        private const val COL_FOLDER_PATH = "folder_path"
        private const val COL_YEAR = "year"
        private const val COL_GENRE = "genre"
        private const val COL_BITRATE = "bitrate"
        private const val COL_TRACK_NUMBER = "track_number"
        private const val COL_FORMAT = "format"
        private const val COL_FILE_SIZE = "file_size"
        private const val COL_PLAY_COUNT = "play_count"
        private const val COL_DATE_ADDED = "date_added"

        private const val TABLE_PLAYLISTS = "playlists"
        private const val COL_PL_ID = "id"
        private const val COL_PL_NAME = "name"
        private const val COL_PL_CREATED = "created_at"

        private const val TABLE_PLAYLIST_SONGS = "playlist_songs"
        private const val COL_PLS_PLAYLIST_ID = "playlist_id"
        private const val COL_PLS_SONG_PATH = "song_path"
        private const val COL_PLS_ADDED_AT = "added_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Şarkılar tablosu
        db.execSQL(
            """
            CREATE TABLE $TABLE_SONGS (
                $COL_ID INTEGER PRIMARY KEY,
                $COL_TITLE TEXT,
                $COL_ARTIST TEXT,
                $COL_ALBUM TEXT,
                $COL_DURATION INTEGER,
                $COL_PATH TEXT UNIQUE,
                $COL_URI TEXT,
                $COL_ALBUM_ID INTEGER,
                $COL_FOLDER_PATH TEXT,
                $COL_YEAR TEXT,
                $COL_GENRE TEXT,
                $COL_BITRATE TEXT,
                $COL_TRACK_NUMBER TEXT,
                $COL_FORMAT TEXT,
                $COL_FILE_SIZE TEXT,
                $COL_PLAY_COUNT INTEGER DEFAULT 0,
                $COL_DATE_ADDED INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_songs_folder ON $TABLE_SONGS ($COL_FOLDER_PATH)")
        db.execSQL("CREATE INDEX idx_songs_title ON $TABLE_SONGS ($COL_TITLE)")
        db.execSQL("CREATE INDEX idx_songs_path ON $TABLE_SONGS ($COL_PATH)")

        // Çalma Listeleri tablosu
        db.execSQL(
            """
            CREATE TABLE $TABLE_PLAYLISTS (
                $COL_PL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PL_NAME TEXT UNIQUE NOT NULL,
                $COL_PL_CREATED INTEGER
            )
            """.trimIndent()
        )

        // Çalma Listesi - Şarkı İlişki tablosu
        db.execSQL(
            """
            CREATE TABLE $TABLE_PLAYLIST_SONGS (
                $COL_PLS_PLAYLIST_ID INTEGER,
                $COL_PLS_SONG_PATH TEXT,
                $COL_PLS_ADDED_AT INTEGER,
                PRIMARY KEY ($COL_PLS_PLAYLIST_ID, $COL_PLS_SONG_PATH),
                FOREIGN KEY ($COL_PLS_PLAYLIST_ID) REFERENCES $TABLE_PLAYLISTS($COL_PL_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_pl_songs_id ON $TABLE_PLAYLIST_SONGS ($COL_PLS_PLAYLIST_ID)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_SONGS ADD COLUMN $COL_PLAY_COUNT INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_SONGS ADD COLUMN $COL_DATE_ADDED INTEGER DEFAULT 0")
        } else {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLIST_SONGS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLISTS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_SONGS")
            onCreate(db)
        }
    }

    // --- Kütüphane / Şarkı İşlemleri ---

    /**
     * Kütüphanedeki toplam şarkı sayısını döner.
     */
    fun getSongCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_SONGS", null)
        var count = 0
        cursor.use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
            }
        }
        return count
    }

    /**
     * Kütüphanedeki tüm şarkıları başlığa göre sıralı döner.
     */
    fun getAllSongs(): List<Song> {
        val list = mutableListOf<Song>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SONGS ORDER BY $COL_TITLE COLLATE NOCASE ASC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToSong(it))
            }
        }
        return list
    }

    /**
     * Belirli bir klasördeki şarkı sayısını döner (Alt klasörleri de kapsar).
     */
    fun getSongCountInFolder(folderPath: String): Int {
        val db = readableDatabase
        val normalized = folderPath.trimEnd('/')
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_SONGS WHERE $COL_FOLDER_PATH = ? OR $COL_FOLDER_PATH LIKE ?",
            arrayOf(normalized, "$normalized/%")
        )
        var count = 0
        cursor.use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
            }
        }
        return count
    }

    /**
     * Belirli bir klasördeki şarkıları döner (Alt klasörleri de kapsar).
     */
    fun getSongsInFolder(folderPath: String): List<Song> {
        val list = mutableListOf<Song>()
        val db = readableDatabase
        val normalized = folderPath.trimEnd('/')
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_SONGS WHERE $COL_FOLDER_PATH = ? OR $COL_FOLDER_PATH LIKE ? ORDER BY $COL_TITLE COLLATE NOCASE ASC",
            arrayOf(normalized, "$normalized/%")
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToSong(it))
            }
        }
        return list
    }

    /**
     * Şarkıları SQLite transaction kullanarak toplu ve ultra hızlı şekilde kaydeder / günceller.
     */
    fun saveSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (song in songs) {
                // Mevcut kayıtları korumak için play_count ve date_added oku
                var playCount = 0
                var dateAdded = System.currentTimeMillis()
                val cursor = db.rawQuery("SELECT $COL_PLAY_COUNT, $COL_DATE_ADDED FROM $TABLE_SONGS WHERE $COL_PATH = ?", arrayOf(song.path))
                if (cursor.moveToFirst()) {
                    playCount = cursor.getInt(0)
                    dateAdded = cursor.getLong(1)
                } else {
                    // DATE_ADDED MediaStore'dan alınmışsa (year gibi alanlardan değilse), ya da sistem saati
                    val dateAddedStr = song.year // Eğer MediaStore DATE_ADDED'i year veya genre üzerinden eklemişsek, vb. ama şu an elimizde yok, şimdiki zaman kalsın.
                }
                cursor.close()

                val values = ContentValues().apply {
                    put(COL_ID, song.id)
                    put(COL_TITLE, song.title)
                    put(COL_ARTIST, song.artist)
                    put(COL_ALBUM, song.album)
                    put(COL_DURATION, song.durationMs)
                    put(COL_PATH, song.path)
                    put(COL_URI, song.uriString)
                    put(COL_ALBUM_ID, song.albumId)
                    put(COL_FOLDER_PATH, song.folderPath)
                    put(COL_YEAR, song.year)
                    put(COL_GENRE, song.genre)
                    put(COL_BITRATE, song.bitrate)
                    put(COL_TRACK_NUMBER, song.trackNumber)
                    put(COL_FORMAT, song.format)
                    put(COL_FILE_SIZE, song.fileSizeFormatted)
                    put(COL_PLAY_COUNT, playCount)
                    put(COL_DATE_ADDED, dateAdded)
                }
                db.insertWithOnConflict(TABLE_SONGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Artık diskte olmayan şarkıları kütüphaneden temizler.
     */
    fun syncWithExistingPaths(existingPaths: Set<String>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.rawQuery("SELECT $COL_PATH FROM $TABLE_SONGS", null)
            val toDelete = mutableListOf<String>()
            cursor.use {
                val pathIdx = it.getColumnIndexOrThrow(COL_PATH)
                while (it.moveToNext()) {
                    val p = it.getString(pathIdx)
                    if (!existingPaths.contains(p)) {
                        toDelete.add(p)
                    }
                }
            }
            for (p in toDelete) {
                db.delete(TABLE_SONGS, "$COL_PATH = ?", arrayOf(p))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Şarkı dosya adı veya meta verisi değiştiğinde kütüphaneyi ve çalma listelerindeki referansları günceller.
     */
    fun updateSongDetails(
        oldPath: String,
        newPath: String,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newGenre: String,
        newYear: String
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COL_PATH, newPath)
                put(COL_URI, Uri.fromFile(java.io.File(newPath)).toString())
                put(COL_TITLE, newTitle)
                put(COL_ARTIST, newArtist)
                put(COL_ALBUM, newAlbum)
                put(COL_GENRE, newGenre)
                put(COL_YEAR, newYear)
            }
            db.update(TABLE_SONGS, values, "$COL_PATH = ?", arrayOf(oldPath))

            if (oldPath != newPath) {
                val plValues = ContentValues().apply {
                    put(COL_PLS_SONG_PATH, newPath)
                }
                db.update(TABLE_PLAYLIST_SONGS, plValues, "$COL_PLS_SONG_PATH = ?", arrayOf(oldPath))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // --- Çalma Listesi (Playlist) İşlemleri ---

    /**
     * Tüm çalma listelerini şarkı sayılarıyla birlikte döner.
     */
    fun getAllPlaylists(): List<Playlist> {
        val list = mutableListOf<Playlist>()
        val db = readableDatabase
        val sql = """
            SELECT p.$COL_PL_ID, p.$COL_PL_NAME, p.$COL_PL_CREATED, COUNT(ps.$COL_PLS_SONG_PATH) as song_count
            FROM $TABLE_PLAYLISTS p
            LEFT JOIN $TABLE_PLAYLIST_SONGS ps ON p.$COL_PL_ID = ps.$COL_PLS_PLAYLIST_ID
            GROUP BY p.$COL_PL_ID
            ORDER BY p.$COL_PL_CREATED DESC
        """.trimIndent()

        val cursor = db.rawQuery(sql, null)
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_PL_ID)
            val nameIdx = it.getColumnIndexOrThrow(COL_PL_NAME)
            val createdIdx = it.getColumnIndexOrThrow(COL_PL_CREATED)
            val countIdx = it.getColumnIndexOrThrow("song_count")
            while (it.moveToNext()) {
                list.add(
                    Playlist(
                        id = it.getLong(idIdx),
                        name = it.getString(nameIdx),
                        createdAt = it.getLong(createdIdx),
                        songCount = it.getInt(countIdx)
                    )
                )
            }
        }
        return list
    }

    /**
     * Yeni bir çalma listesi oluşturur.
     */
    fun createPlaylist(name: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PL_NAME, name.trim())
            put(COL_PL_CREATED, System.currentTimeMillis())
        }
        return db.insertWithOnConflict(TABLE_PLAYLISTS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    /**
     * Çalma listesini siler.
     */
    fun deletePlaylist(playlistId: Long): Boolean {
        val db = writableDatabase
        db.delete(TABLE_PLAYLIST_SONGS, "$COL_PLS_PLAYLIST_ID = ?", arrayOf(playlistId.toString()))
        val count = db.delete(TABLE_PLAYLISTS, "$COL_PL_ID = ?", arrayOf(playlistId.toString()))
        return count > 0
    }

    /**
     * Çalma listesine şarkıları ekler.
     */
    fun addSongsToPlaylist(playlistId: Long, songs: List<Song>): Int {
        if (songs.isEmpty()) return 0
        // Önce şarkıların songs tablosunda olduğundan emin ol
        saveSongs(songs)

        val db = writableDatabase
        var addedCount = 0
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            for (song in songs) {
                val values = ContentValues().apply {
                    put(COL_PLS_PLAYLIST_ID, playlistId)
                    put(COL_PLS_SONG_PATH, song.path)
                    put(COL_PLS_ADDED_AT, now)
                }
                val rowId = db.insertWithOnConflict(TABLE_PLAYLIST_SONGS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
                if (rowId != -1L) addedCount++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return addedCount
    }

    /**
     * Çalma listesinden tek bir şarkıyı kaldırır.
     */
    fun removeSongFromPlaylist(playlistId: Long, songPath: String): Boolean {
        val db = writableDatabase
        val count = db.delete(
            TABLE_PLAYLIST_SONGS,
            "$COL_PLS_PLAYLIST_ID = ? AND $COL_PLS_SONG_PATH = ?",
            arrayOf(playlistId.toString(), songPath)
        )
        return count > 0
    }

    /**
     * Belirli bir çalma listesindeki tüm şarkıları döner.
     */
    fun getSongsForPlaylist(playlistId: Long): List<Song> {
        val list = mutableListOf<Song>()
        val db = readableDatabase
        val sql = """
            SELECT s.* FROM $TABLE_SONGS s
            INNER JOIN $TABLE_PLAYLIST_SONGS ps ON s.$COL_PATH = ps.$COL_PLS_SONG_PATH
            WHERE ps.$COL_PLS_PLAYLIST_ID = ?
            ORDER BY ps.$COL_PLS_ADDED_AT ASC
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(playlistId.toString()))
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToSong(it))
            }
        }
        return list
    }

    private fun cursorToSong(cursor: Cursor): Song {
        val rawTitle = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)) ?: "Bilinmeyen Parça"
        val rawArtist = cursor.getString(cursor.getColumnIndexOrThrow(COL_ARTIST)) ?: "Bilinmeyen Sanatçı"
        val rawAlbum = cursor.getString(cursor.getColumnIndexOrThrow(COL_ALBUM)) ?: "Bilinmeyen Albüm"

        return Song(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            title = com.example.otomuzik.util.TurkishStringFixer.fix(rawTitle),
            artist = com.example.otomuzik.util.TurkishStringFixer.fix(rawArtist),
            album = com.example.otomuzik.util.TurkishStringFixer.fix(rawAlbum),
            durationMs = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DURATION)),
            path = cursor.getString(cursor.getColumnIndexOrThrow(COL_PATH)) ?: "",
            uriString = cursor.getString(cursor.getColumnIndexOrThrow(COL_URI)) ?: "",
            albumId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ALBUM_ID)),
            folderPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_FOLDER_PATH)) ?: "",
            year = cursor.getString(cursor.getColumnIndexOrThrow(COL_YEAR)) ?: "",
            genre = cursor.getString(cursor.getColumnIndexOrThrow(COL_GENRE)) ?: "",
            bitrate = cursor.getString(cursor.getColumnIndexOrThrow(COL_BITRATE)) ?: "",
            trackNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRACK_NUMBER)) ?: "",
            format = cursor.getString(cursor.getColumnIndexOrThrow(COL_FORMAT)) ?: "",
            fileSizeFormatted = cursor.getString(cursor.getColumnIndexOrThrow(COL_FILE_SIZE)) ?: ""
        )
    }

    fun incrementPlayCount(songPath: String) {
        val db = writableDatabase
        db.execSQL("UPDATE $TABLE_SONGS SET $COL_PLAY_COUNT = $COL_PLAY_COUNT + 1 WHERE $COL_PATH = ?", arrayOf(songPath))
    }

    /**
     * Tüm dinlenme geçmişini temizler.
     */
    fun clearPlayHistory() {
        val db = writableDatabase
        db.execSQL("UPDATE $TABLE_SONGS SET $COL_PLAY_COUNT = 0")
    }

    /**
     * En çok dinlenen şarkıları döner.
     */
    fun getMostPlayedSongs(limit: Int): List<Song> {
        val list = mutableListOf<Song>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SONGS WHERE $COL_PLAY_COUNT > 0 ORDER BY $COL_PLAY_COUNT DESC LIMIT ?", arrayOf(limit.toString()))
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToSong(it))
            }
        }
        return list
    }

    /**
     * Son eklenen şarkıları döner.
     */
    fun getRecentlyAddedSongs(limit: Int): List<Song> {
        val list = mutableListOf<Song>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SONGS ORDER BY $COL_DATE_ADDED DESC LIMIT ?", arrayOf(limit.toString()))
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToSong(it))
            }
        }
        return list
    }
}
