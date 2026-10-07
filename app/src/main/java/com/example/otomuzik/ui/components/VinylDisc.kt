package com.example.otomuzik.ui.components
import com.example.otomuzik.theme.*

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.otomuzik.R




@Composable
fun VinylDisc(
    isPlaying: Boolean,
    artworkBitmap: Bitmap? = null,
    size: Dp = 160.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Vinylangle"
    )

    val currentRotation = if (isPlaying) angle else 0f

    Box(
        modifier = modifier
            .size(size)
            .rotate(currentRotation),
        contentAlignment = Alignment.Center
    ) {
        // Vinil Plak Zemin ve Yiv Çizgiileri
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2, size.toPx() / 2)
            val radius = size.toPx() / 2

            // Dış siyah vinil gövde
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF22242B), Color(0xFF101216), Color(0xFF07080a)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Vinil yiv çizgiileri (Hafif gri daireLER)
            val grooveColor = Color(0x22FFFFFF)
            for (i in 1..4) {
                drawCircle(
                    color = grooveColor,
                    radius = radius * (0.48f + i * 0.11f),
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }
        }

        // Orta Göbek (albüm Kapak Resmi veya Mzik İkonu)
        val centerSize = size * 0.48f
        Box(
            modifier = Modifier
                .size(centerSize)
                .clip(CircleShape)
                .background(CarSurfaceVariant)
                .border(2.dp, CarCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (artworkBitmap != null) {
                Image(
                    bitmap = artworkBitmap.asImageBitmap(),
                    contentDescription = "albüm Kapağı",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_music_note),
                    contentDescription = null,
                    tint = CarCyan,
                    modifier = Modifier.size(size * 0.22f)
                )
            }

            // Merkez küçük mil deliği
            Box(
                modifier = Modifier
                    .size(size * 0.09f)
                    .clip(CircleShape)
                    .background(Color(0xFF0C0E12))
                    .border(1.5.dp, CarCyan, CircleShape)
            )
        }
    }
}


















