package com.example.otomuzik.ui.components

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.otomuzik.R
import com.example.otomuzik.model.FolderItem
import com.example.otomuzik.model.Song
import com.example.otomuzik.theme.*
import java.io.File

/**
 * Kullanıcının kütüphaneye dahil edilecek müzik klasörlerini ve USB sürücülerini seçmesini sağlayan diyalog.
 * 
 * - USB Sürücüleri (Harici Bellek): Özel, belirgin kartlar ve tek dokunuşla açılıp kapanabilen Switch'ler.
 * - Dahili Hafıza: Müzik ve indirilenler klasörleri listesi.
 * - Araç kokpiti için optimize edilmiş büyük dokunma hedefleri ve yüksek kontrastlı tasarım.
 */
@Composable
fun FolderSelectionDialog(
    storageRoots: List<FolderItem>,
    includedFolders: Set<String>,
    allSongs: List<Song> = emptyList(),
    onApplyFolders: (Set<String>) -> Unit,
    onResetAll: (() -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    fun normPath(p: String): String = p.replace('\\', '/').trimEnd('/')

    val internalStorage = remember { Environment.getExternalStorageDirectory() }
    val internalStoragePath = remember(internalStorage) {
        internalStorage?.absolutePath?.let { normPath(it) } ?: "/storage/emulated/0"
    }

    fun isUsbPath(p: String): Boolean {
        val clean = normPath(p).lowercase()
        return (clean.startsWith("/storage/") && !clean.contains("emulated") && !clean.contains("self") && !clean.contains("knox")) ||
                clean.contains("udisk") ||
                clean.startsWith("/mnt/media_rw") ||
                clean.startsWith("/mnt/usb") ||
                clean.startsWith("/mnt/extsd") ||
                clean.startsWith("/mnt/sdcard2")
    }

    // 1. USB Sürücüleri
    val usbDrives = remember(storageRoots) {
        val list = mutableListOf<FolderItem>()
        val seen = mutableSetOf<String>()
        for (root in storageRoots) {
            val norm = normPath(root.path)
            if (isUsbPath(norm) && !seen.contains(norm)) {
                list.add(root)
                seen.add(norm)
            }
        }
        list
    }

    // 2. Dahili Hafıza ve Diğer Klasörler
    val internalFolders = remember(storageRoots, includedFolders, allSongs) {
        val list = linkedSetOf<String>()
        val commonNames = listOf("Music", "Download", "snaptube", "Podcasts", "Audiobooks", "Recordings", "Documents")

        if (internalStorage != null && internalStorage.exists()) {
            for (name in commonNames) {
                val f = File(internalStorage, name)
                if (f.exists() && f.isDirectory) {
                    list.add(f.absolutePath)
                }
            }
            try {
                internalStorage.listFiles()?.filter {
                    it.isDirectory &&
                    !it.isHidden &&
                    !it.name.startsWith(".") &&
                    !it.name.equals("Android", ignoreCase = true) &&
                    !commonNames.any { c -> c.equals(it.name, ignoreCase = true) }
                }?.forEach { f ->
                    if (f.canRead()) {
                        list.add(f.absolutePath)
                    }
                }
            } catch (_: Exception) {}
        }

        // Halihazırda taranmış şarkıların bulunduğu dahili klasörler
        for (song in allSongs) {
            val parent = File(song.path).parent
            if (!parent.isNullOrBlank()) {
                val cleanParent = normPath(parent)
                if (!isUsbPath(cleanParent) && !cleanParent.equals(internalStoragePath, ignoreCase = true)) {
                    list.add(parent)
                }
            }
        }

        // Daha önceden kaydedilmiş dahili klasörler
        for (f in includedFolders) {
            if (f.isNotBlank() && !isUsbPath(f) && !normPath(f).equals(internalStoragePath, ignoreCase = true)) {
                list.add(f)
            }
        }

        list.toList().sortedWith(Comparator { a, b ->
            val aName = File(a).name
            val bName = File(b).name
            when {
                aName.equals("Music", ignoreCase = true) -> -1
                bName.equals("Music", ignoreCase = true) -> 1
                aName.equals("Download", ignoreCase = true) -> -1
                bName.equals("Download", ignoreCase = true) -> 1
                else -> aName.compareTo(bName, ignoreCase = true)
            }
        })
    }

    // Tüm keşfedilen öğeler (USB + Dahili)
    val allDiscoveredPaths = remember(usbDrives, internalFolders) {
        val set = linkedSetOf<String>()
        usbDrives.forEach { set.add(it.path) }
        internalFolders.forEach { set.add(it) }
        set.toList()
    }

    // Yerel seçim durumu
    var selectedFolders by remember(includedFolders, allDiscoveredPaths) {
        val initial = mutableSetOf<String>()
        if (includedFolders.isNotEmpty()) {
            for (p in allDiscoveredPaths) {
                val cleanP = normPath(p)
                if (includedFolders.any { normPath(it).equals(cleanP, ignoreCase = true) }) {
                    initial.add(p)
                }
            }
            // Eğer dahil edilen klasörlerde hiç USB yoksa ama cihazda USB takılıysa varsayılan olarak USB'yi seçili yap
            val hasUsbSelected = initial.any { isUsbPath(it) }
            if (!hasUsbSelected) {
                usbDrives.forEach { initial.add(it.path) }
            }
        } else {
            // Varsayılan durum: Tüm USB sürücüler ve güvenilir müzik klasörleri otomatik seçili gelir
            usbDrives.forEach { initial.add(it.path) }
            for (f in internalFolders) {
                val lower = normPath(f).lowercase()
                val isExcluded = lower.contains("snaptube") || lower.contains("recording")
                if (!isExcluded) {
                    initial.add(f)
                }
            }
        }
        mutableStateOf(initial.toSet())
    }

    fun isFolderSelected(path: String): Boolean {
        val clean = normPath(path)
        return selectedFolders.any { normPath(it).equals(clean, ignoreCase = true) }
    }

    fun toggleFolder(path: String) {
        val clean = normPath(path)
        val currentlySelected = isFolderSelected(path)
        val nextSet = selectedFolders.filterNot { normPath(it).equals(clean, ignoreCase = true) }.toMutableSet()
        if (!currentlySelected) {
            nextSet.add(path)
        }
        selectedFolders = nextSet
    }

    val selectedSongsCount = remember(selectedFolders, allSongs) {
        if (selectedFolders.isEmpty()) {
            0
        } else {
            val normalizedSelected = selectedFolders.map { normPath(it) }
            allSongs.count { song ->
                val cleanSong = normPath(song.path)
                normalizedSelected.any { cleanSong.startsWith(it, ignoreCase = true) }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 620.dp)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(22.dp),
            color = CarSurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CarBorder),
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Başlık & Kapat Butonu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CarCyan.copy(alpha = 0.15f))
                                .border(1.dp, CarCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📁", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Müzik Kütüphanelerini Yönet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "USB sürücüleri ve taranacak müzik klasörlerini seçin",
                                fontSize = 12.sp,
                                color = CarTextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text(text = "✕", fontSize = 18.sp, color = CarTextMuted, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // İstatistik ve Hızlı İşlem Paneli
                Surface(
                    color = CarSurfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarBorder.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Seçili Kaynak Rozeti
                            Surface(
                                color = Color(0xFF131826),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CarCyan.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Seçili: ", fontSize = 11.5.sp, color = CarTextMuted)
                                    Text(
                                        text = "${selectedFolders.size} kaynak",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CarCyan
                                    )
                                }
                            }

                            // Şarkı Sayısı Rozeti
                            Surface(
                                color = Color(0xFF131826),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CarBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Dahil: ", fontSize = 11.5.sp, color = CarTextMuted)
                                    Text(
                                        text = "$selectedSongsCount şarkı",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF3F4F6)
                                    )
                                }
                            }
                        }

                        // Hızlı Seçim Butonları
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = { selectedFolders = allDiscoveredPaths.toSet() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = "✓ Tümünü Seç",
                                    fontSize = 12.sp,
                                    color = CarCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            TextButton(
                                onClick = { selectedFolders = emptySet() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = "✕ Temizle",
                                    fontSize = 12.sp,
                                    color = CarTextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ana Liste: USB Sürücüler Üstte, Dahili Klasörler Altta
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // -------------------------------------------------------------
                    // BÖLÜM 1: USB SÜRÜCÜLERİ
                    // -------------------------------------------------------------
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(text = "🔌", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "USB VE HARİCİ SÜRÜCÜLER",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CarCyan,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    if (usbDrives.isEmpty()) {
                        item {
                            Surface(
                                color = CarSurfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CarBorder.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_usb),
                                        contentDescription = null,
                                        tint = CarTextMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Bağlı USB Sürücü Bulunamadı",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CarTextMuted
                                        )
                                        Text(
                                            text = "USB bellek taktığınızda burada otomatik olarak görünecektir.",
                                            fontSize = 11.5.sp,
                                            color = CarTextMuted.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(usbDrives, key = { it.path }) { drive ->
                            val isSelected = isFolderSelected(drive.path)
                            val songCount = remember(drive.path, allSongs) {
                                allSongs.count { normPath(it.path).startsWith(normPath(drive.path), ignoreCase = true) }
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { toggleFolder(drive.path) },
                                color = if (isSelected) Color(0xFF132233) else CarSurfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isSelected) CarCyan else CarBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // USB İkon Kutusu
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) CarCyan.copy(alpha = 0.2f) else Color(0xFF1C2230))
                                                .border(
                                                    1.dp,
                                                    if (isSelected) CarCyan.copy(alpha = 0.6f) else CarBorder,
                                                    RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_usb),
                                                contentDescription = "USB",
                                                tint = if (isSelected) CarCyan else CarTextMuted,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = drive.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) CarTextPrimary else CarTextMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        color = CarCyan.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "TARANIYOR",
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = CarCyan,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = drive.path,
                                                fontSize = 12.sp,
                                                color = CarTextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (songCount > 0) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "🎵 $songCount şarkı kayıtlı",
                                                    fontSize = 11.sp,
                                                    color = CarCyan.copy(alpha = 0.85f),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Büyük Araç Tipi Switch
                                    Switch(
                                        checked = isSelected,
                                        onCheckedChange = { toggleFolder(drive.path) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF0C0E12),
                                            checkedTrackColor = CarCyan,
                                            uncheckedThumbColor = CarTextMuted,
                                            uncheckedTrackColor = Color(0xFF232B3B)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // -------------------------------------------------------------
                    // BÖLÜM 2: DAHİLİ HAFIZA KLASÖRLERİ
                    // -------------------------------------------------------------
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(text = "📱", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DAHİLİ HAFIZA KLASÖRLERİ",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CarAmber,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    if (internalFolders.isEmpty()) {
                        item {
                            Text(
                                text = "Dahili hafızada müzik klasörü bulunamadı",
                                fontSize = 12.sp,
                                color = CarTextMuted,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        items(internalFolders, key = { it }) { folderPath ->
                            val folderFile = File(folderPath)
                            val folderName = folderFile.name.ifEmpty { folderPath }
                            val cleanPath = normPath(folderPath)
                            val isSelected = isFolderSelected(folderPath)

                            val isDownload = cleanPath.lowercase().contains("download")
                            val isMusic = cleanPath.lowercase().endsWith("/music")
                            val songCount = remember(folderPath, allSongs) {
                                allSongs.count { normPath(it.path).startsWith(cleanPath, ignoreCase = true) }
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { toggleFolder(folderPath) },
                                color = if (isSelected) CarSurfaceVariant else CarSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) CarCyan.copy(alpha = 0.6f) else CarBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_folder),
                                        contentDescription = null,
                                        tint = if (isSelected) CarCyan else CarTextMuted,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = folderName,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) CarTextPrimary else CarTextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (isMusic) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = CarCyan.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(5.dp)
                                                ) {
                                                    Text(
                                                        text = "Müzik",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CarCyan,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            } else if (isDownload) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = CarAmber.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(5.dp)
                                                ) {
                                                    Text(
                                                        text = "İndirilenler",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CarAmber,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = folderPath,
                                            fontSize = 11.5.sp,
                                            color = CarTextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (songCount > 0) {
                                            Text(
                                                text = "$songCount şarkı",
                                                fontSize = 11.sp,
                                                color = CarTextMuted.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { toggleFolder(folderPath) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = CarCyan,
                                            uncheckedColor = CarTextMuted,
                                            checkmarkColor = Color(0xFF0C0E12)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sıfırlama Butonu
                if (onResetAll != null) {
                    TextButton(
                        onClick = {
                            onResetAll()
                            onDismissRequest()
                        },
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                    ) {
                        Text(
                            text = "🔄 Filtreleri Sıfırla (Tüm Cihaz ve USB'yi Tara)",
                            color = CarCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Alt Eylem Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CarBorder)
                    ) {
                        Text(
                            text = "Vazgeç",
                            color = CarTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            onApplyFolders(selectedFolders)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(46.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CarCyan)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = null,
                            tint = Color(0xFF0C0E12),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Taramayı Uygula",
                            color = Color(0xFF0C0E12),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
