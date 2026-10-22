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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    onPlayMoreFromArtist: (() -> Unit)? = null,
    isArtistModeActive: Boolean = false,
    onOpenEqualizer: () -> Unit,
    onOpenBlackScreen: () -> Unit = {},
    onOpenId3Info: () -> Unit = {},
    onOpenLyrics: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
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

    val topBtnSize = if (isCompact) 56.dp else 68.dp
    val topBtnSpacing = if (isCompact) 6.dp else 10.dp
    val eqBtnSize = if (isCompact) 62.dp else 74.dp

    val playBtnSize = if (isCompact) 74.dp else 92.dp
    val playIconSize = if (isCompact) 38.dp else 48.dp
    val skipBtnSize = if (isCompact) 58.dp else 70.dp
    val skipIconSize = if (isCompact) 30.dp else 36.dp
    val sideBtnSize = if (isCompact) 44.dp else 54.dp
    val sideIconSize = if (isCompact) 22.dp else 26.dp

    Box(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(panelCornerRadius),
                spotColor = animatedaccent.copy(alpha = 0.40f),
                ambientColor = Color.Black
            )
            .clip(RoundedCornerShape(panelCornerRadius))
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        animatedaccent,
                        accentBorder,
                        Color.Black.copy(alpha = 0.85f)
                    )
                ),
                shape = RoundedCornerShape(panelCornerRadius)
            )
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

        // 3D Dışbükey Cam Yansıması (Convex Glass Sheen Overlay)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.07f), Color.Transparent)
                    )
                )
        )

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
                    .padding(bottom = if (isCompact) 4.dp else 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // En solda 3 Boyutlu Tasarımlı Program Adı (RidoPlay) - Tıklanınca Hakkında açılır
                ThreeDimensionalBrandText(
                    text = "RidoPlay",
                    isCompact = isCompact,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenAbout() }
                )

                // ── SİYAH DAİRE BUTONU (Ekranı Kapat / Gece Saati Modu) ──
                // Otomotiv tarzı gömülü yuvalı (Recessed Bezel) 3D siyah buton
                val outerBezelSize = if (isCompact) 48.dp else 56.dp
                val innerButtonSize = if (isCompact) 36.dp else 42.dp
                Box(
                    modifier = Modifier
                        .size(outerBezelSize)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                        .clip(CircleShape)
                        // Dış Gömülü Yuva: Derin mat titanyum & karbon eğim (Konsola gömülü hissi)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF28303F),
                                    Color(0xFF141822),
                                    Color(0xFF090B0F)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFF4C586B),
                                    Color(0xFF1E2430),
                                    Color(0xFF0D1017)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable { onOpenBlackScreen() },
                    contentAlignment = Alignment.Center
                ) {
                    // İç Buton: Parlak piyano siyahı çekirdek + ince krom LED konturu
                    Box(
                        modifier = Modifier
                            .size(innerButtonSize)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF1E2430),
                                        Color(0xFF0A0C10),
                                        Color.Black
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF64B5F6).copy(alpha = 0.55f),
                                        Color(0xFF1E2838)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Vektörel / Tipografik Minimalist Gece Ayı İkonu (Kristal netlik)
                        Text(
                            text = "☾",
                            fontSize = if (isCompact) 18.sp else 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                // Sağ tarafta EQ ve menü butonları (Şık Altın/Amber Kapsül Container)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF0B0F17).copy(alpha = 0.88f))
                        .border(1.6.dp, Color(0xFFFFB300).copy(alpha = 0.95f), RoundedCornerShape(50))
                        .padding(horizontal = if (isCompact) 8.dp else 10.dp, vertical = if (isCompact) 4.dp else 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pillBtnSize = if (isCompact) 46.dp else 56.dp
                    val pillBorderCyan = Color(0xFF00E5FF)

                    // 1. EQ Butonu
                    Box(
                        modifier = Modifier
                            .size(pillBtnSize)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(1.8.dp, pillBorderCyan, CircleShape)
                            .clickable { onOpenEqualizer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "EQ",
                            fontSize = if (isCompact) 14.sp else 18.sp,
                            fontWeight = FontWeight.Black,
                            color = pillBorderCyan
                        )
                    }

                    // 2. Şarkı Sözleri (Mikrofon) Butonu
                    Box(
                        modifier = Modifier
                            .size(pillBtnSize)
                            .clip(CircleShape)
                            .background(if (showCoverLyrics) pillBorderCyan.copy(alpha = 0.25f) else Color(0xFF0F172A))
                            .border(1.8.dp, pillBorderCyan, CircleShape)
                            .clickable { onToggleCoverLyrics() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎤",
                            fontSize = if (isCompact) 18.sp else 23.sp
                        )
                    }

                    // 3. Bilgi (Info) Butonu
                    Box(
                        modifier = Modifier
                            .size(pillBtnSize)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(1.8.dp, pillBorderCyan, CircleShape)
                            .clickable { onOpenId3Info() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ℹ",
                            fontSize = if (isCompact) 20.sp else 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = pillBorderCyan
                        )
                    }

                    // 4. Şarkı Sözleri / Düzenle Butonu
                    Box(
                        modifier = Modifier
                            .size(pillBtnSize)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(1.8.dp, pillBorderCyan, CircleShape)
                            .clickable { onOpenLyrics() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✏️",
                            fontSize = if (isCompact) 17.sp else 22.sp
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
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                onPlayMoreFromArtist = onPlayMoreFromArtist,
                isArtistModeActive = isArtistModeActive,
                lyricsData = lyricsData,
                showCoverLyrics = showCoverLyrics,
                onToggleCoverLyrics = onToggleCoverLyrics,
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
                showInfoBelow = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // ── 2.5 ŞARKI BİLGİLERİ VE HIZLI EYLEM BUTONLARI (Mikrofon - Şarkı Bilgisi - Favori) ────
            // İlerleme çubuğunun hemen üstünde: Solda Retro Mikrofon, Ortada Şarkı/Sanatçı/Rozet, Sağda Kalp Favori
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (isCompact) 4.dp else 6.dp, end = if (isCompact) 4.dp else 6.dp, bottom = if (isCompact) 4.dp else 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 2 katına çıkarılmış belirgin kokpit buton ve simge boyutları
                val actionBtnSize = if (isCompact) 64.dp else 76.dp
                val actionIconSize = if (isCompact) 40.dp else 48.dp

                // SOL: Sanatçının Diğer Şarkılarını Çal Butonu (🎙️ / ic_mic_retro)
                Box(
                    modifier = Modifier
                        .size(actionBtnSize)
                        .shadow(
                            elevation = if (isArtistModeActive) 12.dp else 6.dp,
                            shape = CircleShape,
                            ambientColor = if (isArtistModeActive) animatedaccent else Color.Black,
                            spotColor = if (isArtistModeActive) animatedaccent else Color.Black
                        )
                        .clip(CircleShape)
                        .background(
                            if (isArtistModeActive)
                                Brush.radialGradient(
                                    listOf(
                                        animatedaccent.copy(alpha = 0.45f),
                                        Color(0xFF0F1A28),
                                        Color(0xFF070B12)
                                    )
                                )
                            else
                                Brush.verticalGradient(
                                    listOf(Color(0xFF252D3D), Color(0xFF131722), Color(0xFF0C0F17))
                                )
                        )
                        .border(
                            width = if (isArtistModeActive) 2.4.dp else 1.8.dp,
                            brush = if (isArtistModeActive)
                                Brush.verticalGradient(listOf(animatedaccent, animatedaccent.copy(alpha = 0.7f)))
                            else
                                Brush.verticalGradient(listOf(Color(0xFF4A5568), Color(0xFF1E2432))),
                            shape = CircleShape
                        )
                        .clickable { onPlayMoreFromArtist?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mic_retro),
                        contentDescription = if (isArtistModeActive) "Sanatçı Modunu Kapat" else "Sanatçının Diğer Şarkılarını Çal",
                        tint = if (isArtistModeActive) animatedaccent else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(actionIconSize)
                    )
                }

                // ORTA: Şarkı Başlığı, Sanatçı Adı ve Teknik Rozet (Mikrofon ve Kalp ile aynı hizada)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayTitle,
                        fontSize = if (isCompact) 17.sp else 21.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(if (isCompact) 1.dp else 3.dp))

                    Text(
                        text = displayartist,
                        fontSize = if (isCompact) 13.sp else 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (badgeText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(if (isCompact) 3.dp else 5.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CarSurfaceVariant)
                                .border(1.dp, CarBorder, RoundedCornerShape(8.dp))
                                .clickable { onOpenId3Info() }
                                .padding(horizontal = 8.dp, vertical = if (isCompact) 2.dp else 3.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                                color = animatedaccent,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // SAĞ: Favorilere Ekle Butonu (❤️)
                Box(
                    modifier = Modifier
                        .size(actionBtnSize)
                        .shadow(
                            elevation = if (isFavorite) 10.dp else 6.dp,
                            shape = CircleShape,
                            ambientColor = if (isFavorite) Color(0xFFE53935) else Color.Black,
                            spotColor = if (isFavorite) Color(0xFFE53935) else Color.Black
                        )
                        .clip(CircleShape)
                        .background(
                            if (isFavorite)
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFE53935).copy(alpha = 0.40f),
                                        Color(0xFF2A1015),
                                        Color(0xFF120A0D)
                                    )
                                )
                            else
                                Brush.verticalGradient(
                                    listOf(Color(0xFF252D3D), Color(0xFF131722), Color(0xFF0C0F17))
                                )
                        )
                        .border(
                            width = if (isFavorite) 2.4.dp else 1.8.dp,
                            brush = if (isFavorite)
                                Brush.verticalGradient(listOf(Color(0xFFFF5252), Color(0xFFD32F2F)))
                            else
                                Brush.verticalGradient(listOf(Color(0xFF4A5568), Color(0xFF1E2432))),
                            shape = CircleShape
                        )
                        .clickable { onToggleFavorite() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                        ),
                        contentDescription = if (isFavorite) "Favorilerden Çıkar" else "Favorilere Ekle",
                        tint = if (isFavorite) Color(0xFFFF3D00) else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(actionIconSize)
                    )
                }
            }

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

            // ── 4. KONTROL BUTONLARI (3D Metalik / Neumorphic Tasarım) ──────────
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
                        .shadow(elevation = 5.dp, shape = CircleShape, ambientColor = Color.Black)
                        .clip(CircleShape)
                        .background(
                            if (playerState.isShuffle)
                                Brush.verticalGradient(
                                    listOf(animatedaccent.copy(alpha = 0.45f), animatedaccent.copy(alpha = 0.15f))
                                )
                            else Brush.verticalGradient(
                                listOf(Color(0xFF2B3346), Color(0xFF141824))
                            )
                        )
                        .border(
                            1.2.dp,
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

                // Önceki Şarkı (3D Metalik Kadran)
                Box(
                    modifier = Modifier
                        .size(skipBtnSize)
                        .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = Color.Black)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2C3446), Color(0xFF161B26), Color(0xFF0D1018))
                            )
                        )
                        .border(
                            width = 1.8.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.40f),
                                    ctrlBorder,
                                    Color.Black.copy(alpha = 0.80f)
                                )
                            ),
                            shape = CircleShape
                        )
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

                // ▶ / ⏸ Oynat/Duraklat — 3D DEV MERKEZ TUŞ (Kabartmalı ve Işıltılı)
                Box(
                    modifier = Modifier
                        .size(playBtnSize)
                        .shadow(
                            elevation = if (isCompact) 10.dp else 16.dp,
                            shape = CircleShape,
                            spotColor = animatedaccent.copy(alpha = 0.85f),
                            ambientColor = Color.Black
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.50f),
                                    animatedaccent,
                                    animatedaccent.copy(alpha = 0.85f),
                                    Color(0xFF003855)
                                )
                            )
                        )
                        .border(
                            width = 3.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.75f),
                                    animatedaccent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            ),
                            shape = CircleShape
                        )
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

                // Sonraki Şarkı (3D Metalik Kadran)
                Box(
                    modifier = Modifier
                        .size(skipBtnSize)
                        .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = Color.Black)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2C3446), Color(0xFF161B26), Color(0xFF0D1018))
                            )
                        )
                        .border(
                            width = 1.8.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.40f),
                                    ctrlBorder,
                                    Color.Black.copy(alpha = 0.80f)
                                )
                            ),
                            shape = CircleShape
                        )
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
                        .shadow(elevation = 5.dp, shape = CircleShape, ambientColor = Color.Black)
                        .clip(CircleShape)
                        .background(
                            if (isRepeatactive)
                                Brush.verticalGradient(
                                    listOf(repeatTInt.copy(alpha = 0.45f), repeatTInt.copy(alpha = 0.15f))
                                )
                            else Brush.verticalGradient(
                                listOf(Color(0xFF2B3346), Color(0xFF141824))
                            )
                        )
                        .border(
                            1.2.dp,
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

/**
 * 3 Boyutlu, metalik gümüş ve neon cyan derinlik efektli RidoPlay başlığı.
 */
@Composable
fun ThreeDimensionalBrandText(
    text: String = "RidoPlay",
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fontSize = if (isCompact) 15.sp else 19.sp
    val letterSpacing = if (isCompact) 1.0.sp else 1.4.sp

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. En Arka Katman: Derin Ortam Gölgesi (Ambient 3D Shadow)
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = letterSpacing,
            color = Color.Black.copy(alpha = 0.85f),
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(2f, 3f),
                    blurRadius = 6f
                )
            ),
            modifier = Modifier.offset(x = 1.5.dp, y = 2.dp)
        )

        // 2. Orta Yan Cephe Katmanı: Koyu Metalik Ekstrüzyon (3D Bevel Edge)
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = letterSpacing,
            color = Color(0xFF1E293B),
            modifier = Modifier.offset(x = 0.8.dp, y = 1.dp)
        )

        // 3. Işık Yansıma Katmanı: Hafif Neon Cyan/Turkuaz Işıma
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = letterSpacing,
            color = Color(0xFF00E5FF).copy(alpha = 0.45f),
            modifier = Modifier.offset(x = (-0.3).dp, y = (-0.3).dp)
        )

        // 4. Ön Parlak Yüzey Katmanı: Beyaz-Gümüş Degrade ve Neon Işıma
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = letterSpacing,
            style = TextStyle(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF), // Parlak üst ışık
                        Color(0xFFF8FAFC),
                        Color(0xFFE2E8F0), // Metalik gümüş
                        Color(0xFF94A3B8)  // Alt gölge
                    )
                ),
                shadow = Shadow(
                    color = Color(0xFF00E5FF).copy(alpha = 0.40f),
                    offset = Offset(0f, 0f),
                    blurRadius = 8f
                )
            )
        )
    }
}





















