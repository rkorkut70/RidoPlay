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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otomuzik.R
import com.example.otomuzik.model.PlayerState
import com.example.otomuzik.theme.CarCyan
import com.example.otomuzik.theme.CarTextMuted
import com.example.otomuzik.util.Id3Metadata
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gece sürüşü ve ekran kapatma (Screen Off / Gece Saati) modu.
 * Ekranı tamamen OLED siyahı yapar, göz almayı sıfıra indirir.
 * Sadece canlı dijital saati, tarihi ve o an çalan şarkıyı gösterir.
 * Ekrana herhangi bir yere dokunulduğunda normal görünüme döner.
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
    // Donanım geri tuşuna basıldığında ekranı kapat modundan çık
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
        ?: "Çalan parça yok"

    val songArtist = currentSongId3?.artist?.takeIf { it.isNotBlank() && it != "<unknown>" }
        ?: currentSong?.artist
        ?: "RidoPlay"

    val songAlbum = currentSongId3?.album?.takeIf { it.isNotBlank() && it != "<unknown>" }
        ?: currentSong?.album
        ?: ""

    // Alt bilgi metninin yumuşak nefes alma (pulsing) animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "NightPulse")
    val hintAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintAlpha"
    )

    // Tamamen saf OLED siyahı zemin
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismiss()
            }
    ) {
        // Arka planda çalan şarkının aksan renginde çok hafif, zarif ambiyans parıltısı (göz almaz)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.06f), Color.Transparent),
                        radius = 900f
                    )
                )
        )

        // ── MERKEZ İÇERİK: SAAT VE ŞARKI BİLGİSİ ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Üst Kısım: Sağ üstte küçük gece modu simgesi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF10141D))
                        .border(1.dp, Color(0xFF263042), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🌙", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gece Modu",
                            color = Color(0xFF8E9BAE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Orta Kısım: Büyük Dijital Saat + Tarih
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 94.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = formattedSeconds,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor.copy(alpha = 0.9f),
                        modifier = Modifier.padding(bottom = 14.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formattedDate,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFA0AEC0),
                    letterSpacing = 0.5.sp
                )
            }

            // Alt Kısım: O An Çalan Şarkı Kartı ve Mini Kontroller
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentSong != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .shadow(12.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF0C0F14).copy(alpha = 0.85f))
                            .border(1.2.dp, Color(0xFF222B38), RoundedCornerShape(24.dp))
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Albüm Kapağı (Varsa) veya 3D Müzik Simgesi
                            if (artworkBitmap != null) {
                                Image(
                                    bitmap = artworkBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .shadow(6.dp, RoundedCornerShape(14.dp))
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF161C26))
                                        .border(1.dp, Color(0xFF2C384A), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_music_note),
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(18.dp))

                            // Şarkı ve Sanatçı İsmi
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = songTitle,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = songArtist,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (songAlbum.isNotBlank()) {
                                        Text(
                                            text = " • $songAlbum",
                                            fontSize = 14.sp,
                                            color = CarTextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Mini Çalma Kontrolleri (Sürücü ekrandan çıkmadan parçayı değiştirebilir)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Önceki Parça
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF161B24))
                                        .clickable { onPrevious() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_skip_previous),
                                        contentDescription = "Önceki",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Oynat / Duraklat
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                        .clickable { onPlayPause() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                        ),
                                        contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Sonraki Parça
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF161B24))
                                        .clickable { onNext() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_skip_next),
                                        contentDescription = "Sonraki",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Alt Uyanma İpucu
                Text(
                    text = "Arayüze dönmek için ekrana dokunun",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = hintAlpha),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
