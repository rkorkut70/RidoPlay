package com.example.otomuzik.model

/**
 * Şarkı sözünün bir satırını ve zaman damgasını temsil eder.
 * @param timestampMs Şarkının çalma konumu (milisaniye cinsinden). Eğer -1L ise zaman damgası yoktur (düz metin).
 * @param text Satır metni
 */
data class LyricLine(
    val timestampMs: Long,
    val text: String
)

/**
 * Şarkı sözü verilerini ve durumunu tutar.
 */
data class LyricsData(
    val songPath: String = "",
    val rawLyrics: String = "",
    val lines: List<LyricLine> = emptyList(),
    val isSynced: Boolean = false,
    val source: LyricsSource = LyricsSource.NONE
)

enum class LyricsSource {
    NONE,
    LOCAL_LRC,
    LOCAL_TXT,
    EMBEDDED_ID3,
    ONLINE_LRCLIB,
    ONLINE_OVH,
    MANUAL_CLIPBOARD
}
