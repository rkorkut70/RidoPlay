package com.example.otomuzik.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AlbumArtHelper {

    // Android Go için hafif bellek önbelleği (En fazla 30 kapak resmi tutar)
    private val memoryCache = object : LruCache<String, Bitmap>(30) {}

    fun clearCache() {
        memoryCache.evictAll()
    }

    suspend fun getArtwork(
        context: Context,
        path: String,
        albumId: Long = -1L,
        uriString: String = ""
    ): Bitmap? = withContext(Dispatchers.IO) {
        // Önbellek anahtarı kesinlikle şarkıya özgü dosya yolu veya URI olmalı, asla ortak albumId olmamalıdır
        val cacheKey = when {
            path.isNotEmpty() -> path
            uriString.isNotEmpty() -> uriString
            else -> return@withContext null
        }

        memoryCache.get(cacheKey)?.let { return@withContext it }

        var bitmap: Bitmap? = null

        // 1. Uygulama içi özel önbelleğe kaydedilen kapak resmi (Kullanıcı tarafından ID3 üzerinden kaydedilen)
        if (path.isNotEmpty()) {
            val customCover = Id3Helper.getCoverFromAppCache(context, path)
            if (customCover != null && customCover.isNotEmpty()) {
                bitmap = decodeSampledBitmap(customCover, 512)
            }
        }

        // 2. Doğrudan dosyanın kendi içine gömülü (APIC frame) ID3 resmi (jaudiotagger)
        if (bitmap == null && path.isNotEmpty()) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                try {
                    val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                    val artwork = audioFile.tag?.firstArtwork
                    if (artwork?.binaryData != null && artwork.binaryData.isNotEmpty()) {
                        bitmap = decodeSampledBitmap(artwork.binaryData, 512)
                    }
                } catch (e: Exception) {}

                // 3. Dosya üzerinden MediaMetadataRetriever ile doğrudan gömülü resim
                if (bitmap == null) {
                    var retriever: MediaMetadataRetriever? = null
                    try {
                        retriever = MediaMetadataRetriever()
                        retriever.setDataSource(path)
                        val rawBytes = retriever.embeddedPicture
                        if (rawBytes != null && rawBytes.isNotEmpty()) {
                            bitmap = decodeSampledBitmap(rawBytes, 400)
                        }
                    } catch (e: Exception) {
                    } finally {
                        try { retriever?.release() } catch (e: Exception) {}
                    }
                }

                // 4. Şarkıya özel yardımcı kapak resmi ("ŞarkıAdı.jpg", "ŞarkıAdı.png")
                if (bitmap == null) {
                    try {
                        val parent = file.parentFile
                        if (parent != null && parent.exists() && parent.canRead()) {
                            val baseName = file.nameWithoutExtension
                            val songSpecificNames = listOf(
                                "$baseName.jpg",
                                "$baseName.png",
                                "$baseName.jpeg",
                                "$baseName.cover.jpg"
                            )
                            for (name in songSpecificNames) {
                                val img = File(parent, name)
                                if (img.exists() && img.canRead() && img.length() > 0) {
                                    val opts = BitmapFactory.Options().apply {
                                        inSampleSize = 2
                                        inPreferredConfig = Bitmap.Config.RGB_565
                                    }
                                    bitmap = BitmapFactory.decodeFile(img.absolutePath, opts)
                                    if (bitmap != null) break
                                }
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
        }

        // 5. ContentResolver FileDescriptor üzerinden gömülü APIC resmi
        if (bitmap == null && uriString.isNotEmpty()) {
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                    val rawBytes = retriever.embeddedPicture
                    if (rawBytes != null && rawBytes.isNotEmpty()) {
                        bitmap = decodeSampledBitmap(rawBytes, 400)
                    }
                }
            } catch (e: Exception) {
            } finally {
                try { retriever?.release() } catch (e: Exception) {}
            }
        }

        if (bitmap != null) {
            memoryCache.put(cacheKey, bitmap)
        }

        bitmap
    }

    private fun decodeSampledBitmap(rawBytes: ByteArray, reqSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)

        var sampleSize = 1
        while (options.outWidth / (sampleSize * 2) >= reqSize && options.outHeight / (sampleSize * 2) >= reqSize) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565 // Low RAM footprint for Android Go
        }
        return BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
    }
}
