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
import com.example.otomuzik.theme.*
import java.io.File

/**
 * Kullanıcının kütüphaneye dahil edilecek müzik klasörlerini seçmesini sağlayan diyalog.
 * İstenmeyen dizinler (Download, snaptube, ses kayıtları vb.) kütüphaneden hariç tutulabilir.
 */
@Composable
fun FolderSelectionDialog(
    storageRoots: List<FolderItem>,
    includedFolders: Set<String>,
    onToggleFolder: (String, Boolean) -> Unit,
    onResetAll: (() -> Unit)? = null,
    onRescan: () -> Unit,
    onDismissRequest: () -> Unit
) {
    // Dahili hafızadaki yaygın müzik klasörlerini tara ve listele
    val discoveredFolders = remember(storageRoots) {
        val list = mutableListOf<String>()
        val internalStorage = Environment.getExternalStorageDirectory()
        if (internalStorage != null && internalStorage.exists()) {
            val commonNames = listOf("Music", "Download", "snaptube", "Podcasts", "Audiobooks", "Recordings", "Documents")
            for (name in commonNames) {
                val f = File(internalStorage, name)
                if (f.exists() && f.isDirectory) {
                    list.add(f.absolutePath)
                }
            }
            // Ayrıca internalStorage içindeki diğer müzik içeren alt klasörler
            internalStorage.listFiles()?.filter { it.isDirectory && !it.isHidden && !commonNames.contains(it.name) }?.forEach { f ->
                if (File(f, "").canRead()) {
                    list.add(f.absolutePath)
                }
            }
        }
        // Harici USB/SD depolama kökleri
        for (root in storageRoots) {
            if (!list.contains(root.path)) {
                list.add(root.path)
            }
        }
        // Daha önce kullanıcı tarafından seçilmiş olanlar
        for (f in includedFolders) {
            if (!list.contains(f)) {
                list.add(f)
            }
        }
        list.distinct()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 340.dp, max = 560.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = CarSurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, CarBorder),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Müzik Klasörlerini Yönet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kütüphaneye eklenecek klasörleri seçin",
                            fontSize = 12.sp,
                            color = CarTextMuted
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text(text = "✕", fontSize = 18.sp, color = CarTextMuted, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bilgilendirme Notu
                Surface(
                    color = CarSurfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💡", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "İşareti kaldırılan klasörler (örn. Download / snaptube) müzik listesinde görünmez.",
                            fontSize = 12.sp,
                            color = CarTextPrimary.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Klasör Listesi
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(discoveredFolders) { folderPath ->
                        val folderFile = File(folderPath)
                        val folderName = folderFile.name.ifEmpty { folderPath }
                        val isDownloadOrVoice = folderPath.lowercase().contains("download") ||
                                folderPath.lowercase().contains("snaptube") ||
                                folderPath.lowercase().contains("recording")

                        val isSelected = if (includedFolders.isEmpty()) {
                            !isDownloadOrVoice
                        } else {
                            includedFolders.any { folderPath.startsWith(it, ignoreCase = true) || it.startsWith(folderPath, ignoreCase = true) }
                        }

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
                                    onToggleFolder(folderPath, !isSelected)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (folderPath.contains("USB") || folderPath.startsWith("/storage/") && !folderPath.contains("emulated"))
                                        R.drawable.ic_usb
                                    else
                                        R.drawable.ic_folder
                                ),
                                contentDescription = null,
                                tint = if (isSelected) CarCyan else CarTextMuted,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = folderName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CarTextPrimary else CarTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isDownloadOrVoice) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = CarAmber.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "İndirilenler",
                                                fontSize = 10.sp,
                                                color = CarAmber,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = folderPath,
                                    fontSize = 11.sp,
                                    color = CarTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    onToggleFolder(folderPath, checked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CarCyan,
                                    uncheckedColor = CarTextMuted,
                                    checkmarkColor = Color(0xFF0C0E12)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (onResetAll != null && includedFolders.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            onResetAll()
                            onDismissRequest()
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "🔄 Filtreleri Sıfırla (Tüm Cihaz & USB'leri Tara)",
                            color = CarCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
                            text = "Kapat",
                            color = CarTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            onRescan()
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CarCyan)
                    ) {
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
