package com.example.otomuzik.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class CoverSuggestion(
    val id: String,
    val title: String,
    val artist: String,
    val source: String,
    val thumbnailUrl: String,
    val fullImageUrl: String
)

sealed class CoverSearchResult {
    data class Success(val bytes: ByteArray, val source: String) : CoverSearchResult()
    object NoInternet : CoverSearchResult()
    object NotFound : CoverSearchResult()
}

object MusicCoverSearcher {

    fun clean(text: String): String {
        var s = text.trim()
        val unknowns = listOf(
            "bilinmeyen sanatçı", "bilinmeyen parça", "bilinmeyen albüm", "bilinmeyen",
            "unknown artist", "unknown title", "unknown album", "unknown", "<unknown>", "null"
        )
        for (u in unknowns) {
            if (s.equals(u, ignoreCase = true)) return ""
        }
        s = s.replace(Regex("(?i)\\.(mp3|flac|wav|m4a|aac|ogg|wma)$"), "")
        s = s.replace(Regex("^\\d{1,3}\\s*[-._]\\s*"), "")
        s = s.replace(Regex("(?i)\\[(official|video|audio|hq|hd|remix|lyrics|klip|netd|320kbps)[^\\]]*\\]"), "")
        s = s.replace(Regex("(?i)\\((official|video|audio|hq|hd|remix|lyrics|klip|netd|320kbps)[^)]*\\)"), "")
        return s.trim()
    }

    fun isNetworkException(e: Throwable): Boolean {
        var cause: Throwable? = e
        while (cause != null) {
            if (cause is java.net.UnknownHostException ||
                cause is java.net.ConnectException ||
                cause is java.net.NoRouteToHostException ||
                cause is java.net.SocketException
            ) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    /**
     * Resmi müzik veritabanlarından (Apple Music / iTunes & Deezer)
     * yalnızca müzikle ilgili doğrulanmış 10 adet albüm kapağı önerisi döndürür.
     */
    suspend fun searchCoverSuggestions(
        artist: String,
        title: String,
        album: String = "",
        fileName: String = "",
        customQuery: String = "",
        limit: Int = 40
    ): List<CoverSuggestion> = withContext(Dispatchers.IO) {
        val suggestions = mutableListOf<CoverSuggestion>()
        val seenImages = mutableSetOf<String>()

        val queries = mutableListOf<String>()

        if (customQuery.isNotBlank()) {
            queries.add(customQuery.trim())
        } else {
            val cleanArtist = clean(artist)
            val cleanTitle = clean(title)
            val cleanAlbum = clean(album)
            val cleanFileName = clean(fileName)

            var derivedArtist = cleanArtist
            var derivedTitle = cleanTitle

            if (derivedArtist.isEmpty()) {
                val sourceText = if (cleanTitle.contains(" - ")) cleanTitle else if (cleanFileName.contains(" - ")) cleanFileName else ""
                if (sourceText.isNotEmpty()) {
                    val parts = sourceText.split(" - ", limit = 2)
                    derivedArtist = clean(parts[0])
                    derivedTitle = clean(parts[1])
                }
            }

            if (derivedArtist.isNotEmpty() && derivedTitle.isNotEmpty()) {
                queries.add("$derivedArtist $derivedTitle")
            }
            if (derivedArtist.isNotEmpty() && cleanAlbum.isNotEmpty() && cleanAlbum != derivedTitle) {
                queries.add("$derivedArtist $cleanAlbum")
            }
            if (derivedTitle.isNotEmpty() && !queries.contains(derivedTitle)) {
                queries.add(derivedTitle)
            }
            if (derivedArtist.isNotEmpty() && !queries.contains(derivedArtist)) {
                queries.add(derivedArtist)
            }
            if (cleanFileName.isNotEmpty() && !queries.contains(cleanFileName)) {
                queries.add(cleanFileName)
            }
        }

        for (q in queries) {
            if (suggestions.size >= limit) break

            // 1. iTunes Albüm ve Parça Arama (100% Resmi Müzik Kapakları)
            try {
                val itunesList = fetchItunesSuggestions(q, (limit - suggestions.size).coerceAtLeast(15))
                for (item in itunesList) {
                    if (seenImages.add(item.fullImageUrl)) {
                        suggestions.add(item)
                        if (suggestions.size >= limit) break
                    }
                }
            } catch (e: Exception) {
                // Log and continue
            }

            if (suggestions.size >= limit) break

            // 2. Deezer Albüm ve Parça Arama (100% Resmi Müzik Kapakları)
            try {
                val deezerList = fetchDeezerSuggestions(q, (limit - suggestions.size).coerceAtLeast(15))
                for (item in deezerList) {
                    if (seenImages.add(item.fullImageUrl)) {
                        suggestions.add(item)
                        if (suggestions.size >= limit) break
                    }
                }
            } catch (e: Exception) {
                // Log and continue
            }
        }

        suggestions.take(limit)
    }

    private fun fetchItunesSuggestions(query: String, limit: Int): List<CoverSuggestion> {
        val list = mutableListOf<CoverSuggestion>()
        val encoded = URLEncoder.encode(query, "UTF-8")
        val endpoints = listOf(
            "https://itunes.apple.com/search?term=$encoded&entity=album&limit=$limit",
            "https://itunes.apple.com/search?term=$encoded&entity=song&limit=$limit"
        )

        for (urlStr in endpoints) {
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val results = json.optJSONArray("results")
                    if (results != null) {
                        for (i in 0 until results.length()) {
                            val item = results.getJSONObject(i)
                            val rawUrl = item.optString("artworkUrl100")
                            if (rawUrl.isNotEmpty()) {
                                val thumbUrl = rawUrl.replace(Regex("""\d+x\d+bb"""), "300x300bb")
                                val fullUrl = rawUrl.replace(Regex("""\d+x\d+bb"""), "1000x1000bb")
                                val title = item.optString("collectionName").ifEmpty { item.optString("trackName") }
                                val artistName = item.optString("artistName")
                                val id = "itunes_${item.optLong("collectionId", item.optLong("trackId", i.toLong()))}"
                                list.add(
                                    CoverSuggestion(
                                        id = id,
                                        title = title,
                                        artist = artistName,
                                        source = "Apple Music",
                                        thumbnailUrl = thumbUrl,
                                        fullImageUrl = fullUrl
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        return list
    }

    private fun fetchDeezerSuggestions(query: String, limit: Int): List<CoverSuggestion> {
        val list = mutableListOf<CoverSuggestion>()
        val encoded = URLEncoder.encode(query, "UTF-8")
        val endpoints = listOf(
            "https://api.deezer.com/search/album?q=$encoded&limit=$limit",
            "https://api.deezer.com/search?q=$encoded&limit=$limit"
        )

        for (urlStr in endpoints) {
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val data = json.optJSONArray("data")
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val item = data.getJSONObject(i)
                            val albumObj = item.optJSONObject("album")
                            val artistObj = item.optJSONObject("artist")

                            val fullCover = albumObj?.optString("cover_xl")?.takeIf { it.isNotEmpty() }
                                ?: item.optString("cover_xl").takeIf { it.isNotEmpty() }
                                ?: albumObj?.optString("cover_big")?.takeIf { it.isNotEmpty() }
                                ?: item.optString("cover_big").takeIf { it.isNotEmpty() }

                            val thumbCover = albumObj?.optString("cover_medium")?.takeIf { it.isNotEmpty() }
                                ?: item.optString("cover_medium").takeIf { it.isNotEmpty() }
                                ?: fullCover

                            if (!fullCover.isNullOrEmpty() && !thumbCover.isNullOrEmpty()) {
                                val title = item.optString("title").ifEmpty { albumObj?.optString("title") ?: "" }
                                val artistName = artistObj?.optString("name") ?: ""
                                val id = "deezer_${item.optLong("id", i.toLong())}"
                                list.add(
                                    CoverSuggestion(
                                        id = id,
                                        title = title,
                                        artist = artistName,
                                        source = "Deezer",
                                        thumbnailUrl = thumbCover,
                                        fullImageUrl = fullCover
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        return list
    }

    suspend fun downloadUrlToByteArray(urlString: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.instanceFollowRedirects = true

            val code = connection.responseCode
            if (code in 300..399) {
                val redirectUrl = connection.getHeaderField("Location")
                if (!redirectUrl.isNullOrEmpty()) {
                    return@withContext downloadUrlToByteArray(redirectUrl)
                }
            }

            if (code == 200) {
                val inputStream = connection.inputStream
                val buffer = ByteArrayOutputStream()
                val data = ByteArray(16384)
                var nRead: Int
                while (inputStream.read(data, 0, data.size).also { nRead = it } != -1) {
                    buffer.write(data, 0, nRead)
                }
                val bytes = buffer.toByteArray()
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                if (opts.outWidth >= 50 && opts.outHeight >= 50) {
                    return@withContext bytes
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        null
    }

    suspend fun downloadUrlToBitmap(urlString: String): Bitmap? = withContext(Dispatchers.IO) {
        val bytes = downloadUrlToByteArray(urlString) ?: return@withContext null
        try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Geriye uyumluluk için tekil arama fonksiyonu
     */
    suspend fun searchAndDownloadCover(
        artist: String,
        title: String,
        album: String = "",
        fileName: String = ""
    ): CoverSearchResult = withContext(Dispatchers.IO) {
        try {
            val suggestions = searchCoverSuggestions(artist, title, album, fileName, limit = 1)
            if (suggestions.isNotEmpty()) {
                val first = suggestions.first()
                val bytes = downloadUrlToByteArray(first.fullImageUrl)
                if (bytes != null) {
                    return@withContext CoverSearchResult.Success(bytes, first.source)
                }
            }
            CoverSearchResult.NotFound
        } catch (e: Exception) {
            if (isNetworkException(e)) CoverSearchResult.NoInternet else CoverSearchResult.NotFound
        }
    }
}
