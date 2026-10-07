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
        val cacheKey = when {
            uriString.isNotEmpty() -> uriString
            path.isNotEmpty() -> path
            albumId > 0 -> "album_$albumId"
            else -> return@withContext null
        }

        memoryCache.get(cacheKey)?.let { return@withContext it }

        var bitmap: Bitmap? = null

        // 0. Uygulama içi özel önbelleğe kaydedilen kapak resmi (Kullanıcı tarafından yüklenen/indirilen)
        if (path.isNotEmpty()) {
            val customCover = Id3Helper.getCoverFromAppCache(context, path)
            if (customCover != null && customCover.isNotEmpty()) {
                bitmap = decodeSampledBitmap(customCover, 512)
            }
        }

        // 1. Android 10+ (API 29+) MediaStore thumbnail yükleyici (Scoped Storage'da en güvenli yöntem)
        if (bitmap == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uriString.isNotEmpty()) {
            try {
                val uri = Uri.parse(uriString)
                bitmap = context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
            } catch (e: Exception) {
                // Fallback to retriever
            }
        }

        // 2. ContentResolver FileDescriptor üzerinden MediaMetadataRetriever ile gömülü APIC resmi
        if (bitmap == null && uriString.isNotEmpty()) {
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                    val rawBytes = retriever.embeddedPicture
                    if (rawBytes != null) {
                        bitmap = decodeSampledBitmap(rawBytes, 400)
                    }
                }
            } catch (e: Exception) {
                // Devam
            } finally {
                try { retriever?.release() } catch (e: Exception) {}
            }
        }

        // 3. Doğrudan dosya yolu üzerinden (USB bellekler, SD kartlar vb.)
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

                if (bitmap == null) {
                    var retriever: MediaMetadataRetriever? = null
                    try {
                        retriever = MediaMetadataRetriever()
                        retriever.setDataSource(path)
                        val rawBytes = retriever.embeddedPicture
                        if (rawBytes != null) {
                            bitmap = decodeSampledBitmap(rawBytes, 400)
                        }
                    } catch (e: Exception) {
                        // Devam
                    } finally {
                        try { retriever?.release() } catch (e: Exception) {}
                    }
                }
            }
        }

        // 4. Şarkının bulunduğu klasörde cover.jpg, folder.jpg vb. resim arama
        if (bitmap == null && path.isNotEmpty()) {
            try {
                val parent = File(path).parentFile
                if (parent != null && parent.exists() && parent.canRead()) {
                    val coverNames = listOf("cover.jpg", "folder.jpg", "album.jpg", "front.jpg", "cover.png", "folder.png")
                    for (name in coverNames) {
                        val img = File(parent, name)
                        if (img.exists() && img.canRead()) {
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

        // 5. MediaStore albumart URI'si
        if (bitmap == null && albumId > 0) {
            try {
                val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                val uri = ContentUris.withAppendedId(sArtworkUri, albumId)
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 2
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    bitmap = BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, opts)
                }
            } catch (e: Exception) {}
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
