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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.otomuzik.R
import com.example.otomuzik.model.RepeatMode
import com.example.otomuzik.model.Song
import com.example.otomuzik.theme.BaseCarBgDark
import com.example.otomuzik.theme.BaseCarCyan
import com.example.otomuzik.theme.CarAmber
import com.example.otomuzik.theme.CarBorder
import com.example.otomuzik.theme.CarSurfaceDark
import com.example.otomuzik.theme.CarSurfaceVariant
import com.example.otomuzik.theme.CarTextMuted
import com.example.otomuzik.theme.CarTextPrimary
private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}

/**
 * Cep telefonu modunda Mini-Player'a dokunulduğunda açılan tam ekran çalan parça ekranı.
 */
@Composable
fun PhoneNowPlayingSheet(
    song: Song?,
    isPlaying: Boolean,
    artworkBitmap: Bitmap?,
    isFavorite: Boolean,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    currentPositionMs: Long,
    durationMs: Long,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenId3Info: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onDismiss: () -> Unit
) {
    if (song == null) return

    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)

    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var isDraggingArtwork by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 55.dp.toPx() }

    val animatedOffset by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "ArtworkSwipeOffset"
    )

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    val currentSliderVal = if (isDraggingSlider) {
        sliderPosition
    } else {
        if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BaseCarBgDark)
        ) {
            // Arka planda kapağın yumuşak renk ışıması
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.20f), Color.Transparent),
                            radius = 800f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── 1. ÜST BAR ──────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .background(CarSurfaceDark, CircleShape)
                    ) {
                        Text(text = "▼", fontSize = 16.sp, color = CarTextPrimary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ŞU AN ÇALIYOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarTextMuted,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = song.folderPath.substringAfterLast("/").ifEmpty { "RidoPlay" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onOpenEqualizer,
                            modifier = Modifier
                                .size(42.dp)
                                .background(CarSurfaceDark, CircleShape)
                        ) {
                            Text(text = "EQ", fontSize = 13.sp, fontWeight = FontWeight.Black, color = accentColor)
                        }

                        IconButton(
                            onClick = onOpenId3Info,
                            modifier = Modifier
                                .size(42.dp)
                                .background(CarSurfaceDark, CircleShape)
                        ) {
                            Text(text = "ℹ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CarTextPrimary)
                        }

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(42.dp)
                                .background(CarSurfaceDark, CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = "Ayarlar",
                                tint = CarTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // ── 2. BÜYÜK ALBÜM KAPAĞI (Kaydırınca şarkı değiştirir) ────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                        .clip(RoundedCornerShape(24.dp))
                        .background(CarSurfaceDark)
                        .border(1.5.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .pointerInput(density) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    isDraggingArtwork = true
                                    dragOffsetX = 0f
                                },
                                onDragEnd = {
                                    when {
                                        dragOffsetX < -swipeThresholdPx -> currentOnNext()
                                        dragOffsetX > swipeThresholdPx  -> currentOnPrevious()
                                    }
                                    dragOffsetX = 0f
                                    isDraggingArtwork = false
                                },
                                onDragCancel = {
                                    dragOffsetX = 0f
                                    isDraggingArtwork = false
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX = (dragOffsetX + dragAmount).coerceIn(-240f, 240f)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (artworkBitmap != null) {
                        Image(
                            bitmap = artworkBitmap.asImageBitmap(),
                            contentDescription = "Albüm Kapağı",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_music_note),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(96.dp)
                        )
                    }
                }

                // ── 3. ŞARKI BİLGİSİ & FAVORİ ─────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = CarTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${song.artist} • ${song.album}",
                            fontSize = 14.sp,
                            color = CarTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .background(CarSurfaceDark, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_favorite),
                            contentDescription = "Favori",
                            tint = if (isFavorite) CarAmber else CarTextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // ── 4. İLERLEME ÇUBUĞU (SCRUBBER) ─────────────────────────────
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = currentSliderVal,
                        onValueChange = {
                            isDraggingSlider = true
                            sliderPosition = it
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            onSeekTo((sliderPosition * durationMs).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = CarBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val posText = formatTime(
                            if (isDraggingSlider) (sliderPosition * durationMs).toLong() else currentPositionMs
                        )
                        val durText = formatTime(durationMs)
                        Text(text = posText, fontSize = 12.sp, color = CarTextMuted)
                        Text(text = durText, fontSize = 12.sp, color = CarTextMuted)
                    }
                }

                // ── 5. ÇALMA KONTROL TUŞLARI ──────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Karıştır (Shuffle)
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shuffle),
                            contentDescription = "Karıştır",
                            tint = if (isShuffle) accentColor else CarTextMuted,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Önceki Parça
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(54.dp)
                            .background(CarSurfaceDark, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_skip_previous),
                            contentDescription = "Önceki",
                            tint = CarTextPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Dev Oynat / Duraklat Butonu
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(72.dp)
                            .background(accentColor, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                            contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                            tint = Color(0xFF0C0E12),
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Sonraki Parça
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(54.dp)
                            .background(CarSurfaceDark, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_skip_next),
                            contentDescription = "Sonraki",
                            tint = CarTextPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Tekrar (Repeat)
                    IconButton(onClick = onToggleRepeat) {
                        val iconRes = when (repeatMode) {
                            RepeatMode.ONE -> R.drawable.ic_repeat_one
                            else -> R.drawable.ic_repeat
                        }
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = "Tekrar",
                            tint = if (repeatMode != RepeatMode.OFF) accentColor else CarTextMuted,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // ── 6. ALT EKSTRA BUTONLAR (Sözler & Sıra) ────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Şarkı Sözleri
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(CarSurfaceDark)
                            .clickable { onOpenLyrics() }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎤", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Şarkı Sözleri", color = CarTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Çalma Sırası
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(CarSurfaceDark)
                            .clickable { onOpenQueue() }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_queue),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Çalma Sırası", color = CarTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
