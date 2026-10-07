package com.example.otomuzik.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.otomuzik.R
import com.example.otomuzik.model.LyricsData
import com.example.otomuzik.model.LyricsSource
import com.example.otomuzik.theme.*
import com.example.otomuzik.util.TurkishStringFixer
import java.net.URLEncoder

@Composable
fun LyricsDialog(
    lyricsData: LyricsData?,
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    songTitle: String,
    artistName: String,
    accentColor: Color,
    isSearching: Boolean,
    statusMessage: String?,
    autoSaveId3Lyrics: Boolean = true,
    onToggleAutoSaveId3: (Boolean) -> Unit = {},
    onSearchOnline: (artist: String, title: String) -> Unit,
    onSaveLyrics: ((onResult: (Boolean, String) -> Unit) -> Unit) = {},
    onPasteLyrics: (String) -> Unit = {},
    onSeekTo: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val prefs = remember(context) {
        context.getSharedPreferences("otomuzik_prefs", android.content.Context.MODE_PRIVATE)
    }
    var fontSizeDelta by remember { mutableStateOf(prefs.getInt("lyrics_font_delta", 0)) }
    var isSaving by remember { mutableStateOf(false) }
    var isSaved by remember(lyricsData?.songPath, lyricsData?.rawLyrics) {
        mutableStateOf(
            lyricsData?.source == LyricsSource.LOCAL_LRC ||
            lyricsData?.source == LyricsSource.LOCAL_TXT ||
            lyricsData?.source == LyricsSource.EMBEDDED_ID3
        )
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    // Açık temada neon/açık renklerin beyaz zeminde yüksek kontrastla görünmesi için düzenleme
    val effectiveAccent = if (isDark) accentColor else {
        if (accentColor.luminance() > 0.40f) Color(0xFF007A87) else accentColor
    }

    val launchWebSearch = { url: String ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Tarayıcı açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val openGoogleSearch = {
        val q = buildString {
            if (artistName.isNotBlank() && artistName != "Bilinmeyen Sanatçı") append(artistName).append(" ")
            append(songTitle)
            append(" şarkı sözleri")
        }.trim()
        launchWebSearch("https://www.google.com/search?q=" + URLEncoder.encode(q, "UTF-8"))
    }

    val openMusixmatchSearch = {
        val q = buildString {
            if (artistName.isNotBlank() && artistName != "Bilinmeyen Sanatçı") append(artistName).append(" ")
            append(songTitle)
        }.trim()
        launchWebSearch("https://www.musixmatch.com/search/" + URLEncoder.encode(q, "UTF-8"))
    }

    val openLyricFindSearch = {
        val q = buildString {
            if (artistName.isNotBlank() && artistName != "Bilinmeyen Sanatçı") append(artistName).append(" ")
            append(songTitle)
            append(" lyrics lyricfind")
        }.trim()
        launchWebSearch("https://www.google.com/search?q=" + URLEncoder.encode(q, "UTF-8"))
    }

    val openGeniusSearch = {
        val q = buildString {
            if (artistName.isNotBlank() && artistName != "Bilinmeyen Sanatçı") append(artistName).append(" ")
            append(songTitle)
        }.trim()
        launchWebSearch("https://genius.com/search?q=" + URLEncoder.encode(q, "UTF-8"))
    }

    val pasteFromClipboard = {
        val clipText = clipboardManager.getText()?.text
        if (!clipText.isNullOrBlank()) {
            onPasteLyrics(clipText)
        } else {
            Toast.makeText(context, "Pano boş! Önce sözleri kopyalayın.", Toast.LENGTH_SHORT).show()
        }
    }

    var showSearchBar by remember { mutableStateOf(false) }
    var searchArtistText by remember(artistName) { mutableStateOf(artistName) }
    var searchTitleText by remember(songTitle) { mutableStateOf(songTitle) }
    var autoScrollEnabled by remember { mutableStateOf(true) }

    val listState = remember(songTitle, lyricsData?.songPath, lyricsData?.rawLyrics) { LazyListState(0, 0) }

    val lines = lyricsData?.lines ?: emptyList()
    val isSynced = lyricsData?.isSynced == true && lines.any { it.timestampMs >= 0 }

    val activeIndex = remember(currentPositionMs, lines, isSynced) {
        if (!isSynced || lines.isEmpty()) -1
        else lines.indexOfLast { it.timestampMs <= currentPositionMs }
    }

    // 1. Şarkı veya söz değiştiğinde listenin en başına (0. satıra) dön
    LaunchedEffect(songTitle, lyricsData?.songPath, lyricsData?.rawLyrics) {
        listState.scrollToItem(0)
    }

    // 2. Senkronize sözler için: Şarkı ilerledikçe aktif satırı ortalayacak şekilde kaydır
    LaunchedEffect(activeIndex, autoScrollEnabled, isSynced) {
        if (autoScrollEnabled && isSynced) {
            val targetScrollIndex = if (activeIndex >= 2) activeIndex - 2 else 0
            if (targetScrollIndex != listState.firstVisibleItemIndex) {
                listState.animateScrollToItem(targetScrollIndex)
            }
        }
    }

    // 3. Düz metin (Senkronize olmayan) sözler için: Her zaman en baştan başlasın
    LaunchedEffect(isSynced, songTitle, lyricsData?.songPath) {
        if (!isSynced) {
            listState.scrollToItem(0)
        }
    }

    // Font Boyutu Hesaplaması
    val baseNormalSp = 23 + fontSizeDelta
    val baseActiveSp = 30 + fontSizeDelta

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.95f) // Araç/tablet ekranının %95'i
                .clip(RoundedCornerShape(24.dp))
                .background(CarSurfaceDark)
                .border(
                    BorderStroke(1.5.dp, if (isDark) effectiveAccent.copy(alpha = 0.35f) else Color(0xFFCBD5E1)),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // --- ÜST BAŞLIK ALANI ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = songTitle.ifBlank { "Şarkı Sözleri" },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Senkronize / Düz Metin Rozeti
                            if (isSynced) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = effectiveAccent.copy(alpha = if (isDark) 0.2f else 0.14f),
                                    border = BorderStroke(1.dp, effectiveAccent.copy(alpha = if (isDark) 0.5f else 0.8f))
                                ) {
                                    Text(
                                        text = "⚡ Senkronize LRC",
                                        color = effectiveAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else if (lyricsData != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CarSurfaceVariant,
                                    border = BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1))
                                ) {
                                    Text(
                                        text = "📄 Düz Metin",
                                        color = CarTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = artistName.ifBlank { "Bilinmeyen Sanatçı" },
                                fontSize = 15.sp,
                                color = CarTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (lyricsData != null) {
                                val sourceLabel = when (lyricsData.source) {
                                    LyricsSource.ONLINE_LRCLIB -> "• LRCLIB"
                                    LyricsSource.ONLINE_OVH -> "• lyrics.ovh"
                                    LyricsSource.MANUAL_CLIPBOARD -> "• Panodan Eklendi"
                                    LyricsSource.LOCAL_LRC -> "• Yerel .lrc"
                                    LyricsSource.LOCAL_TXT -> "• Yerel .txt"
                                    LyricsSource.EMBEDDED_ID3 -> "• ID3 Gömülü"
                                    LyricsSource.NONE -> ""
                                }
                                if (sourceLabel.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = sourceLabel,
                                        fontSize = 13.sp,
                                        color = CarTextMuted
                                    )
                                }
                            }
                        }
                    }

                    // Aksiyon Butonları
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Arama Çubuğunu Aç/Kapat Butonu
                        IconButton(
                            onClick = { showSearchBar = !showSearchBar },
                            modifier = Modifier
                                .background(
                                    if (showSearchBar) effectiveAccent.copy(alpha = if (isDark) 0.25f else 0.15f) else CarSurfaceVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, if (showSearchBar) effectiveAccent else (if (isDark) CarBorder else Color(0xFFCBD5E1))),
                                    RoundedCornerShape(12.dp)
                                )
                                .size(42.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = "Söz Ara",
                                tint = if (showSearchBar) effectiveAccent else CarTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Font Büyüklüğü Ayarı (A- / A+)
                        Row(
                            modifier = Modifier
                                .background(CarSurfaceVariant, RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)), RoundedCornerShape(12.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (fontSizeDelta > -6) {
                                        fontSizeDelta -= 2
                                        prefs.edit().putInt("lyrics_font_delta", fontSizeDelta).apply()
                                    }
                                },
                                enabled = fontSizeDelta > -6,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Text(
                                    text = "A⁻",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fontSizeDelta > -6) CarTextPrimary else CarTextMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(if (isDark) CarBorder else Color(0xFFCBD5E1))
                            )

                            IconButton(
                                onClick = {
                                    if (fontSizeDelta < 14) {
                                        fontSizeDelta += 2
                                        prefs.edit().putInt("lyrics_font_delta", fontSizeDelta).apply()
                                    }
                                },
                                enabled = fontSizeDelta < 14,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Text(
                                    text = "A⁺",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fontSizeDelta < 14) CarTextPrimary else CarTextMuted
                                )
                            }
                        }

                        // 🌐 Google Butonu
                        Button(
                            onClick = openGoogleSearch,
                            colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                            border = BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text(text = "🌐", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Google", color = CarTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // 🎵 Musixmatch Butonu
                        Button(
                            onClick = openMusixmatchSearch,
                            colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                            border = BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text(text = "🎵", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Musixmatch", color = CarTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // 📋 Panodan Yapıştır Butonu
                        Button(
                            onClick = pasteFromClipboard,
                            colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                            border = BorderStroke(1.dp, effectiveAccent.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text(text = "📋", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Yapıştır", color = effectiveAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // 🏷️ Oto ID3 Aç/Kapat Butonu
                        Button(
                            onClick = {
                                val nextState = !autoSaveId3Lyrics
                                onToggleAutoSaveId3(nextState)
                                Toast.makeText(
                                    context,
                                    if (nextState) "✓ Şarkı sözleri otomatik ID3 tag'e kaydedilecek" else "Otomatik ID3 kaydı kapatıldı",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (autoSaveId3Lyrics) effectiveAccent.copy(alpha = if (isDark) 0.2f else 0.15f) else CarSurfaceVariant
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (autoSaveId3Lyrics) effectiveAccent.copy(alpha = 0.5f) else (if (isDark) CarBorder else Color(0xFFCBD5E1))
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text(text = "🏷️", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (autoSaveId3Lyrics) "ID3: Açık" else "ID3: Kapalı",
                                color = if (autoSaveId3Lyrics) effectiveAccent else CarTextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // "Kaydet" Butonu - Anında Toast geri bildirimi verir ve kaydedilince pasif hale gelir
                        if (lyricsData != null) {
                            Button(
                                onClick = {
                                    if (!isSaved && !isSaving) {
                                        isSaving = true
                                        onSaveLyrics { success, msg ->
                                            isSaving = false
                                            if (success) {
                                                isSaved = true
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isSaved && !isSaving,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSaved) CarSurfaceVariant.copy(alpha = 0.6f) else CarSurfaceVariant,
                                    disabledContainerColor = CarSurfaceVariant.copy(alpha = 0.6f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSaved) Color(0xFF10B981).copy(alpha = 0.5f) else (if (isDark) CarBorder else Color(0xFFCBD5E1))
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = effectiveAccent
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Kaydediliyor...",
                                        color = effectiveAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else if (isSaved) {
                                    Text(text = "✓", fontSize = 14.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Kaydedildi",
                                        color = Color(0xFF10B981),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(text = "💾", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Kaydet",
                                        color = effectiveAccent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Senkronize veya Düz Metin ise Oto-Kaydır Aç/Kapa Butonu
                        Button(
                            onClick = { autoScrollEnabled = !autoScrollEnabled },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (autoScrollEnabled) effectiveAccent.copy(alpha = if (isDark) 0.2f else 0.15f) else CarSurfaceVariant
                            ),
                            border = BorderStroke(1.dp, if (autoScrollEnabled) effectiveAccent.copy(alpha = 0.5f) else (if (isDark) CarBorder else Color(0xFFCBD5E1))),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text(
                                text = if (autoScrollEnabled) "⇅ Oto: Açık" else "⇅ Oto: Kapalı",
                                color = if (autoScrollEnabled) effectiveAccent else CarTextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Kapat Butonu
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(CarSurfaceVariant, RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)), RoundedCornerShape(12.dp))
                                .size(42.dp)
                        ) {
                            Text(text = "✕", color = CarTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // --- GELİŞMİŞ ARAMA ÇUBUĞU (Açıldığında Görünür) ---
                AnimatedVisibility(visible = showSearchBar) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .background(CarSurfaceVariant.copy(alpha = if (isDark) 0.6f else 0.9f), RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchArtistText,
                                onValueChange = { searchArtistText = it },
                                label = { Text("Sanatçı", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = effectiveAccent,
                                    unfocusedBorderColor = CarBorder,
                                    focusedTextColor = CarTextPrimary,
                                    unfocusedTextColor = CarTextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = searchTitleText,
                                onValueChange = { searchTitleText = it },
                                label = { Text("Şarkı Adı", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = effectiveAccent,
                                    unfocusedBorderColor = CarBorder,
                                    focusedTextColor = CarTextPrimary,
                                    unfocusedTextColor = CarTextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    onSearchOnline(searchArtistText, searchTitleText)
                                },
                                enabled = !isSearching,
                                colors = ButtonDefaults.buttonColors(containerColor = effectiveAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        color = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_search),
                                        contentDescription = null,
                                        tint = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Ara",
                                        color = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sağlayıcı arama butonları (Google, Musixmatch, LyricFind, Genius, Panodan Yapıştır)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = openGoogleSearch,
                                colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceDark),
                                border = BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(text = "🌐 Google", fontSize = 12.sp, color = CarTextPrimary, maxLines = 1)
                            }

                            Button(
                                onClick = openMusixmatchSearch,
                                colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceDark),
                                border = BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(text = "🎵 Musixmatch", fontSize = 12.sp, color = CarTextPrimary, maxLines = 1)
                            }

                            Button(
                                onClick = openLyricFindSearch,
                                colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceDark),
                                border = BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(text = "📑 LyricFind", fontSize = 12.sp, color = CarTextPrimary, maxLines = 1)
                            }

                            Button(
                                onClick = openGeniusSearch,
                                colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceDark),
                                border = BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(text = "🎶 Genius", fontSize = 12.sp, color = CarTextPrimary, maxLines = 1)
                            }

                            Button(
                                onClick = pasteFromClipboard,
                                colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceDark),
                                border = BorderStroke(1.dp, effectiveAccent.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.2f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(text = "📋 Yapıştır", fontSize = 12.sp, color = effectiveAccent, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }

                // Durum Mesajı Bildirimi (örn: "✓ Şarkı sözü bulundu!")
                if (!statusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 13.sp,
                        color = if (statusMessage.startsWith("✓")) effectiveAccent else CarTextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = if (isDark) CarBorder.copy(alpha = 0.5f) else Color(0xFFCBD5E1))
                Spacer(modifier = Modifier.height(10.dp))

                // --- ŞARKI SÖZLERİ ANA GÖVDE ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when {
                        // 1. Durum: Aranıyor ve henüz söz yok
                        isSearching && (lyricsData == null || lines.isEmpty()) -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = effectiveAccent,
                                    modifier = Modifier.size(52.dp),
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "LRCLIB & lyrics.ovh veritabanlarından şarkı sözleri aranıyor...",
                                    fontSize = 18.sp,
                                    color = CarTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // 2. Durum: Sözler yüklendi (Senkronize LRC veya Düz Metin)
                        lyricsData != null && lines.isNotEmpty() -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 36.dp)
                            ) {
                                itemsIndexed(lines) { index, line ->
                                    val isActive = isSynced && index == activeIndex
                                    val isNext = isSynced && activeIndex >= 0 && index == activeIndex + 1

                                    val lineAlpha by animateFloatAsState(
                                        targetValue = if (isSynced) {
                                            when {
                                                isActive -> 1f
                                                isNext -> if (isDark) 0.75f else 0.80f // Sıradaki satır aktif olandan daha az dikkat çeksin
                                                else -> if (isDark) 0.38f else 0.50f // Diğer satırlar hafif arka planda
                                            }
                                        } else 1f,
                                        label = "lineAlpha"
                                    )

                                    val scaleFontSize = when {
                                        isSynced && isActive -> baseActiveSp.sp
                                        isSynced && isNext -> (baseNormalSp + 2).sp
                                        else -> baseNormalSp.sp
                                    }
                                    val lineHeight = (scaleFontSize.value * 1.34f).sp

                                    val textColor = when {
                                        isSynced && isActive -> Color.White // Parlak beyaz
                                        isSynced && isNext -> if (isDark) Color(0xFFB0B0B0) else Color(0xFF475569) // Beyaza yakın gri
                                        !isDark -> Color(0xFF64748B) // Açık temada diğer satırlar
                                        else -> CarTextSecondary
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .then(
                                                when {
                                                    isSynced && isActive -> Modifier
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(
                                                                    effectiveAccent.copy(alpha = if (isDark) 0.25f else 0.18f),
                                                                    effectiveAccent.copy(alpha = if (isDark) 0.08f else 0.06f),
                                                                    Color.Transparent
                                                                )
                                                            )
                                                        )
                                                        .border(
                                                            BorderStroke(1.2.dp, effectiveAccent.copy(alpha = if (isDark) 0.5f else 0.75f)),
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                    isSynced && isNext -> Modifier
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(
                                                                    effectiveAccent.copy(alpha = if (isDark) 0.10f else 0.08f),
                                                                    effectiveAccent.copy(alpha = if (isDark) 0.03f else 0.02f),
                                                                    Color.Transparent
                                                                )
                                                            )
                                                        )
                                                        .border(
                                                            BorderStroke(0.9.dp, effectiveAccent.copy(alpha = if (isDark) 0.25f else 0.35f)),
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                    else -> Modifier
                                                }
                                            )
                                            .clickable(enabled = line.timestampMs >= 0) {
                                                if (line.timestampMs >= 0) {
                                                    onSeekTo(line.timestampMs)
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = if (isSynced && (isActive || isNext)) 10.dp else 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Aktif satır veya sonraki satır indikatörü
                                            if (isSynced) {
                                                if (isActive) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_play),
                                                        contentDescription = null,
                                                        tint = effectiveAccent,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                } else if (isNext) {
                                                    Text(
                                                        text = "›",
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = effectiveAccent.copy(alpha = if (isDark) 0.75f else 0.85f),
                                                        modifier = Modifier.width(20.dp),
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                } else {
                                                    Spacer(modifier = Modifier.width(28.dp))
                                                }
                                            }

                                            Text(
                                                text = TurkishStringFixer.fix(line.text).ifBlank { "♪ ♪ ♪" },
                                                fontSize = scaleFontSize,
                                                fontWeight = when {
                                                    isSynced && isActive -> FontWeight.Bold
                                                    isSynced && isNext -> FontWeight.SemiBold
                                                    else -> FontWeight.Normal
                                                },
                                                color = textColor.copy(alpha = lineAlpha),
                                                textAlign = if (isSynced) TextAlign.Start else TextAlign.Center,
                                                lineHeight = lineHeight,
                                                modifier = Modifier.weight(1f)
                                            )

                                            // Dokunarak Git (Zaman Etiketi)
                                            if (line.timestampMs >= 0) {
                                                val totalSec = line.timestampMs / 1000
                                                val m = totalSec / 60
                                                val s = totalSec % 60
                                                val timeFormatted = "%02d:%02d".format(m, s)

                                                Text(
                                                    text = timeFormatted,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isActive || isNext) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isActive) effectiveAccent else (if (isNext) effectiveAccent.copy(alpha = 0.7f) else CarTextMuted),
                                                    modifier = Modifier.padding(start = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Durum: Söz bulunamadı (Boş Ekran)
                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_music_note),
                                    contentDescription = null,
                                    tint = CarTextMuted,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Bu parça için şarkı sözü bulunamadı.",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarTextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Otomatik arama yapabilir ya da Google, Musixmatch ve LyricFind servislerinden sözleri bularak tek tıkla yapıştırabilirsiniz.",
                                    fontSize = 14.sp,
                                    color = CarTextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                // 1. Sıra: Çevrimiçi Ara & Panodan Yapıştır
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onSearchOnline(artistName, songTitle) },
                                        enabled = !isSearching,
                                        colors = ButtonDefaults.buttonColors(containerColor = effectiveAccent),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_search),
                                            contentDescription = null,
                                            tint = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Çevrimiçi Otomatik Ara (LRCLIB & ovh)",
                                            color = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Button(
                                        onClick = pasteFromClipboard,
                                        colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                                        border = BorderStroke(1.dp, effectiveAccent.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text(text = "📋", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Panodan Yapıştır",
                                            color = effectiveAccent,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 2. Sıra: Servis Arama Butonları (Google, Musixmatch, LyricFind, Genius)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = openGoogleSearch,
                                        colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                                        border = BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(42.dp)
                                    ) {
                                        Text(text = "🌐 Google", color = CarTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = openMusixmatchSearch,
                                        colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                                        border = BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(42.dp)
                                    ) {
                                        Text(text = "🎵 Musixmatch", color = CarTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = openLyricFindSearch,
                                        colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                                        border = BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(42.dp)
                                    ) {
                                        Text(text = "📑 LyricFind", color = CarTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = openGeniusSearch,
                                        colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                                        border = BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(42.dp)
                                    ) {
                                        Text(text = "🎶 Genius", color = CarTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = if (isDark) CarBorder.copy(alpha = 0.5f) else Color(0xFFCBD5E1))
                Spacer(modifier = Modifier.height(10.dp))

                // --- ALT MİNİ ÇALAR KONTROL ÇUBUĞU (KARAOKE İÇİN) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CarSurfaceVariant.copy(alpha = if (isDark) 0.4f else 0.9f), RoundedCornerShape(16.dp))
                        .border(BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Süre Bilgisi
                    val posSec = currentPositionMs / 1000
                    val durSec = durationMs / 1000
                    val posStr = "%02d:%02d".format(posSec / 60, posSec % 60)
                    val durStr = "%02d:%02d".format(durSec / 60, durSec % 60)

                    Text(
                        text = "$posStr / $durStr",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = effectiveAccent
                    )

                    // Çalma Kontrolleri
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier
                                .background(CarSurfaceVariant, CircleShape)
                                .border(BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)), CircleShape)
                                .size(42.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_skip_previous),
                                contentDescription = "Önceki",
                                tint = CarTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier
                                .background(effectiveAccent, CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                                contentDescription = if (isPlaying) "Duraklat" else "Çal",
                                tint = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        IconButton(
                            onClick = onNext,
                            modifier = Modifier
                                .background(CarSurfaceVariant, CircleShape)
                                .border(BorderStroke(1.dp, if (isDark) CarBorder else Color(0xFFCBD5E1)), CircleShape)
                                .size(42.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_skip_next),
                                contentDescription = "Sonraki",
                                tint = CarTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Şarkı sözü satırına dokunma ipucu
                    if (isSynced) {
                        Text(
                            text = "💡 Satıra dokunarak atlayın",
                            fontSize = 12.sp,
                            color = CarTextMuted
                        )
                    } else {
                        Text(
                            text = "⇅ Otomatik kaydırılıyor",
                            fontSize = 12.sp,
                            color = CarTextMuted
                        )
                    }
                }
            }
        }
    }
}
