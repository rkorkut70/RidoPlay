package com.example.otomuzik.ui.phone

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otomuzik.R
import com.example.otomuzik.model.Song
import com.example.otomuzik.theme.BaseCarCyan
import com.example.otomuzik.theme.CarAmber
import com.example.otomuzik.theme.CarBorder
import com.example.otomuzik.theme.CarSurfaceDark
import com.example.otomuzik.theme.CarSurfaceVariant
import com.example.otomuzik.theme.CarTextMuted
import com.example.otomuzik.theme.CarTextPrimary

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * Cep telefonu modunda ekranın altında duran modern Mini Oynatıcı barı.
 * Dokunulduğunda tam ekran çalan şarkı ekranını açar. Sağa/sola kaydırınca şarkı değiştirir.
 */
@Composable
fun PhoneMiniPlayer(
    currentSong: Song?,
    isPlaying: Boolean,
    artworkBitmap: Bitmap?,
    isFavorite: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    swipeSensitivity: String = "HIGH",
    modifier: Modifier = Modifier
) {
    if (currentSong == null) return

    val progress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)

    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var totalDrag by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val swipeThresholdDp = when (swipeSensitivity) {
        "VERY_HIGH" -> 22.dp
        "HIGH" -> 35.dp
        "NORMAL" -> 50.dp
        "LOW" -> 75.dp
        else -> 35.dp
    }
    val swipeThresholdPx = with(density) { swipeThresholdDp.toPx() }

    val animatedOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.75f),
        label = "MiniPlayerSwipeOffsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.75f),
        label = "MiniPlayerSwipeOffsetY"
    )

    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
            .clip(shape)
            .background(CarSurfaceDark.copy(alpha = 0.96f))
            .border(1.2.dp, accentColor.copy(alpha = 0.45f), shape)
            .pointerInput(density, swipeThresholdPx) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                        totalDrag = 0f
                    },
                    onDragEnd = {
                        val swipeUpThresholdPx = (swipeThresholdPx * 0.8f).coerceAtLeast(28f)
                        when {
                            // 1. Yukarı sürükleme (Mini oynatıcıyı yukarı çekince tam ekran oynatıcı açılır)
                            dragOffsetY < -swipeUpThresholdPx && kotlin.math.abs(dragOffsetY) >= kotlin.math.abs(dragOffsetX) -> {
                                onClick()
                            }
                            // 2. Sağa/Sola sürükleme (Şarkı geçişi)
                            dragOffsetX < -swipeThresholdPx -> currentOnNext()
                            dragOffsetX > swipeThresholdPx  -> currentOnPrevious()
                            // 3. Dokunma (Tap)
                            totalDrag < 15f -> onClick()
                        }
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                        isDragging = false
                    },
                    onDragCancel = {
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += kotlin.math.abs(dragAmount.x) + kotlin.math.abs(dragAmount.y)
                        dragOffsetX = (dragOffsetX + dragAmount.x).coerceIn(-180f, 180f)
                        dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(-240f, 20f)
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Yukarı çekme / sürükleme tutacağı (Swipe up handle indicator)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp, bottom = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 3.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.5f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Albüm Kapağı Küçük Resmi (Thumbnail)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (artworkBitmap != null) {
                        Image(
                            bitmap = artworkBitmap.asImageBitmap(),
                            contentDescription = "Kapak",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_music_note),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 2. Şarkı Başlığı ve Sanatçı
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = com.example.otomuzik.util.TurkishStringFixer.fix(currentSong.title),
                        color = Color(0xFFFFFFFF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = com.example.otomuzik.util.TurkishStringFixer.fix(currentSong.artist),
                        color = CarTextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3. Hızlı Favori Butonu
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_favorite),
                        contentDescription = "Favori",
                        tint = if (isFavorite) CarAmber else CarTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 4. Oynat / Duraklat Butonu
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(42.dp)
                        .background(accentColor, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                        tint = Color(0xFF0C0E12),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 5. Sonraki Şarkı Butonu
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_next),
                        contentDescription = "Sonraki",
                        tint = CarTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // En altta ince ilerleme çizgisi
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = accentColor,
                trackColor = Color.Transparent
            )
        }
    }
}
