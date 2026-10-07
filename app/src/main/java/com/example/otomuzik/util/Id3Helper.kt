package com.example.otomuzik.util

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import com.example.otomuzik.model.Song
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.Locale

data class Id3Metadata(
    val title: String,
    val artist: String,
    val album: String,
    val year: String,
    val genre: String,
    val bitrate: String,
    val trackNumber: String,
    val durationMs: Long,
    val format: String,
    val fileSizeFormatted: String
)

/**
 * ID3 etiket okuyucu.
 *
 * ## T�rk�e Karakter Sorunu ve ��z�m
 *
 * ID3v2.3 standard�nda metin �er�eveleri 3 farkl� encoding ile kaydedilebilir:
 *   - 0x00 � ISO-8859-1 (Latin-1)  � T�rk�e Windows'ta �o�unlukla ISO-8859-9 olarak kaydedilir
 *   - 0x01 � UTF-16 (BOM ile)
 *   - 0x03 � UTF-8  (ID3v2.4)
 *
 * Android'un MediaMetadataRetriever'� encoding byte'�n� g�rmezden gelip her �eyi
 * UTF-8 gibi okuyunca T�rk�e karakterler (��ø, ��z vb.) bozulur.
 *
 * ��z�m:
 *   1. �nce MediaMetadataRetriever ile okuruz.
 *   2. Sonu� bozuk g�r�n�yorsa (T�rk�e karakterlerin hatal� UTF-8 temsilleri)
 *      dosyay� ham olarak okuyup ID3v2 frame'lerini elle parse ederiz.
 *   3. Bozuk Latin-1 metnini Windows-1252 veya ISO-8859-9 (T�rk�e) ile yeniden
 *      decode ederek d�zeltiriz.
 */
object Id3Helper {

    private val cache = object : LruCache<String, Id3Metadata>(200) {}

    fun parseFile(filePath: String): Id3Metadata {
        return parseSong(context = null, filePath = filePath, uriString = "")
    }

    fun parseSong(context: Context? = null, filePath: String, uriString: String = ""): Id3Metadata {
        val cacheKey = if (uriString.isNotEmpty()) uriString else filePath
        cache.get(cacheKey)?.let { return it }

        val file = if (filePath.isNotEmpty()) File(filePath) else null
        val ext = file?.extension?.lowercase(Locale.ROOT) ?: "mp3"
        val sizeMb = if (file != null && file.exists()) {
            "%.1f MB".format(file.length() / (1024.0 * 1024.0))
        } else ""

        var title = TurkishStringFixer.fix(file?.nameWithoutExtension ?: "Bilinmeyen Par�a")
        var artist = "Bilinmeyen Sanat��"
        var album = TurkishStringFixer.fix(file?.parentFile?.name ?: "Bilinmeyen Alb�m")
        var year = ""
        var genre = ""
        var bitrate = ""
        var trackNumber = ""
        var durationMs = 0L

        var retriever: MediaMetadataRetriever? = null
        var isRetrieverSet = false

        try {
            retriever = MediaMetadataRetriever()

            // 1. ContentResolver �zerinden dene (Scoped storage i�in en g�venli)
            if (context != null && uriString.isNotEmpty()) {
                try {
                    context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")?.use { pfd ->
                        retriever.setDataSource(pfd.fileDescriptor)
                        isRetrieverSet = true
                    }
                } catch (e: Exception) {
                    // Devam et
                }
            }

            // 2. Do�rudan dosya yolu �zerinden dene (USB bellekler vb.)
            if (!isRetrieverSet && filePath.isNotEmpty()) {
                val f = File(filePath)
                if (f.exists() && f.canRead()) {
                    try {
                        retriever.setDataSource(filePath)
                        isRetrieverSet = true
                    } catch (e: Exception) {
                        // Devam et
                    }
                }
            }

            if (isRetrieverSet) {
                val id3Title  = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val id3Artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val id3Album  = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val id3Year   = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                val id3Genre  = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                val id3Track  = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                val id3Bitrate  = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                val id3Duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                if (!id3Title.isNullOrBlank() && id3Title != "<unknown>") {
                    title = fixEncoding(id3Title).trim()
                }
                if (!id3Artist.isNullOrBlank() && id3Artist != "<unknown>") {
                    artist = fixEncoding(id3Artist).trim()
                }
                if (!id3Album.isNullOrBlank() && id3Album != "<unknown>") {
                    album = fixEncoding(id3Album).trim()
                }
                if (!id3Year.isNullOrBlank()) {
                    year = id3Year.take(4).trim()
                }
                if (!id3Genre.isNullOrBlank()) {
                    genre = fixEncoding(cleanGenre(id3Genre))
                }
                if (!id3Track.isNullOrBlank()) {
                    trackNumber = id3Track.substringBefore('/').trim()
                }
                if (!id3Bitrate.isNullOrBlank()) {
                    val bps = id3Bitrate.toLongOrNull() ?: 0L
                    bitrate = if (bps > 0) "${bps / 1000} kbps" else ""
                }
                if (!id3Duration.isNullOrBlank()) {
                    durationMs = id3Duration.toLongOrNull() ?: 0L
                }
            }

            //    ham ID3v2 parse ile yeniden dene
            if (file != null && file.exists() && file.canRead() && ext == "mp3") {
                val rawTags = parseRawId3v2(file)
                if (rawTags != null) {
                    rawTags["TIT2"]?.let { if (it.isNotBlank() && it != "<unknown>") title = TurkishStringFixer.fix(it) }
                    rawTags["TPE1"]?.let { if (it.isNotBlank() && it != "<unknown>") artist = TurkishStringFixer.fix(it) }
                    rawTags["TALB"]?.let { if (it.isNotBlank() && it != "<unknown>") album = TurkishStringFixer.fix(it) }
                    rawTags["TYER"]?.let { if (it.isNotBlank()) year = it.take(4) }
                    rawTags["TDRC"]?.let { if (it.isNotBlank() && year.isEmpty()) year = it.take(4) }
                    rawTags["TCON"]?.let { if (it.isNotBlank()) genre = TurkishStringFixer.fix(cleanGenre(it)) }
                    rawTags["TRCK"]?.let { if (it.isNotBlank()) trackNumber = it.substringBefore('/').trim() }
                }
            }

        } catch (e: Exception) {
            // Hata durumunda varsay�lanlar kullan�l�r
        } finally {
            try { retriever?.release() } catch (e: Exception) {}
        }

        val metadata = Id3Metadata(
            title = title,
            artist = artist,
            album = album,
            year = year,
            genre = genre,
            bitrate = bitrate,
            trackNumber = trackNumber,
            durationMs = durationMs,
            format = ext.uppercase(Locale.ROOT),
            fileSizeFormatted = sizeMb
        )

        cache.put(cacheKey, metadata)
        return metadata
    }

    // -------------------------------------------------------------------------
    // Encoding Tespiti ve D�zeltme
    // -------------------------------------------------------------------------

    /**
     * MediaMetadataRetriever'�n yanl�� decode etti�i T�rk�e karakterleri d�zeltir.
     *
     * Algoritma:
     *   1. Metin bozuk mu? � hasBrokenTurkishChars() ile kontrol
     *   2. Bozuksa: String'i ISO-8859-1 baytlar�na �evir (orijinal baytlar� geri al)
     *   3. Bu baytlar� Windows-1252, ard�ndan ISO-8859-9 (T�rk�e) ile decode et
     *   4. En az bozuk karakter i�ereni se�
     */
    private fun fixEncoding(text: String): String {
        // Zaten d�zg�n g�r�n�yorsa dokunma
        if (!looksLikeBrokenUtf8(text)) return text

        return try {
            // UTF-8 olarak yanl�� okunan metni ISO-8859-1 baytlar�na �evir
            val rawBytes = text.toByteArray(Charsets.ISO_8859_1)

            val candidates = listOf(
                // Windows-1252: T�rkiye'de �ok kullan�lan Windows sistemi
                decodeWith(rawBytes, Charsets.ISO_8859_1),
                // ISO-8859-9: Resmi T�rk�e Latin charset
                decodeWith(rawBytes, Charset.forName("ISO-8859-9")),
                // Windows-1254: T�rk�e Windows kod sayfas�
                decodeWith(rawBytes, Charset.forName("windows-1254"))
            )

            // En az bozuk karakter (U+FFFD) i�ereni se�,
            // yoksa en fazla tan�nan T�rk�e karakter i�ereni se�
            val best = candidates
                .filter { it != null }
                .maxByOrNull { scoreTurkish(it!!) }

            best ?: text
        } catch (e: Exception) {
            text
        }
    }

    private fun decodeWith(bytes: ByteArray, charset: Charset): String? {
        return try {
            String(bytes, charset)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * T�rk�e metni puanlar: tan�nan T�rk�e harfler +2, bozuk karakterler -3
     */
    private fun scoreTurkish(text: String): Int {
        var score = 0
        val turkishChars = setOf('�', '�', '�', '�', '�', '�', '�', '�', '�', '�', '�', '�')
        for (ch in text) {
            when {
                ch in turkishChars    -> score += 2
                ch == '\uFFFD'        -> score -= 3   // Replacement char
                ch.code in 0x80..0x9F -> score -= 2  // C1 control chars (bozuk Latin-1)
                ch.isLetterOrDigit() || ch.isWhitespace() || ch in ",-.'!?()[]" -> score += 1
            }
        }
        return score
    }

    /**
     * Metnin bozuk UTF-8 decode sonucu gibi g�r�n�p g�r�nmedi�ini kontrol eder.
     * T�rk�e karakterlerin yayg�n bozuk UTF-8 temsilleri: ü=�, ö=�, ş=� vb.
     */
    private fun looksLikeBrokenUtf8(text: String): Boolean {
        // C1 control block karakterleri: ISO-8859-x'in UTF-8 olarak yanl�� okunmas�
        val c1Range = 0x80..0x9F
        var suspiciousCount = 0
        for (ch in text) {
            if (ch.code in c1Range) suspiciousCount++
            // T�rk�e karakterlerin bozuk temsilleri (�, �, �, � vb.)
            if (ch == '�' || ch == '�') suspiciousCount++
        }
        return suspiciousCount > 0
    }

    // -------------------------------------------------------------------------
    // Ham ID3v2 Parser (Encoding-Aware)
    // -------------------------------------------------------------------------

    /**
     * MP3 dosyas�n�n ba��ndaki ID3v2 etiketini ham bayt seviyesinde okur.
     * Her metin frame'inin encoding byte'�na bakarak do�ru Charset ile decode eder.
     *
     * Desteklenen frame'ler: TIT2, TPE1, TALB, TYER, TDRC, TCON, TRCK
     *
     * @return Frame ad� � decode edilmi� metin e�lemesi, hata durumunda null
     */
    private fun parseRawId3v2(file: File): Map<String, String>? {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(10)
                raf.read(header)

                // ID3 magic kontrol�
                if (header[0] != 'I'.code.toByte() ||
                    header[1] != 'D'.code.toByte() ||
                    header[2] != '3'.code.toByte()
                ) return null

                val majorVersion = header[3].toInt() and 0xFF
                if (majorVersion !in 2..4) return null  // v2.2, v2.3, v2.4 destekli

                val flagByte = header[5].toInt() and 0xFF
                val hasExtHeader = (flagByte and 0x40) != 0

                // Syncsafe int ile toplam ID3 boyutunu hesapla
                val size = ((header[6].toLong() and 0x7F) shl 21) or
                           ((header[7].toLong() and 0x7F) shl 14) or
                           ((header[8].toLong() and 0x7F) shl 7) or
                            (header[9].toLong() and 0x7F)

                // ID3v2.2 frame boyutu 3 byte, v2.3+ 4 byte
                val frameHeaderSize = if (majorVersion == 2) 6 else 10
                val frameIdSize = if (majorVersion == 2) 3 else 4

                // Geni�letilmi� ba�l�k varsa atla
                if (hasExtHeader) {
                    val extSizeBytes = ByteArray(4)
                    raf.read(extSizeBytes)
                    val extSize = ByteBuffer.wrap(extSizeBytes).int
                    raf.skipBytes(extSize - 4)
                }

                val result = mutableMapOf<String, String>()
                val targetFrames = setOf("TIT2", "TPE1", "TALB", "TYER", "TDRC", "TCON", "TRCK",
                                         "TT2", "TP1", "TAL", "TYE", "TCO", "TRK") // ID3v2.2 isimleri

                var bytesRead = 0L
                val frameHeaderBuf = ByteArray(frameHeaderSize)

                while (bytesRead < size - frameHeaderSize) {
                    val read = raf.read(frameHeaderBuf)
                    if (read < frameHeaderSize) break
                    bytesRead += read

                    val frameId = String(frameHeaderBuf, 0, frameIdSize, Charsets.ISO_8859_1)

                    // Bo� frame ID � padding ba�lad�, dur
                    if (frameId.all { it == '\u0000' }) break

                    val frameSize = if (majorVersion == 2) {
                        // 3-byte b�y�k endian
                        ((frameHeaderBuf[3].toLong() and 0xFF) shl 16) or
                        ((frameHeaderBuf[4].toLong() and 0xFF) shl 8) or
                         (frameHeaderBuf[5].toLong() and 0xFF)
                    } else {
                        // 4-byte b�y�k endian (v2.4'te syncsafe, v2.3'te normal)
                        if (majorVersion == 4) {
                            ((frameHeaderBuf[4].toLong() and 0x7F) shl 21) or
                            ((frameHeaderBuf[5].toLong() and 0x7F) shl 14) or
                            ((frameHeaderBuf[6].toLong() and 0x7F) shl 7) or
                             (frameHeaderBuf[7].toLong() and 0x7F)
                        } else {
                            ((frameHeaderBuf[4].toLong() and 0xFF) shl 24) or
                            ((frameHeaderBuf[5].toLong() and 0xFF) shl 16) or
                            ((frameHeaderBuf[6].toLong() and 0xFF) shl 8) or
                             (frameHeaderBuf[7].toLong() and 0xFF)
                        }
                    }

                    if (frameSize <= 0 || frameSize > 512 * 1024) break // Makul s�n�r

                    val frameData = ByteArray(frameSize.toInt())
                    val dataRead = raf.read(frameData)
                    if (dataRead < 1) break
                    bytesRead += dataRead

                    // Frame ID normalize (ID3v2.2 � v2.3 e�lemesi)
                    val normalizedId = when (frameId) {
                        "TT2" -> "TIT2"; "TP1" -> "TPE1"; "TAL" -> "TALB"
                        "TYE" -> "TYER"; "TCO" -> "TCON"; "TRK" -> "TRCK"
                        else  -> frameId
                    }

                    if (normalizedId in targetFrames) {
                        val decoded = decodeId3TextFrame(frameData)
                        if (decoded.isNotBlank() && decoded != "<unknown>") {
                            result[normalizedId] = decoded
                        }
                    }
                }

                result.ifEmpty { null }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * ID3v2 metin frame'ini encoding byte'a g�re decode eder.
     *
     * Byte 0 = encoding:
     *   0x00 � ISO-8859-1 (Latin-1) � T�rk�e dosyalarda genellikle ISO-8859-9/Windows-1254
     *   0x01 � UTF-16 (BOM ile)
     *   0x02 � UTF-16BE (BOM'suz)
     *   0x03 � UTF-8
     */
    private fun decodeId3TextFrame(data: ByteArray): String {
        if (data.isEmpty()) return ""

        val encodingByte = data[0].toInt() and 0xFF
        val content = data.copyOfRange(1, data.size)

        return when (encodingByte) {
            0x01 -> { // UTF-16 with BOM
                if (content.size >= 2) String(content, Charsets.UTF_16)
                else ""
            }
            0x02 -> { // UTF-16BE without BOM
                if (content.size >= 2) String(content, Charsets.UTF_16BE)
                else ""
            }
            0x03 -> { // UTF-8
                String(content, Charsets.UTF_8)
            }
            else -> { // 0x00 = ISO-8859-1 � T�rk�e i�in Windows-1254 dene
                decodeLatin1WithTurkishFallback(content)
            }
        }.trimEnd('\u0000').trim() // Null terminator temizle
    }

    /**
     * Latin-1 olarak i�aretlenmi� veriyi T�rk�e karakterler i�in ak�ll�ca decode eder.
     * �nce Windows-1254 (T�rk�e Windows), sonra ISO-8859-9 dener;
     * T�rk�e karakter skoru en y�ksek olan� d�ner.
     */
    private fun decodeLatin1WithTurkishFallback(bytes: ByteArray): String {
        val candidates = mutableListOf<String>()

        // Standart ISO-8859-1
        try { candidates.add(String(bytes, Charsets.ISO_8859_1)) } catch (e: Exception) {}

        // Windows-1254 (T�rk�e Windows kod sayfas�)
        try { candidates.add(String(bytes, Charset.forName("windows-1254"))) } catch (e: Exception) {}

        // ISO-8859-9 (ISO T�rk�e)
        try { candidates.add(String(bytes, Charset.forName("ISO-8859-9"))) } catch (e: Exception) {}

        return candidates.maxByOrNull { scoreTurkish(it) } ?: ""
    }

    // -------------------------------------------------------------------------
    // Yard�mc� Fonksiyonlar
    // -------------------------------------------------------------------------

    /**
     * �nbelle�i temizler � k�t�phane yeniden tarand���nda �a�r�lmal�.
     */
    fun clearCache() {
        cache.evictAll()
    }

    private fun getCoverCacheFile(context: Context, path: String): File {
        val hash = try {
            val md = java.security.MessageDigest.getInstance("MD5")
            md.digest(path.toByteArray()).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            path.hashCode().toString()
        }
        val dir = File(context.filesDir, "custom_album_art")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$hash.jpg")
    }

    fun saveCoverToAppCache(context: Context, path: String, artworkBytes: ByteArray?) {
        try {
            val file = getCoverCacheFile(context, path)
            if (artworkBytes != null && artworkBytes.isNotEmpty()) {
                file.writeBytes(artworkBytes)
            } else {
                if (file.exists()) file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCoverFromAppCache(context: Context, path: String): ByteArray? {
        return try {
            val file = getCoverCacheFile(context, path)
            if (file.exists() && file.length() > 0) file.readBytes() else null
        } catch (e: Exception) {
            null
        }
    }

    fun getArtworkBytes(song: Song, context: Context? = null): ByteArray? {
        // 1. Uygulama içi özel albüm kapağı önbelleği (Kullanıcının yüklediği / kaydettiği)
        if (context != null && song.path.isNotEmpty()) {
            val custom = getCoverFromAppCache(context, song.path)
            if (custom != null && custom.isNotEmpty()) return custom
        }

        // 2. MP3 dosyasından jaudiotagger ile gömülü APIC resmi
        if (song.path.isNotEmpty()) {
            try {
                val f = File(song.path)
                if (f.exists() && f.canRead()) {
                    val audioFile = org.jaudiotagger.audio.AudioFileIO.read(f)
                    val artwork = audioFile.tag?.firstArtwork
                    if (artwork?.binaryData != null && artwork.binaryData.isNotEmpty()) {
                        return artwork.binaryData
                    }
                }
            } catch (e: Exception) {
                // Devam
            }
        }

        // 3. ContentResolver / MediaMetadataRetriever ile gömülü kapak resmi
        try {
            var retriever: MediaMetadataRetriever? = null
            if (context != null && song.uriString.isNotEmpty()) {
                try {
                    retriever = MediaMetadataRetriever()
                    context.contentResolver.openFileDescriptor(Uri.parse(song.uriString), "r")?.use { pfd ->
                        retriever.setDataSource(pfd.fileDescriptor)
                        val bytes = retriever.embeddedPicture
                        if (bytes != null && bytes.isNotEmpty()) return bytes
                    }
                } catch (e: Exception) {}
                finally { try { retriever?.release() } catch (e: Exception) {} }
            }

            if (song.path.isNotEmpty()) {
                val f = File(song.path)
                if (f.exists() && f.canRead()) {
                    retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(song.path)
                        val bytes = retriever.embeddedPicture
                        if (bytes != null && bytes.isNotEmpty()) return bytes
                    } catch (e: Exception) {}
                    finally { try { retriever?.release() } catch (e: Exception) {} }
                }
            }
        } catch (e: Exception) {}

        // 4. Şarkı klasöründeki kapak resimleri (cover.jpg, folder.jpg vb.)
        if (song.path.isNotEmpty()) {
            try {
                val parent = File(song.path).parentFile
                if (parent != null && parent.exists() && parent.canRead()) {
                    val names = listOf("cover.jpg", "folder.jpg", "album.jpg", "front.jpg", "cover.png", "folder.png")
                    for (name in names) {
                        val img = File(parent, name)
                        if (img.exists() && img.canRead() && img.length() > 0) {
                            return img.readBytes()
                        }
                    }
                }
            } catch (e: Exception) {}
        }

        // 5. MediaStore album art URI
        if (context != null && song.albumId > 0) {
            try {
                val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                val uri = ContentUris.withAppendedId(sArtworkUri, song.albumId)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.isNotEmpty()) return bytes
                }
            } catch (e: Exception) {}
        }

        return null
    }

    fun getArtworkBytes(song: Song): ByteArray? {
        return getArtworkBytes(song, null)
    }

    fun writeTag(
        context: Context,
        path: String,
        title: String,
        artist: String,
        album: String,
        genre: String,
        year: String,
        artworkBytes: ByteArray? = null
    ): Boolean {
        var tagSaved = false
        try {
            val file = File(path)
            if (file.exists()) {
                val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                val tag = audioFile.tagOrCreateAndSetDefault

                tag.setField(org.jaudiotagger.tag.FieldKey.TITLE, title)
                tag.setField(org.jaudiotagger.tag.FieldKey.ARTIST, artist)
                tag.setField(org.jaudiotagger.tag.FieldKey.ALBUM, album)
                tag.setField(org.jaudiotagger.tag.FieldKey.GENRE, genre)
                tag.setField(org.jaudiotagger.tag.FieldKey.YEAR, year)

                if (artworkBytes != null && artworkBytes.isNotEmpty()) {
                    try {
                        val artwork = org.jaudiotagger.tag.images.ArtworkFactory.getNew()
                        artwork.binaryData = artworkBytes
                        artwork.mimeType = "image/jpeg"
                        tag.deleteArtworkField()
                        tag.setField(artwork)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else if (artworkBytes == null) {
                    try {
                        tag.deleteArtworkField()
                    } catch (e: Exception) {}
                }

                audioFile.commit()
                tagSaved = true

                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(path),
                    null,
                    null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Uygulama içi önbelleğe ve klasöre kaydet (Dosya salt okunur olsa bile uygulama resmi hatırlar)
        try {
            saveCoverToAppCache(context, path, artworkBytes)
            if (path.isNotEmpty()) {
                val parent = File(path).parentFile
                if (parent != null && parent.exists() && parent.canWrite()) {
                    val coverFile = File(parent, "cover.jpg")
                    if (artworkBytes != null && artworkBytes.isNotEmpty()) {
                        coverFile.writeBytes(artworkBytes)
                    } else if (artworkBytes == null && coverFile.exists()) {
                        coverFile.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        clearCache()
        AlbumArtHelper.clearCache()
        return true
    }

    private fun cleanGenre(rawGenre: String): String {
        val trimmed = rawGenre.trim()
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            val numStr = trimmed.substring(1, trimmed.length - 1)
            val code = numStr.toIntOrNull()
            if (code != null) {
                return id3v1Genres.getOrElse(code) { trimmed }
            }
        }
        val codeOnly = trimmed.toIntOrNull()
        if (codeOnly != null) {
            return id3v1Genres.getOrElse(codeOnly) { trimmed }
        }
        return trimmed
    }

    private val id3v1Genres = arrayOf(
        "Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop",
        "Jazz", "Metal", "New Age", "Oldies", "Other", "Pop", "R&B", "Rap", "Reggae", "Rock",
        "Techno", "Industrial", "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack",
        "Euro-Techno", "Ambient", "Trip-Hop", "Vocal", "Jazz+Funk", "Fusion", "Trance",
        "Classical", "Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise",
        "Alternative Rock", "Bass", "Soul", "Punk", "Space", "Meditative", "Instrumental Pop",
        "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial", "Electronic",
        "Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy", "Cult", "Gangsta",
        "Top 40", "Christian Rap", "Pop/Funk", "Jungle", "Native American", "Cabaret",
        "New Wave", "Psychedelic", "Rave", "Showtunes", "Trailer", "Lo-Fi", "Tribal",
        "Acid Punk", "Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll", "Hard Rock",
        // T�rk m�zi�i ek genre'lar�
        "Arabesk", "Turkish Folk", "T�rk Sanat M�zi�i", "T�rk Pop", "T�rk Rock"
    )
}

