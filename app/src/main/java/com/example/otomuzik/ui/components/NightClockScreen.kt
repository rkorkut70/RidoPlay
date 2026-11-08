package com.example.otomuzik.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.otomuzik.R
import com.example.otomuzik.model.PlayerState
import com.example.otomuzik.theme.CarCyan
import com.example.otomuzik.util.Id3Metadata
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * RidoPlay & RidoLauncher Birebir Ortak Gece Saati & Ekran Koruyucu (Night Clock & Screen Saver)
 * Saf OLED Siyahı zemin, göz almayan Luxury Ice Blue dijital saat, büyük tarih ve
 * alt kısımda büyütülmüş dokunmatik kontrollere sahip şık çalan şarkı kartı.
 */
@Composable
fun NightClockScreen(
    playerState: PlayerState,
    artworkBitmap: Bitmap?,
    currentSongId3: Id3Metadata? = null,
    accentColor: Color = CarCyan,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Donanım geri tuşuna basıldığında ekran koruyucudan çık
    BackHandler {
        onDismiss()
    }

    // Canlı saat sayacı (Her saniye güncellenir)
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            delay(1000L)
        }
    }

    val locale = remember { Locale.forLanguageTag("tr-TR") }
    val timeFormat = remember { SimpleDateFormat("HH:mm", locale) }
    val secondsFormat = remember { SimpleDateFormat(":ss", locale) }
    val dateFormat = remember { SimpleDateFormat("d MMMM yyyy, EEEE", locale) }

    val formattedTime = timeFormat.format(currentTime.time)
    val formattedSeconds = secondsFormat.format(currentTime.time)
    val formattedDate = dateFormat.format(currentTime.time)

    val currentSong = playerState.currentSong
    val isPlaying = playerState.isPlaying

    val songTitle = currentSongId3?.title?.takeIf { it.isNotBlank() }
        ?: currentSong?.title
        ?: ""

    val songArtist = currentSongId3?.artist?.takeIf { it.isNotBlank() && it != "<unknown>" }
        ?: currentSong?.artist
        ?: "RidoPlay"

    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600 || configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    // Alt/üst ipucu ve butonlar için yumuşak nefes alma animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "night_pulse")
    val hintAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_alpha"
    )

    // RidoLauncher CyberCockpit Gece Saati Renkleri (Göz almayan Luxury Ice Blue)
    val iceBlue = Color(0xFF38BDF8)
    val darkBlue = Color(0xFF0284C7)

    // Fullscreen Pure OLED Black Background (Sanki Ekran Kapalı Gibi - Tam Siyah)
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(150f)
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isCompact) 20.dp else 40.dp,
                    vertical = if (isCompact) 18.dp else 32.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Minimalist Night Mode Indicator & Close Hint (Kutusuz / Saf Siyah Zemin)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bedtime),
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Gece Saati",
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Kapatmak için ekrana dokunun",
                    color = Color(0xFF334155).copy(alpha = hintAlpha),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Center: Massive Digital Clock & Full Date (Göz Almayan Yumuşak OLED Tonları)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = if (isCompact) 76.sp else 108.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFFCBD5E1).copy(alpha = 0.88f),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = formattedSeconds,
                        fontSize = if (isCompact) 32.sp else 42.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF64748B).copy(alpha = 0.70f),
                        modifier = Modifier.padding(bottom = if (isCompact) 10.dp else 18.dp, start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formattedDate,
                    fontSize = if (isCompact) 18.sp else 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF475569),
                    letterSpacing = 0.5.sp
                )
            }

            // Bottom: Floating Now Playing (Kutusuz / Kart Çerçevesi Yok / Tamamen Siyah Zemin Üzerinde)
            if (currentSong != null && (songTitle.isNotBlank() || songArtist.isNotBlank())) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(if (isCompact) 0.94f else 0.72f)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (artworkBitmap != null) {
                            Image(
                                bitmap = artworkBitmap.asImageBitmap(),
                                contentDescription = "Album Art",
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF111827)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_music_note),
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = songTitle.ifEmpty { "Bilinmeyen Parça" },
                                color = Color(0xFFE2E8F0),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = songArtist.ifEmpty { "RidoPlay" },
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Floating Mini Playback Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_skip_previous),
                                contentDescription = "Prev",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                ),
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_skip_next),
                                contentDescription = "Next",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
