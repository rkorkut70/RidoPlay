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
 * Kullanıcının kütüphaneye dahil edilecek müzik klasörlerini seçmesini sağlayan diyalog.
 * Telefon (dikey/dar ekran) ve Tablet/Araç (yatay/geniş ekran) için optimize edilmiş 3 katmanlı hiyerarşi sunar.
 * 
 * - Klasör Adı: Üst satırda tam genişlikte net ve kalın görünür.
 * - Klasör Yolu: 2 satıra kadar sarılarak uzun dizin isimlerinin ve alt klasörlerin okunabilmesini sağlar.
 * - Rozetler: Ayrı bir satırda yatay düzenlenir, `softWrap = false` ile asla dikey sütun şeklinde ezilmez.
 * - Yerel Seçim Durumu: Kutucuk tıklamalarında arka plan taraması tetiklenmez; yalnızca "Taramayı Uygula"ya basıldığında kaydedilir.
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

    // Dahili hafıza, USB bellekler ve mevcut şarkıların bulunduğu klasörleri keşfet
    val discoveredFolders = remember(storageRoots, includedFolders, allSongs) {
        val list = linkedSetOf<String>()
        val commonNames = listOf("Music", "Download", "snaptube", "Podcasts", "Audiobooks", "Recordings", "Documents")

        if (internalStorage != null && internalStorage.exists()) {
            // Bilinen yaygın medya klasörleri
            for (name in commonNames) {
                val f = File(internalStorage, name)
                if (f.exists() && f.isDirectory) {
                    list.add(f.absolutePath)
                }
            }
            // Dahili hafızadaki diğer okunabilir kullanıcı klasörleri (Android ve gizli klasörler hariç)
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

        // Halihazırda taranmış şarkıların bulunduğu ana klasörler
        for (song in allSongs) {
            val parent = File(song.path).parent
            if (!parent.isNullOrBlank()) {
                val cleanParent = normPath(parent)
                // Dahili hafıza kök dizininin kendisi değilse ekle
                if (!cleanParent.equals(internalStoragePath, ignoreCase = true)) {
                    list.add(parent)
                }
            }
        }

        // Harici USB/SD depolama kökleri
        for (root in storageRoots) {
            val cleanRoot = normPath(root.path)
            // Dahili hafıza kökü (/storage/emulated/0) yerine alt klasörlerini listelediğimiz için kökü ekleme
            if (!cleanRoot.equals(internalStoragePath, ignoreCase = true)) {
                list.add(root.path)
            }
        }

        // Daha önceden kaydedilmiş özel klasörler varsa listeye dahil et
        for (f in includedFolders) {
            if (f.isNotBlank()) {
                list.add(f)
            }
        }

        // Düzenleme: "Music" ve "Download" en üstte, kalanlar alfabetik
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

    // Yerel seçim durumu: Kullanıcı seçim yaparken sadece bu state güncellenir.
    var selectedFolders by remember(includedFolders, discoveredFolders) {
        val initial = mutableSetOf<String>()
        if (includedFolders.isNotEmpty()) {
            for (f in discoveredFolders) {
                val cleanF = normPath(f)
                if (includedFolders.any { normPath(it).equals(cleanF, ignoreCase = true) }) {
                    initial.add(f)
                }
            }
        } else {
            // Varsayılan durum: Şarkısı olan veya güvenilir müzik klasörleri seçili gelsin
            for (f in discoveredFolders) {
                val cleanF = normPath(f)
                val lower = cleanF.lowercase()
                val hasSongs = allSongs.any { normPath(it.path).startsWith(cleanF, ignoreCase = true) }
                val isExcludedByDefault = (lower.contains("snaptube") || lower.contains("recording")) && !hasSongs
                if (!isExcludedByDefault) {
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

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 560.dp)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(20.dp),
            color = CarSurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, CarBorder),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Başlık & Kapat Butonu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Müzik Klasörlerini Yönet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kütüphaneye dahil edilecek klasörleri seçin",
                            fontSize = 11.5.sp,
                            color = CarTextMuted
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Text(text = "✕", fontSize = 17.sp, color = CarTextMuted, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bilgilendirme ve Hızlı Eylem Çubuğu
                Surface(
                    color = CarSurfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💡", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "İşareti kaldırılan klasörler listeden çıkarılır. Değişiklikler 'Taramayı Uygula'ya basınca geçerli olur.",
                                fontSize = 11.sp,
                                color = CarTextPrimary.copy(alpha = 0.85f),
                                lineHeight = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Seçili Sayısı ve Hızlı Seçim Butonları
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = CarCyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, CarCyan.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Seçili: ${selectedFolders.size} / ${discoveredFolders.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarCyan,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = { selectedFolders = discoveredFolders.toSet() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(text = "Tümünü Seç", fontSize = 11.sp, color = CarCyan, fontWeight = FontWeight.SemiBold)
                                }

                                TextButton(
                                    onClick = { selectedFolders = emptySet() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(text = "Temizle", fontSize = 11.sp, color = CarTextMuted, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Klasör Listesi
                if (discoveredFolders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Taranabilir klasör bulunamadı",
                            fontSize = 13.sp,
                            color = CarTextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(discoveredFolders, key = { it }) { folderPath ->
                            val folderFile = File(folderPath)
                            val folderName = folderFile.name.ifEmpty { folderPath }
                            val cleanPath = normPath(folderPath)
                            val isSelected = isFolderSelected(folderPath)

                            val isDownload = cleanPath.lowercase().contains("download")
                            val isSnaptube = cleanPath.lowercase().contains("snaptube")
                            val isRecording = cleanPath.lowercase().contains("recording")
                            val isUsb = cleanPath.contains("USB", ignoreCase = true) ||
                                    (cleanPath.startsWith("/storage/") && !cleanPath.contains("emulated")) ||
                                    cleanPath.startsWith("/mnt/media_rw/")

                            val songCount = remember(folderPath, allSongs) {
                                allSongs.count { normPath(it.path).startsWith(cleanPath, ignoreCase = true) }
                            }

                            val hasBadges = isUsb || isDownload || isSnaptube || isRecording || songCount > 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) CarSurfaceVariant else CarSurfaceVariant.copy(alpha = 0.4f))
                                    .border(
                                        1.dp,
                                        if (isSelected) CarCyan.copy(alpha = 0.6f) else CarBorder,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        toggleFolder(folderPath)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (isUsb) R.drawable.ic_usb else R.drawable.ic_folder
                                    ),
                                    contentDescription = null,
                                    tint = if (isSelected) CarCyan else CarTextMuted,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Orta İçerik: 3 Katmanlı Hiyerarşi (Ad -> Uzun Dizin Yolu -> Rozetler)
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // 1. Klasör Başlığı (Tüm satır genişliğini kullanabilir)
                                    Text(
                                        text = folderName,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CarTextPrimary else CarTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // 2. Klasör Yolu (Uzun dizinler için 2 satıra izin verilir, telefonda alt klasörler net okunur)
                                    Text(
                                        text = folderPath,
                                        fontSize = 11.sp,
                                        color = CarTextMuted.copy(alpha = 0.85f),
                                        maxLines = 2,
                                        lineHeight = 14.sp,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // 3. Rozetler Satırı (Ayrı satırda düzenlenir, asla dikey ezilmez)
                                    if (hasBadges) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (songCount > 0) {
                                                Surface(
                                                    color = if (isSelected) CarCyan.copy(alpha = 0.15f) else CarSurfaceDark.copy(alpha = 0.6f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        0.8.dp,
                                                        if (isSelected) CarCyan.copy(alpha = 0.4f) else CarBorder
                                                    )
                                                ) {
                                                    Text(
                                                        text = "🎵 $songCount şarkı",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isSelected) CarCyan else CarTextMuted,
                                                        softWrap = false,
                                                        maxLines = 1,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            if (isUsb) {
                                                Surface(
                                                    color = CarCyan.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "USB Bellek",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CarCyan,
                                                        softWrap = false,
                                                        maxLines = 1,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            if (isDownload) {
                                                Surface(
                                                    color = CarAmber.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "İndirilenler",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = CarAmber,
                                                        softWrap = false,
                                                        maxLines = 1,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else if (isSnaptube) {
                                                Surface(
                                                    color = CarAmber.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "SnapTube",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = CarAmber,
                                                        softWrap = false,
                                                        maxLines = 1,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else if (isRecording) {
                                                Surface(
                                                    color = CarAmber.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Ses Kayıtları",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = CarAmber,
                                                        softWrap = false,
                                                        maxLines = 1,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
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

                Spacer(modifier = Modifier.height(10.dp))

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
                            text = "🔄 Filtreleri Sıfırla (Tüm Depolamayı Tara)",
                            color = CarCyan,
                            fontSize = 11.5.sp,
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
                            .height(44.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CarBorder)
                    ) {
                        Text(
                            text = "Kapat",
                            color = CarTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            onApplyFolders(selectedFolders)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CarCyan)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = null,
                            tint = Color(0xFF0C0E12),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Taramayı Uygula",
                            color = Color(0xFF0C0E12),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
