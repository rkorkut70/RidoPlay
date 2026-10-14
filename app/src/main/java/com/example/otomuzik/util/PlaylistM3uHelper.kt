package com.example.otomuzik.util

import android.os.Environment
import com.example.otomuzik.model.Playlist
import com.example.otomuzik.model.Song
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object PlaylistM3uHelper {

    /**
     * Çalma listesini UTF-8 kodlamalı standart .m3u8 formatında Müzik klasörüne dışa aktarır.
     */
    fun exportToM3u(playlist: Playlist, songs: List<Song>): Result<File> {
        return try {
            val safeName = playlist.name.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val exportDir = if (musicDir.exists() || musicDir.mkdirs()) musicDir else Environment.getExternalStorageDirectory()
            val m3uFile = File(exportDir, "$safeName.m3u8")

            OutputStreamWriter(FileOutputStream(m3uFile), StandardCharsets.UTF_8).use { writer ->
                writer.write("#EXTM3U\n")
                writer.write("#PLAYLIST:${playlist.name}\n")
                for (song in songs) {
                    val durationSec = (song.durationMs / 1000).coerceAtLeast(0)
                    val artist = song.artist.ifEmpty { "Bilinmeyen Sanatçı" }
                    val title = song.title.ifEmpty { File(song.path).nameWithoutExtension }
                    writer.write("#EXTINF:$durationSec,$artist - $title\n")
                    writer.write("${song.path}\n")
                }
            }
            Result.success(m3uFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Seçilen .m3u veya .m3u8 dosyasını ayrıştırarak kütüphanedeki eşleşen şarkıları bulur.
     */
    fun parseM3u(file: File, allSongs: List<Song>): Pair<String, List<Song>> {
        val playlistName = file.nameWithoutExtension
        val matchedSongs = mutableListOf<Song>()
        val songMapByPath = allSongs.associateBy { it.path }
        val songMapByName = allSongs.associateBy { File(it.path).name.lowercase() }

        try {
            file.forEachLine(StandardCharsets.UTF_8) { rawLine ->
                val line = rawLine.trim()
                if (line.isNotEmpty() && !line.startsWith("#")) {
                    val songByExactPath = songMapByPath[line]
                    if (songByExactPath != null) {
                        matchedSongs.add(songByExactPath)
                    } else {
                        val filename = File(line).name.lowercase()
                        val songByName = songMapByName[filename]
                        if (songByName != null) {
                            matchedSongs.add(songByName)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(playlistName, matchedSongs.distinctBy { it.path })
    }

    /**
     * Müzik ve İndirilenler klasörlerindeki mevcut .m3u / .m3u8 dosyalarını tarar.
     */
    fun findAvailableM3uFiles(): List<File> {
        val candidates = mutableListOf<File>()
        val searchDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStorageDirectory()
        )
        for (dir in searchDirs) {
            if (dir != null && dir.exists() && dir.isDirectory) {
                dir.listFiles { f ->
                    f.isFile && (f.extension.equals("m3u", ignoreCase = true) || f.extension.equals("m3u8", ignoreCase = true))
                }?.let { candidates.addAll(it) }
            }
        }
        return candidates.distinctBy { it.absolutePath }
    }
}
