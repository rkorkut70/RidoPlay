package com.example.otomuzik.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.otomuzik.model.Song
import com.example.otomuzik.theme.CarAmber
import com.example.otomuzik.theme.CarBorder
import com.example.otomuzik.theme.CarCyan
import com.example.otomuzik.theme.CarSurfaceDark
import com.example.otomuzik.theme.CarSurfaceVariant
import com.example.otomuzik.theme.CarTextMuted
import com.example.otomuzik.theme.CarTextPrimary
import com.example.otomuzik.theme.CarTextSecondary
import com.example.otomuzik.util.CoverSuggestion
import com.example.otomuzik.util.Id3Helper
import com.example.otomuzik.util.MusicCoverSearcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun Id3InfoDialog(
    song: Song,
    onDismiss: () -> Unit,
    onSave: (newFileName: String, title: String, artist: String, album: String, genre: String, year: String, artworkBytes: ByteArray?) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val songFile = remember(song.path) { File(song.path) }
    val fileExt = remember(song.path) { songFile.extension.ifEmpty { "mp3" } }

    // Sekme durumu: 0 = ID3 Etiketleri, 1 = Albüm Kapakları Arama
    var selectedTab by remember { mutableIntStateOf(0) }

    var fileName by remember(song.path) { mutableStateOf(TextFieldValue(songFile.nameWithoutExtension)) }
    var title by remember { mutableStateOf(TextFieldValue(song.title)) }
    var artist by remember { mutableStateOf(TextFieldValue(song.artist)) }
    var album by remember { mutableStateOf(TextFieldValue(song.album)) }
    var genre by remember { mutableStateOf(TextFieldValue(song.genre)) }
    var year by remember { mutableStateOf(TextFieldValue(song.year)) }
    var artworkBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Kapak Arama Durumu
    var isSearchingCover by remember { mutableStateOf(false) }
    var isDownloadingCover by remember { mutableStateOf(false) }
    var coverSearchMessage by remember { mutableStateOf("") }
    var coverSuggestions by remember { mutableStateOf<List<CoverSuggestion>>(emptyList()) }
    var suggestionBitmaps by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    var selectedSuggestionId by remember { mutableStateOf<String?>(null) }

    // Arama Metni (varsayılan: Sanatçı + Parça veya Albüm)
    val defaultSearchQuery = remember(song) {
        listOf(song.artist, song.title).filter { it.isNotBlank() && !it.contains("bilinmeyen", ignoreCase = true) }.joinToString(" ")
    }
    var customSearchText by remember { mutableStateOf(defaultSearchQuery) }

    // Sayfalama (Pagination): 10'lu gruplar
    val pageSize = 10
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = remember(coverSuggestions.size) {
        maxOf(1, (coverSuggestions.size + pageSize - 1) / pageSize)
    }
    val currentSuggestions = remember(coverSuggestions, currentPage) {
        coverSuggestions.drop(currentPage * pageSize).take(pageSize)
    }

    val coroutineScope = rememberCoroutineScope()

    // Kapak Arama Fonksiyonu
    fun performCoverSearch(query: String = customSearchText) {
        coroutineScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isSearchingCover = true
                coverSearchMessage = "'${query.trim()}' için resmi müzik veritabanlarında aranıyor..."
                currentPage = 0
            }
            val list = MusicCoverSearcher.searchCoverSuggestions(
                artist = artist.text,
                title = title.text,
                album = album.text,
                fileName = songFile.name,
                customQuery = query,
                limit = 50
            )
            withContext(Dispatchers.Main) {
                coverSuggestions = list
                isSearchingCover = false
                if (list.isNotEmpty()) {
                    coverSearchMessage = "✅ ${list.size} adet resmi müzik kapağı bulundu"
                } else {
                    coverSearchMessage = "⚠️ Aradığınız kriterlere uygun kapak bulunamadı. Farklı kelimeler deneyin."
                }
            }
        }
    }

    // Galeriden / Cihazdan resim seçici launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val rawBytes = stream.readBytes()
                        val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                        if (bmp != null) {
                            val maxDim = 800
                            val scaled = if (bmp.width > maxDim || bmp.height > maxDim) {
                                val ratio = bmp.width.toFloat() / bmp.height.toFloat()
                                val (w, h) = if (ratio > 1f) maxDim to (maxDim / ratio).toInt() else (maxDim * ratio).toInt() to maxDim
                                Bitmap.createScaledBitmap(bmp, w, h, true)
                            } else bmp
                            val bos = ByteArrayOutputStream()
                            scaled.compress(Bitmap.CompressFormat.JPEG, 90, bos)
                            val finalBytes = bos.toByteArray()
                            withContext(Dispatchers.Main) {
                                artworkBytes = finalBytes
                                coverSearchMessage = "✅ Galeriden resim seçildi"
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                coverSearchMessage = "⚠️ Geçersiz resim dosyası"
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        coverSearchMessage = "⚠️ Resim yüklenemedi: ${e.message}"
                    }
                }
            }
        }
    }

    // Mevcut şarkının albüm kapağını başlangıçta yükle
    LaunchedEffect(song.path) {
        val existing = withContext(Dispatchers.IO) {
            Id3Helper.getArtworkBytes(song, context)
        }
        if (existing != null) artworkBytes = existing
    }

    // Görünür olan 10'lu kapak grubunun küçük resimlerini (thumbnails) arka planda hızlıca indir
    LaunchedEffect(currentSuggestions) {
        for (item in currentSuggestions) {
            if (!suggestionBitmaps.containsKey(item.id)) {
                coroutineScope.launch(Dispatchers.IO) {
                    val bmp = MusicCoverSearcher.downloadUrlToBitmap(item.thumbnailUrl)
                    if (bmp != null) {
                        withContext(Dispatchers.Main) {
                            suggestionBitmaps = suggestionBitmaps + (item.id to bmp)
                        }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(22.dp))
                .background(CarSurfaceDark)
                .border(1.5.dp, CarCyan, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── ÜST BAR: Başlık, Sekmeler & Kapat ──────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✏️ Şarkı & Kapak Düzenle",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarCyan
                    )

                    // Sekme Butonları (Tabs)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CarSurfaceVariant)
                            .border(1.dp, CarBorder, RoundedCornerShape(12.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Sekme 0: ID3 Etiketleri
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedTab == 0) CarCyan else Color.Transparent)
                                .clickable { selectedTab = 0 }
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🏷️ ID3 Bilgileri",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) Color(0xFF0C0E12) else CarTextPrimary
                            )
                        }

                        // Sekme 1: Albüm Kapakları Ara
                        val coverBadge = if (coverSuggestions.isNotEmpty()) " (${coverSuggestions.size})" else ""
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedTab == 1) CarCyan else Color.Transparent)
                                .clickable {
                                    selectedTab = 1
                                    if (coverSuggestions.isEmpty() && !isSearchingCover) {
                                        performCoverSearch()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🖼️ Albüm Kapakları$coverBadge",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) Color(0xFF0C0E12) else CarTextPrimary
                            )
                        }
                    }

                    // Kapat Butonu
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CarSurfaceVariant)
                            .border(1.dp, CarBorder, CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✕", color = CarTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── İÇERİK: SEÇİLİ SEKMENİN GÖRÜNÜMÜ ─────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (selectedTab == 0) {
                        // ──────────────────────────────────────────────────────
                        // SEKME 0: ID3 ETİKETLERİ VE DETAYLAR
                        // ──────────────────────────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Sol Kolon: Albüm Kapağı Önizleme & Hızlı İşlemler
                            Column(
                                modifier = Modifier
                                    .width(220.dp)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Albüm Kapağı",
                                    color = CarTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                // Büyük Kapak Önizleme
                                Box(
                                    modifier = Modifier
                                        .size(190.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(CarSurfaceVariant)
                                        .border(2.dp, if (artworkBytes != null) CarCyan else CarBorder, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (artworkBytes != null) {
                                        val bitmap = remember(artworkBytes) {
                                            BitmapFactory.decodeByteArray(artworkBytes, 0, artworkBytes!!.size)
                                        }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Albüm Kapağı",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("🖼️", fontSize = 38.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("Kapak Yok", color = CarTextMuted, fontSize = 12.sp)
                                        }
                                    }
                                }

                                // Kapak Bul Butonu (Kapak arama sekmesine geçirir)
                                Button(
                                    onClick = {
                                        selectedTab = 1
                                        if (coverSuggestions.isEmpty() && !isSearchingCover) {
                                            performCoverSearch()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CarCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🔍 Kapak Bul (İnternet)", color = Color(0xFF0C0E12), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                // Galeriden Seç
                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, CarBorder),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("📁 Galeriden Seç", color = CarTextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                }

                                if (artworkBytes != null) {
                                    Button(
                                        onClick = {
                                            artworkBytes = null
                                            selectedSuggestionId = null
                                            coverSearchMessage = "Kapak kaldırıldı"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF5252)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.fillMaxWidth().height(32.dp)
                                    ) {
                                        Text("🗑️ Kapağı Kaldır", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                if (coverSearchMessage.isNotEmpty()) {
                                    Text(
                                        text = coverSearchMessage,
                                        color = if (coverSearchMessage.startsWith("✅")) CarCyan else if (coverSearchMessage.startsWith("❌")) Color(0xFFFF5252) else CarAmber,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Sağ Kolon: ID3 Etiket Alanları & Teknik Bilgiler
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Id3EditRow("📁 Dosya Adı (.${fileExt})", fileName) { fileName = it }
                                Id3EditRow("🎵 Başlık (Title)", title) { title = it }
                                Id3EditRow("👤 Sanatçı (Artist)", artist) { artist = it }
                                Id3EditRow("💿 Albüm (Album)", album) { album = it }
                                Id3EditRow("🎭 Tür (Genre)", genre) { genre = it }
                                Id3EditRow("📅 Yıl (Year)", year) { year = it }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Salt Okunur Detaylar Kartı
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CarSurfaceVariant)
                                        .border(1.dp, CarBorder, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (song.trackNumber.isNotEmpty()) Id3ReadRow("Parça No", song.trackNumber)
                                    Id3ReadRow("Süre", song.durationFormatted)
                                    if (song.bitrate.isNotEmpty() || song.format.isNotEmpty()) {
                                        Id3ReadRow("Format", "${song.format.uppercase()} ${song.bitrate}".trim())
                                    }
                                    if (song.fileSizeFormatted.isNotEmpty()) Id3ReadRow("Boyut", song.fileSizeFormatted)
                                    if (song.path.isNotEmpty()) Id3ReadRow("Yol", song.path, isMonospace = true)
                                }
                            }
                        }
                    } else {
                        // ──────────────────────────────────────────────────────
                        // SEKME 1: BÜYÜK ALBÜM KAPAKLARI ARAMA & 10'LU GRUPLAR
                        // ──────────────────────────────────────────────────────
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Arama Çubuğu ve Hızlı Arama Butonları
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customSearchText,
                                    onValueChange = { customSearchText = it },
                                    placeholder = { Text("Sanatçı, şarkı veya albüm adı yazın... (örn: Sezen Aksu Sen Ağlama)", fontSize = 12.sp, color = CarTextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = CarSurfaceVariant,
                                        unfocusedContainerColor = CarSurfaceVariant,
                                        focusedTextColor = CarTextPrimary,
                                        unfocusedTextColor = CarTextPrimary,
                                        cursorColor = CarCyan,
                                        focusedBorderColor = CarCyan,
                                        unfocusedBorderColor = CarBorder
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Button(
                                    onClick = { performCoverSearch(customSearchText) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CarCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    enabled = !isSearchingCover && customSearchText.isNotBlank(),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("🔍 Ara", color = Color(0xFF0C0E12), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            // Hızlı Arama Öneri Çipleri
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Hızlı Arama:", color = CarTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)

                                if (artist.text.isNotBlank() && title.text.isNotBlank()) {
                                    QuickSearchChip("🎵 ${artist.text} - ${title.text}") {
                                        customSearchText = "${artist.text} ${title.text}"
                                        performCoverSearch(customSearchText)
                                    }
                                }
                                if (artist.text.isNotBlank() && album.text.isNotBlank() && album.text != title.text) {
                                    QuickSearchChip("💿 ${artist.text} - ${album.text}") {
                                        customSearchText = "${artist.text} ${album.text}"
                                        performCoverSearch(customSearchText)
                                    }
                                }
                                if (artist.text.isNotBlank()) {
                                    QuickSearchChip("👤 Yalnızca ${artist.text}") {
                                        customSearchText = artist.text
                                        performCoverSearch(customSearchText)
                                    }
                                }
                            }

                            // 2. Durum ve Sayfalama (Pagination) Kontrolleri (Önceki 10 / Sonraki 10)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CarSurfaceVariant.copy(alpha = 0.6f))
                                    .border(1.dp, CarBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Sol: Durum Bilgisi
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isSearchingCover) {
                                        CircularProgressIndicator(color = CarCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text("Resmi müzik veritabanlarında aranıyor...", color = CarCyan, fontSize = 12.sp)
                                    } else if (isDownloadingCover) {
                                        CircularProgressIndicator(color = CarAmber, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text("Kapak tam boyutta indiriliyor...", color = CarAmber, fontSize = 12.sp)
                                    } else {
                                        val statusDisplayText = when {
                                            selectedSuggestionId != null && coverSearchMessage.startsWith("✅ Kapak seçildi") -> coverSearchMessage
                                            coverSuggestions.isNotEmpty() -> "✅ ${coverSuggestions.size} adet resmi müzik kapağı bulundu (Grup ${currentPage + 1} / $totalPages)"
                                            else -> coverSearchMessage.ifEmpty { "Beğendiğiniz kapağa dokunarak seçebilirsiniz." }
                                        }
                                        Text(
                                            text = statusDisplayText,
                                            color = if (statusDisplayText.startsWith("✅")) CarCyan else CarTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Sağ: 10'lu Grup Sayfalama Butonları
                                if (coverSuggestions.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Önceki 10
                                        Button(
                                            onClick = { if (currentPage > 0) currentPage-- },
                                            enabled = currentPage > 0 && !isSearchingCover,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CarSurfaceVariant,
                                                disabledContainerColor = CarSurfaceVariant.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (currentPage > 0) CarCyan else CarBorder),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(
                                                text = "◀ Önceki 10",
                                                color = if (currentPage > 0) CarCyan else CarTextMuted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Grup Göstergesi
                                        val startNum = currentPage * pageSize + 1
                                        val endNum = minOf((currentPage + 1) * pageSize, coverSuggestions.size)
                                        Text(
                                            text = "$startNum-$endNum / ${coverSuggestions.size}",
                                            color = CarTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        // Sonraki 10
                                        val hasNext = currentPage < totalPages - 1
                                        Button(
                                            onClick = { if (hasNext) currentPage++ },
                                            enabled = hasNext && !isSearchingCover,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CarSurfaceVariant,
                                                disabledContainerColor = CarSurfaceVariant.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (hasNext) CarCyan else CarBorder),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(
                                                text = "Sonraki 10 ▶",
                                                color = if (hasNext) CarCyan else CarTextMuted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Büyük Kapak Kartları Izgarası (Grid: 5 sütun x 2 satır = 10 kart)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                if (isSearchingCover && coverSuggestions.isEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(color = CarCyan, modifier = Modifier.size(44.dp))
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text("Apple Music ve Deezer üzerinde resmi kapaklar aranıyor...", color = CarTextPrimary, fontSize = 13.sp)
                                    }
                                } else if (coverSuggestions.isEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text("🖼️", fontSize = 48.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Kapak aramak için yukarıdaki '🔍 Ara' butonuna basın.", color = CarTextMuted, fontSize = 13.sp)
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(5),
                                        contentPadding = PaddingValues(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(currentSuggestions) { item ->
                                            val isSelected = selectedSuggestionId == item.id
                                            val thumbBmp = suggestionBitmaps[item.id]

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) CarCyan.copy(alpha = 0.20f) else CarSurfaceVariant)
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 1.dp,
                                                        color = if (isSelected) CarCyan else CarBorder,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        selectedSuggestionId = item.id
                                                        coroutineScope.launch(Dispatchers.IO) {
                                                            withContext(Dispatchers.Main) {
                                                                isDownloadingCover = true
                                                                coverSearchMessage = "Kapak tam çözünürlükte indiriliyor..."
                                                            }
                                                            val fullBytes = MusicCoverSearcher.downloadUrlToByteArray(item.fullImageUrl)
                                                            withContext(Dispatchers.Main) {
                                                                isDownloadingCover = false
                                                                if (fullBytes != null) {
                                                                    artworkBytes = fullBytes
                                                                    coverSearchMessage = "✅ Kapak seçildi: ${item.title} (${item.source})"
                                                                } else {
                                                                    coverSearchMessage = "⚠️ Kapak tam boyutta indirilemedi."
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .padding(7.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                // Büyük Resim Alanı (~145x145 dp)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(130.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(Color.Black),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (thumbBmp != null) {
                                                        Image(
                                                            bitmap = thumbBmp.asImageBitmap(),
                                                            contentDescription = item.title,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        CircularProgressIndicator(
                                                            color = CarCyan,
                                                            modifier = Modifier.size(24.dp),
                                                            strokeWidth = 2.dp
                                                        )
                                                    }

                                                    // Kaynak Etiketi (Apple Music / Deezer)
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopStart)
                                                            .padding(4.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color.Black.copy(alpha = 0.75f))
                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = item.source,
                                                            color = CarCyan,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    // Seçildi Rozeti
                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color.Black.copy(alpha = 0.35f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(CarCyan)
                                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                                            ) {
                                                                Text("✓ SEÇİLDİ", color = Color(0xFF0C0E12), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(5.dp))

                                                // Başlık ve Sanatçı
                                                Text(
                                                    text = item.title,
                                                    color = CarTextPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.artist,
                                                    color = CarTextSecondary,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── ALT BAR: İptal / Kaydet veya Sekmeler Arası Geçiş ─────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedTab == 1) {
                        // Kapak sekmesindeyken sol tarafta "ID3 Sekmesine Dön"
                        Button(
                            onClick = { selectedTab = 0 },
                            colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CarBorder),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(
                                text = "🏷️ ID3 Bilgilerine Dön",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarTextPrimary
                            )
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CarBorder),
                            modifier = Modifier.width(130.dp).height(44.dp)
                        ) {
                            Text(
                                text = "İptal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarTextPrimary
                            )
                        }
                    }

                    // Kaydet Butonu (Her iki sekmeden de doğrudan kaydedilebilir!)
                    Button(
                        onClick = {
                            onSave(fileName.text, title.text, artist.text, album.text, genre.text, year.text, artworkBytes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CarCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(220.dp)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "💾 Değişiklikleri Kaydet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0C0E12)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSearchChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CarSurfaceVariant)
            .border(1.dp, CarBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = CarCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Id3EditRow(label: String, value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = CarTextMuted, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CarCyan,
            unfocusedBorderColor = CarBorder,
            focusedTextColor = CarTextPrimary,
            unfocusedTextColor = CarTextPrimary
        ),
        singleLine = true
    )
}

@Composable
private fun Id3ReadRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = CarTextMuted,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = CarTextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            fontFamily = if (isMonospace) FontFamily.Monospace else null
        )
    }
}
