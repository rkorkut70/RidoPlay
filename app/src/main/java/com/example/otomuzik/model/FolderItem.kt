package com.example.otomuzik.model

data class FolderItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val isStorageRoot: Boolean = false,
    val songCount: Int = 0,
    val song: Song? = null
)
