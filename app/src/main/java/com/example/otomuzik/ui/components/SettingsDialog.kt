package com.example.otomuzik.ui.components
import com.example.otomuzik.theme.*

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
    val configuration = LocalConfiguration.current
    val isPhone = configuration.screenWidthDp < 600
    
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
    val uiMode by viewModel.uiMode.collectAsStateWithLifecycle()
    val autoOpenQueueOnStart by viewModel.autoOpenQueueOnStart.collectAsStateWithLifecycle()
    val lyricsFontSize by viewModel.lyricsFontSize.collectAsStateWithLifecycle()
    val autoSaveId3Lyrics by viewModel.autoSaveId3Lyrics.collectAsStateWithLifecycle()
    val swipeSensitivity by viewModel.swipeSensitivity.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Header (Tam Ekran Başlığı ve Kapat Butonu)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isPhone) 16.dp else 28.dp,
                                vertical = if (isPhone) 12.dp else 16.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚙️",
                                fontSize = if (isPhone) 20.sp else 24.sp,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Text(
                                text = "Ayarlar",
                                style = if (isPhone) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                       else MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FilledTonalButton(
                            onClick = onDismissRequest,
                            contentPadding = PaddingValues(
                                horizontal = if (isPhone) 16.dp else 24.dp,
                                vertical = if (isPhone) 6.dp else 10.dp
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "✕ Kapat",
                                fontSize = if (isPhone) 14.sp else 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Ayarlar Sekmeleri (Hem Telefon hem Araç modunda düzenli ve kategorik)
                var selectedSettingsTab by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
                val settingsTabs = listOf(
                    "Oynatma & Ses",
                    "Görünüm",
                    "Kütüphane",
                    "Araç Modu",
                    "Sistem"
                )

                if (isPhone) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedSettingsTab,
                        edgePadding = 12.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    ) {
                        settingsTabs.forEachIndexed { index, tabTitle ->
                            Tab(
                                selected = selectedSettingsTab == index,
                                onClick = { selectedSettingsTab = index },
                                text = {
                                    Text(
                                        text = tabTitle,
                                        fontWeight = if (selectedSettingsTab == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                        }
                    }
                } else {
                    // Araba / Geniş Ekran Modu: Tam ekran genişliğinde büyük dokunmatik sekmeler
                    TabRow(
                        selectedTabIndex = selectedSettingsTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    ) {
                        settingsTabs.forEachIndexed { index, tabTitle ->
                            Tab(
                                selected = selectedSettingsTab == index,
                                onClick = { selectedSettingsTab = index },
                                modifier = Modifier.height(56.dp),
                                text = {
                                    Text(
                                        text = tabTitle,
                                        fontWeight = if (selectedSettingsTab == index) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 15.sp,
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = if (isPhone) 14.dp else 28.dp,
                            vertical = if (isPhone) 6.dp else 14.dp
                        )
                ) {
                    // 1. Sekme / Bölüm: Oynatma ve Ses (Tab 0)
                    if (selectedSettingsTab == 0) {
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
                            Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                            SettingactionRow(
                                title = "Şarkı Geçiş Hassasiyeti (Kaydırma / Swipe)",
                                description = "Kapak ve mini player üzerinde sağa/sola kaydırarak parça değiştirme eşiği."
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TabButton("Çok Hassas", "VERY_HIGH", swipeSensitivity) { viewModel.setSwipeSensitivity(it) }
                                    TabButton("Hassas", "HIGH", swipeSensitivity) { viewModel.setSwipeSensitivity(it) }
                                    TabButton("Normal", "NORMAL", swipeSensitivity) { viewModel.setSwipeSensitivity(it) }
                                    TabButton("Düşük", "LOW", swipeSensitivity) { viewModel.setSwipeSensitivity(it) }
                                }
                            }
                            Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                            SettingactionRow(
                                title = "Oynatma Hızı",
                                description = "Müziklerin çalma hızını belirler."
                            ) {
                                val currentSpeedStr = when {
                                    playbackSpeed <= 0.85f -> "0.8"
                                    playbackSpeed <= 1.1f -> "1.0"
                                    playbackSpeed <= 1.35f -> "1.25"
                                    else -> "1.5"
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TabButton("0.8x", "0.8", currentSpeedStr) { viewModel.setPlaybackSpeed(0.8f) }
                                    TabButton("1.0x", "1.0", currentSpeedStr) { viewModel.setPlaybackSpeed(1.0f) }
                                    TabButton("1.25x", "1.25", currentSpeedStr) { viewModel.setPlaybackSpeed(1.25f) }
                                    TabButton("1.5x", "1.5", currentSpeedStr) { viewModel.setPlaybackSpeed(1.5f) }
                                }
                            }
                        }
                    }

                    // 2. Sekme / Bölüm: Görünüm ve Tema (Tab 1)
                    if (selectedSettingsTab == 1) {
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
                                title = "Cihaz / Arayüz Modu",
                                description = "Araç/Tablet veya Cep Telefonu ekran düzeni."
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TabButton("Oto", "AUTO", uiMode) { viewModel.setUiMode(it) }
                                    TabButton("Araç", "CAR", uiMode) { viewModel.setUiMode(it) }
                                    TabButton("Telefon", "PHONE", uiMode) { viewModel.setUiMode(it) }
                                }
                            }
                            Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                            SettingactionRow(
                                title = "Araç İçi Renk Uyumu",
                                description = "Araç konsoluna uygun vurgu rengini seçin veya albüm kapağına göre dinamikleştirin."
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TabButton("Dinamik", "DYNAMIC", themePalette) { viewModel.setThemePalette(it) }
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
                    }

                    // 3. Sekme / Bölüm: Kütüphane (Tab 2 - Ayrı Kütüphane Sekmesi)
                    if (selectedSettingsTab == 2) {
                        SettingsSection(title = "Kütüphane Yönetimi") {
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
                            
                            // Kütüphane Eylem Butonları
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.openDuplicateFinderDialog() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Text("🔍 Aynı / Mükerrer Şarkıları Bul", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.openFolderSelectionDialog() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Text("📁 Müzik Klasörlerini Yönet", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.rescanLibrary() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                ) {
                                    Text("🔄 Kütüphaneyi Yeniden Tara", fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = { viewModel.clearPlayHistory() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text("🗑️ Çalma Geçmişini Temizle")
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
                    }

                    // 4. Sekme / Bölüm: Otomobil Modu (Tab 3)
                    if (selectedSettingsTab == 3) {
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
                    }

                    // 5. Sekme / Bölüm: Sistem (Tab 4)
                    if (selectedSettingsTab == 4) {
                        SettingsSection(title = "Sistem") {
                            SettingSwitchRow(
                                title = "Kapatılınca Müziği Durdur",
                                description = "Arka plandan uygulamayı sildiğinizde müziği anında durdurur.",
                                checked = stopOnTaskRemoved,
                                onCheckedChange = { viewModel.setStopOnTaskRemoved(it) }
                            )
                            Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val roleMusic = "android.app.role.MUSIC"
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager?.isRoleAvailable(roleMusic) == true) {
                                            val isRoleHeld = roleManager.isRoleHeld(roleMusic)
                                            if (!isRoleHeld) {
                                                val intent = roleManager.createRequestRoleIntent(roleMusic)
                                                launcher.launch(intent)
                                            }
                                        } else {
                                            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = android.net.Uri.parse("package:${context.packageName}")
                                                }
                                                try {
                                                    context.startActivity(appSettingsIntent)
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
                            }
                        }

                        // Uygulama ve Sürüm Bilgisi
                        SettingsSection(title = "Hakkında") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val versionName = androidx.compose.runtime.remember(context) {
                                    try {
                                        val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                                        } else {
                                            @Suppress("DEPRECATION")
                                            context.packageManager.getPackageInfo(context.packageName, 0)
                                        }
                                        pInfo.versionName ?: "3.3"
                                    } catch (e: Exception) {
                                        "3.3"
                                    }
                                }
                                Text("Sürüm", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("v$versionName - RidoPlay", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("vibecoded by RK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
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
    val configuration = LocalConfiguration.current
    val isPhone = configuration.screenWidthDp < 600

    if (isPhone) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                content()
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp), color = MaterialTheme.colorScheme.onSurface)
                if (description != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                contentAlignment = Alignment.CenterEnd
            ) {
                content()
            }
        }
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


















