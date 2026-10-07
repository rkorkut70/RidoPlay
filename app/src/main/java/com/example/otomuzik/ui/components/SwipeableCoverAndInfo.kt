package com.example.otomuzik.ui.components
import com.example.otomuzik.theme.*
import com.example.otomuzik.util.TurkishStringFixer

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.zIndex
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import com.example.otomuzik.model.LyricsData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otomuzik.R



import com.example.otomuzik.theme.CarTextMuted


import kotlin.math.roundToInt

/**
 * albüm kapağı + şarkı başlığı + Sanatçı bilgisini tek bir
 * yatay kaydrma (swipe) alanı olarak sunar.
 *
 * Kullanıcı kapak resminde VEYa şarkı bilgisinde sağa/sola
 * kaydrarak parça Deitirebilir — ikisi birlikte hareket eder.
 *
 * @param isPlaying         Mzik çÇalIYOR mu?
 * @param artworkBitmap     albüm kapağı bitmap (null → VinylDisc gösterilir)
 * @param currentSongId3    ID3 metadata (başlık/Sanatçı için)
 * @param songTitle         Fallback şarkı adı
 * @param songartist        Fallback Sanatçı adı
 * @param badgeText         ID3 teknik rozet metni (MP3 • 320kbps • Pop)
 * @param coverSize         Kapak boyutu
 * @param onSwipeNext       Sola kaydrıldığında çağrılır → Sonraki şarkı
 * @param onSwipePrevious   Sağa kaydrıldığında çağrılır → Önceki şarkı
 * @param onInfoBadgeClick  Rozete tıklandığında çağrılır → ID3 detay penceresi
 * @param modifier          Compose modifier
 */
@Composable
fun SwipeableCoverandInfo(
    isPlaying: Boolean,
    artworkBitmap: Bitmap?,
    currentSongId3: com.example.otomuzik.util.Id3Metadata?,
    songTitle: String,
    songartist: String,
    badgeText: String,
    accentColor: Color = CarCyan,       // ← dinamik aksan rengi
    coverSize: Dp = 260.dp,
    lyricsData: com.example.otomuzik.model.LyricsData? = null,
    showCoverLyrics: Boolean = false,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    onSeekTo: (Long) -> Unit = {},
    onOpenLyricsDialog: () -> Unit = {},
    onSwipeNext: () -> Unit = {},
    onSwipePrevious: () -> Unit = {},
    onSeekForward: () -> Unit = {},
    onSeekBackward: () -> Unit = {},
    onInfoBadgeClick: () -> Unit = {},
    isalbumartCropped: Boolean = false,
    lyricsFontSize: String = "LARGE",
    modifier: Modifier = Modifier
) {
    val hasLyrics = lyricsData != null && lyricsData.lines.isNotEmpty()
    val isCoverLyricsActive = showCoverLyrics && hasLyrics

    val currentOnSwipeNext by rememberUpdatedState(onSwipeNext)
    val currentOnSwipePrevious by rememberUpdatedState(onSwipePrevious)
    val currentOnSeekForward by rememberUpdatedState(onSeekForward)
    val currentOnSeekBackward by rememberUpdatedState(onSeekBackward)

    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    // kaydrma eşiği: 55dp — hem hassas hem de kazara tetiklenmeye karşı korumalı
    val swipeThresholdPx = with(density) { 55.dp.toPx() }

    val animatedOffset by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "SwipeOffset"
    )

    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var volumeDragamount by remember { mutableFloatStateOf(0f) }

    val handleVolumeDrag: (Float) -> Unit = { dragamount ->
        if (dragamount == 0f) {
            volumeDragamount = 0f
        } else {
            volumeDragamount += dragamount
            if (volumeDragamount > 28f) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                volumeDragamount = 0f
            } else if (volumeDragamount < -28f) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                volumeDragamount = 0f
            }
        }
    }

    // tm içerik tek bir swipe algılayıcı ile sarılır
    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2) {
                            currentOnSeekBackward()
                        } else {
                            currentOnSeekForward()
                        }
                    }
                )
            }
            .then(
                if (!isCoverLyricsActive) {
                    Modifier.pointerInput(Unit) {
                        var isVolumeGestureAllowed = false
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                // Ses kontrolü YALNIZCA sol alanda (%40) çalışır; sağ taraf tamamen devre dışıdır.
                                // Böylece sonraki şarkıya geçmek için sağdan sola kaydırırken ani ses patlaması yaşanmaz.
                                isVolumeGestureAllowed = offset.x < size.width * 0.40f && !isDragging
                                if (!isVolumeGestureAllowed) {
                                    volumeDragamount = 0f
                                }
                            },
                            onVerticalDrag = { change, dragamount ->
                                if (isVolumeGestureAllowed && !isDragging) {
                                    change.consume()
                                    handleVolumeDrag(dragamount)
                                }
                            },
                            onDragEnd = {
                                volumeDragamount = 0f
                                isVolumeGestureAllowed = false
                            },
                            onDragCancel = {
                                volumeDragamount = 0f
                                isVolumeGestureAllowed = false
                            }
                        )
                    }
                } else Modifier
            )
            .pointerInput(density) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragOffsetX = 0f
                    },
                    onDragEnd = {
                        when {
                            dragOffsetX < -swipeThresholdPx -> currentOnSwipeNext()
                            dragOffsetX > swipeThresholdPx  -> currentOnSwipePrevious()
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
                        // Parmakla esnek takip (maks ±150px)
                        dragOffsetX = (dragOffsetX + dragamount * 0.75f).coerceIn(-150f, 150f)
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

        if (isalbumartCropped && artworkBitmap != null) {
            // ── TAM EKRAN KAPAK MODU ──────────────────────────────────────────────
            // Kapak resmi tüm NowPlayingPanel arka planına yayıldı.
            // Orta alanda şarkı adı, sanatçı ve rozet yarı şeffaf zarif bir kart içinde
            // kapağın üzerinde yüzer. Swipe, double-tap ve dikey ses jestleri aynen çalışır.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isCoverLyricsActive && lyricsData != null) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val leftVolumeStripWidth = if (isCompact) 44.dp else 54.dp
                        val rightPadding = if (isCompact) 6.dp else 12.dp
                        val cardWidth = (maxWidth - leftVolumeStripWidth - rightPadding).coerceAtLeast(140.dp)
                        val cardHeight = maxHeight * 0.94f

                        VolumeControlStrip(
                            isCompact = isCompact,
                            accentColor = accentColor,
                            onVolumeDrag = handleVolumeDrag,
                            onVolumeRaise = {
                                audioManager.adjustStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    AudioManager.ADJUST_RAISE,
                                    AudioManager.FLAG_SHOW_UI
                                )
                            },
                            onVolumeLower = {
                                audioManager.adjustStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    AudioManager.ADJUST_LOWER,
                                    AudioManager.FLAG_SHOW_UI
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(leftVolumeStripWidth)
                                .height(cardHeight)
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = rightPadding)
                                .size(width = cardWidth, height = cardHeight)
                                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                                .clip(RoundedCornerShape(if (isCompact) 16.dp else 22.dp))
                                .border(
                                    width = 1.2.dp,
                                    color = accentColor.copy(alpha = 0.60f),
                                    shape = RoundedCornerShape(if (isCompact) 16.dp else 22.dp)
                                )
                        ) {
                            CoverLyricsOverlay(
                                lyricsData = lyricsData,
                                currentPositionMs = currentPositionMs,
                                durationMs = durationMs,
                                isPlaying = isPlaying,
                                accentColor = accentColor,
                                isCompact = isCompact,
                                lyricsFontSize = lyricsFontSize,
                                onSeekTo = onSeekTo,
                                onOpenLyricsDialog = onOpenLyricsDialog
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                            .padding(horizontal = if (isCompact) 4.dp else 6.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.50f),
                                shape = RoundedCornerShape(if (isCompact) 16.dp else 20.dp)
                            )
                            .border(
                                width = 1.2.dp,
                                color = accentColor.copy(alpha = 0.50f),
                                shape = RoundedCornerShape(if (isCompact) 16.dp else 20.dp)
                            )
                        .padding(
                            horizontal = if (isCompact) 12.dp else 16.dp,
                            vertical = if (isCompact) 8.dp else 14.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isPlaying) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = 0.22f))
                                .border(1.dp, accentColor.copy(alpha = 0.70f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = if (isCompact) 2.dp else 4.dp)
                        ) {
                            Text(
                                text = "▶ ÇALIYOR",
                                fontSize = if (isCompact) 10.sp else 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor
                            )
                        }
                        Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 10.dp))
                    }

                    Text(
                        text = songTitle,
                        fontSize = if (isCompact) 18.sp else 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))

                    Text(
                        text = songartist,
                        fontSize = if (isCompact) 13.sp else 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (badgeText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .border(1.dp, accentColor.copy(alpha = 0.60f), RoundedCornerShape(8.dp))
                                .clickable { onInfoBadgeClick() }
                                .padding(horizontal = 8.dp, vertical = if (isCompact) 2.dp else 4.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = if (isCompact) 11.sp else 12.sp,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

                // Kaydırma ipucu rozetleri ─ Sola (Sonraki)
                if (isDragging && dragOffsetX < -28f) {
                    SwipeHIntBadge(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp),
                        text = "SONRAKİ",
                        iconRes = R.drawable.ic_skip_next,
                        iconatEnd = true
                    )
                }
                // Kaydırma ipucu rozetleri ─ Sağa (Önceki)
                else if (isDragging && dragOffsetX > 28f) {
                    SwipeHIntBadge(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp),
                        text = "Önceki",
                        iconRes = R.drawable.ic_skip_previous,
                        iconatEnd = false
                    )
                }
            }
        } else {
            // ── KLASİK KUTULU / PİKAP MODU ────────────────────────────────────────
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val maxAllowed = if (isCompact) 185.dp else 260.dp
                val minAllowed = if (isCompact) 80.dp else 120.dp
                val dynamicSize = minOf(maxWidth * 0.95f, maxHeight).coerceIn(minAllowed, maxAllowed)

                if (isCoverLyricsActive && lyricsData != null) {
                    val leftVolumeStripWidth = if (isCompact) 44.dp else 54.dp
                    val rightPadding = if (isCompact) 6.dp else 12.dp
                    val cardWidth = (maxWidth - leftVolumeStripWidth - rightPadding).coerceAtLeast(minAllowed)
                    val cardHeight = (maxHeight - (if (isCompact) 4.dp else 8.dp)).coerceAtLeast(minAllowed)

                    // 1. Sol ses kontrol şeridi
                    VolumeControlStrip(
                        isCompact = isCompact,
                        accentColor = accentColor,
                        onVolumeDrag = handleVolumeDrag,
                        onVolumeRaise = {
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_RAISE,
                                AudioManager.FLAG_SHOW_UI
                            )
                        },
                        onVolumeLower = {
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_LOWER,
                                AudioManager.FLAG_SHOW_UI
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(leftVolumeStripWidth)
                            .height(cardHeight)
                    )

                    // 2. Genişletilmiş Şarkı Sözü Kartı
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = rightPadding)
                            .size(width = cardWidth, height = cardHeight)
                            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                            .shadow(
                                elevation = if (isPlaying) (if (isCompact) 8.dp else 14.dp) else 4.dp,
                                shape = RoundedCornerShape(if (isCompact) 18.dp else 24.dp)
                            )
                            .clip(RoundedCornerShape(if (isCompact) 18.dp else 24.dp))
                            .background(CarSurfaceVariant)
                            .border(
                                width = if (isPlaying) 2.dp else 1.5.dp,
                                color = if (isPlaying) accentColor else CarBorder,
                                shape = RoundedCornerShape(if (isCompact) 18.dp else 24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val coverAlpha = 0.10f
                        if (artworkBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = artworkBitmap.asImageBitmap(),
                                contentDescription = "Albüm Kapağı",
                                contentScale = ContentScale.Crop,
                                alpha = coverAlpha,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = coverAlpha)) {
                                VinylDisc(
                                    isPlaying = isPlaying,
                                    artworkBitmap = null,
                                    size = minOf(cardWidth, cardHeight)
                                )
                            }
                        }

                        CoverLyricsOverlay(
                            lyricsData = lyricsData,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            isPlaying = isPlaying,
                            accentColor = accentColor,
                            isCompact = isCompact,
                            lyricsFontSize = lyricsFontSize,
                            onSeekTo = onSeekTo,
                            onOpenLyricsDialog = onOpenLyricsDialog
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                            .size(dynamicSize)
                            .shadow(
                                elevation = if (isPlaying) (if (isCompact) 8.dp else 14.dp) else 4.dp,
                                shape = RoundedCornerShape(if (isCompact) 18.dp else 26.dp)
                            )
                            .clip(RoundedCornerShape(if (isCompact) 18.dp else 26.dp))
                            .background(CarSurfaceVariant)
                            .border(
                                width = if (isPlaying) 2.dp else 1.5.dp,
                                color = if (isPlaying) accentColor else CarBorder,
                                shape = RoundedCornerShape(if (isCompact) 18.dp else 26.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artworkBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = artworkBitmap.asImageBitmap(),
                                contentDescription = "Albüm Kapağı",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                VinylDisc(
                                    isPlaying = isPlaying,
                                    artworkBitmap = null,
                                    size = dynamicSize
                                )
                            }
                        }

                        // Çalarken sağ altta rozet
                        if (isPlaying) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(if (isCompact) 6.dp else 10.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xDD0C0E12))
                                    .border(1.dp, CarCyan, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = if (isCompact) 3.dp else 5.dp)
                            ) {
                                Text(
                                    text = "▶ ÇALIYOR",
                                    fontSize = if (isCompact) 10.sp else 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CarCyan
                                )
                            }
                        }
                    }
                }

                // Kaydırma ipucu rozetleri ─ Sola (Sonraki)
                if (isDragging && dragOffsetX < -28f) {
                    SwipeHIntBadge(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp),
                        text = "SONRAKİ",
                        iconRes = R.drawable.ic_skip_next,
                        iconatEnd = true
                    )
                }
                // Kaydırma ipucu rozetleri ─ Sağa (Önceki)
                else if (isDragging && dragOffsetX > 28f) {
                    SwipeHIntBadge(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp),
                        text = "Önceki",
                        iconRes = R.drawable.ic_skip_previous,
                        iconatEnd = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 10.dp))

            // ── ŞARKI BİLGİSİ (başlık + Sanatçı + rozet) — swipe ile birlikte kayar ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                    .padding(horizontal = 4.dp)
                    .then(
                        if (isCoverLyricsActive) {
                            Modifier.pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { change, dragamount ->
                                        change.consume()
                                        handleVolumeDrag(dragamount)
                                    },
                                    onDragEnd = { handleVolumeDrag(0f) },
                                    onDragCancel = { handleVolumeDrag(0f) }
                                )
                            }
                        } else Modifier
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = songTitle,
                    fontSize = if (isCompact) 18.sp else 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CarTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(if (isCompact) 1.dp else 3.dp))

                Text(
                    text = songartist,
                    fontSize = if (isCompact) 13.sp else 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = CarTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // ID3 Teknik Rozet
                if (badgeText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(if (isCompact) 3.dp else 6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarSurfaceVariant)
                            .border(1.dp, CarBorder, RoundedCornerShape(8.dp))
                            .clickable { onInfoBadgeClick() }
                            .padding(horizontal = 8.dp, vertical = if (isCompact) 2.dp else 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            color = CarCyan,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** kaydrma yönü rozeti — "SONRaKİ" veya "Önceki" */
@Composable
private fun SwipeHIntBadge(
    modifier: Modifier = Modifier,
    text: String,
    iconRes: Int,
    iconatEnd: Boolean
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xDD00E5FF))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!iconatEnd) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color(0xFF0C0E12),
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0C0E12)
            )
            if (iconatEnd) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color(0xFF0C0E12),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Şarkı sözleri açıkken sol kenarda dikey kaydırma veya dokunma ile ses seviyesini
 * ayarlamayı sağlayan zarif sürüş dostu ses kontrol şeridi.
 */
@Composable
private fun VolumeControlStrip(
    isCompact: Boolean,
    accentColor: Color,
    onVolumeDrag: (Float) -> Unit,
    onVolumeRaise: () -> Unit,
    onVolumeLower: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = CarSurfaceDark.luminance() < 0.5f
    val stripBg = if (isDark) Color(0xFF141822).copy(alpha = 0.82f) else Color(0xFFEDF2F7).copy(alpha = 0.90f)
    val stripBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.12f)
    val stripContentColor = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF334155).copy(alpha = 0.75f)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragamount ->
                        change.consume()
                        onVolumeDrag(dragamount)
                    },
                    onDragEnd = { onVolumeDrag(0f) },
                    onDragCancel = { onVolumeDrag(0f) }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (offset.y < size.height / 2) {
                        onVolumeRaise()
                    } else {
                        onVolumeLower()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .width(if (isCompact) 36.dp else 44.dp)
                .clip(RoundedCornerShape(if (isCompact) 18.dp else 22.dp))
                .background(stripBg)
                .border(1.dp, stripBorder, RoundedCornerShape(if (isCompact) 18.dp else 22.dp))
                .padding(vertical = if (isCompact) 8.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "▲",
                fontSize = if (isCompact) 11.sp else 13.sp,
                color = stripContentColor,
                fontWeight = FontWeight.Bold
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "🔊",
                    fontSize = if (isCompact) 14.sp else 18.sp
                )
                Text(
                    text = "SES",
                    fontSize = if (isCompact) 8.sp else 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = "▼",
                fontSize = if (isCompact) 11.sp else 13.sp,
                color = stripContentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Albüm kapağı alanında gösterilen kompakt ve yarı-opak şarkı sözü arayüzü.
 * - Senkronize sözlerde aktif satırı vurgular ve ortalayarak kaydırır.
 * - Senkronize olmayan (düz metin) sözlerde şarkı süresiyle orantılı olarak yavaşça aşağı kayar.
 * - Açık ve koyu temalarda yüksek kontrastlı renkler kullanır.
 */
@Composable
fun CoverLyricsOverlay(
    lyricsData: com.example.otomuzik.model.LyricsData,
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    accentColor: Color,
    isCompact: Boolean,
    lyricsFontSize: String = "LARGE",
    onSeekTo: (Long) -> Unit,
    onOpenLyricsDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lines = lyricsData.lines
    val isSynced = lyricsData.isSynced && lines.any { it.timestampMs >= 0 }
    val listState = remember(lyricsData.songPath, lyricsData.rawLyrics) { LazyListState(0, 0) }

    val fontDelta = when (lyricsFontSize) {
        "SMALL" -> -2
        "NORMAL" -> 0
        "LARGE" -> 4
        "XLARGE" -> 8
        else -> 4
    }

    val isDark = CarSurfaceDark.luminance() < 0.5f
    // Kapağın görünürlüğünü kırıp metin okunurluğunu maksimuma çıkaran opak zemin
    val overlayBg = if (isDark) Color(0xFF090D16).copy(alpha = 0.88f) else Color(0xFFF8FAFC).copy(alpha = 0.92f)
    val effectiveAccent = if (isDark) accentColor else Color(0xFF007A87)
    // Aktif satır: Standart parlak beyaz (kullanıcı isteği)
    val activeTextColor = Color.White
    // Sonraki satır: Beyaz değil, beyaza yakın gri (kullanıcı isteği: dikkat çekmemesi için)
    val nextTextColor = if (isDark) Color(0xFFB0B0B0) else Color(0xFF475569)
    val inactiveTextColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    val activeIndex = remember(currentPositionMs, lines, isSynced) {
        if (!isSynced || lines.isEmpty()) -1
        else lines.indexOfLast { it.timestampMs <= currentPositionMs }
    }

    // 1. Şarkı veya söz değiştiğinde listeyi kesinlikle en başa (0. satıra) sıfırla
    LaunchedEffect(lyricsData.songPath, lyricsData.rawLyrics) {
        listState.scrollToItem(0)
    }

    // 2. Senkronize sözler: Aktif satırı ortalayacak şekilde kaydır (ilk satırlarda en başta kalsın)
    LaunchedEffect(activeIndex, isSynced) {
        if (isSynced) {
            val target = if (activeIndex >= 2) activeIndex - 1 else 0
            if (target != listState.firstVisibleItemIndex) {
                listState.animateScrollToItem(target)
            }
        }
    }

    // 3. Senkronize olmayan (düz metin) sözler: Her zaman en baştan başlasın
    LaunchedEffect(isSynced, lyricsData.songPath) {
        if (!isSynced) {
            listState.scrollToItem(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(overlayBg)
    ) {
        // Sözler Listesi
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isCompact) 8.dp else 12.dp),
            contentPadding = PaddingValues(top = if (isCompact) 28.dp else 34.dp, bottom = if (isCompact) 24.dp else 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)
        ) {
            itemsIndexed(lines) { index, line ->
                val isCurrent = isSynced && index == activeIndex
                val isNext = isSynced && activeIndex >= 0 && index == activeIndex + 1

                if (isSynced) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                when {
                                    isCurrent -> Modifier
                                        .background(effectiveAccent.copy(alpha = if (isDark) 0.24f else 0.16f))
                                        .border(1.2.dp, effectiveAccent.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                    isNext -> Modifier
                                        .background(Color.White.copy(alpha = if (isDark) 0.04f else 0.03f))
                                        .border(0.8.dp, nextTextColor.copy(alpha = if (isDark) 0.30f else 0.40f), RoundedCornerShape(8.dp))
                                    else -> Modifier
                                }
                            )
                            .clickable {
                                if (line.timestampMs >= 0) {
                                    onSeekTo(line.timestampMs)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = if (isCurrent) 4.dp else (if (isNext) 3.dp else 2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val lineFontSize = when {
                            isCurrent -> (if (isCompact) 15 else 18) + fontDelta
                            isNext -> (if (isCompact) 13 else 16) + fontDelta
                            else -> (if (isCompact) 12 else 14) + fontDelta
                        }.sp

                        val lineFontWeight = when {
                            isCurrent -> FontWeight.ExtraBold
                            isNext -> FontWeight.Bold
                            else -> FontWeight.Normal
                        }

                        val lineTextColor = when {
                            isCurrent -> activeTextColor // Parlak beyaz
                            isNext -> nextTextColor // Beyaza yakın gri
                            else -> inactiveTextColor
                        }

                        val lineTextHeight = when {
                            isCurrent -> (if (isCompact) 20 else 24) + fontDelta + 2
                            isNext -> (if (isCompact) 18 else 21) + fontDelta + 2
                            else -> (if (isCompact) 16 else 19) + fontDelta + 2
                        }.sp

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isNext) {
                                Text(
                                    text = "›",
                                    fontSize = ((if (isCompact) 13 else 16) + fontDelta).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = nextTextColor,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            Text(
                                text = TurkishStringFixer.fix(line.text).ifBlank { "• • •" },
                                fontSize = lineFontSize,
                                fontWeight = lineFontWeight,
                                color = lineTextColor,
                                textAlign = TextAlign.Center,
                                lineHeight = lineTextHeight
                            )
                        }
                    }
                } else {
                    Text(
                        text = TurkishStringFixer.fix(line.text),
                        fontSize = ((if (isCompact) 13 else 16) + fontDelta).sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A),
                        textAlign = TextAlign.Center,
                        lineHeight = ((if (isCompact) 18 else 22) + fontDelta + 2).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                    )
                }
            }
        }

        // Üst yumuşak geçiş maskesi
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(if (isCompact) 26.dp else 32.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(overlayBg, overlayBg.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )

        // Alt yumuşak geçiş maskesi
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(if (isCompact) 26.dp else 32.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, overlayBg.copy(alpha = 0.6f), overlayBg)
                    )
                )
        )

        // Üst kontrol / bilgilendirme barı (Z-index 10f ile en üst katmanda, tıklamaları doğrudan alır)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .align(Alignment.TopCenter)
                .zIndex(10f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(effectiveAccent.copy(alpha = if (isDark) 0.22f else 0.14f))
                    .border(0.8.dp, effectiveAccent.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
            ) {
                Text(
                    text = if (isSynced) "⚡ Senkronize" else "📜 Düz Metin",
                    fontSize = if (isCompact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = effectiveAccent
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xDD121620) else Color(0xEEFFFFFF))
                    .border(1.dp, effectiveAccent.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true)
                    ) { onOpenLyricsDialog() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "⤢",
                    fontSize = if (isCompact) 11.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = effectiveAccent
                )
                Text(
                    text = "Tam Ekran",
                    fontSize = if (isCompact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            }
        }
    }
}





















