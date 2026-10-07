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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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

    val configuration = LocalConfiguration.current
    val isPhone = configuration.screenWidthDp < 600

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = (if (isPhone) {
                Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.95f)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            } else {
                Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.92f)
            })
                .clip(RoundedCornerShape(22.dp))
                .background(CarSurfaceDark)
                .border(1.5.dp, CarCyan, RoundedCornerShape(22.dp))
                .padding(if (isPhone) 12.dp else 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── ÜST BAR: Başlık, Sekmeler & Kapat ──────────────────────
                val tabsRow = @Composable {
                    Row(
                        modifier = Modifier
                            .then(if (isPhone) Modifier.fillMaxWidth() else Modifier)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CarSurfaceVariant)
                            .border(1.dp, CarBorder, RoundedCornerShape(12.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Sekme 0: ID3 Etiketleri
                        Box(
                            modifier = Modifier
                                .then(if (isPhone) Modifier.weight(1f) else Modifier)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedTab == 0) CarCyan else Color.Transparent)
                                .clickable { selectedTab = 0 }
                                .padding(horizontal = if (isPhone) 8.dp else 16.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🏷️ ID3 Bilgileri",
                                fontSize = if (isPhone) 12.sp else 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) Color(0xFF0C0E12) else CarTextPrimary
                            )
                        }

                        // Sekme 1: Albüm Kapakları Ara
                        val coverBadge = if (coverSuggestions.isNotEmpty()) " (${coverSuggestions.size})" else ""
                        Box(
                            modifier = Modifier
                                .then(if (isPhone) Modifier.weight(1f) else Modifier)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedTab == 1) CarCyan else Color.Transparent)
                                .clickable {
                                    selectedTab = 1
                                    if (coverSuggestions.isEmpty() && !isSearchingCover) {
                                        performCoverSearch()
                                    }
                                }
                                .padding(horizontal = if (isPhone) 8.dp else 16.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🖼️ Kapaklar$coverBadge",
                                fontSize = if (isPhone) 12.sp else 13.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) Color(0xFF0C0E12) else CarTextPrimary
                            )
                        }
                    }
                }

                val closeBtn = @Composable {
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

                if (isPhone) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✏️ Şarkı & Kapak Düzenle",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarCyan
                        )
                        closeBtn()
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    tabsRow()
                } else {
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
                        tabsRow()
                        closeBtn()
                    }
                }

                Spacer(modifier = Modifier.height(if (isPhone) 10.dp else 14.dp))

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
                        val coverPreviewBox = @Composable { sizeDp: Int ->
                            Box(
                                modifier = Modifier
                                    .size(sizeDp.dp)
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
                                        Text("🖼️", fontSize = if (sizeDp < 150) 28.sp else 38.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Kapak Yok", color = CarTextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        val coverButtons = @Composable {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        selectedTab = 1
                                        if (coverSuggestions.isEmpty() && !isSearchingCover) {
                                            performCoverSearch()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CarCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🔍 Kapak Bul (İnternet)", color = Color(0xFF0C0E12), fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                }

                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, CarBorder),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("📁 Galeriden Seç", color = CarTextPrimary, fontWeight = FontWeight.Medium, fontSize = 11.5.sp)
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
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.fillMaxWidth().height(30.dp)
                                    ) {
                                        Text("🗑️ Kapağı Kaldır", color = Color(0xFFFF5252), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                if (coverSearchMessage.isNotEmpty()) {
                                    Text(
                                        text = coverSearchMessage,
                                        color = if (coverSearchMessage.startsWith("✅")) CarCyan else if (coverSearchMessage.startsWith("❌")) Color(0xFFFF5252) else CarAmber,
                                        fontSize = 10.5.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        val id3Fields = @Composable {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Id3EditRow("🎵 Başlık (Title)", title) { title = it }
                                Id3EditRow("👤 Sanatçı (Artist)", artist) { artist = it }
                                Id3EditRow("💿 Albüm (Album)", album) { album = it }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Id3EditRow("🎭 Tür (Genre)", genre, modifier = Modifier.weight(1.2f)) { genre = it }
                                    Id3EditRow("📅 Yıl (Year)", year, modifier = Modifier.weight(0.8f)) { year = it }
                                }

                                Id3EditRow("📁 Dosya Adı (.${fileExt})", fileName) { fileName = it }

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

                        if (isPhone) {
                            // TELEFONDA: Dikey akıcı tek kolon, tüm alanlar geniş ve okunur
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Kapak kartı: Sol tarafta 105x105 kapak, sağ tarafta butonlar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(CarSurfaceVariant.copy(alpha = 0.5f))
                                        .border(1.dp, CarBorder, RoundedCornerShape(14.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    coverPreviewBox(105)
                                    Box(modifier = Modifier.weight(1f)) {
                                        coverButtons()
                                    }
                                }

                                // ID3 Metin Girişleri
                                id3Fields()
                            }
                        } else {
                            // TABLET/ARAÇTA: Çift kolonlu geniş düzen
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
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
                                    coverPreviewBox(190)
                                    coverButtons()
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    id3Fields()
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
                            // 1. Arama Çubuğu (Kırpılmayan, akıcı kapsül arama çubuğu)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CarSurfaceVariant)
                                    .border(1.dp, if (customSearchText.isNotBlank()) CarCyan else CarBorder, RoundedCornerShape(12.dp))
                                    .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔍", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = customSearchText,
                                    onValueChange = { customSearchText = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = CarTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(CarCyan),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 8.dp),
                                    decorationBox = { innerTextField ->
                                        if (customSearchText.isEmpty()) {
                                            Text(
                                                "Şarkı veya sanatçı adı...",
                                                color = CarTextMuted,
                                                fontSize = 13.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (customSearchText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { customSearchText = "" },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("✕", color = CarTextMuted, fontSize = 13.sp)
                                    }
                                }
                                Button(
                                    onClick = { performCoverSearch(customSearchText) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CarCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    enabled = !isSearchingCover && customSearchText.isNotBlank(),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Ara", color = Color(0xFF0C0E12), fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
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
                            val statusIndicator = @Composable {
                                if (isSearchingCover) {
                                    CircularProgressIndicator(color = CarCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("Resmi müzik veritabanlarında aranıyor...", color = CarCyan, fontSize = 11.5.sp)
                                } else if (isDownloadingCover) {
                                    CircularProgressIndicator(color = CarAmber, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("Kapak tam boyutta indiriliyor...", color = CarAmber, fontSize = 11.5.sp)
                                } else {
                                    val statusDisplayText = when {
                                        selectedSuggestionId != null && coverSearchMessage.startsWith("✅ Kapak seçildi") -> coverSearchMessage
                                        coverSuggestions.isNotEmpty() -> "✅ ${coverSuggestions.size} kapak bulundu (Grup ${currentPage + 1}/$totalPages)"
                                        else -> coverSearchMessage.ifEmpty { "Beğendiğiniz kapağa dokunarak seçebilirsiniz." }
                                    }
                                    Text(
                                        text = statusDisplayText,
                                        color = if (statusDisplayText.startsWith("✅")) CarCyan else if (statusDisplayText.startsWith("⚠️")) CarAmber else CarTextPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            val paginationControls = @Composable {
                                if (coverSuggestions.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { if (currentPage > 0) currentPage-- },
                                            enabled = currentPage > 0 && !isSearchingCover,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CarSurfaceVariant,
                                                disabledContainerColor = CarSurfaceVariant.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (currentPage > 0) CarCyan else CarBorder),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text(
                                                text = "◀ Önceki",
                                                color = if (currentPage > 0) CarCyan else CarTextMuted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        val startNum = currentPage * pageSize + 1
                                        val endNum = minOf((currentPage + 1) * pageSize, coverSuggestions.size)
                                        Text(
                                            text = "$startNum-$endNum / ${coverSuggestions.size}",
                                            color = CarTextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

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
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text(
                                                text = "Sonraki ▶",
                                                color = if (hasNext) CarCyan else CarTextMuted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            if (isPhone) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CarSurfaceVariant.copy(alpha = 0.6f))
                                        .border(1.dp, CarBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        statusIndicator()
                                    }
                                    if (coverSuggestions.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            paginationControls()
                                        }
                                    }
                                }
                            } else {
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        statusIndicator()
                                    }
                                    paginationControls()
                                }
                            }

                            // 3. Büyük Kapak Kartları Izgarası (Grid: Telefonda 2 sütun, Tablette 5 sütun)
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
                                        modifier = Modifier.fillMaxSize().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(CarSurfaceVariant)
                                                .border(1.dp, CarBorder, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🖼️", fontSize = 30.sp)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Resmi Albüm Kapakları Arayın", color = CarTextPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Yukarıdaki arama çubuğunu veya hızlı etiketleri kullanarak parça için yüksek çözünürlüklü kapaklar bulabilirsiniz.", color = CarTextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(if (isPhone) 2 else 5),
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
                                                // Büyük Resim Alanı (Kare en boy oranı)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
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

                Spacer(modifier = Modifier.height(if (isPhone) 8.dp else 14.dp))

                // ── ALT BAR: İptal / Kaydet veya Sekmeler Arası Geçiş ─────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedTab == 1) {
                        // Kapak sekmesindeyken sol tarafta "ID3 Sekmesine Dön"
                        Button(
                            onClick = { selectedTab = 0 },
                            colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CarBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = "🏷️ Bilgiler",
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
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
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
                            .weight(1.3f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "💾 Kaydet",
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
private fun Id3EditRow(
    label: String,
    value: TextFieldValue,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onValueChange: (TextFieldValue) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = CarTextMuted, fontSize = 12.sp) },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CarCyan,
            unfocusedBorderColor = CarBorder,
            focusedTextColor = CarTextPrimary,
            unfocusedTextColor = CarTextPrimary,
            focusedContainerColor = CarSurfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = CarSurfaceVariant.copy(alpha = 0.3f)
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
