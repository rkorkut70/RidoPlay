package com.example.otomuzik.ui.phone

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.otomuzik.ui.components.CoverLyricsOverlay
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
import com.example.otomuzik.ui.components.WaveformVisualizer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.otomuzik.R
import com.example.otomuzik.ui.components.PlaybackQueueView
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
    onPlayMoreFromArtist: (() -> Unit)? = null,
    onOpenLyrics: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenId3Info: () -> Unit,
    onOpenSettings: () -> Unit = {},
    currentQueue: List<Song> = emptyList(),
    currentQueueIndex: Int = -1,
    onPlayQueueIndex: (Int) -> Unit = {},
    onReshuffleUpcoming: () -> Unit = {},
    swipeSensitivity: String = "HIGH",
    playbackSpeed: Float = 1.0f,
    onSetPlaybackSpeed: (Float) -> Unit = {},
    showWaveform: Boolean = true,
    lyricsData: com.example.otomuzik.model.LyricsData? = null,
    showCoverLyrics: Boolean = false,
    onToggleCoverLyrics: () -> Unit = {},
    lyricsFontSize: String = "LARGE",
    onDismiss: () -> Unit
) {
    if (song == null) return

    var showQueueOverlay by remember { mutableStateOf(false) }

    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)

    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var isDraggingArtwork by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val swipeThresholdDp = when (swipeSensitivity) {
        "VERY_HIGH" -> 22.dp
        "HIGH" -> 35.dp
        "NORMAL" -> 50.dp
        "LOW" -> 75.dp
        else -> 35.dp
    }
    val swipeThresholdPx = with(density) { swipeThresholdDp.toPx() }

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
        com.example.otomuzik.theme.OtoMuzikTheme {
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

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sol Taraf: AlbÃ¼m KapaÄŸÄ± & BaÅŸlÄ±k
                    Column(
                        modifier = Modifier
                            .weight(0.40f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val landscapeCoverShape = RoundedCornerShape(20.dp)
                        val landscapeHaptic = LocalHapticFeedback.current

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                                .shadow(
                                    elevation = 20.dp,
                                    shape = landscapeCoverShape,
                                    ambientColor = Color.Black.copy(alpha = 0.70f),
                                    spotColor = Color.Black.copy(alpha = 0.85f)
                                )
                                .clip(landscapeCoverShape)
                                .background(CarSurfaceDark)
                                .border(
                                    width = 2.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.45f),
                                            accentColor.copy(alpha = 0.40f),
                                            Color(0xFF1E2330),
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    ),
                                    shape = landscapeCoverShape
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            try {
                                                landscapeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } catch (_: Exception) {}
                                            onToggleCoverLyrics()
                                        }
                                    )
                                }
                                .then(
                                    if (!showCoverLyrics) {
                                        Modifier.pointerInput(density, swipeThresholdPx) {
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
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showCoverLyrics) {
                                if (lyricsData != null && lyricsData.lines.isNotEmpty()) {
                                    CoverLyricsOverlay(
                                        lyricsData = lyricsData,
                                        currentPositionMs = currentPositionMs,
                                        durationMs = durationMs,
                                        isPlaying = isPlaying,
                                        accentColor = accentColor,
                                        isCompact = true,
                                        lyricsFontSize = lyricsFontSize,
                                        onSeekTo = onSeekTo,
                                        onOpenLyricsDialog = onOpenLyrics,
                                        onToggleCoverLyrics = onToggleCoverLyrics,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF0C101A).copy(alpha = 0.94f))
                                            .clickable { onToggleCoverLyrics() }
                                            .padding(14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(text = "🎵", fontSize = 28.sp)
                                            Text(
                                                text = "Bu şarkı için söz bulunamadı",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(accentColor.copy(alpha = 0.25f))
                                                    .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                                                    .clickable { onToggleCoverLyrics() }
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "🖼️ Kapağa Dön",
                                                    color = accentColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
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
                                        modifier = Modifier.size(64.dp)
                                    )
                                }

                                // 3D cam ışıltısı katmanı
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.40f)
                                        .align(Alignment.TopCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                                            )
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = com.example.otomuzik.util.TurkishStringFixer.fix(song.title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = com.example.otomuzik.util.TurkishStringFixer.fix(song.artist),
                            fontSize = 12.sp,
                            color = Color(0xFFA5B0C2),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Sağ Taraf: Kontroller, Slider & Butonlar
                    Column(
                        modifier = Modifier
                            .weight(0.60f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Üst Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp).background(CarSurfaceDark, CircleShape)
                            ) {
                                Text(text = "▼", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = onToggleFavorite,
                                    modifier = Modifier.size(36.dp).background(CarSurfaceDark, CircleShape)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_favorite),
                                        contentDescription = "Favori",
                                        tint = if (isFavorite) CarAmber else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val next = when {
                                            playbackSpeed <= 0.85f -> 1.0f
                                            playbackSpeed <= 1.1f -> 1.25f
                                            playbackSpeed <= 1.35f -> 1.5f
                                            else -> 0.8f
                                        }
                                        onSetPlaybackSpeed(next)
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .shadow(3.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF282E3E), Color(0xFF141720))
                                            ),
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            Brush.verticalGradient(
                                                listOf(Color.White.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                            ),
                                            CircleShape
                                        )
                                ) {
                                    Text(
                                        text = if (playbackSpeed == 1.0f) "1x" else "${playbackSpeed}x",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (playbackSpeed != 1.0f) accentColor else Color.White
                                    )
                                }
                                IconButton(
                                    onClick = onOpenEqualizer,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .shadow(3.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF282E3E), Color(0xFF141720))
                                            ),
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            Brush.verticalGradient(
                                                listOf(Color.White.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                            ),
                                            CircleShape
                                        )
                                ) {
                                    Text(text = "EQ", fontSize = 11.sp, fontWeight = FontWeight.Black, color = accentColor)
                                }
                                IconButton(
                                    onClick = onOpenId3Info,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .shadow(3.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF282E3E), Color(0xFF141720))
                                            ),
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            Brush.verticalGradient(
                                                listOf(Color.White.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                            ),
                                            CircleShape
                                        )
                                ) {
                                    Text(text = "ℹ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                IconButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .shadow(3.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF282E3E), Color(0xFF141720))
                                            ),
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            Brush.verticalGradient(
                                                listOf(Color.White.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                            ),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_settings),
                                        contentDescription = "Ayarlar",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Canlı Ses Dalgası (Waveform)
                        if (showWaveform) {
                            WaveformVisualizer(
                                isPlaying = isPlaying,
                                accentColor = accentColor,
                                height = 26.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            )
                        }

                        // Scrubber Slider
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
                                    inactiveTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val posText = formatTime(if (isDraggingSlider) (sliderPosition * durationMs).toLong() else currentPositionMs)
                                val durText = formatTime(durationMs)
                                Text(text = posText, fontSize = 11.sp, color = Color(0xFFA5B0C2))
                                Text(text = durText, fontSize = 11.sp, color = Color(0xFFA5B0C2))
                            }
                        }

                        // Oynatma Butonları
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onToggleShuffle, modifier = Modifier.size(38.dp)) {
                                Icon(painter = painterResource(id = R.drawable.ic_shuffle), contentDescription = "Karıştır", tint = if (isShuffle) accentColor else Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = onPrevious,
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF282E3E), Color(0xFF141720))
                                        )
                                    )
                                    .border(1.dp, Color(0x44FFFFFF), CircleShape)
                            ) {
                                Icon(painter = painterResource(id = R.drawable.ic_skip_previous), contentDescription = "Önceki", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            IconButton(
                                onClick = onPlayPause,
                                modifier = Modifier
                                    .size(56.dp)
                                    .shadow(10.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(accentColor, accentColor.copy(alpha = 0.85f))
                                        )
                                    )
                                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(painter = painterResource(id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play), contentDescription = if (isPlaying) "Duraklat" else "Oynat", tint = Color(0xFF0C0E12), modifier = Modifier.size(30.dp))
                            }
                            IconButton(
                                onClick = onNext,
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF282E3E), Color(0xFF141720))
                                        )
                                    )
                                    .border(1.dp, Color(0x44FFFFFF), CircleShape)
                            ) {
                                Icon(painter = painterResource(id = R.drawable.ic_skip_next), contentDescription = "Sonraki", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            IconButton(onClick = onToggleRepeat, modifier = Modifier.size(38.dp)) {
                                val iconRes = when (repeatMode) { RepeatMode.ONE -> R.drawable.ic_repeat_one else -> R.drawable.ic_repeat }
                                Icon(painter = painterResource(id = iconRes), contentDescription = "Tekrar", tint = if (repeatMode != RepeatMode.OFF) accentColor else Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                            }
                        }

                        // Sözler & Sıra Butonları
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF282E3E), Color(0xFF141720))
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onOpenLyrics() }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🎤", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Sözler", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier
                                    .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (showQueueOverlay) {
                                            Brush.verticalGradient(
                                                listOf(accentColor.copy(alpha = 0.35f), accentColor.copy(alpha = 0.15f))
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF282E3E), Color(0xFF141720))
                                            )
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        Brush.verticalGradient(
                                            listOf(
                                                if (showQueueOverlay) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.6f)
                                            )
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { showQueueOverlay = true }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(painter = painterResource(id = R.drawable.ic_queue), contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Sıra", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
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
                            .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF282E3E), Color(0xFF141720))
                                ),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                ),
                                CircleShape
                            )
                    ) {
                        Text(text = "▼", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                val next = when {
                                    playbackSpeed <= 0.85f -> 1.0f
                                    playbackSpeed <= 1.1f -> 1.25f
                                    playbackSpeed <= 1.35f -> 1.5f
                                    else -> 0.8f
                                }
                                onSetPlaybackSpeed(next)
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF282E3E), Color(0xFF141720))
                                    ),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                    ),
                                    CircleShape
                                )
                        ) {
                            Text(
                                text = if (playbackSpeed == 1.0f) "1x" else "${playbackSpeed}x",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (playbackSpeed != 1.0f) accentColor else Color.White
                            )
                        }

                        IconButton(
                            onClick = onOpenEqualizer,
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF282E3E), Color(0xFF141720))
                                    ),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                    ),
                                    CircleShape
                                )
                        ) {
                            Text(text = "EQ", fontSize = 13.sp, fontWeight = FontWeight.Black, color = accentColor)
                        }

                        IconButton(
                            onClick = onOpenId3Info,
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF282E3E), Color(0xFF141720))
                                    ),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                    ),
                                    CircleShape
                                )
                        ) {
                            Text(text = "ℹ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF282E3E), Color(0xFF141720))
                                    ),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                    ),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = "Ayarlar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // ── 2. BÜYÜK ALBÜM KAPAĞI (3D derinlik & basılı tutunca şarkı sözü geçişi) ────────
                val coverShape = RoundedCornerShape(24.dp)
                val haptic = LocalHapticFeedback.current

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                        .shadow(
                            elevation = 22.dp,
                            shape = coverShape,
                            ambientColor = Color.Black.copy(alpha = 0.70f),
                            spotColor = Color.Black.copy(alpha = 0.85f)
                        )
                        .clip(coverShape)
                        .background(CarSurfaceDark)
                        .border(
                            width = 2.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.45f),
                                    accentColor.copy(alpha = 0.40f),
                                    Color(0xFF1E2330),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            ),
                            shape = coverShape
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } catch (_: Exception) {}
                                    onToggleCoverLyrics()
                                }
                            )
                        }
                        .then(
                            if (!showCoverLyrics) {
                                Modifier.pointerInput(density, swipeThresholdPx) {
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
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (showCoverLyrics) {
                        if (lyricsData != null && lyricsData.lines.isNotEmpty()) {
                            CoverLyricsOverlay(
                                lyricsData = lyricsData,
                                currentPositionMs = currentPositionMs,
                                durationMs = durationMs,
                                isPlaying = isPlaying,
                                accentColor = accentColor,
                                isCompact = false,
                                lyricsFontSize = lyricsFontSize,
                                onSeekTo = onSeekTo,
                                onOpenLyricsDialog = onOpenLyrics,
                                onToggleCoverLyrics = onToggleCoverLyrics,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF0C101A).copy(alpha = 0.94f))
                                    .clickable { onToggleCoverLyrics() }
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "🎵", fontSize = 36.sp)
                                    Text(
                                        text = "Bu şarkı için söz bulunamadı",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Kapağa dönmek için dokunun veya basılı tutun",
                                        color = CarTextMuted,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(accentColor.copy(alpha = 0.25f))
                                            .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                                            .clickable { onToggleCoverLyrics() }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "🖼️ Kapağa Dön",
                                            color = accentColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
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

                        // Üst dışbükey cam ışıltısı katmanı (3D gloss efekti)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.40f)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                                    )
                                )
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
                            text = com.example.otomuzik.util.TurkishStringFixer.fix(song.title),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🎙️ ${com.example.otomuzik.util.TurkishStringFixer.fix(song.artist)} • ${com.example.otomuzik.util.TurkishStringFixer.fix(song.album)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (onPlayMoreFromArtist != null) accentColor.copy(alpha = 0.95f) else Color(0xFFA5B0C2),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = if (onPlayMoreFromArtist != null) {
                                Modifier.clickable { onPlayMoreFromArtist() }
                            } else Modifier
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onPlayMoreFromArtist != null && song.artist.isNotBlank() && song.artist != "Bilinmeyen Sanatçı") {
                            IconButton(
                                onClick = onPlayMoreFromArtist,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(CarSurfaceDark, CircleShape)
                            ) {
                                Text(text = "🎙️", fontSize = 20.sp)
                            }
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
                                tint = if (isFavorite) CarAmber else Color(0xFF94A3B8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Canlı Ses Dalgası (Waveform)
                if (showWaveform) {
                    WaveformVisualizer(
                        isPlaying = isPlaying,
                        accentColor = accentColor,
                        height = 34.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
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
                            inactiveTrackColor = Color(0xFF334155)
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
                        Text(text = posText, fontSize = 12.sp, color = Color(0xFFA5B0C2))
                        Text(text = durText, fontSize = 12.sp, color = Color(0xFFA5B0C2))
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
                            tint = if (isShuffle) accentColor else Color(0xFF94A3B8),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Önceki Parça
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = 0.5f),
                                spotColor = Color.Black.copy(alpha = 0.7f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF282E3E), Color(0xFF141720))
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.40f),
                                        Color(0xFF282E3E),
                                        Color.Black.copy(alpha = 0.70f)
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_skip_previous),
                            contentDescription = "Önceki",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Dev Oynat / Duraklat Butonu (3D Embossed Dial)
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(
                                elevation = 14.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = 0.6f),
                                spotColor = Color.Black.copy(alpha = 0.85f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        accentColor,
                                        accentColor.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .border(
                                width = 2.5.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.65f),
                                        accentColor,
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                ),
                                shape = CircleShape
                            )
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
                            .shadow(
                                elevation = 6.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = 0.5f),
                                spotColor = Color.Black.copy(alpha = 0.7f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF282E3E), Color(0xFF141720))
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.40f),
                                        Color(0xFF282E3E),
                                        Color.Black.copy(alpha = 0.70f)
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_skip_next),
                            contentDescription = "Sonraki",
                            tint = Color.White,
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
                            tint = if (repeatMode != RepeatMode.OFF) accentColor else Color(0xFF94A3B8),
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
                            .shadow(4.dp, RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF282E3E), Color(0xFF141720))
                                )
                            )
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onOpenLyrics() }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎤", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Şarkı Sözleri", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Çalma Sırası
                    Row(
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (showQueueOverlay) {
                                    Brush.verticalGradient(
                                        listOf(accentColor.copy(alpha = 0.35f), accentColor.copy(alpha = 0.15f))
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF282E3E), Color(0xFF141720))
                                    )
                                }
                            )
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        if (showQueueOverlay) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { showQueueOverlay = true }
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
                        Text(text = "Çalma Sırası", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            }

            // ── Çalma Sırası Tam Ekran Katmanı (Modern Slide-Up Overlay) ──
            AnimatedVisibility(
                visible = showQueueOverlay,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BaseCarBgDark)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    PlaybackQueueView(
                        queue = currentQueue,
                        currentIndex = currentQueueIndex,
                        isShuffle = isShuffle,
                        accentColor = accentColor,
                        onPlayAtIndex = { idx ->
                            onPlayQueueIndex(idx)
                        },
                        onPlayNext = onNext,
                        onPlayPrevious = onPrevious,
                        onToggleShuffle = onToggleShuffle,
                        onReshuffleUpcoming = onReshuffleUpcoming,
                        onClose = { showQueueOverlay = false },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
}
