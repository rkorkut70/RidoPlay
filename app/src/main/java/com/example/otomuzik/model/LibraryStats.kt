package com.example.otomuzik.model

data class LibraryStats(
    val totalSongs: Int = 0,
    val totalFolders: Int = 0,
    val uniqueArtists: Int = 0,
    val uniqueAlbums: Int = 0,
    val totalPlaylists: Int = 0,
    val totalDurationFormatted: String = "0 dk",
    val totalSizeFormatted: String = "0 MB"
)
