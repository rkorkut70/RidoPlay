package com.example.otomuzik.ui.components

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.otomuzik.theme.*
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
    val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()

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
        // ── 3D KOKPİT ARKA PLAN YÜZEYİ ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF141924),
                            Color(0xFF0D1018),
                            Color(0xFF07090F)
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── 1. 3D KOKPİT ÜST BAŞLIK ÇUBUĞU (Header) ───────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (isPhone) 8.dp else 16.dp,
                            shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                            spotColor = accentColor.copy(alpha = 0.35f),
                            ambientColor = Color.Black
                        )
                        .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF222B3D),
                                    Color(0xFF141A25),
                                    Color(0xFF0E121A)
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    accentColor.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            ),
                            RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
                        )
                ) {
                    // Üst ince neon hat
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        accentColor.copy(alpha = 0.9f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

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
                            // 3D Çıkıntılı Dişli İkon Kutusu
                            Box(
                                modifier = Modifier
                                    .size(if (isPhone) 38.dp else 46.dp)
                                    .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = accentColor.copy(0.45f))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF2E384D), Color(0xFF161C27))
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.5f),
                                                accentColor.copy(alpha = 0.35f),
                                                Color.Black.copy(alpha = 0.7f)
                                            )
                                        ),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚙️",
                                    fontSize = if (isPhone) 18.sp else 23.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Ayarlar",
                                    fontSize = if (isPhone) 19.sp else 23.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFFFFF)
                                )
                                Text(
                                    text = if (isPhone) "Sistem & Müzik Ayarları" else "Araç Kokpit & Ses Yapılandırma Paneli",
                                    fontSize = if (isPhone) 11.5.sp else 13.sp,
                                    color = CarTextMuted
                                )
                            }
                        }

                        // 3D Kapatma Butonu
                        Box(
                            modifier = Modifier
                                .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = Color.Black)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF323B4E),
                                            Color(0xFF1E2430),
                                            Color(0xFF141822)
                                        )
                                    )
                                )
                                .border(
                                    1.2.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.45f),
                                            Color(0xFF4B5563),
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    ),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onDismissRequest() }
                                .padding(
                                    horizontal = if (isPhone) 16.dp else 24.dp,
                                    vertical = if (isPhone) 8.dp else 11.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕ Kapat",
                                fontSize = if (isPhone) 13.5.sp else 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF3F4F6)
                            )
                        }
                    }
                }

                // ── 2. 3D AYARLAR SEKME SEÇİCİ (Tab Selector) ────────────────
                var selectedSettingsTab by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
                val settingsTabs = listOf(
                    "Oynatma & Ses",
                    "Görünüm",
                    "Kütüphane",
                    "Araç Modu",
                    "Sistem"
                )

                if (!isPhone) {
                    // Araba / Geniş Ekran Modu: 3D Segmented Switcher Paneli
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = Color(0x66000000))
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF0C0F17), Color(0xFF121622))
                                )
                            )
                            .border(
                                1.2.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.20f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                ),
                                RoundedCornerShape(18.dp)
                            )
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            settingsTabs.forEachIndexed { index, tabTitle ->
                                val isSelected = selectedSettingsTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .then(
                                            if (isSelected) {
                                                Modifier
                                                    .shadow(
                                                        6.dp,
                                                        RoundedCornerShape(12.dp),
                                                        spotColor = accentColor.copy(0.6f)
                                                    )
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(
                                                                accentColor.copy(alpha = 0.38f),
                                                                accentColor.copy(alpha = 0.16f),
                                                                Color(0xFF161F2E)
                                                            )
                                                        )
                                                    )
                                                    .border(
                                                        1.5.dp,
                                                        Brush.verticalGradient(
                                                            listOf(
                                                                Color.White.copy(alpha = 0.65f),
                                                                accentColor,
                                                                Color.Black.copy(alpha = 0.80f)
                                                            )
                                                        ),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                            } else {
                                                Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF141924))
                                                    .border(
                                                        1.dp,
                                                        Brush.verticalGradient(
                                                            listOf(
                                                                Color.White.copy(alpha = 0.12f),
                                                                Color.Black.copy(alpha = 0.6f)
                                                            )
                                                        ),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                            }
                                        )
                                        .clickable { selectedSettingsTab = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabTitle,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 14.5.sp,
                                        color = if (isSelected) accentColor else CarTextMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Telefon Modu İçin Akıcı Sekmeler
                    ScrollableTabRow(
                        selectedTabIndex = selectedSettingsTab,
                        edgePadding = 12.dp,
                        containerColor = Color.Transparent,
                        contentColor = accentColor,
                        divider = {}
                    ) {
                        settingsTabs.forEachIndexed { index, tabTitle ->
                            val isSelected = selectedSettingsTab == index
                            Tab(
                                selected = isSelected,
                                onClick = { selectedSettingsTab = index },
                                text = {
                                    Text(
                                        text = tabTitle,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (isSelected) accentColor else CarTextMuted
                                    )
                                }
                            )
                        }
                    }
                }

                // ── 3. AYAR İÇERİK ALANI (Gövde) ──────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = if (isPhone) 14.dp else 24.dp,
                            vertical = if (isPhone) 6.dp else 14.dp
                        )
                ) {
                    // 1. Sekme / Bölüm: Oynatma ve Ses (Tab 0)
                    if (selectedSettingsTab == 0) {
                        SettingsSection(title = "Oynatma & Ses", accentColor = accentColor) {
                            SettingSwitchRow(
                                title = "Açılışta Otomatik Başla",
                                description = "Uygulama açılır açılmaz en son çalınan müziğe devam eder.",
                                checked = autoPlayOnStart,
                                accentColor = accentColor,
                                onCheckedChange = { viewModel.setAutoPlayOnStart(it) }
                            )
                            SettingsDivider()
                            SettingSwitchRow(
                                title = "Açılışta Sıraya Otomatik Geç",
                                description = "Açılışta müzik seçiliyse ve işlem yapılmıyorsa 4 saniye sonra çalma sırasını otomatik açar.",
                                checked = autoOpenQueueOnStart,
                                accentColor = accentColor,
                                onCheckedChange = { viewModel.setAutoOpenQueueOnStart(it) }
                            )
                            SettingsDivider()
                            SettingSwitchRow(
                                title = "Boşluksuz Çalma (Gapless)",
                                description = "Şarkı geçişlerindeki sessizliği kaldırır.",
                                checked = gaplessPlayback,
                                accentColor = accentColor,
                                onCheckedChange = { viewModel.setGaplessPlayback(it) }
                            )
                            SettingsDivider()
                            SettingActionRow(
                                title = "Crossfade Süresi",
                                description = "Şarkılar arası yumuşak geçiş süresi."
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CrossfadeButton("0s", 0, crossfadeDuration, accentColor) { viewModel.setCrossfadeDuration(it) }
                                    CrossfadeButton("1s", 1000, crossfadeDuration, accentColor) { viewModel.setCrossfadeDuration(it) }
                                    CrossfadeButton("3s", 3000, crossfadeDuration, accentColor) { viewModel.setCrossfadeDuration(it) }
                                    CrossfadeButton("5s", 5000, crossfadeDuration, accentColor) { viewModel.setCrossfadeDuration(it) }
                                }
                            }
                            SettingsDivider()
                            SettingSwitchRow(
                                title = "Ses Seviyesi Eşitleme (Loudness)",
                                description = "Şarkıların ses seviyelerini dengeler.",
                                checked = equalizerState.isLoudnessEnhancerEnabled,
                                accentColor = accentColor,
                                onCheckedChange = { viewModel.setLoudnessEnhancerEnabled(it) }
                            )
                            SettingsDivider()
                            SettingActionRow(
                                title = "Şarkı Geçiş Hassasiyeti (Kaydırma / Swipe)",
                                description = "Kapak ve mini player üzerinde sağa/sola kaydırarak parça değiştirme eşiği."
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TabButton("Çok Hassas", "VERY_HIGH", swipeSensitivity, accentColor) { viewModel.setSwipeSensitivity(it) }
                                    TabButton("Hassas", "HIGH", swipeSensitivity, accentColor) { viewModel.setSwipeSensitivity(it) }
                                    TabButton("Normal", "NORMAL", swipeSensitivity, accentColor) { viewModel.setSwipeSensitivity(it) }
                                    TabButton("Düşük", "LOW", swipeSensitivity, accentColor) { viewModel.setSwipeSensitivity(it) }
                                }
                            }
                            SettingsDivider()
                            SettingActionRow(
                                title = "Oynatma Hızı",
                                description = "Müziklerin çalma hızını belirler."
                            ) {
                                val currentSpeedStr = when {
                                    playbackSpeed <= 0.85f -> "0.8"
                                    playbackSpeed <= 1.1f -> "1.0"
                                    playbackSpeed <= 1.35f -> "1.25"
                                    else -> "1.5"
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TabButton("0.8x", "0.8", currentSpeedStr, accentColor) { viewModel.setPlaybackSpeed(0.8f) }
                                    TabButton("1.0x", "1.0", currentSpeedStr, accentColor) { viewModel.setPlaybackSpeed(1.0f) }
                                    TabButton("1.25x", "1.25", currentSpeedStr, accentColor) { viewModel.setPlaybackSpeed(1.25f) }
                                    TabButton("1.5x", "1.5", currentSpeedStr, accentColor) { viewModel.setPlaybackSpeed(1.5f) }
                                }
                            }
                        }
                    }

                    // 2. Sekme / Bölüm: Görünüm ve Tema (Tab 1)
                    if (selectedSettingsTab == 1) {
                        SettingsSection(title = "Görünüm & Tema", accentColor = accentColor) {
                            SettingActionRow(
                                title = "Tema Modu",
                                description = "Uygulamanın karanlık veya aydınlık mod tercihi."
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TabButton("Oto", "AUTO", themeMode, accentColor) { viewModel.setThemeMode(it) }
                                    TabButton("Açık", "LIGHT", themeMode, accentColor) { viewModel.setThemeMode(it) }
                                    TabButton("Koyu", "DARK", themeMode, accentColor) { viewModel.setThemeMode(it) }
                                }
                            }
                            SettingsDivider()
                            SettingActionRow(
                                title = "Cihaz / Arayüz Modu",
                                description = "Araç/Tablet veya Cep Telefonu ekran düzeni."
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TabButton("Oto", "AUTO", uiMode, accentColor) { viewModel.setUiMode(it) }
                                    TabButton("Araç", "CAR", uiMode, accentColor) { viewModel.setUiMode(it) }
                                    TabButton("Telefon", "PHONE", uiMode, accentColor) { viewModel.setUiMode(it) }
                                }
                            }
                            SettingsDivider()
                            SettingActionRow(
                                title = "Araç İçi Renk Uyumu",
                                description = "Araç konsoluna uygun vurgu rengini seçin veya albüm kapağına göre dinamikleştirin."
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TabButton("Dinamik", "DYNAMIC", themePalette, accentColor) { viewModel.setThemePalette(it) }
                                        TabButton("Neon", "NEON_CYAN", themePalette, accentColor) { viewModel.setThemePalette(it) }
                                        TabButton("Mocha", "GREY_BROWN", themePalette, accentColor) { viewModel.setThemePalette(it) }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TabButton("Kırmızı", "SPORT_RED", themePalette, accentColor) { viewModel.setThemePalette(it) }
                                        TabButton("Mavi", "DEEP_BLUE", themePalette, accentColor) { viewModel.setThemePalette(it) }
                                    }
                                }
                            }
                            SettingsDivider()
                            SettingSwitchRow(
                                title = "Bulanık Arka Plan Efekti",
                                description = "Oynatıcı ekranında albüm kapağını bulanıklaştırarak şık bir arka plan yapar.",
                                checked = showBlurredBackground,
                                accentColor = accentColor,
                                onCheckedChange = { viewModel.setShowBlurredBackground(it) }
                            )
                            SettingsDivider()
                            SettingSwitchRow(
                                title = "Müzik Dalgası (Waveform)",
                                description = "Oynatıcıda müzik ritmine göre hareket eden dalga efekti gösterir.",
                                checked = showWaveform,
                                accentColor = accentColor,
                                onCheckedChange = { viewModel.setShowWaveform(it) }
                            )
                            SettingsDivider()
                            SettingSwitchRow(
                                title = "Albüm Kapağını Alana Yay (Tam Ekran)",
                                description = "Sol alandaki boşlukları doldurarak albüm kapağını tam ekran genişletir.",
                                checked = isalbumartCropped,
                                accentColor = accentColor,
                                onCheckedChange = { viewModel.setIsAlbumArtCropped(it) }
                            )
                            SettingsDivider()
                            SettingActionRow(
                                title = "Şarkı Sözü Font Boyutu",
                                description = "Kapakta ve söz penceresinde yazıların büyüklüğünü belirler."
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TabButton("Standart", "NORMAL", lyricsFontSize, accentColor) { viewModel.setLyricsFontSize(it) }
                                    TabButton("Büyük", "LARGE", lyricsFontSize, accentColor) { viewModel.setLyricsFontSize(it) }
                                    TabButton("Çok Büyük", "XLARGE", lyricsFontSize, accentColor) { viewModel.setLyricsFontSize(it) }
                                }
                            }
                            SettingsDivider()
                            SettingSwitchRow(
                                title = "Sözleri Otomatik ID3 Tag'e Kaydet",
                                description = "Bulunan veya panodan eklenen sözleri şarkı dosyasına (ID3) otomatik işler.",
                                checked = autoSaveId3Lyrics,
                                accentColor = accentColor,
                                onCheckedChange = { viewModel.setAutoSaveId3Lyrics(it) }
                            )
                        }
                    }

                    // 3. Sekme / Bölüm: Kütüphane (Tab 2)
                    if (selectedSettingsTab == 2) {
                        SettingsSection(title = "Kütüphane Yönetimi", accentColor = accentColor) {
                            SettingSwitchRow(
                                title = "Akıllı Listeleri Göster",
                                description = "En çok çalınanlar, son eklenenler gibi otomatik listeleri aktif eder.",
                                checked = showSmartPlaylists,
                                accentColor = accentColor,
                                onCheckedChange = { viewModel.setShowSmartPlaylists(it) }
                            )
                            SettingsDivider()
                            SettingActionRow(
                                title = "Başlangıç Sekmesi",
                                description = "Uygulama açıldığında gösterilecek ana sayfa."
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TabButton("Ana Panel", "HOME", defaultStartTab, accentColor) { viewModel.setDefaultStartTab(it) }
                                    TabButton("Klasör", "FOLDERS", defaultStartTab, accentColor) { viewModel.setDefaultStartTab(it) }
                                    TabButton("Şarkı", "ALL_SONGS", defaultStartTab, accentColor) { viewModel.setDefaultStartTab(it) }
                                    TabButton("Liste", "PLAYLISTS", defaultStartTab, accentColor) { viewModel.setDefaultStartTab(it) }
                                }
                            }
                            SettingsDivider()

                            // 3D Kütüphane Eylem Butonları
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Settings3DActionButton(
                                    title = "🔍 Aynı / Mükerrer Şarkıları Bul",
                                    accentColor = accentColor,
                                    onClick = { viewModel.openDuplicateFinderDialog() }
                                )
                                Settings3DActionButton(
                                    title = "📁 Müzik Klasörlerini Yönet",
                                    accentColor = Color(0xFF64B5F6),
                                    onClick = { viewModel.openFolderSelectionDialog() }
                                )
                                Settings3DActionButton(
                                    title = "🔄 Kütüphaneyi Yeniden Tara",
                                    accentColor = Color(0xFF81C784),
                                    onClick = { viewModel.rescanLibrary() }
                                )
                                Settings3DActionButton(
                                    title = "🗑️ Çalma Geçmişini Temizle",
                                    accentColor = Color(0xFFE57373),
                                    onClick = { viewModel.clearPlayHistory() }
                                )
                            }
                        }

                        // Kütüphane İstatistikleri
                        SettingsSection(title = "Kütüphane İstatistikleri", accentColor = accentColor) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatRow("🎵 Toplam Şarkı", "${libraryStats.totalSongs} Şarkı", accentColor)
                                StatRow("📁 Toplam Klasör", "${libraryStats.totalFolders} Klasör", accentColor)
                                StatRow("🎤 Farklı Sanatçı", "${libraryStats.uniqueArtists} Sanatçı", accentColor)
                                StatRow("💿 Farklı Albüm", "${libraryStats.uniqueAlbums} Albüm", accentColor)
                                StatRow("📑 Çalma Listesi", "${libraryStats.totalPlaylists} Liste", accentColor)
                                StatRow("⏱️ Toplam Çalma Süresi", libraryStats.totalDurationFormatted, accentColor)
                                StatRow("💾 Medya Boyutu", libraryStats.totalSizeFormatted, accentColor)
                            }
                        }
                    }

                    // 4. Sekme / Bölüm: Otomobil Modu (Tab 3)
                    if (selectedSettingsTab == 3) {
                        SettingsSection(title = "Otomobil Modu", accentColor = accentColor) {
                            SettingSwitchRow(
                                title = "Ekranı Uyanık Tut",
                                description = "Uygulama açıkken ekranın kapanmasını engeller.",
                                checked = keepScreenOn,
                                accentColor = accentColor,
                                onCheckedChange = { viewModel.setKeepScreenOn(it) }
                            )
                            SettingsDivider()
                            SettingSwitchRow(
                                title = "Bluetooth ile Otomatik Çal",
                                description = "Araç kitine veya kulaklığa bağlanıldığında müziği otomatik başlatır.",
                                checked = autoPlayOnBluetooth,
                                accentColor = accentColor,
                                onCheckedChange = { viewModel.setAutoPlayOnBluetooth(it) }
                            )
                        }
                    }

                    // 5. Sekme / Bölüm: Sistem (Tab 4)
                    if (selectedSettingsTab == 4) {
                        SettingsSection(title = "Sistem", accentColor = accentColor) {
                            SettingSwitchRow(
                                title = "Kapatılınca Müziği Durdur",
                                description = "Arka plandan uygulamayı sildiğinizde müziği anında durdurur.",
                                checked = stopOnTaskRemoved,
                                accentColor = accentColor,
                                onCheckedChange = { viewModel.setStopOnTaskRemoved(it) }
                            )
                            SettingsDivider()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Settings3DActionButton(
                                    title = "⭐ Varsayılan Müzik Çalar Yap",
                                    accentColor = accentColor,
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
                                    }
                                )
                            }
                        }

                        // Uygulama ve Sürüm Bilgisi
                        SettingsSection(title = "Hakkında", accentColor = accentColor) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                        pInfo.versionName ?: "3.6"
                                    } catch (e: Exception) {
                                        "3.6"
                                    }
                                }
                                Column {
                                    Text("Sürüm Bilgisi", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFFFFF))
                                    Text("RidoPlay Otomobil & Tablet", fontSize = 12.sp, color = CarTextMuted)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "v$versionName",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accentColor
                                    )
                                    Text("vibecoded by RK", fontSize = 11.5.sp, color = CarTextMuted)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }
}

// ── 3D AYAR BÖLÜMÜ KARTI (SettingsSection) ──────────────────────────────────
@Composable
fun SettingsSection(
    title: String,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val isPhone = configuration.screenWidthDp < 600

    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        // Bölüm Başlığı ve 3D Neon Vurgu Çubuğu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 15.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                ),
                color = Color(0xFFF1F5F9)
            )
        }

        // 3D Yükseltilmiş Kokpit Kartı
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isPhone) 6.dp else 12.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = Color.Black,
                    ambientColor = Color.Black
                )
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1B2230),
                            Color(0xFF121622),
                            Color(0xFF0A0D14)
                        )
                    )
                )
                .border(
                    1.4.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            accentColor.copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    ),
                    RoundedCornerShape(18.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Kart üst içi parlama çizgisi (Gloss highlight)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                content()
            }
        }
    }
}

// ── 3D KANALLI AYRAÇ (SettingsDivider) ──────────────────────────────────────
@Composable
fun SettingsDivider() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black.copy(alpha = 0.70f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )
    }
}

// ── 3D AYAR ANAHTAR SATIRI (SettingSwitchRow) ────────────────────────────────
@Composable
fun SettingSwitchRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                ),
                color = Color(0xFFFFFFFF)
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = CarTextMuted
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = accentColor,
                checkedThumbColor = Color(0xFF0B0E14),
                uncheckedTrackColor = Color(0xFF1E2432),
                uncheckedThumbColor = Color(0xFF94A3B8)
            )
        )
    }
}

// ── 3D AYAR EYLEM SATIRI (SettingActionRow) ──────────────────────────────────
@Composable
fun SettingActionRow(
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
                .padding(horizontal = 16.dp, vertical = 11.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.5.sp),
                color = Color(0xFFFFFFFF)
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = CarTextMuted
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
                .padding(horizontal = 16.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.5.sp),
                    color = Color(0xFFFFFFFF)
                )
                if (description != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = CarTextMuted
                    )
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

// ── 3D DOKUNMATİK SEÇİM BUTONU (CrossfadeButton) ─────────────────────────────
@Composable
fun CrossfadeButton(
    label: String,
    valueMs: Int,
    currentValue: Int,
    accentColor: Color,
    onClick: (Int) -> Unit
) {
    val isSelected = currentValue == valueMs
    Box(
        modifier = Modifier
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(6.dp, RoundedCornerShape(10.dp), spotColor = accentColor.copy(0.6f))
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    accentColor,
                                    accentColor.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .border(1.2.dp, Color.White.copy(0.6f), RoundedCornerShape(10.dp))
                } else {
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF222B3B), Color(0xFF141924))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(0.18f), Color.Black.copy(0.6f))
                            ),
                            RoundedCornerShape(10.dp)
                        )
                }
            )
            .clickable { onClick(valueMs) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF080C14) else Color(0xFFE2E8F0)
        )
    }
}

// ── 3D DOKUNMATİK SEÇİM BUTONU (TabButton) ───────────────────────────────────
@Composable
fun TabButton(
    label: String,
    value: String,
    currentValue: String,
    accentColor: Color,
    onClick: (String) -> Unit
) {
    val isSelected = currentValue == value
    Box(
        modifier = Modifier
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(6.dp, RoundedCornerShape(10.dp), spotColor = accentColor.copy(0.6f))
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    accentColor,
                                    accentColor.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .border(1.2.dp, Color.White.copy(0.6f), RoundedCornerShape(10.dp))
                } else {
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF222B3B), Color(0xFF141924))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(0.18f), Color.Black.copy(0.6f))
                            ),
                            RoundedCornerShape(10.dp)
                        )
                }
            )
            .clickable { onClick(value) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF080C14) else Color(0xFFE2E8F0)
        )
    }
}

// ── 3D KÜTÜPHANE VE SİSTEM EYLEM BUTONU ──────────────────────────────────────
@Composable
fun Settings3DActionButton(
    title: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = accentColor.copy(0.35f))
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF242E40),
                        Color(0xFF161C27)
                    )
                )
            )
            .border(
                1.2.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        accentColor.copy(alpha = 0.45f),
                        Color.Black.copy(alpha = 0.75f)
                    )
                ),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
            color = Color(0xFFFFFFFF)
        )
    }
}

// ── 3D İSTATİSTİK ROZETİ SATIRI (StatRow) ────────────────────────────────────
@Composable
private fun StatRow(label: String, value: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141A26).copy(alpha = 0.65f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.5.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Medium)
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}
