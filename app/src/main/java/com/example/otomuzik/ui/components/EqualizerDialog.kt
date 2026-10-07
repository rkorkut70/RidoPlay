package com.example.otomuzik.ui.components
import com.example.otomuzik.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.otomuzik.model.EqualizerState





import com.example.otomuzik.theme.CarTextMuted



@Composable
fun EqualizerDialog(
    eqState: EqualizerState,
    onToggleEnable: (Boolean) -> Unit,
    onSelectPreset: (String) -> Unit,
    onBandChange: (Short, Short) -> Unit,
    onBassBoostChange: (Short) -> Unit,
    onLoudnessEnhancerChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    // usePlatformDefaultWidth = false → dialog kendi genişliğini belirLER,
    // platform'un %80 kısıtlamasından kurtulur
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 360.dp, max = 660.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(22.dp))
                .background(CarSurfaceDark)
                .border(1.5.dp, CarCyan, RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()) // içerik taşarsa kaydr
            ) {

                // ── 1. BaŞLIK & aÇ/KaPaT & KaPaT ────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🎛️",
                            fontSize = 22.sp
                        )
                        Text(
                            text = "Ekolayzer & Bas ayarı",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CarTextPrimary
                        )
                        Switch(
                            checked = eqState.isEnabled,
                            onCheckedChange = onToggleEnable,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CarCyan,
                                checkedTrackColor = CarSurfaceVariant,
                                uncheckedThumbColor = CarTextMuted,
                                uncheckedTrackColor = CarBorder
                            )
                        )
                    }

                    // X kapat butonu
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CarSurfaceVariant)
                            .border(1.dp, CarBorder, CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = CarTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── 2. HaZIR SES PROFİLileri ──────────────────────────────────
                if (eqState.presets.isNotEmpty()) {
                    SectionLabel("SES PROFİLileri")
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(eqState.presets) { preset ->
                            val isSelected = preset.equals(eqState.currentPreset, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CarCyan else CarSurfaceVariant)
                                    .border(
                                        width = if (isSelected) 0.dp else 1.dp,
                                        color = if (isSelected) CarCyan else CarBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectPreset(preset) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF0C0E12) else CarTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // ── 3. FREKaNS BaNTLaRI ──────────────────────────────────────
                SectionLabel("FREKaNS aYaRLaRI")
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    eqState.bands.forEach { band ->
                        val min = band.minLevelMb.toFloat()
                        val max = band.maxLevelMb.toFloat()
                        val currentVal = band.levelMb.toFloat().coerceIn(min, max)
                        val dBValue = "%.1f dB".format(band.levelMb / 100.0)
                        val isPositive = band.levelMb > 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Frekans etiketi — sabit genişlik
                            Text(
                                text = band.frequencyLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CarTextSecondary,
                                modifier = Modifier.width(64.dp)
                            )

                            // Slider — kalan tm genişliği kullan
                            Slider(
                                value = currentVal,
                                onValueChange = { newVal ->
                                    onBandChange(band.index, newVal.toInt().toShort())
                                },
                                valueRange = min..max,
                                enabled = eqState.isEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (isPositive) CarCyan else CarTextSecondary,
                                    activeTrackColor = if (isPositive) CarCyan else CarTextMuted,
                                    inactiveTrackColor = CarBorder,
                                    disabledThumbColor = CarBorder,
                                    disabledActiveTrackColor = CarBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // dB değeri — sağ hizalı, sabit genişlik
                            Text(
                                text = dBValue,
                                fontSize = 13.sp,
                                color = if (isPositive) CarCyan else CarTextMuted,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(58.dp)
                            )
                        }
                    }
                }

                // ── 4. BaSS BOOST ─────────────────────────────────────────────
                if (eqState.isBassBoostSupported) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionLabel("🔊  DERİN BaS GÜÇLENDİRME")
                        Text(
                            text = "%${eqState.bassBoostStrength / 10}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CarAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = eqState.bassBoostStrength.toFloat(),
                        onValueChange = { onBassBoostChange(it.toInt().toShort()) },
                        valueRange = 0f..1000f,
                        enabled = eqState.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = CarAmber,
                            activeTrackColor = CarAmber,
                            inactiveTrackColor = CarBorder,
                            disabledThumbColor = CarBorder,
                            disabledActiveTrackColor = CarBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ── 5. OtoMaTİK SES SEVİYESİ EŞİTLEME (LOUDNESS ENHaNCER) ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionLabel("🎧 OtoMaTİK SES SEVİYESİ EŞİTLEME")
                    Switch(
                        checked = eqState.isLoudnessEnhancerEnabled,
                        onCheckedChange = onLoudnessEnhancerChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CarCyan,
                            checkedTrackColor = CarSurfaceVariant,
                            uncheckedThumbColor = CarTextMuted,
                            uncheckedTrackColor = CarBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── 6. KaPaT BUTONU ───────────────────────────────────────────
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CarCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "ayarları Kapat",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0C0E12)
                    )
                }
            }
        }
    }
}

/** Blm başlığı etiketi */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = CarCyan,
        letterSpacing = 1.5.sp
    )
}



















