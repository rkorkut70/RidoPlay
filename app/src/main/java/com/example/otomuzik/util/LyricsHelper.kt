package com.example.otomuzik.util

import com.example.otomuzik.model.LyricLine
import com.example.otomuzik.model.LyricsData
import com.example.otomuzik.model.LyricsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset

object LyricsHelper {

    private val TIMESTAMP_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?\]""")
    private val HEADER_TAG_REGEX = Regex("""^\[[a-zA-Z]+:.*\]$""")

    init {
        try {
            org.jaudiotagger.tag.TagOptionSingleton.getInstance().apply {
                isResetTextEncodingForExistingFrames = true
                id3v23DefaultTextEncoding = org.jaudiotagger.tag.id3.valuepair.TextEncoding.UTF_16
                id3v24DefaultTextEncoding = org.jaudiotagger.tag.id3.valuepair.TextEncoding.UTF_8
                isTruncateTextWithoutErrors = true
            }
        } catch (_: Throwable) {}
    }

    fun getLyricsCacheFile(context: android.content.Context, path: String): File {
        val cacheDir = File(context.filesDir, "lyrics")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val safeName = java.security.MessageDigest.getInstance("MD5")
            .digest(path.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(cacheDir, "$safeName.txt")
    }

    fun saveLyricsToAppCache(context: android.content.Context, path: String, lyricsText: String?) {
        try {
            val file = getLyricsCacheFile(context, path)
            if (!lyricsText.isNullOrBlank()) {
                file.writeText(TurkishStringFixer.fix(lyricsText.trim()), Charsets.UTF_8)
            } else {
                if (file.exists()) file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLyricsFromAppCache(context: android.content.Context, path: String): String? {
        return try {
            val file = getLyricsCacheFile(context, path)
            if (file.exists() && file.length() > 0) {
                val content = file.readText(Charsets.UTF_8)
                if (content.isNotBlank()) TurkishStringFixer.fix(content.trim()) else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun deleteLyricsFromAppCache(context: android.content.Context, path: String) {
        try {
            val file = getLyricsCacheFile(context, path)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Şarkı için yerel depodan (önbellek, .lrc, .txt veya ID3 gömülü) sözleri yükler.
     */
    suspend fun getLyricsForSong(songPath: String): LyricsData? = getLyricsForSong(null, songPath)

    suspend fun getLyricsForSong(context: android.content.Context? = null, songPath: String): LyricsData? = withContext(Dispatchers.IO) {
        if (songPath.isBlank()) return@withContext null

        val effectiveContext = context ?: com.example.otomuzik.OtoMuzikApplication.instanceOrNull

        // 1. Öncelik: Uygulama içi önbellek (Kullanıcının kaydettiği/düzenlediği en güncel söz)
        if (effectiveContext != null) {
            val cachedLyrics = getLyricsFromAppCache(effectiveContext, songPath)
            if (!cachedLyrics.isNullOrBlank()) {
                val fixedContent = TurkishStringFixer.fix(cachedLyrics)
                val lines = parseLyrics(fixedContent)
                val isSynced = lines.any { it.timestampMs >= 0 }
                return@withContext LyricsData(
                    songPath = songPath,
                    rawLyrics = fixedContent,
                    lines = lines,
                    isSynced = isSynced,
                    source = if (isSynced) LyricsSource.LOCAL_LRC else LyricsSource.LOCAL_TXT
                )
            }
        }

        val songFile = File(songPath)
        val directory = songFile.parentFile
        val songNameWithoutExtension = songFile.nameWithoutExtension

        // 2. Öncelik: .lrc dosyası
        if (directory != null) {
            val lrcFile = File(directory, "$songNameWithoutExtension.lrc")
            if (lrcFile.exists() && lrcFile.length() > 0) {
                val content = readFileWithEncodingFallback(lrcFile)
                if (!content.isNullOrBlank()) {
                    val fixedContent = TurkishStringFixer.fix(content)
                    val lines = parseLyrics(fixedContent)
                    val isSynced = lines.any { it.timestampMs >= 0 }
                    return@withContext LyricsData(
                        songPath = songPath,
                        rawLyrics = fixedContent,
                        lines = lines,
                        isSynced = isSynced,
                        source = LyricsSource.LOCAL_LRC
                    )
                }
            }

            // 3. Öncelik: .txt dosyası
            val txtFile = File(directory, "$songNameWithoutExtension.txt")
            if (txtFile.exists() && txtFile.length() > 0) {
                val content = readFileWithEncodingFallback(txtFile)
                if (!content.isNullOrBlank()) {
                    val fixedContent = TurkishStringFixer.fix(content)
                    val lines = parseLyrics(fixedContent)
                    val isSynced = lines.any { it.timestampMs >= 0 }
                    return@withContext LyricsData(
                        songPath = songPath,
                        rawLyrics = fixedContent,
                        lines = lines,
                        isSynced = isSynced,
                        source = LyricsSource.LOCAL_TXT
                    )
                }
            }
        }

        // 4. Öncelik: MP3 ID3 etiketindeki gömülü USLT / LYRICS çerçevesi
        val embeddedLyrics = readEmbeddedLyrics(songPath)
        if (!embeddedLyrics.isNullOrBlank()) {
            val fixedContent = TurkishStringFixer.fix(embeddedLyrics)
            val lines = parseLyrics(fixedContent)
            val isSynced = lines.any { it.timestampMs >= 0 }
            return@withContext LyricsData(
                songPath = songPath,
                rawLyrics = fixedContent,
                lines = lines,
                isSynced = isSynced,
                source = LyricsSource.EMBEDDED_ID3
            )
        }

        null
    }

    /**
     * Ham metin veya LRC içeriğini satırlara ayrıştırır.
     */
    fun parseLyrics(content: String): List<LyricLine> {
        if (content.isBlank()) return emptyList()

        val fixedContent = TurkishStringFixer.fix(content)
        val rawLines = fixedContent.lines()
        val parsedLines = mutableListOf<LyricLine>()
        var foundAnyTimestamp = false

        for (rawLine in rawLines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            // ID3/LRC üstbilgi etiketlerini atla ([ar:Sanatçı], [ti:Başlık], vb.)
            if (HEADER_TAG_REGEX.matches(line)) continue

            val matches = TIMESTAMP_REGEX.findAll(line).toList()
            if (matches.isNotEmpty()) {
                foundAnyTimestamp = true
                val textOnly = TurkishStringFixer.fix(line.replace(TIMESTAMP_REGEX, "").trim())

                for (match in matches) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val msPart = match.groupValues[3]
                    val ms = when (msPart.length) {
                        1 -> msPart.toLong() * 100
                        2 -> msPart.toLong() * 10
                        3 -> msPart.toLong()
                        else -> 0L
                    }
                    val timestampMs = minutes * 60_000L + seconds * 1000L + ms
                    parsedLines.add(LyricLine(timestampMs = timestampMs, text = textOnly))
                }
            }
        }

        return if (foundAnyTimestamp) {
            parsedLines.sortedBy { it.timestampMs }
        } else {
            // Düz metin sözler: zaman damgası olmadan satır satır böl
            rawLines
                .map { TurkishStringFixer.fix(it.trim()) }
                .filter { it.isNotBlank() }
                .map { LyricLine(timestampMs = -1L, text = it) }
        }
    }

    /**
     * İnternet üzerinden şarkı sözlerini arar:
     * 1. LRCLIB /api/get (track_name + artist_name) -> Senkronize LRC ve düz metin
     * 2. Ters kombinasyon (şarkı adı / sanatçı yeri değişmiş olabilir)
     * 3. LRCLIB /api/get (sadece track_name)
     * 4. LRCLIB /api/search (Cloudflare 503 vermezse)
     * 5. Fallback API: lyrics.ovh (/v1/{artist}/{title})
     */
    suspend fun fetchOnlineLyrics(
        artist: String,
        title: String,
        album: String = "",
        durationSec: Int = 0,
        songPath: String = ""
    ): LyricsData? = withContext(Dispatchers.IO) {
        val (cleanArtist, cleanTitle) = resolveArtistAndTitle(artist, title, songPath)

        if (cleanTitle.isBlank()) return@withContext null

        // 1. Adım: LRCLIB /api/get (Sanatçı + Şarkı Adı)
        // DİKKAT: &duration ve &album_name parametreleri kaldırıldı!
        // LRCLIB süresi ±2 saniye uymayan MP3'lerde 404 döner.
        if (cleanArtist.isNotBlank()) {
            val exactData = tryFetchExact(cleanArtist, cleanTitle, songPath)
            if (exactData != null) return@withContext exactData
        }

        // 2. Adım: İsimler ters verilmiş olabilir (Şarkı / Sanatçı)
        if (cleanArtist.isNotBlank()) {
            val swappedData = tryFetchExact(cleanTitle, cleanArtist, songPath)
            if (swappedData != null) return@withContext swappedData
        }

        // 3. Adım: Sadece şarkı adı ile LRCLIB /api/get dene
        val titleOnlyData = tryFetchExact("", cleanTitle, songPath)
        if (titleOnlyData != null) return@withContext titleOnlyData

        // 4. Adım: /api/search uç noktasını arama sorgusu ile dene (Cloudflare 503 vermezse)
        val searchData = tryFetchSearch(cleanArtist, cleanTitle, songPath)
        if (searchData != null) return@withContext searchData

        // 5. Adım: Fallback: lyrics.ovh API (Geniş düz metin veritabanı)
        if (cleanArtist.isNotBlank()) {
            val ovhData = tryFetchLyricsOvh(cleanArtist, cleanTitle, songPath)
            if (ovhData != null) return@withContext ovhData
        }

        null
    }

    private fun tryFetchExact(
        artist: String,
        title: String,
        songPath: String
    ): LyricsData? {
        try {
            val urlString = buildString {
                append("https://lrclib.net/api/get?")
                append("track_name=").append(URLEncoder.encode(title, "UTF-8"))
                if (artist.isNotBlank() && artist != "Bilinmeyen Sanatçı") {
                    append("&artist_name=").append(URLEncoder.encode(artist, "UTF-8"))
                }
            }

            val response = executeHttpGet(urlString) ?: return null
            val json = JSONObject(response)

            val syncedLyrics = json.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
            val plainLyrics = json.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }

            return createLyricsDataFromStrings(syncedLyrics, plainLyrics, songPath, LyricsSource.ONLINE_LRCLIB)
        } catch (e: Exception) {
            return null
        }
    }

    private fun tryFetchLyricsOvh(
        artist: String,
        title: String,
        songPath: String
    ): LyricsData? {
        try {
            val encArtist = URLEncoder.encode(artist, "UTF-8")
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val urlString = "https://api.lyrics.ovh/v1/$encArtist/$encTitle"

            val response = executeHttpGet(urlString) ?: return null
            val json = JSONObject(response)
            val lyrics = json.optString("lyrics").takeIf { it.isNotBlank() && it != "null" }

            if (lyrics != null) {
                return createLyricsDataFromStrings(null, lyrics, songPath, LyricsSource.ONLINE_OVH)
            }
        } catch (e: Exception) {
            // lyrics.ovh başarısız olursa sessizce geç
        }
        return null
    }

    private fun tryFetchSearch(artist: String, title: String, songPath: String): LyricsData? {
        try {
            val query = if (artist.isNotBlank() && artist != "Bilinmeyen Sanatçı") {
                "$artist $title"
            } else {
                title
            }

            val urlString = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query, "UTF-8")
            val response = executeHttpGet(urlString) ?: return null
            val jsonArray = JSONArray(response)

            if (jsonArray.length() == 0) return null

            var firstPlainCandidate: JSONObject? = null

            for (i in 0 until jsonArray.length().coerceAtMost(5)) {
                val item = jsonArray.getJSONObject(i)
                val syncedLyrics = item.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
                val plainLyrics = item.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }

                // Senkronize sözler birinci önceliklidir
                if (syncedLyrics != null) {
                    return createLyricsDataFromStrings(syncedLyrics, plainLyrics, songPath, LyricsSource.ONLINE_LRCLIB)
                }

                if (firstPlainCandidate == null && plainLyrics != null) {
                    firstPlainCandidate = item
                }
            }

            // Senkronize bulunamadıysa düz sözü kullan
            if (firstPlainCandidate != null) {
                val plainLyrics = firstPlainCandidate.optString("plainLyrics")
                return createLyricsDataFromStrings(null, plainLyrics, songPath, LyricsSource.ONLINE_LRCLIB)
            }

            return null
        } catch (e: Exception) {
            return null
        }
    }

    private fun createLyricsDataFromStrings(
        syncedLyrics: String?,
        plainLyrics: String?,
        songPath: String,
        source: LyricsSource = LyricsSource.ONLINE_LRCLIB
    ): LyricsData? {
        val rawLyrics = syncedLyrics ?: plainLyrics ?: return null
        val fixedRawLyrics = TurkishStringFixer.fix(rawLyrics)
        val lines = parseLyrics(fixedRawLyrics)
        if (lines.isEmpty()) return null
        val isSynced = syncedLyrics != null && lines.any { it.timestampMs >= 0 }

        return LyricsData(
            songPath = songPath,
            rawLyrics = fixedRawLyrics,
            lines = lines,
            isSynced = isSynced,
            source = source
        )
    }

    private fun executeHttpGet(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "RidoPlay/2.0 (Android In-Car Media Player)")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Şarkı sözünü şarkının yanına .lrc veya .txt dosyası olarak kaydeder.
     */
    suspend fun saveLyricsToFile(songPath: String, lyricsData: LyricsData): Boolean = withContext(Dispatchers.IO) {
        if (songPath.isBlank() || lyricsData.rawLyrics.isBlank()) return@withContext false

        try {
            val songFile = File(songPath)
            val dir = songFile.parentFile ?: return@withContext false
            val name = songFile.nameWithoutExtension

            val cleanLyrics = TurkishStringFixer.fix(lyricsData.rawLyrics.trim())
            val lrcFile = File(dir, "$name.lrc")
            val txtFile = File(dir, "$name.txt")

            if (lyricsData.isSynced) {
                lrcFile.writeText(cleanLyrics, Charsets.UTF_8)
                if (txtFile.exists()) txtFile.delete()
            } else {
                txtFile.writeText(cleanLyrics, Charsets.UTF_8)
                if (lrcFile.exists()) lrcFile.delete()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * ID3 etiketindeki gömülü USLT / LYRICS çerçevesini okur.
     */
    fun readEmbeddedLyrics(songPath: String): String? {
        if (songPath.isBlank()) return null
        val file = File(songPath)
        if (!file.exists() || !file.canRead()) return null

        val fromJaudiotagger = try {
            val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
            val tag = audioFile.tag
            val lyrics = tag?.getFirst(org.jaudiotagger.tag.FieldKey.LYRICS)
            if (!lyrics.isNullOrBlank()) TurkishStringFixer.fix(lyrics.trim()) else null
        } catch (e: Throwable) {
            null
        }

        if (fromJaudiotagger != null) return fromJaudiotagger

        // Fallback: Ham ID3v2 USLT parser (Jaudiotagger audio frame hatalarında çökerse)
        return readRawUslt(file)
    }

    /**
     * Ham ID3v2 binary verisinden USLT veya ULT çerçevesini okur.
     */
    private fun readRawUslt(file: File): String? {
        return try {
            java.io.RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(10)
                raf.read(header)
                if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) return null
                val majorVersion = header[3].toInt() and 0xFF
                if (majorVersion !in 2..4) return null

                val flagByte = header[5].toInt() and 0xFF
                val hasExtHeader = (flagByte and 0x40) != 0

                val size = ((header[6].toLong() and 0x7F) shl 21) or
                           ((header[7].toLong() and 0x7F) shl 14) or
                           ((header[8].toLong() and 0x7F) shl 7) or
                            (header[9].toLong() and 0x7F)

                val frameHeaderSize = if (majorVersion == 2) 6 else 10
                val frameIdSize = if (majorVersion == 2) 3 else 4

                if (hasExtHeader) {
                    val extSizeBytes = ByteArray(4)
                    raf.read(extSizeBytes)
                    val extSize = java.nio.ByteBuffer.wrap(extSizeBytes).int
                    raf.skipBytes(extSize - 4)
                }

                var bytesRead = 0L
                val frameHeaderBuf = ByteArray(frameHeaderSize)

                while (bytesRead < size - frameHeaderSize) {
                    val read = raf.read(frameHeaderBuf)
                    if (read < frameHeaderSize) break
                    bytesRead += read

                    val frameId = String(frameHeaderBuf, 0, frameIdSize, Charsets.ISO_8859_1)
                    if (frameId.all { it == '\u0000' }) break

                    val frameSize = if (majorVersion == 2) {
                        ((frameHeaderBuf[3].toLong() and 0xFF) shl 16) or
                        ((frameHeaderBuf[4].toLong() and 0xFF) shl 8) or
                         (frameHeaderBuf[5].toLong() and 0xFF)
                    } else {
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

                    if (frameSize <= 0 || frameSize > 2 * 1024 * 1024) break

                    val frameData = ByteArray(frameSize.toInt())
                    val dataRead = raf.read(frameData)
                    if (dataRead < 1) break
                    bytesRead += dataRead

                    val normalizedId = if (majorVersion == 2 && frameId == "ULT") "USLT" else frameId
                    if (normalizedId == "USLT" && frameData.size > 4) {
                        val enc = frameData[0].toInt() and 0xFF
                        var offset = 4 // encoding (1) + language (3)
                        val isUtf16 = enc == 1 || enc == 2
                        if (isUtf16) {
                            while (offset < frameData.size - 1) {
                                if (frameData[offset] == 0.toByte() && frameData[offset + 1] == 0.toByte()) {
                                    offset += 2
                                    break
                                }
                                offset += 2
                            }
                        } else {
                            while (offset < frameData.size) {
                                if (frameData[offset] == 0.toByte()) {
                                    offset += 1
                                    break
                                }
                                offset++
                            }
                        }

                        if (offset < frameData.size) {
                            val lyricBytes = frameData.copyOfRange(offset, frameData.size)
                            val decoded = when (enc) {
                                1 -> String(lyricBytes, Charsets.UTF_16)
                                2 -> String(lyricBytes, Charsets.UTF_16BE)
                                3 -> String(lyricBytes, Charsets.UTF_8)
                                else -> {
                                    var s = String(lyricBytes, Charsets.UTF_8)
                                    if (s.contains("\uFFFD")) {
                                        s = try { String(lyricBytes, java.nio.charset.Charset.forName("windows-1254")) }
                                            catch (e: Exception) { String(lyricBytes, java.nio.charset.Charset.forName("ISO-8859-9")) }
                                    }
                                    s
                                }
                            }.trimEnd('\u0000').trim()

                            if (decoded.isNotBlank()) {
                                return TurkishStringFixer.fix(decoded)
                            }
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Şarkı sözünü MP3 dosyasının ID3 etiketine gömer.
     */
    suspend fun saveEmbeddedLyrics(songPath: String, lyricsText: String): Boolean = withContext(Dispatchers.IO) {
        if (songPath.isBlank()) return@withContext false
        val file = File(songPath)
        if (!file.exists()) return@withContext false

        return@withContext try {
            val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault
            val cleanLyrics = TurkishStringFixer.fix(lyricsText.trim())

            try {
                tag.deleteField(org.jaudiotagger.tag.FieldKey.LYRICS)
            } catch (_: Throwable) {}

            if (cleanLyrics.isNotBlank()) {
                try {
                    tag.setField(org.jaudiotagger.tag.FieldKey.LYRICS, cleanLyrics)
                } catch (e: Throwable) {
                    try {
                        val uslt = org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT(
                            org.jaudiotagger.tag.id3.valuepair.TextEncoding.UTF_16,
                            "eng",
                            "",
                            cleanLyrics
                        )
                        val frame = org.jaudiotagger.tag.id3.ID3v23Frame("USLT")
                        frame.body = uslt
                        tag.setField(frame)
                    } catch (e2: Throwable) {
                        e2.printStackTrace()
                    }
                }
            }
            audioFile.commit()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun readFileWithEncodingFallback(file: File): String? {
        return try {
            val bytes = file.readBytes()
            var text = String(bytes, Charsets.UTF_8)
            if (text.contains("\uFFFD")) {
                text = try {
                    String(bytes, Charset.forName("windows-1254"))
                } catch (e: Exception) {
                    String(bytes, Charset.forName("ISO-8859-9"))
                }
            }
            TurkishStringFixer.fix(text)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Başlık veya dosya adından sanatçı ve şarkı adını akıllıca ayıklar.
     */
    fun resolveArtistAndTitle(rawArtist: String, rawTitle: String, songPath: String = ""): Pair<String, String> {
        var a = cleanArtistName(rawArtist)
        var t = TurkishStringFixer.fix(rawTitle).trim()

        // Başlangıçtaki parça numaralarını temizle ("01 - ", "01. ", "1-")
        t = t.replace(Regex("""^\d+[\s.\-_]+"""), "").trim()

        // Eğer sanatçı boş veya bilinmeyen ise ve t "Sanatçı - Şarkı" şeklindeyse
        if (a.isBlank() && t.contains(" - ")) {
            val parts = t.split(" - ", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                a = cleanArtistName(parts[0])
                t = parts[1]
            }
        }

        // Eğer hala sanatçı boş ise ve dosya adı varsa:
        if (a.isBlank() && songPath.isNotBlank()) {
            try {
                val fileName = File(songPath).nameWithoutExtension
                    .replace(Regex("""^\d+[\s.\-_]+"""), "").trim()
                if (fileName.contains(" - ")) {
                    val parts = fileName.split(" - ", limit = 2)
                    if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                        a = cleanArtistName(parts[0])
                        if (t.isBlank() || t == fileName || t == File(songPath).nameWithoutExtension) {
                            t = parts[1]
                        }
                    }
                }
            } catch (e: Exception) {
                // Dosya adı okuma hatası yok sayılır
            }
        }

        t = cleanSongTitle(t)
        return Pair(a, t)
    }

    /**
     * Arama başarısını artırmak için şarkı adındaki gereksiz kalıpları temizler.
     */
    fun cleanSongTitle(rawTitle: String): String {
        var t = TurkishStringFixer.fix(rawTitle)
        // Başlangıçtaki parça numaralarını temizle ("01 - ", "01. ", "1-")
        t = t.replace(Regex("""^\d+[\s.\-_]+"""), "")
        // Sanatçı - Şarkı formatındaysa ve tek başlık olarak geldiyse ayır
        if (t.contains(" - ")) {
            val parts = t.split(" - ", limit = 2)
            if (parts.size == 2 && parts[1].isNotBlank()) {
                t = parts[1]
            }
        }
        // Parantez içi resmi video/klip/remaster yazılarını temizle
        t = t.replace(Regex("""(?i)\(official.*?\)|\[official.*?\]|\(audio\)|\(video\)|\[audio\]|\[video\]|\(lyric.*?\)|\[lyric.*?\]|\(klip\)|\(video klip\)"""), "")
        t = t.replace(Regex("""(?i)\(remaster.*?\)|\[remaster.*?\]|\(live.*?\)|\[live.*?\]"""), "")
        t = t.replace(Regex("""(?i)\(320kbps\)|\[320kbps\]|\(128kbps\)|\[128kbps\]|\.mp3|\.flac|\.m4a|\.wav|\.aac|\.ogg"""), "")
        return t.trim()
    }

    /**
     * Sanatçı adını temizler.
     */
    fun cleanArtistName(rawArtist: String): String {
        var a = TurkishStringFixer.fix(rawArtist)
        if (a.equals("Bilinmeyen Sanatçı", ignoreCase = true) ||
            a.equals("Unknown Artist", ignoreCase = true) ||
            a.equals("<unknown>", ignoreCase = true)) {
            return ""
        }
        return a.trim()
    }
}
