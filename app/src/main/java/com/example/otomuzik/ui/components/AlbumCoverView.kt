package com.example.otomuzik.ui.components
import com.example.otomuzik.theme.*

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otomuzik.R



import kotlin.math.roundToInt

@Composable
fun albumCoverView(
    isPlaying: Boolean,
    artworkBitmap: Bitmap?,
    size: Dp = 360.dp,
    onSwipeNext: () -> Unit = {},
    onSwipePrevious: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentOnSwipeNext by rememberUpdatedState(onSwipeNext)
    val currentOnSwipePrevious by rememberUpdatedState(onSwipePrevious)

    // Sağa - Sola kaydrma (Horizontal Swipe) algılama ve Parmak Takip Efekti
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 50.dp.toPx() }

    val animatedOffset by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "SwipeOffset"
    )

    val swipeModifier = Modifier
        .offset { IntOffset(animatedOffset.roundToInt(), 0) }
        .pointerInput(density) {
            detectHorizontalDragGestures(
                onDragStart = {
                    isDragging = true
                    dragOffsetX = 0f
                },
                onDragEnd = {
                    if (dragOffsetX < -swipeThresholdPx) {
                        // Sola kaydrdı -> Sonraki şarkı
                        currentOnSwipeNext()
                    } else if (dragOffsetX > swipeThresholdPx) {
                        // Sağa kaydrdı -> Önceki şarkı
                        currentOnSwipePrevious()
                    }
                    dragOffsetX = 0f
                    isDragging = false
                },
                onDragCancel = {
                    dragOffsetX = 0f
                    isDragging = false
                },
                onHorizontalDrag = { change, dragamount ->
                    change.consume()
                    // Parmakla takip (maksimum 140px esneme)
                    dragOffsetX = (dragOffsetX + dragamount * 0.75f).coerceIn(-140f, 140f)
                }
            )
        }

    Crossfade(targetState = (artworkBitmap != null), label = "CoverCrossfade") { hasartwork ->
        if (hasartwork && artworkBitmap != null) {
            // 1. MP3 Gömülü Kapak Resmi (%100 Büyütülmüş Dev Kare Kart, Sağa/Sola kaydrmalı)
            Box(
                modifier = modifier
                    .then(swipeModifier)
                    .size(size)
                    .shadow(elevation = if (isPlaying) 12.dp else 4.dp, shape = RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .background(CarSurfaceVariant)
                    .border(
                        width = if (isPlaying) 2.5.dp else 1.5.dp,
                        color = if (isPlaying) CarCyan else CarBorder,
                        shape = RoundedCornerShape(26.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = artworkBitmap.asImageBitmap(),
                    contentDescription = "albüm Kapağı",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Sola kaydrırken: "SONRaKİ" Rozeti (Sağ kenarda parıldar)
                if (isDragging && dragOffsetX < -25f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xDD00E5FF))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "SONRAKİ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0C0E12)
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_skip_next),
                                contentDescription = "Sonraki",
                                tint = Color(0xFF0C0E12),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else if (isDragging && dragOffsetX > 25f) {
                    // Sağa kaydırırken: "Önceki" Rozeti (Sol kenarda parıldar)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xDD00E5FF))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_skip_previous),
                                contentDescription = "Önceki",
                                tint = Color(0xFF0C0E12),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Önceki",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0C0E12)
                            )
                        }
                    }
                }

                // Çalarken sağ altta canlı çalan rozeti
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xDD0C0E12))
                            .border(1.dp, CarCyan, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "▶ ÇALIYOR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CarCyan
                        )
                    }
                }
            }
        } else {
            // 2. Dönen Vinil Plak Görünümü (Kapak gömülü değilse, Sağa/Sola kaydırmalı)
            Box(
                modifier = modifier
                    .then(swipeModifier),
                contentAlignment = Alignment.Center
            ) {
                VinylDisc(
                    isPlaying = isPlaying,
                    artworkBitmap = artworkBitmap,
                    size = size
                )

                // Vinil üzerinde de kaydırma ipucu rozetleri
                if (isDragging && dragOffsetX < -25f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xDD00E5FF))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "SONRAKİ ▶▶",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0C0E12)
                        )
                    }
                } else if (isDragging && dragOffsetX > 25f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xDD00E5FF))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "◀◀ Önceki",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0C0E12)
                        )
                    }
                }
            }
        }
    }
}


















