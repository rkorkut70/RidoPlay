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

    /**
     * Şarkı için yerel depodan (.lrc, .txt veya ID3 gümülü) sözleri yükler.
     */
    suspend fun getLyricsForSong(songPath: String): LyricsData? = withContext(Dispatchers.IO) {
        if (songPath.isBlank()) return@withContext null

        val songFile = File(songPath)
        if (!songFile.exists()) return@withContext null

        val directory = songFile.parentFile ?: return@withContext null
        val songNameWithoutExtension = songFile.nameWithoutExtension

        // 1. Öncelik: .lrc dosyası
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

        // 2. Öncelik: .txt dosyası
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

        // 3. Öncelik: MP3 ID3 etiketindeki gömülü USLT / LYRICS çerçevesi
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

            val extension = if (lyricsData.isSynced) "lrc" else "txt"
            val targetFile = File(dir, "$name.$extension")

            targetFile.writeText(TurkishStringFixer.fix(lyricsData.rawLyrics), Charsets.UTF_8)
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

        return try {
            val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
            val tag = audioFile.tag ?: return null
            val lyrics = tag.getFirst(org.jaudiotagger.tag.FieldKey.LYRICS)
            if (!lyrics.isNullOrBlank()) TurkishStringFixer.fix(lyrics.trim()) else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Şarkı sözünü MP3 dosyasının ID3 etiketine gömer.
     */
    suspend fun saveEmbeddedLyrics(songPath: String, lyricsText: String): Boolean = withContext(Dispatchers.IO) {
        if (songPath.isBlank() || lyricsText.isBlank()) return@withContext false
        val file = File(songPath)
        if (!file.exists()) return@withContext false

        return@withContext try {
            val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault
            tag.setField(org.jaudiotagger.tag.FieldKey.LYRICS, TurkishStringFixer.fix(lyricsText))
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
