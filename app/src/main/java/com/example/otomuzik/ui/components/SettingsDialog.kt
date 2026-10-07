package com.example.otomuzik.ui.components
import com.example.otomuzik.theme.*

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.otomuzik.ui.main.MainScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit,
    viewModel: MainScreenViewModel
) {
    val context = LocalContext.current
    
    // States from viewModel
    val autoPlayOnBluetooth by viewModel.autoPlayOnBluetooth.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val crossfadeDuration by viewModel.crossfadeDuration.collectAsStateWithLifecycle()
    val autoPlayOnStart by viewModel.autoPlayOnStart.collectAsStateWithLifecycle()
    val gaplessPlayback by viewModel.gaplessPlayback.collectAsStateWithLifecycle()
    val defaultStartTab by viewModel.defaultStartTab.collectAsStateWithLifecycle()
    val showSmartPlaylists by viewModel.showSmartPlaylists.collectAsStateWithLifecycle()
    
    val showBlurredBackground by viewModel.showBlurredBackground.collectAsStateWithLifecycle()
    val showWaveform by viewModel.showWaveform.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val isalbumartCropped by viewModel.isalbumartCropped.collectAsStateWithLifecycle()
    val stopOnTaskRemoved by viewModel.stopOnTaskRemoved.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val themePalette by viewModel.themePalette.collectAsStateWithLifecycle()
    val autoOpenQueueOnStart by viewModel.autoOpenQueueOnStart.collectAsStateWithLifecycle()
    val lyricsFontSize by viewModel.lyricsFontSize.collectAsStateWithLifecycle()
    val autoSaveId3Lyrics by viewModel.autoSaveId3Lyrics.collectAsStateWithLifecycle()
    val libraryStats by viewModel.libraryStats.collectAsStateWithLifecycle()
    
    val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
    } else {
        null
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 360.dp, max = 560.dp)
                .fillMaxWidth(0.90f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ayarlar",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FilledTonalButton(
                            onClick = onDismissRequest,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Kapat")
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    // Otomobil Modu
                    SettingsSection(title = "Otomobil Modu") {
                        SettingSwitchRow(
                            title = "Ekranı Uyanık Tut",
                            description = "Uygulama açıkken ekranın kapanmasını engeller.",
                            checked = keepScreenOn,
                            onCheckedChange = { viewModel.setKeepScreenOn(it) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingSwitchRow(
                            title = "Bluetooth ile Otomatik Çal",
                            description = "Araç kitine veya kulaklığa bağlanıldığında müziği otomatik başlatır.",
                            checked = autoPlayOnBluetooth,
                            onCheckedChange = { viewModel.setAutoPlayOnBluetooth(it) }
                        )
                    }

                    // Görünüm ve Tema
                    SettingsSection(title = "Görünüm & Tema") {
                        SettingactionRow(
                            title = "Tema Modu",
                            description = "Uygulamanın karanlık veya aydınlık mod tercihi."
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TabButton("Oto", "AUTO", themeMode) { viewModel.setThemeMode(it) }
                                TabButton("Açık", "LIGHT", themeMode) { viewModel.setThemeMode(it) }
                                TabButton("Koyu", "DARK", themeMode) { viewModel.setThemeMode(it) }
                            }
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingactionRow(
                            title = "Araç İçi Renk Uyumu",
                            description = "Araç konsoluna uygun vurgu rengini seçin."
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TabButton("Neon", "NEON_CYAN", themePalette) { viewModel.setThemePalette(it) }
                                    TabButton("Mocha", "GREY_BROWN", themePalette) { viewModel.setThemePalette(it) }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TabButton("Kırmızı", "SPORT_RED", themePalette) { viewModel.setThemePalette(it) }
                                    TabButton("Mavi", "DEEP_BLUE", themePalette) { viewModel.setThemePalette(it) }
                                }
                            }
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingSwitchRow(
                            title = "Bulanık Arka Plan Efekti",
                            description = "Oynatıcı ekranında albüm kapağını bulanıklaştırarak şık bir arka plan yapar.",
                            checked = showBlurredBackground,
                            onCheckedChange = { viewModel.setShowBlurredBackground(it) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingSwitchRow(
                            title = "Müzik Dalgası (Waveform)",
                            description = "Oynatıcıda müzik ritmine göre hareket eden dalga efekti gösterir.",
                            checked = showWaveform,
                            onCheckedChange = { viewModel.setShowWaveform(it) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingSwitchRow(
                            title = "Albüm Kapağını Alana Yay (Tam Ekran)",
                            description = "Sol alandaki boşlukları doldurarak albüm kapağını tam ekran genişletir.",
                            checked = isalbumartCropped,
                            onCheckedChange = { viewModel.setIsAlbumArtCropped(it) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingactionRow(
                            title = "Şarkı Sözü Font Boyutu",
                            description = "Kapakta ve söz penceresinde yazıların büyüklüğünü belirler."
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TabButton("Standart", "NORMAL", lyricsFontSize) { viewModel.setLyricsFontSize(it) }
                                TabButton("Büyük", "LARGE", lyricsFontSize) { viewModel.setLyricsFontSize(it) }
                                TabButton("Çok Büyük", "XLARGE", lyricsFontSize) { viewModel.setLyricsFontSize(it) }
                            }
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingSwitchRow(
                            title = "Sözleri Otomatik ID3 Tag'e Kaydet",
                            description = "Bulunan veya panodan eklenen sözleri şarkı dosyasına (ID3) otomatik işler.",
                            checked = autoSaveId3Lyrics,
                            onCheckedChange = { viewModel.setAutoSaveId3Lyrics(it) }
                        )
                    }

                    // Oynatma ve Ses
                    SettingsSection(title = "Oynatma & Ses") {
                        SettingSwitchRow(
                            title = "Açılışta Otomatik Başla",
                            description = "Uygulama açılır açılmaz en son çalınan müziğe devam eder.",
                            checked = autoPlayOnStart,
                            onCheckedChange = { viewModel.setAutoPlayOnStart(it) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingSwitchRow(
                            title = "Açılışta Sıraya Otomatik Geç",
                            description = "Açılışta müzik seçiliyse ve işlem yapılmıyorsa 4 saniye sonra çalma sırasını otomatik açar.",
                            checked = autoOpenQueueOnStart,
                            onCheckedChange = { viewModel.setAutoOpenQueueOnStart(it) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingSwitchRow(
                            title = "Kapatılınca Müziği Durdur",
                            description = "Arka plandan uygulamayı sildiğinizde müziği anında durdurur.",
                            checked = stopOnTaskRemoved,
                            onCheckedChange = { viewModel.setStopOnTaskRemoved(it) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingSwitchRow(
                            title = "Boşluksuz Çalma (Gapless)",
                            description = "Şarkı geçişlerindeki sessizliği kaldırır.",
                            checked = gaplessPlayback,
                            onCheckedChange = { viewModel.setGaplessPlayback(it) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingactionRow(
                            title = "Crossfade Süresi",
                            description = "Şarkılar arası yumuşak geçiş süresi."
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                CrossfadeButton("0s", 0, crossfadeDuration) { viewModel.setCrossfadeDuration(it) }
                                CrossfadeButton("1s", 1000, crossfadeDuration) { viewModel.setCrossfadeDuration(it) }
                                CrossfadeButton("3s", 3000, crossfadeDuration) { viewModel.setCrossfadeDuration(it) }
                                CrossfadeButton("5s", 5000, crossfadeDuration) { viewModel.setCrossfadeDuration(it) }
                            }
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingSwitchRow(
                            title = "Ses Seviyesi Eşitleme (Loudness)",
                            description = "Şarkıların ses seviyelerini dengeler.",
                            checked = equalizerState.isLoudnessEnhancerEnabled,
                            onCheckedChange = { viewModel.setLoudnessEnhancerEnabled(it) }
                        )
                    }

                    // Sistem ve Veri
                    SettingsSection(title = "Sistem & Veri") {
                        SettingSwitchRow(
                            title = "Akıllı Listeleri Göster",
                            description = "En çok çalınanlar, son eklenenler gibi otomatik listeleri aktif eder.",
                            checked = showSmartPlaylists,
                            onCheckedChange = { viewModel.setShowSmartPlaylists(it) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingactionRow(
                            title = "Başlangıç Sekmesi",
                            description = "Uygulama açıldığında gösterilecek ana sayfa."
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TabButton("Ana Panel", "HOME", defaultStartTab) { viewModel.setDefaultStartTab(it) }
                                TabButton("Klasör", "FOLDERS", defaultStartTab) { viewModel.setDefaultStartTab(it) }
                                TabButton("Şarkı", "ALL_SONGS", defaultStartTab) { viewModel.setDefaultStartTab(it) }
                                TabButton("Liste", "PLAYLISTS", defaultStartTab) { viewModel.setDefaultStartTab(it) }
                            }
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        
                        // Action Buttons
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val roleMusic = "android.app.role.MUSIC"
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager?.isRoleAvailable(roleMusic) == true) {
                                        val isRoleHeld = roleManager.isRoleHeld(roleMusic)
                                        if (!isRoleHeld) {
                                            val Intent = roleManager.createRequestRoleIntent(roleMusic)
                                            launcher.launch(Intent)
                                        }
                                    } else {
                                        val Intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                                        try {
                                            context.startActivity(Intent)
                                        } catch (e: Exception) {
                                            val AppSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                            try {
                                                context.startActivity(AppSettingsIntent)
                                            } catch (e2: Exception) {
                                                // Fallback
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("Varsayılan Müzik Çalar Yap")
                            }

                            Button(
                                onClick = { viewModel.rescanLibrary() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            ) {
                                Text("Kütüphaneyi Yeniden Tara", fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = { viewModel.clearPlayHistory() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text("Çalma Geçmişini Temizle")
                            }
                        }
                    }

                    // Kütüphane İstatistikleri
                    SettingsSection(title = "Kütüphane İstatistikleri") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatRow("🎵 Toplam Şarkı", "${libraryStats.totalSongs} Şarkı")
                            StatRow("📁 Toplam Klasör", "${libraryStats.totalFolders} Klasör")
                            StatRow("🎤 Farklı Sanatçı", "${libraryStats.uniqueArtists} Sanatçı")
                            StatRow("💿 Farklı Albüm", "${libraryStats.uniqueAlbums} Albüm")
                            StatRow("📑 Çalma Listesi", "${libraryStats.totalPlaylists} Liste")
                            StatRow("⏱️ Toplam Çalma Süresi", libraryStats.totalDurationFormatted)
                            StatRow("💾 Medya Boyutu", libraryStats.totalSizeFormatted)
                        }
                    }

                    // Uygulama Bilgisi
                    SettingsSection(title = "Hakkında") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val versionName = androidx.compose.runtime.remember(context) {
                                try {
                                    val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        context.packageManager.getPackageInfo(context.packageName, 0)
                                    }
                                    pInfo.versionName ?: "2.0"
                                } catch (e: Exception) {
                                    "2.0"
                                }
                            }
                            Text("Sürüm", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Column(horizontalAlignment = Alignment.End) {
                                Text("v$versionName - RidoPlay", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("vibecoded by RK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurface)
            if (description != null) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked, 
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}

@Composable
fun SettingactionRow(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurface)
            if (description != null) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossfadeButton(label: String, valueMs: Int, currentValue: Int, onClick: (Int) -> Unit) {
    FilterChip(
        selected = currentValue == valueMs,
        onClick = { onClick(valueMs) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = null,
        shape = MaterialTheme.shapes.small
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabButton(label: String, value: String, currentValue: String, onClick: (String) -> Unit) {
    FilterChip(
        selected = currentValue == value,
        onClick = { onClick(value) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = null,
        shape = MaterialTheme.shapes.small
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
    }
}


















