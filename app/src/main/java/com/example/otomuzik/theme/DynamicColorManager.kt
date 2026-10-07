package com.example.otomuzik.theme

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Albüm kapağı bitmap'inden baskın rengi çıkarır ve UI aksan rengi olarak döner.
 * Orta parlaklıkta, doygun ve okunaklı renkler tercih edilir.
 */
object DynamicColorManager {

    /**
     * Bitmap'den ortalama/baskın renk çıkarır.
     * Performans için bitmap'i 32x32'ye küçültür.
     */
    suspend fun extractDominantColor(bitmap: Bitmap?): Color = withContext(Dispatchers.Default) {
        if (bitmap == null) return@withContext Color(0xFF00E5FF)

        try {
            // Küçültülmüş bitmap üzerinde çalış
            val scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
            val pixels = IntArray(scaled.width * scaled.height)
            scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)

            var r = 0L; var g = 0L; var b = 0L; var count = 0

            for (pixel in pixels) {
                val pr = (pixel shr 16) and 0xFF
                val pg = (pixel shr 8) and 0xFF
                val pb = pixel and 0xFF
                // Çok koyu veya çok açık pikselleri atla
                val brightness = (pr * 299 + pg * 587 + pb * 114) / 1000
                if (brightness in 40..220) {
                    r += pr; g += pg; b += pb; count++
                }
            }

            if (count == 0) return@withContext Color(0xFF00E5FF)

            val avgR = (r / count).toInt()
            val avgG = (g / count).toInt()
            val avgB = (b / count).toInt()

            // Doygunluğu artır (canlı renk için)
            val boosted = boostSaturation(
                Color(avgR / 255f, avgG / 255f, avgB / 255f, 1f),
                factor = 1.6f,
                minBrightness = 0.55f
            )

            if (scaled != bitmap) scaled.recycle()
            boosted
        } catch (e: Exception) {
            Color(0xFF00E5FF)
        }
    }

    /**
     * Rengin doygunluğunu artırır ve minimum parlaklık garantisi verir.
     */
    private fun boostSaturation(color: Color, factor: Float, minBrightness: Float): Color {
        val r = color.red; val g = color.green; val b = color.blue
        val max = maxOf(r, g, b); val min = minOf(r, g, b)
        val delta = max - min

        if (delta < 0.05f) {
            // Gri tonlar → Varsayılan Cyan
            return Color(0xFF00E5FF)
        }

        // HSV'ye çevir
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), hsv
        )

        hsv[1] = (hsv[1] * factor).coerceIn(0.5f, 1.0f) // Doygunluk
        hsv[2] = hsv[2].coerceAtLeast(minBrightness)     // Parlaklık

        val boostedArgb = android.graphics.Color.HSVToColor(hsv)
        return Color(boostedArgb)
    }

    /** Aksan renginden glow rengi üretir (%20 alfa) */
    fun toGlowColor(accent: Color): Color = accent.copy(alpha = 0.20f)

    /** Aksan renginden parlak kenar rengi üretir (%80 alfa) */
    fun toBorderColor(accent: Color): Color = accent.copy(alpha = 0.80f)
}



