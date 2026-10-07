package com.example.otomuzik.ui.components
import com.example.otomuzik.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp




import kotlin.math.roundToInt

@Composable
fun CarProgressBar(
    progress: Float,
    currentFormatted: String,
    durationFormatted: String,
    onSeekToProgress: (Float) -> Unit,
    accentColor: Color = CarCyan,   // ← dinamik aksan rengi
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val activeFraction = if (isDragging) dragFraction else progress.coerceIn(0f, 1f)

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    val barHeight = if (isCompact) 34.dp else 44.dp
    val trackHeight = if (isCompact) 10.dp else 14.dp
    val trackCorner = if (isCompact) 5.dp else 7.dp
    val thumbSize = if (isCompact) 22.dp else 28.dp
    val timeFontSize = if (isCompact) 13.sp else 15.sp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isCompact) 4.dp else 6.dp)
    ) {
        // Dokunmatik ve Sürüklenebilir Kalın İlerleme Çubuğu Alanı
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeekToProgress(newProgress)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeekToProgress(dragFraction)
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val totalWidthPx = constraints.maxWidth.toFloat()
            val thumbRadiusPx = with(density) { (thumbSize / 2).toPx() }
            val effectiveWidthPx = (totalWidthPx - thumbRadiusPx * 2).coerceAtLeast(1f)
            val thumbOffsetXPx = (activeFraction * effectiveWidthPx)

            // 1. Zemin Arka Plan Çubuğu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(RoundedCornerShape(trackCorner))
                    .background(Color(0xFF1E222B))
                    .border(1.dp, CarBorder, RoundedCornerShape(trackCorner))
            )

            // 2. Aktif İlerleme Çubuğu (Dinamik Renk Gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = activeFraction.coerceIn(0.01f, 1f))
                    .height(trackHeight)
                    .clip(RoundedCornerShape(trackCorner))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.7f),
                                accentColor,
                                accentColor.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // 3. Büyük Dokunmatik Tutamaç / Düğme (Thumb)
            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbOffsetXPx.roundToInt(), 0) }
                    .size(thumbSize)
                    .shadow(elevation = if (isCompact) 4.dp else 6.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(if (isCompact) 3.dp else 4.dp, accentColor, CircleShape)
            )
        }

        // Zaman Bilgileri (Büyük ve Net Yazı Tipi)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentFormatted,
                fontSize = timeFontSize,
                fontWeight = FontWeight.Bold,
                color = CarTextPrimary
            )

            Text(
                text = durationFormatted,
                fontSize = timeFontSize,
                fontWeight = FontWeight.Bold,
                color = CarTextSecondary
            )
        }
    }
}




















