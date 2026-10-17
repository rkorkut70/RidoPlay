package com.example.otomuzik.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.otomuzik.model.Song
import com.example.otomuzik.theme.BaseCarBgDark
import com.example.otomuzik.theme.CarBorder
import com.example.otomuzik.theme.CarSurfaceDark
import com.example.otomuzik.theme.CarSurfaceVariant
import com.example.otomuzik.theme.CarTextMuted
import com.example.otomuzik.theme.CarTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs

enum class MatchConfidence(val label: String, val emoji: String) {
    EXACT("Kesin Kopya", "🔴"),
    HIGH("Yüksek İhtimal", "🟠"),
    MEDIUM("Muhtemel", "🟡"),
    LOW("Düşük İhtimal", "⚪")
}

data class DuplicateGroupItem(
    val id: String,
    val matchType: String,
    val matchDetail: String,
    val songs: List<Song>,
    val selectedSongPath: String,
    val confidence: MatchConfidence = MatchConfidence.MEDIUM
)

@Composable
fun DuplicateFinderDialog(
    allSongs: List<Song>,
    accentColor: Color,
    onDismissRequest: () -> Unit,
    onDeleteSong: (Song, (Boolean) -> Unit) -> Unit,
    onDeleteMultipleSongs: ((List<Song>, (Int) -> Unit) -> Unit)? = null,
    onRenameSong: (Song, String, (Boolean, Song?) -> Unit) -> Unit,
    onPreviewSong: (Song) -> Unit = {}
) {
    var isScanning by remember { mutableStateOf(true) }
    var scanStatus by remember { mutableStateOf("Kütüphane taranıyor...") }
    var duplicateGroups by remember { mutableStateOf<List<DuplicateGroupItem>>(emptyList()) }
    var songToDelete by remember { mutableStateOf<Pair<Song, String>?>(null) } // (Song, GroupId)
    var songToRename by remember { mutableStateOf<Pair<Song, String>?>(null) } // (Song, GroupId)
    var renameInput by remember { mutableStateOf("") }
    var showDeleteAllExactDialog by remember { mutableStateOf(false) }
    var isDeletingAllExact by remember { mutableStateOf(false) }
    var selectedConfidenceFilter by remember { mutableStateOf<MatchConfidence?>(null) }

    val exactCount = remember(duplicateGroups) { duplicateGroups.count { it.confidence == MatchConfidence.EXACT } }
    val highCount = remember(duplicateGroups) { duplicateGroups.count { it.confidence == MatchConfidence.HIGH } }
    val mediumCount = remember(duplicateGroups) { duplicateGroups.count { it.confidence == MatchConfidence.MEDIUM } }
    val lowCount = remember(duplicateGroups) { duplicateGroups.count { it.confidence == MatchConfidence.LOW } }

    val displayedGroups = remember(duplicateGroups, selectedConfidenceFilter) {
        if (selectedConfidenceFilter == null) duplicateGroups
        else duplicateGroups.filter { it.confidence == selectedConfidenceFilter }
    }

    // Kesin kopya (MD5 Hash) grupları ve silinecek fazlalık kopyalar
    val exactGroups = remember(duplicateGroups) {
        duplicateGroups.filter { it.confidence == MatchConfidence.EXACT }
    }
    val exactDuplicateSongs = remember(exactGroups) {
        exactGroups.flatMap { g ->
            val keepSong = g.songs.firstOrNull { it.path == g.selectedSongPath } ?: g.songs.first()
            g.songs.filter { it.path != keepSong.path }
        }
    }
    val wastedSpaceStr = remember(exactDuplicateSongs) {
        val totalBytes = exactDuplicateSongs.sumOf { song ->
            try {
                val f = File(song.path)
                if (f.exists()) f.length() else 0L
            } catch (_: Exception) { 0L }
        }
        formatBytes(totalBytes)
    }

    // Arka planda kademeli mükerrer şarkı tespiti
    LaunchedEffect(allSongs) {
        withContext(Dispatchers.Default) {
            val groups = findDuplicateSongGroups(allSongs) { status ->
                scanStatus = status
            }
            duplicateGroups = groups
            isScanning = false
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, CarBorder, RoundedCornerShape(20.dp)),
            color = BaseCarBgDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // ── ÜST BAŞLIK & KAPAT BUTONU ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f))
                                .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔍", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Aynı / Mükerrer Şarkıları Bul",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarTextPrimary
                            )
                            Text(
                                text = if (isScanning) scanStatus else "${duplicateGroups.size} mükerrer grup tespit edildi",
                                fontSize = 12.sp,
                                color = if (duplicateGroups.isNotEmpty()) accentColor else CarTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = onDismissRequest,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CarBorder)
                Spacer(modifier = Modifier.height(10.dp))

                // ── TÜM KESİN KOPYALARI SİL BANNER'I ──
                AnimatedVisibility(visible = !isScanning && exactDuplicateSongs.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .border(1.dp, Color(0xFFE53935).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE53935).copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🔴", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Tüm Kesin Kopyaları Temizle (${exactDuplicateSongs.size} dosya)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CarTextPrimary
                                    )
                                    Text(
                                        text = "Her gruptan 1 kopya korunur, fazlalıklar silinir (~$wastedSpaceStr)",
                                        fontSize = 11.sp,
                                        color = CarTextMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { showDeleteAllExactDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                enabled = !isDeletingAllExact
                            ) {
                                if (isDeletingAllExact) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Tümünü Sil", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ── İÇERİK ALANI ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when {
                        isScanning -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = accentColor)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = scanStatus,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarTextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Boyut, süre, isim ve dosya hash'i karşılaştırılıyor.",
                                    fontSize = 13.sp,
                                    color = CarTextMuted
                                )
                            }
                        }

                        duplicateGroups.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎉", fontSize = 32.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Mükerrer Şarkı Bulunamadı!",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarTextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Kütüphanenizde aynı dosya boyutuna veya ada sahip yinelenen şarkı bulunmuyor.",
                                    fontSize = 13.sp,
                                    color = CarTextMuted,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = onDismissRequest,
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    Text("Tamam", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        else -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // ── KATEGORİ FİLTRELEME ÇUBUĞU ──
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(bottom = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedConfidenceFilter == null,
                                        onClick = { selectedConfidenceFilter = null },
                                        label = { Text("Tümü (${duplicateGroups.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = accentColor.copy(alpha = 0.25f),
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFF1E2430),
                                            labelColor = Color(0xFFA5B0C2)
                                        )
                                    )
                                    if (exactCount > 0) {
                                        FilterChip(
                                            selected = selectedConfidenceFilter == MatchConfidence.EXACT,
                                            onClick = { selectedConfidenceFilter = if (selectedConfidenceFilter == MatchConfidence.EXACT) null else MatchConfidence.EXACT },
                                            label = { Text("🔴 Kesin Kopya ($exactCount)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.25f),
                                                selectedLabelColor = Color(0xFF81C784),
                                                containerColor = Color(0xFF1E2430),
                                                labelColor = Color(0xFFA5B0C2)
                                            )
                                        )
                                    }
                                    if (highCount > 0) {
                                        FilterChip(
                                            selected = selectedConfidenceFilter == MatchConfidence.HIGH,
                                            onClick = { selectedConfidenceFilter = if (selectedConfidenceFilter == MatchConfidence.HIGH) null else MatchConfidence.HIGH },
                                            label = { Text("🟠 Yüksek ($highCount)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.25f),
                                                selectedLabelColor = Color(0xFFFFB74D),
                                                containerColor = Color(0xFF1E2430),
                                                labelColor = Color(0xFFA5B0C2)
                                            )
                                        )
                                    }
                                    if (mediumCount > 0) {
                                        FilterChip(
                                            selected = selectedConfidenceFilter == MatchConfidence.MEDIUM,
                                            onClick = { selectedConfidenceFilter = if (selectedConfidenceFilter == MatchConfidence.MEDIUM) null else MatchConfidence.MEDIUM },
                                            label = { Text("🟡 Muhtemel ($mediumCount)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFFFC107).copy(alpha = 0.25f),
                                                selectedLabelColor = Color(0xFFFFD54F),
                                                containerColor = Color(0xFF1E2430),
                                                labelColor = Color(0xFFA5B0C2)
                                            )
                                        )
                                    }
                                    if (lowCount > 0) {
                                        FilterChip(
                                            selected = selectedConfidenceFilter == MatchConfidence.LOW,
                                            onClick = { selectedConfidenceFilter = if (selectedConfidenceFilter == MatchConfidence.LOW) null else MatchConfidence.LOW },
                                            label = { Text("⚪ Düşük İhtimal ($lowCount)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF78909C).copy(alpha = 0.35f),
                                                selectedLabelColor = Color(0xFFECEFF1),
                                                containerColor = Color(0xFF1E2430),
                                                labelColor = Color(0xFFA5B0C2)
                                            )
                                        )
                                    }
                                }

                                if (displayedGroups.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Bu kategoride mükerrer grup bulunamadı.",
                                            fontSize = 13.sp,
                                            color = CarTextMuted
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        itemsIndexed(displayedGroups, key = { _, item -> item.id }) { index, group ->
                                            DuplicateGroupCard(
                                                groupIndex = index + 1,
                                                totalGroups = displayedGroups.size,
                                                group = group,
                                                accentColor = accentColor,
                                                onSelectSong = { songPath ->
                                                    duplicateGroups = duplicateGroups.map {
                                                        if (it.id == group.id) it.copy(selectedSongPath = songPath) else it
                                                    }
                                                },
                                                onDeleteSelected = {
                                                    val selected = group.songs.firstOrNull { it.path == group.selectedSongPath }
                                                        ?: group.songs.firstOrNull()
                                                    if (selected != null) {
                                                        songToDelete = Pair(selected, group.id)
                                                    }
                                                },
                                                onRenameSelected = {
                                                    val selected = group.songs.firstOrNull { it.path == group.selectedSongPath }
                                                        ?: group.songs.firstOrNull()
                                                    if (selected != null) {
                                                        renameInput = try {
                                                            File(selected.path).nameWithoutExtension
                                                        } catch (e: Exception) {
                                                            selected.title
                                                        }
                                                        songToRename = Pair(selected, group.id)
                                                    }
                                                },
                                                onKeepBoth = {
                                                    // İkisini de tut: Bu grubu mükerrer listesinden kaldır
                                                    duplicateGroups = duplicateGroups.filter { it.id != group.id }
                                                },
                                                onPreview = onPreviewSong
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── 1. SİLME ONAY DİYALOĞU ──
    songToDelete?.let { (song, groupId) ->
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            icon = { Text("🗑️", fontSize = 28.sp) },
            title = { Text("Seçileni Sil", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Bu şarkı dosyası cihazınızdan kalıcı olarak silinecektir:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🎵 ${song.title}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "📁 ${song.path}",
                        fontSize = 11.sp,
                        color = CarTextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bu işlemi onaylıyor musunuz?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentGroupId = groupId
                        val currentSong = song
                        songToDelete = null
                        onDeleteSong(currentSong) { success ->
                            if (success) {
                                // Grubu güncelle: silinen şarkıyı çıkar, grupta <= 1 kaldıysa grubu da kaldır
                                duplicateGroups = duplicateGroups.mapNotNull { g ->
                                    if (g.id == currentGroupId) {
                                        val remaining = g.songs.filter { it.path != currentSong.path }
                                        if (remaining.size > 1) {
                                            g.copy(
                                                songs = remaining,
                                                selectedSongPath = remaining.first().path
                                            )
                                        } else null
                                    } else g
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Evet, Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { songToDelete = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // ── 2. YENİDEN ADLANDIRMA DİYALOĞU ──
    songToRename?.let { (song, groupId) ->
        AlertDialog(
            onDismissRequest = { songToRename = null },
            icon = { Text("✏️", fontSize = 28.sp) },
            title = { Text("Seçilenin Adını Değiştir", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Dosya için yeni bir ad girin (Uzantı otomatik korunacaktır):")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("Yeni Şarkı / Dosya Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mevcut: ${File(song.path).name}",
                        fontSize = 11.sp,
                        color = CarTextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = renameInput.trim()
                        val currentSong = song
                        val currentGroupId = groupId
                        songToRename = null
                        if (newName.isNotBlank()) {
                            onRenameSong(currentSong, newName) { success, updatedSong ->
                                if (success && updatedSong != null) {
                                    // Gruptaki şarkıyı güncelle
                                    duplicateGroups = duplicateGroups.map { g ->
                                        if (g.id == currentGroupId) {
                                            val updatedList = g.songs.map { if (it.path == currentSong.path) updatedSong else it }
                                            g.copy(
                                                songs = updatedList,
                                                selectedSongPath = updatedSong.path
                                            )
                                        } else g
                                    }
                                }
                            }
                        }
                    },
                    enabled = renameInput.isNotBlank()
                ) {
                    Text("Kaydet", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { songToRename = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // ── 3. TÜM KESİN KOPYALARI SİLME ONAY DİYALOĞU ──
    if (showDeleteAllExactDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAllExact) showDeleteAllExactDialog = false },
            icon = { Text("⚠️", fontSize = 28.sp) },
            title = { Text("Tüm Kesin Kopyaları Sil?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("MD5 Hash doğrulamasıyla birebir aynı olduğu kesinleşen:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• ${exactGroups.size} farklı şarkı grubu\n• Toplam ${exactDuplicateSongs.size} adet gereksiz kopya dosya\n• Kurtarılacak alan: ~$wastedSpaceStr",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Her şarkının 1 kopyası kütüphanenizde korunacak, diğer fazlalık kopyalar cihazınızdan kalıcı olarak silinecektir.",
                        fontSize = 12.sp,
                        color = CarTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bu işlemi onaylıyor musunuz?", fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllExactDialog = false
                        isDeletingAllExact = true
                        val toDelete = exactDuplicateSongs
                        val toDeletePaths = toDelete.map { it.path }.toSet()

                        val onDeletionFinished: (Int) -> Unit = { count ->
                            isDeletingAllExact = false
                            if (count > 0) {
                                // EXACT gruplarındaki kopyalar silindiği için artık mükerrer değiller
                                duplicateGroups = duplicateGroups.mapNotNull { g ->
                                    if (g.confidence == MatchConfidence.EXACT) {
                                        null
                                    } else {
                                        val remaining = g.songs.filter { it.path !in toDeletePaths }
                                        if (remaining.size > 1) {
                                            g.copy(
                                                songs = remaining,
                                                selectedSongPath = remaining.first().path
                                            )
                                        } else null
                                    }
                                }
                            }
                        }

                        if (onDeleteMultipleSongs != null) {
                            onDeleteMultipleSongs(toDelete, onDeletionFinished)
                        } else {
                            var deletedCount = 0
                            var processedCount = 0
                            for (s in toDelete) {
                                onDeleteSong(s) { success ->
                                    processedCount++
                                    if (success) deletedCount++
                                    if (processedCount == toDelete.size) {
                                        onDeletionFinished(deletedCount)
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Evet, Hepsini Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteAllExactDialog = false },
                    enabled = !isDeletingAllExact
                ) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
private fun DuplicateGroupCard(
    groupIndex: Int,
    totalGroups: Int,
    group: DuplicateGroupItem,
    accentColor: Color,
    onSelectSong: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onRenameSelected: () -> Unit,
    onKeepBoth: () -> Unit,
    onPreview: (Song) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, CarBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CarSurfaceDark)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Grup Başlığı ve Eşleşme Sebebi
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Grup $groupIndex / $totalGroups",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = group.matchType,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CarTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                // Güvenilirlik badge'i
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (group.confidence) {
                                MatchConfidence.EXACT -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                MatchConfidence.HIGH -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                MatchConfidence.MEDIUM -> Color(0xFFFFC107).copy(alpha = 0.2f)
                                MatchConfidence.LOW -> Color(0xFF78909C).copy(alpha = 0.25f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${group.confidence.emoji} ${group.confidence.label}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (group.confidence) {
                            MatchConfidence.EXACT -> Color(0xFF4CAF50)
                            MatchConfidence.HIGH -> Color(0xFFFF9800)
                            MatchConfidence.MEDIUM -> Color(0xFFFFC107)
                            MatchConfidence.LOW -> Color(0xFFB0BEC5)
                        }
                    )
                }
            }

            if (group.confidence == MatchConfidence.LOW) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF263238).copy(alpha = 0.5f))
                        .border(1.dp, Color(0xFF546E7A).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ℹ️", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Farklı sanatçı veya dosya boyutu. Birebir uyuşma olmadığından aynı şarkı olma olasılığı düşüktür.",
                        fontSize = 10.5.sp,
                        color = Color(0xFFCFD8DC),
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gruptaki Şarkılar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                group.songs.forEach { song ->
                    val isSelected = song.path == group.selectedSongPath
                    val fileSizeStr = remember(song.path) {
                        try {
                            val f = File(song.path)
                            if (f.exists()) formatBytes(f.length()) else song.fileSizeFormatted
                        } catch (e: Exception) {
                            song.fileSizeFormatted
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.12f) else CarSurfaceVariant.copy(alpha = 0.5f))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) accentColor else CarBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectSong(song.path) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectSong(song.path) },
                            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) accentColor else CarTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${song.artist} • ${song.durationFormatted} • $fileSizeStr",
                                fontSize = 11.sp,
                                color = CarTextMuted,
                                maxLines = 1
                            )
                            Text(
                                text = "📁 ${File(song.path).parent ?: song.folderPath}",
                                fontSize = 10.sp,
                                color = CarTextMuted.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { onPreview(song) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_play),
                                contentDescription = "Dinle",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── ÜÇ SEÇENEK BUTONLARI (Kullanıcı İsteği) ──
            // 1. Seçileni Sil, 2. Seçilenin Adını Değiştir, 3. İkisini de Tut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. SEÇİLENİ SİL
                Button(
                    onClick = onDeleteSelected,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text("🗑️ Seçileni Sil", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // 2. SEÇİLENİN ADINI DEĞİŞTİR
                Button(
                    onClick = onRenameSelected,
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text("✏️ Adını Değiştir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // 3. İKİSİNİ DE TUT
                OutlinedButton(
                    onClick = onKeepBoth,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text("🤝 İkisini Tut", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * Kademeli mükerrer şarkı tespit sistemi:
 *   Adım 1: Boyut + Süre (±1sn) → ön eleme
 *   Adım 2: Kısmi hash (ilk 64KB) → hızlı doğrulama
 *   Adım 3: Tam MD5 hash → kesin sonuç
 *   + Aynı isim eşleştirmesi (ek kontrol)
 */
private fun findDuplicateSongGroups(
    songs: List<Song>,
    onProgress: (String) -> Unit
): List<DuplicateGroupItem> {
    val n = songs.size
    if (n < 2) return emptyList()

    onProgress("Adım 1/3 — Dosya boyutları ve süreler karşılaştırılıyor...")

    // ── Her şarkının dosya boyutunu al ──
    val fileSizes = LongArray(n) { idx ->
        try {
            val f = File(songs[idx].path)
            if (f.exists()) f.length() else -1L
        } catch (_: Exception) { -1L }
    }

    fun clean(str: String): String {
        return str.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("""[^\p{L}\p{Nd}]+"""), "")
    }

    fun cleanArtist(str: String): String {
        val s = str.trim().lowercase(Locale.ROOT)
        if (s.contains("bilinmeyen") || s.contains("unknown") || s == "<unknown>" || s.isBlank()) {
            return ""
        }
        return s.replace(Regex("""[^\p{L}\p{Nd}]+"""), "")
    }

    val titles = Array(n) { idx -> clean(songs[idx].title) }
    val artists = Array(n) { idx -> cleanArtist(songs[idx].artist) }
    val fileNames = Array(n) { idx ->
        try { clean(File(songs[idx].path).nameWithoutExtension) }
        catch (_: Exception) { "" }
    }

    // ══════════════════════════════════════════════
    // ADIM 1: Boyut + Süre ile aday gruplar oluştur
    // ══════════════════════════════════════════════
    // Anahtar: "boyut|süre_saniye" → aynı boyut VE benzer süre (±1sn)
    data class SizeDurationKey(val size: Long, val durationSec: Long)

    val sizeDurMap = mutableMapOf<SizeDurationKey, MutableList<Int>>()
    for (i in 0 until n) {
        val sz = fileSizes[i]
        if (sz > 100_000L) { // 100 KB altı hariç
            val durSec = songs[i].durationMs / 1000
            val key = SizeDurationKey(sz, durSec)
            sizeDurMap.getOrPut(key) { mutableListOf() }.add(i)
            // ±1 saniye tolerans: komşu sürelere de bak
            val keyMinus = SizeDurationKey(sz, durSec - 1)
            val keyPlus = SizeDurationKey(sz, durSec + 1)
            sizeDurMap[keyMinus]?.let { existing ->
                // Bu listeye ekleme, sadece merge için sonra bakacağız
            }
            sizeDurMap[keyPlus]?.let { existing -> }
        }
    }

    // Boyut+süre eşleşen adaylar
    val sizeDurCandidates = mutableListOf<List<Int>>()
    for ((_, indices) in sizeDurMap) {
        if (indices.size > 1) {
            // ±1sn toleransla tekrar filtrele
            val grouped = mutableListOf<MutableList<Int>>()
            for (idx in indices) {
                val durMs = songs[idx].durationMs
                var found = false
                for (g in grouped) {
                    if (abs(songs[g[0]].durationMs - durMs) <= 1500L) { // 1.5sn tolerans
                        g.add(idx)
                        found = true
                        break
                    }
                }
                if (!found) grouped.add(mutableListOf(idx))
            }
            for (g in grouped) {
                if (g.size > 1) sizeDurCandidates.add(g)
            }
        }
    }

    // ══════════════════════════════════════════════
    // ADIM 2: Kısmi hash (ilk 64KB) ile doğrulama
    // ══════════════════════════════════════════════
    onProgress("Adım 2/3 — Kısmi dosya hash'leri hesaplanıyor...")

    val partialHashCache = mutableMapOf<Int, String>()

    fun computePartialHash(songIndex: Int): String {
        partialHashCache[songIndex]?.let { return it }
        val hash = try {
            val file = File(songs[songIndex].path)
            if (!file.exists()) ""
            else {
                val md = MessageDigest.getInstance("MD5")
                FileInputStream(file).use { fis ->
                    val buf = ByteArray(65_536) // 64 KB
                    val read = fis.read(buf)
                    if (read > 0) md.update(buf, 0, read)
                }
                md.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (_: Exception) { "" }
        partialHashCache[songIndex] = hash
        return hash
    }

    // Kısmi hash ile doğrulanmış adaylar
    data class VerifiedGroup(
        val indices: List<Int>,
        val hashMatch: Boolean // kısmi hash eşleşti mi
    )

    val partialVerified = mutableListOf<VerifiedGroup>()
    for (candidates in sizeDurCandidates) {
        // Kısmi hash'e göre alt grupla
        val hashGroups = mutableMapOf<String, MutableList<Int>>()
        for (idx in candidates) {
            val ph = computePartialHash(idx)
            if (ph.isNotEmpty()) {
                hashGroups.getOrPut(ph) { mutableListOf() }.add(idx)
            }
        }
        for ((_, hIndices) in hashGroups) {
            if (hIndices.size > 1) {
                partialVerified.add(VerifiedGroup(hIndices, hashMatch = true))
            }
        }
    }

    // ══════════════════════════════════════════════
    // ADIM 3: Tam MD5 hash ile kesin doğrulama
    // ══════════════════════════════════════════════
    onProgress("Adım 3/3 — Tam dosya hash'leri doğrulanıyor...")

    val fullHashCache = mutableMapOf<Int, String>()

    fun computeFullHash(songIndex: Int): String {
        fullHashCache[songIndex]?.let { return it }
        val hash = try {
            val file = File(songs[songIndex].path)
            if (!file.exists()) ""
            else {
                val md = MessageDigest.getInstance("MD5")
                FileInputStream(file).use { fis ->
                    val buf = ByteArray(8192)
                    var read: Int
                    while (fis.read(buf).also { read = it } != -1) {
                        md.update(buf, 0, read)
                    }
                }
                md.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (_: Exception) { "" }
        fullHashCache[songIndex] = hash
        return hash
    }

    // ── Sonuçları topla ──
    val result = mutableListOf<DuplicateGroupItem>()
    val usedIndices = mutableSetOf<Int>() // Aynı şarkıyı birden fazla gruba koymamak için
    var groupCounter = 0

    // 3A) Tam hash eşleşenler → EXACT
    for (vg in partialVerified) {
        val fullHashGroups = mutableMapOf<String, MutableList<Int>>()
        for (idx in vg.indices) {
            val fh = computeFullHash(idx)
            if (fh.isNotEmpty()) {
                fullHashGroups.getOrPut(fh) { mutableListOf() }.add(idx)
            }
        }
        for ((_, fIndices) in fullHashGroups) {
            if (fIndices.size > 1) {
                val groupSongs = fIndices.map { songs[it] }
                groupCounter++
                result.add(
                    DuplicateGroupItem(
                        id = "dup_exact_$groupCounter",
                        matchType = "🔐 Birebir Aynı Dosya (MD5 Hash)",
                        matchDetail = formatBytes(fileSizes[fIndices[0]]),
                        songs = groupSongs,
                        selectedSongPath = groupSongs.first().path,
                        confidence = MatchConfidence.EXACT
                    )
                )
                usedIndices.addAll(fIndices)
            }
        }
    }

    // 3B) Boyut+süre eşleşip kısmi hash farklı olanlar
    for (candidates in sizeDurCandidates) {
        val unusedInGroup = candidates.filter { it !in usedIndices }
        if (unusedInGroup.size > 1) {
            val firstIdx = unusedInGroup[0]
            val sameNameToo = unusedInGroup.all {
                (titles[it].isNotEmpty() && titles[it] == titles[firstIdx]) ||
                (fileNames[it].isNotEmpty() && fileNames[it] == fileNames[firstIdx])
            }
            val sameArtistToo = unusedInGroup.all {
                artists[it].isNotEmpty() && artists[it] == artists[firstIdx]
            }
            val hasDifferentArtist = unusedInGroup.any {
                artists[it].isNotEmpty() && artists[firstIdx].isNotEmpty() && artists[it] != artists[firstIdx]
            }
            val hasDifferentTitle = unusedInGroup.any {
                titles[it].isNotEmpty() && titles[firstIdx].isNotEmpty() && titles[it] != titles[firstIdx] &&
                fileNames[it].isNotEmpty() && fileNames[firstIdx].isNotEmpty() && fileNames[it] != fileNames[firstIdx]
            }

            val groupSongs = unusedInGroup.map { songs[it] }
            groupCounter++

            val conf = when {
                sameNameToo && sameArtistToo -> MatchConfidence.HIGH
                sameNameToo || sameArtistToo -> MatchConfidence.HIGH
                hasDifferentArtist && hasDifferentTitle -> MatchConfidence.LOW
                else -> MatchConfidence.MEDIUM
            }

            val typeStr = when (conf) {
                MatchConfidence.HIGH -> if (sameNameToo) "⚖️🏷️ Aynı Boyut, Süre ve İsim" else "⚖️🎤 Aynı Boyut, Süre ve Sanatçı"
                MatchConfidence.LOW -> "⚪ Aynı Boyut/Süre Fakat Farklı Sanatçı & İsim"
                else -> "⚖️ Aynı Boyut ve Süre (${formatBytes(fileSizes[firstIdx])})"
            }

            result.add(
                DuplicateGroupItem(
                    id = "dup_high_$groupCounter",
                    matchType = typeStr,
                    matchDetail = if (conf == MatchConfidence.LOW) "Aynı şarkı olma olasılığı düşüktür" else formatBytes(fileSizes[firstIdx]),
                    songs = groupSongs,
                    selectedSongPath = groupSongs.first().path,
                    confidence = conf
                )
            )
            usedIndices.addAll(unusedInGroup)
        }
    }

    // 3C) İsim eşleştirmesi (boyut/süre farklı olabilir)
    onProgress("İsim ve sanatçı eşleştirmesi yapılıyor...")
    val nameMap = mutableMapOf<String, MutableList<Int>>()
    for (i in 0 until n) {
        if (i in usedIndices) continue
        val t = titles[i]
        val fn = fileNames[i]
        val key = when {
            fn.length >= 3 && fn != "track" && fn != "audio" && fn != "music" -> fn
            t.length >= 3 && t != "bilinmeyensarki" && t != "track" -> t
            else -> ""
        }
        if (key.isNotEmpty()) {
            nameMap.getOrPut(key) { mutableListOf() }.add(i)
        }
    }
    for ((_, indices) in nameMap) {
        if (indices.size > 1) {
            val firstIdx = indices[0]
            val hasDifferentArtist = indices.any {
                artists[it].isNotEmpty() && artists[firstIdx].isNotEmpty() && artists[it] != artists[firstIdx]
            }
            val hasDifferentSize = indices.any {
                abs(fileSizes[it] - fileSizes[firstIdx]) > 100_000L
            }
            val sameArtist = indices.all {
                artists[it].isNotEmpty() && artists[it] == artists[firstIdx]
            }
            val similarDuration = indices.all {
                abs(songs[it].durationMs - songs[firstIdx].durationMs) <= 4000L
            }

            val groupSongs = indices.map { songs[it] }
            groupCounter++

            // Kullanıcı kuralı: Şarkı veya sanatçı isimleri farklı ise VE dosya boyutları farklı ise birebir uyuşma yoksa ayrı kategoride (LOW) göster
            val conf = when {
                sameArtist && similarDuration -> MatchConfidence.HIGH
                sameArtist -> MatchConfidence.MEDIUM
                similarDuration && !hasDifferentArtist && !hasDifferentSize -> MatchConfidence.MEDIUM
                hasDifferentArtist && hasDifferentSize -> MatchConfidence.LOW
                hasDifferentArtist -> MatchConfidence.LOW
                hasDifferentSize && !similarDuration -> MatchConfidence.LOW
                else -> MatchConfidence.MEDIUM
            }

            val typeStr = when (conf) {
                MatchConfidence.HIGH -> "🏷️🎤 Aynı Şarkı ve Sanatçı"
                MatchConfidence.MEDIUM -> "🏷️ Aynı Şarkı Adı (${groupSongs.first().title})"
                MatchConfidence.LOW -> if (hasDifferentArtist) "⚪ Farklı Sanatçı & Boyut (${groupSongs.first().title})" else "⚪ Farklı Boyut & Süre (${groupSongs.first().title})"
                else -> "🏷️ Aynı Şarkı / Dosya Adı"
            }

            result.add(
                DuplicateGroupItem(
                    id = "dup_name_$groupCounter",
                    matchType = typeStr,
                    matchDetail = if (conf == MatchConfidence.LOW) "Aynı şarkı olma olasılığı düşüktür" else groupSongs.first().title,
                    songs = groupSongs,
                    selectedSongPath = groupSongs.first().path,
                    confidence = conf
                )
            )
        }
    }

    // Güvenilirlik sırasına göre sırala: EXACT → HIGH → MEDIUM → LOW
    return result.sortedBy { it.confidence.ordinal }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.2f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}
