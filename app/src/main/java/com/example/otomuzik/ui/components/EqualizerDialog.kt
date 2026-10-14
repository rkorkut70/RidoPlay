package com.example.otomuzik.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.otomuzik.model.EqualizerState
import com.example.otomuzik.model.EqBand
import com.example.otomuzik.theme.*
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * Kullanıcının talep ettiği 4 Ekolayzır Türü:
 * 1. 3 Band (Bass, Mid, Treble)
 * 2. 5 - 7 Band (Giriş Seviyesi)
 * 3. 10 Band (1 Oktav EQ)
 * 4. 15 Band (2/3 Oktav EQ)
 */
enum class EqBandMode(
    val title: String,
    val subtitle: String,
    val frequencies: List<Int>,
    val labels: List<String>,
    val description: String
) {
    THREE_BAND(
        title = "3 Band",
        subtitle = "Bass, Mid, Treble",
        frequencies = listOf(100, 1000, 10000),
        labels = listOf("Bass", "Mid", "Treble"),
        description = "En temel ekolayzır türüdür. Eski müzik setlerinde, araba teyplerinde ve basit amfilerde sesin genel karakterini (kalın, orta, ince sesler) hızlıca ayarlamak için kullanılır."
    ),
    FIVE_BAND(
        title = "5 - 7 Band",
        subtitle = "Standart Mobil & Araç",
        frequencies = listOf(60, 230, 910, 3600, 14000),
        labels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz"),
        description = "Giriş seviyesi ses sistemlerinde ve bazı mobil müzik uygulamalarında bulunur. Bas ve tizlerin yanı sıra vokal aralıklarına biraz daha müdahale etme şansı tanır."
    ),
    TEN_BAND(
        title = "10 Band",
        subtitle = "1 Oktav EQ",
        frequencies = listOf(31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000),
        labels = listOf("31", "63", "125", "250", "500", "1k", "2k", "4k", "8k", "16k"),
        description = "Ev sinema sistemlerinde, kulaklık yazılımlarında ve amatör stüdyolarda sıklıkla tercih edilir. Her bir band bir oktavlık frekans alanını temsil eder ve müzik dinleyicileri için oldukça yeterli bir hassasiyet sunar."
    ),
    FIFTEEN_BAND(
        title = "15 Band",
        subtitle = "2/3 Oktav EQ",
        frequencies = listOf(25, 40, 63, 100, 160, 250, 400, 630, 1000, 1600, 2500, 4000, 6300, 10000, 16000),
        labels = listOf("25", "40", "63", "100", "160", "250", "400", "630", "1k", "1.6k", "2.5k", "4k", "6.3k", "10k", "16k"),
        description = "Canlı ses performanslarında (PA sistemleri) ve küçük sahnelerde, feedback (ötme) yapan frekansları yakalayıp kısmak için kullanılır."
    )
}

@Composable
fun EqualizerDialog(
    eqState: EqualizerState,
    onToggleEnable: (Boolean) -> Unit,
    onSelectPreset: (String) -> Unit,
    onBandChange: (Short, Short) -> Unit,
    onBassBoostChange: (Short) -> Unit,
    onLoudnessEnhancerChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color = CarCyan
) {
    var activeMode by remember { mutableStateOf(EqBandMode.FIVE_BAND) }
    var showInfoDescription by remember { mutableStateOf(false) }

    // Her mod için ayrı dB kazanımları (-15.0f .. +15.0f dB)
    val gains3Band = remember { mutableStateMapOf(0 to 0f, 1 to 0f, 2 to 0f) }
    val gains5Band = remember { mutableStateMapOf(0 to 0f, 1 to 0f, 2 to 0f, 3 to 0f, 4 to 0f) }
    val gains10Band = remember { mutableStateMapOf<Int, Float>().apply { repeat(10) { put(it, 0f) } } }
    val gains15Band = remember { mutableStateMapOf<Int, Float>().apply { repeat(15) { put(it, 0f) } } }

    // Donanım bantlarından SADECE İLK AÇILIŞTA senkronize et (Kullanıcı kaydırdığında feedback döngüsü olmaması için)
    var isInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(eqState.bands) {
        if (isInitialized) return@LaunchedEffect
        val bands = if (eqState.bands.isNotEmpty()) eqState.bands else com.example.otomuzik.model.DEFAULT_EQ_BANDS
        if (bands.isEmpty()) return@LaunchedEffect

        // 5 Band senkronizasyonu
        bands.forEachIndexed { i, band ->
            val db = (band.levelMb / 100f).coerceIn(-15f, 15f)
            if (i < 5) gains5Band[i] = db
        }
        // 3 Band senkronizasyonu (Bass, Mid, Treble)
        gains3Band[0] = (bands[0].levelMb / 100f).coerceIn(-15f, 15f)
        val midBand = bands.getOrNull(2) ?: bands.getOrNull(1) ?: bands[0]
        gains3Band[1] = (midBand.levelMb / 100f).coerceIn(-15f, 15f)
        val trebleBand = bands.last()
        gains3Band[2] = (trebleBand.levelMb / 100f).coerceIn(-15f, 15f)

        // 10 ve 15 band interpolasyonu
        if (bands.size >= 2) {
            val logHw = bands.map { log10(it.centerFreqHz.toDouble().coerceAtLeast(10.0)) }
            val hwGains = bands.map { it.levelMb / 100f }
            fun interp(f: Int): Float {
                val lf = log10(f.toDouble().coerceAtLeast(10.0))
                return when {
                    lf <= logHw.first() -> hwGains.first()
                    lf >= logHw.last() -> hwGains.last()
                    else -> {
                        var k = 0
                        while (k < logHw.size - 1 && logHw[k + 1] < lf) k++
                        val t = (lf - logHw[k]) / (logHw[k + 1] - logHw[k])
                        ((1.0 - t) * hwGains[k] + t * hwGains[k + 1]).toFloat()
                    }
                }
            }
            EqBandMode.TEN_BAND.frequencies.forEachIndexed { i, f -> gains10Band[i] = interp(f) }
            EqBandMode.FIFTEEN_BAND.frequencies.forEachIndexed { i, f -> gains15Band[i] = interp(f) }
        }
        isInitialized = true
    }

    val currentGains = when (activeMode) {
        EqBandMode.THREE_BAND -> gains3Band
        EqBandMode.FIVE_BAND -> gains5Band
        EqBandMode.TEN_BAND -> gains10Band
        EqBandMode.FIFTEEN_BAND -> gains15Band
    }

    // Sanal fader hareket ettiğinde donanım bantlarına frekans ağırlıklı etki uygula
    // Böylece her fader bağımsız kalır, tek bir çubuk oynatıldığında diğerleri ASLA oynamaz
    // ve en soldaki bas çubuğu hardware bas bandını doğru şekilde besler (flat yapmaz).
    fun applyGainsToHardware() {
        val virtFreqs = activeMode.frequencies
        val virtGains = virtFreqs.indices.map { currentGains[it] ?: 0f }
        val targetBands = if (eqState.bands.isNotEmpty()) eqState.bands else com.example.otomuzik.model.DEFAULT_EQ_BANDS

        targetBands.forEach { hwBand ->
            val hwFreq = hwBand.centerFreqHz.toDouble().coerceAtLeast(10.0)
            val logHw = log10(hwFreq)

            var totalWeight = 0.0
            var weightedSum = 0.0

            virtFreqs.forEachIndexed { i, vf ->
                val logV = log10(vf.toDouble().coerceAtLeast(10.0))
                val dist = kotlin.math.abs(logHw - logV)
                // 0.45 dekad (~1.5 oktav) band nüfuz yarıçapı
                val radius = 0.45
                if (dist < radius) {
                    val w = (1.0 - (dist / radius)) * (1.0 - (dist / radius))
                    weightedSum += virtGains[i] * w
                    totalWeight += w
                }
            }

            val gainDb = if (totalWeight > 0.001) {
                (weightedSum / totalWeight).toFloat()
            } else {
                val closestIdx = virtFreqs.indices.minByOrNull { 
                    kotlin.math.abs(logHw - log10(virtFreqs[it].toDouble().coerceAtLeast(10.0))) 
                } ?: 0
                virtGains[closestIdx]
            }

            val gainMb = (gainDb * 100f).toInt().coerceIn(hwBand.minLevelMb.toInt(), hwBand.maxLevelMb.toInt()).toShort()
            onBandChange(hwBand.index, gainMb)
        }
    }

    // Hazır profil seçildiğinde bantları güncelle
    fun applyPresetValues(preset: String) {
        onSelectPreset(preset)
        val profileCurves = mapOf(
            "Rock" to listOf(4.5f, 3.0f, -1.0f, 2.5f, 5.0f),
            "Pop" to listOf(-1.5f, 2.0f, 4.0f, 3.0f, -1.0f),
            "Jazz" to listOf(3.5f, 1.5f, -1.5f, 2.0f, 3.5f),
            "Klasik" to listOf(4.0f, 2.5f, -1.0f, 2.5f, 4.0f),
            "Bass Boost" to listOf(8.0f, 5.0f, 1.0f, 0.0f, 0.0f),
            "Vokal" to listOf(-2.5f, 1.0f, 5.5f, 3.0f, -1.0f),
            "Akustik" to listOf(3.5f, 2.0f, 1.0f, 3.0f, 4.0f),
            "Metal" to listOf(6.0f, 1.0f, -2.5f, 3.5f, 6.0f),
            "Düz" to listOf(0f, 0f, 0f, 0f, 0f)
        )
        val curve5 = profileCurves[preset] ?: return
        val virtFreqs = activeMode.frequencies
        virtFreqs.indices.forEach { i ->
            val ratio = i.toFloat() / (virtFreqs.size - 1).coerceAtLeast(1)
            val cIdx = ratio * (curve5.size - 1)
            val base = cIdx.toInt().coerceIn(0, curve5.size - 2)
            val frac = cIdx - base
            val valDb = (1f - frac) * curve5[base] + frac * curve5[base + 1]
            currentGains[i] = valDb
        }
        applyGainsToHardware()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // ── 3D STUDIO RACK PANEL GÖVDESİ ──
        Box(
            modifier = Modifier
                .widthIn(min = 360.dp, max = 820.dp)
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E232E),
                            Color(0xFF12151B),
                            Color(0xFF0C0E12)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.8f),
                            Color(0x33FFFFFF),
                            Color(0x10000000)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── 1. RACK ÜST PANEL: BAŞLIK, 3D LED GÜÇ ANAHTARI & KAPAT ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 3D Metalik Donanım Vidaları
                        RackScrew()

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "STUDIO DSP EQUALIZER",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Canlı 3D Durum LED'i
                                StatusLed(isActive = eqState.isEnabled, activeColor = accentColor)
                            }
                            Text(
                                text = "Profesyonel Çok Bantlı Akustik İşlemci",
                                fontSize = 11.sp,
                                color = CarTextMuted
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Güç Switch'i (Master Toggle)
                        Switch(
                            checked = eqState.isEnabled,
                            onCheckedChange = onToggleEnable,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accentColor,
                                checkedTrackColor = accentColor.copy(alpha = 0.35f),
                                uncheckedThumbColor = Color(0xFF6B7280),
                                uncheckedTrackColor = Color(0xFF1F2937)
                            )
                        )

                        // 3D Kapat Butonu
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF2E3440), Color(0xFF191D24))
                                    )
                                )
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                color = CarTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 2. GELİŞMİŞ BAND SEÇENEKLERİ (3, 5-7, 10, 15 BAND) ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🎚️ EKOLAYZIR TÜRÜ (BANT SAYISI)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        letterSpacing = 0.8.sp
                    )

                    // Bilgi Göster/Gizle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showInfoDescription = !showInfoDescription }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showInfoDescription) "ℹ️ Bilgiyi Gizle" else "ℹ️ Nedir?",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showInfoDescription) accentColor else CarTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4 Band Mod Seçim Butonları (3D Metal Butonlar)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EqBandMode.values().forEach { mode ->
                        val isSelected = activeMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.verticalGradient(
                                            listOf(
                                                accentColor.copy(alpha = 0.35f),
                                                accentColor.copy(alpha = 0.15f)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF232832), Color(0xFF14171E))
                                        )
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) accentColor else Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    val oldMode = activeMode
                                    val oldFreqs = oldMode.frequencies
                                    val oldGains = oldFreqs.indices.map { currentGains[it] ?: 0f }
                                    activeMode = mode

                                    val targetGains = when (mode) {
                                        EqBandMode.THREE_BAND -> gains3Band
                                        EqBandMode.FIVE_BAND -> gains5Band
                                        EqBandMode.TEN_BAND -> gains10Band
                                        EqBandMode.FIFTEEN_BAND -> gains15Band
                                    }
                                    val hasNonZero = mode.frequencies.indices.any { (targetGains[it] ?: 0f) != 0f }
                                    if (!hasNonZero && oldGains.any { it != 0f }) {
                                        val logOld = oldFreqs.map { log10(it.toDouble().coerceAtLeast(10.0)) }
                                        mode.frequencies.forEachIndexed { i, f ->
                                            val lf = log10(f.toDouble().coerceAtLeast(10.0))
                                            val valDb = when {
                                                lf <= logOld.first() -> oldGains.first()
                                                lf >= logOld.last() -> oldGains.last()
                                                else -> {
                                                    var k = 0
                                                    while (k < logOld.size - 1 && logOld[k + 1] < lf) k++
                                                    val t = (lf - logOld[k]) / (logOld[k + 1] - logOld[k])
                                                    ((1.0 - t) * oldGains[k] + t * oldGains[k + 1]).toFloat()
                                                }
                                            }
                                            targetGains[i] = valDb
                                        }
                                    }
                                    applyGainsToHardware()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = mode.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) accentColor else Color(0xFFE2E8F0)
                                )
                                Text(
                                    text = mode.subtitle,
                                    fontSize = 9.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else CarTextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Kullanıcının Talep Ettiği Detaylı Açıklama Kutusu
                AnimatedVisibility(visible = showInfoDescription) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15181F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "• ${activeMode.title} (${activeMode.subtitle}):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = activeMode.description,
                                fontSize = 11.sp,
                                color = Color(0xFFD1D5DB),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── 3. CANLI 3D FREKANS TEPKİ EĞRİSİ (CANVAS DSP ANALYZER) ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF090B0E), Color(0xFF10131A))
                            )
                        )
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    SpectrumResponseCurve(
                        gains = activeMode.frequencies.indices.map { currentGains[it] ?: 0f },
                        accentColor = accentColor,
                        isEnabled = eqState.isEnabled
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── 4. 3D DİKEY FADER KONSOLU (CHANNEL STRIPS) ──
                val faderScrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF13171F), Color(0xFF0E1015))
                            )
                        )
                        .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(14.dp))
                        .padding(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (activeMode.frequencies.size > 5) Modifier.horizontalScroll(faderScrollState) else Modifier),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeMode.frequencies.forEachIndexed { index, freq ->
                            val label = activeMode.labels[index]
                            val currentDb = currentGains[index] ?: 0f

                            Studio3DFaderChannel(
                                label = label,
                                freqHz = freq,
                                currentDb = currentDb,
                                accentColor = accentColor,
                                isEnabled = eqState.isEnabled,
                                isCompact = activeMode.frequencies.size > 7,
                                onGainChange = { newDb ->
                                    currentGains[index] = newDb
                                    applyGainsToHardware()
                                },
                                onInteractionStart = {
                                    if (!eqState.isEnabled) {
                                        onToggleEnable(true)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── 5. SES PROFİLLERİ & HIZLI SIFIRLAMA ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎛️ SES PROFİLLERİ (PRESETS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        letterSpacing = 0.8.sp
                    )

                    // 3D Sıfırla (Flat) Butonu
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF2C3240), Color(0xFF1A1D24))
                                )
                            )
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .clickable {
                                activeMode.frequencies.indices.forEach { currentGains[it] = 0f }
                                applyGainsToHardware()
                                onSelectPreset("Düz")
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⟲ SIFIRLA (FLAT)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val presetNames = listOf("Rock", "Pop", "Jazz", "Klasik", "Bass Boost", "Vokal", "Akustik", "Metal", "Düz")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetNames) { preset ->
                        val isSelected = preset.equals(eqState.currentPreset, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.verticalGradient(
                                            listOf(accentColor, accentColor.copy(alpha = 0.7f))
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF242A36), Color(0xFF161920))
                                        )
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { applyPresetValues(preset) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0C0E12) else Color(0xFFE2E8F0)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── 6. ALT PANEL: 3D DERİN BAS (BASS BOOST) & LOUDNESS SES KUVVETLENDİRME ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Derin Bas Güçlendirme (Bass Boost)
                    if (eqState.isBassBoostSupported) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF12151D))
                                .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔊 DERİN BAS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                    Text(
                                        text = "%${eqState.bassBoostStrength / 10}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Slider(
                                    value = eqState.bassBoostStrength.toFloat(),
                                    onValueChange = { onBassBoostChange(it.toInt().toShort()) },
                                    valueRange = 0f..1000f,
                                    enabled = eqState.isEnabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = accentColor,
                                        activeTrackColor = accentColor,
                                        inactiveTrackColor = Color(0xFF262C38)
                                    ),
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }

                    // Loudness Enhancer (Ses Kuvvetlendirici)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF12151D))
                            .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "⚡ LOUDNESS GAIN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (eqState.isLoudnessEnhancerEnabled) accentColor else CarTextPrimary
                                )
                                Text(
                                    text = "+5 dB Net Dinamik Ses",
                                    fontSize = 10.sp,
                                    color = CarTextMuted
                                )
                            }
                            Switch(
                                checked = eqState.isLoudnessEnhancerEnabled,
                                onCheckedChange = onLoudnessEnhancerChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = accentColor,
                                    checkedTrackColor = accentColor.copy(alpha = 0.35f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Profesyonel 3D Stüdyo Dikey Fader Kanalı.
 */
@Composable
private fun Studio3DFaderChannel(
    label: String,
    freqHz: Int,
    currentDb: Float,
    accentColor: Color,
    isEnabled: Boolean,
    isCompact: Boolean,
    onGainChange: (Float) -> Unit,
    onInteractionStart: () -> Unit = {}
) {
    val trackHeight = 130.dp
    val trackHeightPx = with(LocalDensity.current) { trackHeight.toPx() }
    val isPositive = currentDb > 0.05f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(if (isCompact) 42.dp else 54.dp)
            .padding(horizontal = 2.dp)
    ) {
        // dB Değeri
        Text(
            text = "${if (currentDb > 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", currentDb)}",
            fontSize = if (isCompact) 10.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                !isEnabled -> CarTextMuted
                isPositive -> accentColor
                currentDb < -0.05f -> Color(0xFFF87171)
                else -> Color(0xFF9CA3AF)
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Fader Yuvası & 3D Fader Başlığı (Handle)
        // Geniş dokunma alanı: kanal boyunca parmakla sürükleme ve tıklama
        Box(
            modifier = Modifier
                .height(trackHeight)
                .width(if (isCompact) 36.dp else 48.dp)
                .pointerInput(trackHeightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        down.consume()
                        onInteractionStart()

                        fun updateFromY(y: Float) {
                            val norm = 1f - (y / trackHeightPx).coerceIn(0f, 1f)
                            val rawDb = (norm * 30f - 15f).coerceIn(-15f, 15f)
                            val snappedDb = if (kotlin.math.abs(rawDb) < 0.4f) 0f else rawDb
                            onGainChange(snappedDb)
                        }

                        updateFromY(down.position.y)

                        var isDragging = true
                        while (isDragging) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull()
                            if (change == null || !change.pressed) {
                                isDragging = false
                            } else {
                                change.consume()
                                updateFromY(change.position.y)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 3D Oluk (Groove Track)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF090B0E))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(3.dp))
            )

            // Sıfır dB Merkez Çizgisi
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(1.5.dp)
                    .background(Color(0x60FFFFFF))
            )

            // 3D Metalik Fader Başlığı (Slider Knob)
            val knobHeightPx = with(LocalDensity.current) { 24.dp.toPx() }
            val travelDistance = trackHeightPx - knobHeightPx
            val normalizedPos = ((15f - currentDb) / 30f).coerceIn(0f, 1f)
            val knobOffsetFromCenter = (normalizedPos - 0.5f) * travelDistance

            Box(
                modifier = Modifier
                    .offset { IntOffset(0, knobOffsetFromCenter.roundToInt()) }
                    .width(if (isCompact) 30.dp else 38.dp)
                    .height(24.dp)
                    .shadow(4.dp, RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF5A6372),
                                Color(0xFF383F4B),
                                Color(0xFF1E222A)
                            )
                        )
                    )
                    .border(1.dp, Color(0x60FFFFFF), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Fader üzerindeki neon gösterge çizgisi
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(2.5.dp)
                        .background(if (isEnabled) accentColor else Color(0xFF6B7280))
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Frekans Etiketi
        Text(
            text = label,
            fontSize = if (isCompact) 10.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFD1D5DB),
            maxLines = 1
        )
    }
}

/**
 * Canlı 3D Frekans Tepki Grafiği (DSP Curve).
 */
@Composable
private fun SpectrumResponseCurve(
    gains: List<Float>,
    accentColor: Color,
    isEnabled: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Izgara Çizgileri
        val gridColor = Color(0x18FFFFFF)
        drawLine(gridColor, Offset(0f, midY), Offset(w, midY), strokeWidth = 1f)
        drawLine(gridColor, Offset(0f, midY - h * 0.35f), Offset(w, midY - h * 0.35f), strokeWidth = 1f)
        drawLine(gridColor, Offset(0f, midY + h * 0.35f), Offset(w, midY + h * 0.35f), strokeWidth = 1f)

        if (gains.size < 2) return@Canvas

        val stepX = w / (gains.size - 1)
        val points = gains.mapIndexed { i, db ->
            val clampedDb = if (isEnabled) db.coerceIn(-15f, 15f) else 0f
            val y = midY - (clampedDb / 15f) * (h * 0.42f)
            Offset(i * stepX, y)
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val ctrlX = (p0.x + p1.x) / 2f
                cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
            }
        }

        // Dolgu gradyanı (Alan Işıltısı)
        val fillPath = Path().apply {
            addPath(path)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(
                    (if (isEnabled) accentColor else Color.Gray).copy(alpha = 0.28f),
                    Color.Transparent
                )
            )
        )

        // Neon Işıltılı Ana Tepki Eğrisi
        drawPath(
            path = path,
            color = if (isEnabled) accentColor else Color.Gray,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Frekans Düğümleri (Noktalar)
        points.forEach { pt ->
            drawCircle(
                color = if (isEnabled) accentColor else Color.Gray,
                radius = 3.5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = 1.5.dp.toPx(),
                center = pt
            )
        }
    }
}

/**
 * 3D Metalik Donanım Vidası (Rack Screw).
 */
@Composable
private fun RackScrew() {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF6B7280), Color(0xFF1F2937))
                )
            )
            .border(1.dp, Color(0x55FFFFFF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(1.5.dp)
                .background(Color(0xFF111827))
        )
    }
}

/**
 * Canlı 3D Durum LED'i.
 */
@Composable
private fun StatusLed(isActive: Boolean, activeColor: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (isActive) activeColor else Color(0xFF374151))
            .shadow(if (isActive) 6.dp else 0.dp, CircleShape)
    )
}
