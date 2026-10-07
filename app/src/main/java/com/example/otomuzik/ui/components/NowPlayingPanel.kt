package com.example.otomuzik.ui.components
import com.example.otomuzik.theme.*

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otomuzik.R
import com.example.otomuzik.model.PlayerState
import com.example.otomuzik.model.RepeatMode





import com.example.otomuzik.theme.CarTextMuted


@Composable
fun NowPlayingPanel(
    playerState: PlayerState,
    artworkBitmap: Bitmap? = null,
    currentSongId3: com.example.otomuzik.util.Id3Metadata? = null,
    /** albüm kapağından çıkarılan dinamik aksan rengi */
    accentColor: Color = CarCyan,
    isFavorite: Boolean,
    lyricsData: com.example.otomuzik.model.LyricsData? = null,
    showCoverLyrics: Boolean = false,
    onToggleCoverLyrics: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenId3Info: () -> Unit = {},
    onOpenLyrics: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggLERepeat: () -> Unit,
    showWaveform: Boolean = false,
    isalbumartCropped: Boolean = false,
    lyricsFontSize: String = "LARGE",
    coverSize: androidx.compose.ui.unit.Dp = 260.dp,
    showBlurredBackground: Boolean = false,
    modifier: Modifier = Modifier
) {
    val currentSong = playerState.currentSong

    // aksan rengi yumuşak geçişle animasyon yapar (şarkı değişiminde ani Renk sıçraması olmaz)
    val animatedaccent by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(durationMillis = 800),
        label = "accentColor"
    )

    // Glow/border için hafif alfa varyantları
    val accentGlow   = animatedaccent.copy(alpha = 0.15f)
    val accentBorder = animatedaccent.copy(alpha = 0.75f)
    val accentDim    = animatedaccent.copy(alpha = 0.50f)

    val isFullscreenCover = isalbumartCropped && artworkBitmap != null

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    // Dinamik boyutlandırma (9 inç ve araç ekranları için optimize)
    val panelCornerRadius = if (isCompact) 18.dp else 22.dp
    val panelPaddingH = if (isCompact) 12.dp else 18.dp
    val panelPaddingV = if (isCompact) 8.dp else 14.dp

    val topBtnSize = if (isCompact) 36.dp else 44.dp
    val topBtnSpacing = if (isCompact) 3.dp else 6.dp
    val eqBtnSize = if (isCompact) 40.dp else 50.dp

    val playBtnSize = if (isCompact) 74.dp else 92.dp
    val playIconSize = if (isCompact) 38.dp else 48.dp
    val skipBtnSize = if (isCompact) 58.dp else 70.dp
    val skipIconSize = if (isCompact) 30.dp else 36.dp
    val sideBtnSize = if (isCompact) 44.dp else 54.dp
    val sideIconSize = if (isCompact) 22.dp else 26.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(panelCornerRadius))
            .border(1.5.dp, accentBorder, RoundedCornerShape(panelCornerRadius))
    ) {
        if (isFullscreenCover && artworkBitmap != null) {
            // Tam ekran albüm kapağı arka planı
            Image(
                bitmap = artworkBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Okunabilirlik ve şık arayüz için dikey karartma katmanı (gradient overlay)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.72f), // Üst bar butonları ve logo için
                                Color.Black.copy(alpha = 0.28f), // Orta alan kapağı net görsün
                                Color.Black.copy(alpha = 0.58f), // Alt orta geçiş
                                Color.Black.copy(alpha = 0.88f)  // Alt ilerleme çubuğu ve kontrol butonları için
                            )
                        )
                    )
            )
        } else {
            // Standart koyu arka plan + aksan rengi yansıması (Bulanık arka plan aktifse zarif cam efekti)
            val panelBg = if (showBlurredBackground) {
                CarSurfaceDark.copy(alpha = 0.52f)
            } else {
                CarSurfaceDark.copy(alpha = 0.92f)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(accentGlow, Color.Transparent),
                            radius = 600f
                        )
                    )
                    .background(panelBg)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = panelPaddingH, vertical = panelPaddingV),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── 1. ÜST KISIM: EQ, Logo, ID3 / Favori Butonları ──────────────
            val topBtnBg = if (isFullscreenCover) Color.Black.copy(alpha = 0.55f) else CarSurfaceVariant
            val topBtnBorder = if (isFullscreenCover) animatedaccent.copy(alpha = 0.35f) else CarBorder

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isCompact) 2.dp else 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol üstte EQ butonu — aksan rengiyle çerçeveli
                IconButton(
                    onClick = onOpenEqualizer,
                    modifier = Modifier
                        .size(eqBtnSize)
                        .background(topBtnBg, CircleShape)
                        .border(1.5.dp, animatedaccent, CircleShape)
                ) {
                    Text(
                        text = "EQ",
                        fontSize = if (isCompact) 13.sp else 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = animatedaccent
                    )
                }

                Text(
                    text = "RidoPlay",
                    fontSize = if (isCompact) 12.sp else 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = animatedaccent.copy(alpha = 0.95f),
                    letterSpacing = if (isCompact) 1.sp else 2.sp
                )

                // Sağ üstte Sleep Timer + Kapak Sözleri + Queue + ID3 + Favori butonları
                Row(horizontalArrangement = Arrangement.spacedBy(topBtnSpacing)) {
                    IconButton(
                        onClick = onOpenSleepTimer,
                        modifier = Modifier
                            .size(topBtnSize)
                            .background(topBtnBg, CircleShape)
                            .border(1.dp, topBtnBorder, CircleShape)
                    ) {
                        Text(
                            text = "💤",
                            fontSize = if (isCompact) 12.sp else 14.sp
                        )
                    }

                    // Kapakta Şarkı Sözleri Butonu (Uyku zamanlayıcının hemen yanında)
                    val hasLyrics = lyricsData != null && lyricsData.lines.isNotEmpty()
                    IconButton(
                        onClick = onToggleCoverLyrics,
                        modifier = Modifier
                            .size(topBtnSize)
                            .background(
                                if (showCoverLyrics) animatedaccent.copy(alpha = 0.25f) else topBtnBg,
                                CircleShape
                            )
                            .border(
                                if (showCoverLyrics) 1.5.dp else 1.dp,
                                if (showCoverLyrics) animatedaccent else topBtnBorder,
                                CircleShape
                            )
                    ) {
                        Text(
                            text = "🎤",
                            fontSize = if (isCompact) 13.sp else 16.sp
                        )
                    }

                    IconButton(
                        onClick = onOpenQueue,
                        modifier = Modifier
                            .size(topBtnSize)
                            .background(topBtnBg, CircleShape)
                            .border(1.dp, topBtnBorder, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_queue),
                            contentDescription = "Sıra",
                            tint = animatedaccent,
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenId3Info,
                        modifier = Modifier
                            .size(topBtnSize)
                            .background(topBtnBg, CircleShape)
                            .border(1.dp, topBtnBorder, CircleShape)
                    ) {
                        Text(
                            text = "ℹ",
                            fontSize = if (isCompact) 16.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = animatedaccent
                        )
                    }

                    IconButton(
                        onClick = onOpenLyrics,
                        modifier = Modifier
                            .size(topBtnSize)
                            .background(topBtnBg, CircleShape)
                            .border(1.dp, topBtnBorder, CircleShape)
                    ) {
                        Text(
                            text = "📝",
                            fontSize = if (isCompact) 16.sp else 20.sp
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(topBtnSize)
                            .background(topBtnBg, CircleShape)
                            .border(
                                1.5.dp,
                                if (isFavorite) CarAmber else topBtnBorder,
                                CircleShape
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_favorite),
                            contentDescription = "Favori",
                            tint = if (isFavorite) CarAmber else CarTextMuted,
                            modifier = Modifier.size(if (isCompact) 20.dp else 26.dp)
                        )
                    }
                }
            }

            // ── 2. alBÜM KaPaĞI + ŞarkI BİLGİSİ (Birleşik Swipe alanı) ─────
            val displayTitle = currentSongId3?.title
                ?.takeIf { it.isNotBlank() && it != "Bilinmeyen Parça" }
                ?: currentSong?.title ?: "Parça Seçilmedi"
            val displayartist = currentSongId3?.artist
                ?.takeIf { it.isNotBlank() && it != "Bilinmeyen Sanatçı" }
                ?: currentSong?.artist ?: "Müzik Listesinden bir şarkı seçin"
            val badgeText = currentSongId3?.let {
                listOf(it.format, it.bitrate, it.genre, it.year)
                    .filter { s -> s.isNotEmpty() }.joinToString(" • ")
            }?.takeIf { it.isNotEmpty() } ?: currentSong?.id3Badge ?: ""

            SwipeableCoverandInfo(
                isPlaying = playerState.isPlaying,
                artworkBitmap = artworkBitmap,
                currentSongId3 = currentSongId3,
                songTitle = displayTitle,
                songartist = displayartist,
                badgeText = badgeText,
                accentColor = animatedaccent,
                lyricsData = lyricsData,
                showCoverLyrics = showCoverLyrics,
                currentPositionMs = playerState.currentPositionMs,
                durationMs = playerState.durationMs,
                onSeekTo = onSeekTo,
                onOpenLyricsDialog = onOpenLyrics,
                onSwipeNext = onNext,
                onSwipePrevious = onPrevious,
                onSeekForward = { onSeekTo(playerState.currentPositionMs + 10000) },
                onSeekBackward = { onSeekTo((playerState.currentPositionMs - 10000).coerceAtLeast(0)) },
                onInfoBadgeClick = onOpenId3Info,
                isalbumartCropped = isalbumartCropped,
                lyricsFontSize = lyricsFontSize,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // ── 3. İLERLEME ÇUBUĞU ───────────────────────────────────────────
            CarProgressBar(
                progress = playerState.progressFraction,
                currentFormatted = playerState.positionFormatted,
                durationFormatted = playerState.durationFormatted,
                accentColor = animatedaccent,          // ← progress bar da dinamik Renk
                onSeekToProgress = { fraction ->
                    val targetms = (fraction * playerState.durationMs).toLong()
                    onSeekTo(targetms)
                },
                modifier = Modifier.padding(bottom = if (isCompact) 2.dp else 6.dp)
            )

            if (showWaveform) {
                WaveformVisualizer(
                    isPlaying = playerState.isPlaying,
                    accentColor = animatedaccent,
                    modifier = Modifier.padding(bottom = if (isCompact) 3.dp else 8.dp)
                )
            }

            // ── 4. KONTROL BUTONLARI ──────────────────────────────────────────
            val ctrlBtnBg = if (isFullscreenCover) Color.Black.copy(alpha = 0.55f) else CarSurfaceVariant
            val ctrlBorder = if (isFullscreenCover) animatedaccent.copy(alpha = 0.35f) else CarBorder

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp, bottom = if (isCompact) 2.dp else 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Karışık Çal (Shuffle)
                Box(
                    modifier = Modifier
                        .size(sideBtnSize)
                        .clip(CircleShape)
                        .background(
                            if (playerState.isShuffle)
                                animatedaccent.copy(alpha = 0.22f)
                            else ctrlBtnBg.copy(alpha = 0.8f)
                        )
                        .border(
                            1.dp,
                            if (playerState.isShuffle) animatedaccent else ctrlBorder,
                            CircleShape
                        )
                        .clickable { onToggleShuffle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shuffle),
                        contentDescription = "Karışık Çal",
                        tint = if (playerState.isShuffle) animatedaccent else CarTextMuted,
                        modifier = Modifier.size(sideIconSize)
                    )
                }

                // Önceki Şarkı
                Box(
                    modifier = Modifier
                        .size(skipBtnSize)
                        .clip(CircleShape)
                        .background(ctrlBtnBg)
                        .border(1.5.dp, ctrlBorder, CircleShape)
                        .clickable { onPrevious() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_previous),
                        contentDescription = "Önceki Parça",
                        tint = CarTextPrimary,
                        modifier = Modifier.size(skipIconSize)
                    )
                }

                // ▶ / ⏸ Oynat/Duraklat — DEV MERKEZ TUŞ — aksan rengiyle
                Box(
                    modifier = Modifier
                        .size(playBtnSize)
                        .shadow(elevation = if (isCompact) 6.dp else 10.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(animatedaccent)    // ← dinamik Renk
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (playerState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (playerState.isPlaying) "Duraklat" else "Oynat",
                        tint = Color(0xFF0C0E12),
                        modifier = Modifier.size(playIconSize)
                    )
                }

                // Sonraki Şarkı
                Box(
                    modifier = Modifier
                        .size(skipBtnSize)
                        .clip(CircleShape)
                        .background(ctrlBtnBg)
                        .border(1.5.dp, ctrlBorder, CircleShape)
                        .clickable { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_next),
                        contentDescription = "Sonraki Parça",
                        tint = CarTextPrimary,
                        modifier = Modifier.size(skipIconSize)
                    )
                }

                // Tekrar Modu (Repeat)
                val (iconRes, repeatTInt, isRepeatactive) = when (playerState.repeatMode) {
                    RepeatMode.OFF -> Triple(R.drawable.ic_repeat,     CarTextMuted,    false)
                    RepeatMode.ALL -> Triple(R.drawable.ic_repeat,     animatedaccent,  true)
                    RepeatMode.ONE -> Triple(R.drawable.ic_repeat_one, CarAmber,        true)
                }
                Box(
                    modifier = Modifier
                        .size(sideBtnSize)
                        .clip(CircleShape)
                        .background(
                            if (isRepeatactive)
                                animatedaccent.copy(alpha = 0.22f)
                            else ctrlBtnBg.copy(alpha = 0.8f)
                        )
                        .border(
                            1.dp,
                            if (isRepeatactive) repeatTInt else ctrlBorder,
                            CircleShape
                        )
                        .clickable { onToggLERepeat() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = "Tekrar Modu",
                        tint = repeatTInt,
                        modifier = Modifier.size(sideIconSize)
                    )
                }
            }
        }
    }
}





















