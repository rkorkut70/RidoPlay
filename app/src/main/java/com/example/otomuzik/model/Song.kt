package com.example.otomuzik.model

import java.io.File

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val path: String,
    val uriString: String,
    val albumId: Long = -1L,
    val folderPath: String = "",
    val year: String = "",
    val genre: String = "",
    val bitrate: String = "",
    val trackNumber: String = "",
    val format: String = "",
    val fileSizeFormatted: String = ""
) {
    val durationFormatted: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    val id3Badge: String
        get() {
            val parts = mutableListOf<String>()
            if (format.isNotEmpty()) parts.add(format.uppercase())
            if (bitrate.isNotEmpty()) parts.add(bitrate)
            if (genre.isNotEmpty()) parts.add(genre)
            if (year.isNotEmpty()) parts.add(year)
            return parts.joinToString(" • ")
        }
}
