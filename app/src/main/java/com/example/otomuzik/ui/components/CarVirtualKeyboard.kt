package com.example.otomuzik.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otomuzik.R
import com.example.otomuzik.theme.*

/**
 * Otomobil ekranları için özel tasarlanmış dahili sanal klavye paneli.
 * Android sistem klavyesini açmadan, ekranı yukarı kaydırmadan sol panelde çalışır.
 * Sağ paneldeki arama sonuçları kesintisiz tam boyutta görünür kalır.
 */
@Composable
fun CarSearchKeyboardPanel(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    resultCount: Int,
    onClose: () -> Unit,
    accentColor: Color,
    showBlurredBackground: Boolean,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    val panelBg = if (showBlurredBackground) {
        CarSurfaceDark.copy(alpha = 0.60f)
    } else {
        CarSurfaceDark
    }

    val panelShape = RoundedCornerShape(if (isCompact) 18.dp else 22.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = panelShape,
                ambientColor = Color.Black.copy(alpha = 0.70f),
                spotColor = Color.Black.copy(alpha = 0.85f)
            )
            .clip(panelShape)
            .background(panelBg)
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        accentColor.copy(alpha = 0.40f),
                        Color(0xFF1E2330),
                        Color.Black.copy(alpha = 0.85f)
                    )
                ),
                shape = panelShape
            )
            .padding(if (isCompact) 10.dp else 14.dp)
    ) {
        // Üst dışbükey cam ışıltısı katmanı (3D gloss efekti)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(panelShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.20f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── 1. ÜST PANEL: ARAMA METNİ GÖSTERGESİ & KAPAT ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 48.dp else 56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090B0E).copy(alpha = 0.85f))
                    .border(1.5.dp, accentColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "Ara",
                    tint = accentColor,
                    modifier = Modifier.size(if (isCompact) 22.dp else 26.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Yazılan metin ve yanıp sönen imleç
                val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                val cursorAlpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "cursorAlpha"
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Aramak için tuşlara dokunun...",
                            color = CarTextMuted,
                            fontSize = if (isCompact) 14.sp else 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = searchQuery,
                                color = CarTextPrimary,
                                fontSize = if (isCompact) 17.sp else 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .height(if (isCompact) 18.dp else 22.dp)
                                    .background(accentColor.copy(alpha = cursorAlpha))
                            )
                        }
                    }
                }

                // Bulunan Parça Rozeti
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.20f))
                            .border(0.5.dp, accentColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$resultCount parça",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Temizle Butonu
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FFFFFF))
                            .clickable { onSearchQueryChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CarTextPrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Paneli Kapat Butonu
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2C3240))
                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(8.dp))
                        .clickable { onClose() }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Kapat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 10.dp))

            // ── 2. ORTA BİLGİ & SIRADAKİ PARÇA İPUCU KARTI ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (searchQuery.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131720).copy(alpha = 0.55f))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🛡️", fontSize = 14.sp)
                            Text(
                                text = "Akıllı Sıra Korumalı Arama",
                                fontSize = if (isCompact) 12.sp else 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                        Text(
                            text = "Aramada bulduğunuz parçaya dokunduğunuzda mevcut çalma listeniz silinmez, sıradaki parça olarak eklenir.",
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            color = CarTextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF131720).copy(alpha = 0.65f))
                            .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Sonuç: ",
                                fontSize = if (isCompact) 11.5.sp else 13.sp,
                                color = CarTextMuted
                            )
                            Text(
                                text = "$resultCount parça",
                                fontSize = if (isCompact) 12.sp else 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (resultCount > 0) Color(0xFF00E5FF) else Color(0xFFFF5252)
                            )
                        }
                        Text(
                            text = "Dokunun ➔ Sıradaki Parça Olarak Ekler",
                            fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = CarTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 10.dp))

            // ── 3. DAHİLİ SANAL KLAVYE (Ergonomik ve kullanıcı dostu yükseklik) ──
            CarVirtualKeyboard(
                onKeyPress = { key ->
                    onSearchQueryChange(searchQuery + key)
                },
                onBackspace = {
                    if (searchQuery.isNotEmpty()) {
                        onSearchQueryChange(searchQuery.dropLast(1))
                    }
                },
                onClear = {
                    onSearchQueryChange("")
                },
                onClose = onClose,
                accentColor = accentColor,
                isCompact = isCompact,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Türkçe ve İngilizce destekli, ergonomik yükseklikli araç içi Q klavye.
 */
@Composable
fun CarVirtualKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    accentColor: Color,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    var isNumberMode by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Türkçe Q Klavye Düzeni
    val row1Letters = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "Ğ", "Ü")
    val row2Letters = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L", "Ş", "İ")
    val row3Letters = listOf("Z", "X", "C", "V", "B", "N", "M", "Ö", "Ç")

    // Rakam ve Sembol Düzeni
    val row1Numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "/")
    val row2Numbers = listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "=")
    val row3Numbers = listOf("?", ";", ":", "\"", "'", "<", ">", ",", ".")

    val verticalSpacing = if (isCompact) 4.dp else 6.dp
    val horizontalSpacing = if (isCompact) 3.dp else 4.dp
    val rowHeight = if (isCompact) 42.dp else 46.dp

    fun playClickSound() {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        // --- 1. SATIR (12 Tuş = 12.0f) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
        ) {
            val keys = if (isNumberMode) row1Numbers else row1Letters
            keys.forEach { key ->
                CarKeyButton(
                    text = key,
                    onClick = {
                        playClickSound()
                        onKeyPress(key)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 2. SATIR (11 Tuş, 0.5f kenar boşlukları ile toplam 12.0f, doğal klavye girintisi) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            val keys = if (isNumberMode) row2Numbers else row2Letters
            keys.forEach { key ->
                CarKeyButton(
                    text = key,
                    onClick = {
                        playClickSound()
                        onKeyPress(key)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.weight(0.5f))
        }

        // --- 3. SATIR (1.5f + 9 Tuş + 1.5f = 12.0f) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
        ) {
            // 123 / ABC Mod Değiştirici Tuşu
            CarSpecialKeyButton(
                text = if (isNumberMode) "ABC" else "123",
                bgColor = Color(0xFF262C3A),
                textColor = accentColor,
                onClick = {
                    playClickSound()
                    isNumberMode = !isNumberMode
                },
                modifier = Modifier.weight(1.5f)
            )

            val keys = if (isNumberMode) row3Numbers else row3Letters
            keys.forEach { key ->
                CarKeyButton(
                    text = key,
                    onClick = {
                        playClickSound()
                        onKeyPress(key)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Geri Silme (Backspace) Tuşu
            CarSpecialKeyButton(
                text = "⌫",
                bgColor = Color(0xFF382326),
                textColor = Color(0xFFFF5252),
                onClick = {
                    playClickSound()
                    onBackspace()
                },
                modifier = Modifier.weight(1.5f)
            )
        }

        // --- 4. SATIR (2.2f + 6.8f + 3.0f = 12.0f) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
        ) {
            // Temizle Butonu
            CarSpecialKeyButton(
                text = "Temizle",
                bgColor = Color(0xFF232834),
                textColor = Color(0xFFE2E8F0),
                onClick = {
                    playClickSound()
                    onClear()
                },
                modifier = Modifier.weight(2.2f)
            )

            // BÜYÜK BOŞLUK (SPACE) TUŞU
            CarSpecialKeyButton(
                text = "BOŞLUK",
                bgColor = Color(0xFF1B202C),
                textColor = Color(0xFFFFFFFF),
                borderColor = Color(0x44FFFFFF),
                onClick = {
                    playClickSound()
                    onKeyPress(" ")
                },
                modifier = Modifier.weight(6.8f)
            )

            // Kapat (Geri Dön) Butonu
            CarSpecialKeyButton(
                text = "Tamam",
                bgColor = accentColor.copy(alpha = 0.25f),
                textColor = accentColor,
                borderColor = accentColor,
                onClick = {
                    playClickSound()
                    onClose()
                },
                modifier = Modifier.weight(3.0f)
            )
        }
    }
}

/**
 * Standart Harf / Rakam Tuşu (Büyük, yüksek kontrastlı ve dokunmatik geri bildirimli)
 */
@Composable
private fun CarKeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980
    val keyShape = RoundedCornerShape(if (isCompact) 8.dp else 10.dp)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .shadow(
                elevation = 3.dp,
                shape = keyShape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.7f)
            )
            .clip(keyShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF282E3E), Color(0xFF141720))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color(0xFF282E3E),
                        Color.Black.copy(alpha = 0.6f)
                    )
                ),
                shape = keyShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color(0xFF00E5FF)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Üst ışıltı
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )
        Text(
            text = text,
            fontSize = if (isCompact) 16.sp else 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F5F9),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Özel Eylem Tuşu (Mod değiştirici, Boşluk, Silme, Temizle vb.)
 */
@Composable
private fun CarSpecialKeyButton(
    text: String,
    bgColor: Color,
    textColor: Color,
    borderColor: Color = Color(0x28FFFFFF),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980
    val keyShape = RoundedCornerShape(if (isCompact) 8.dp else 10.dp)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .shadow(
                elevation = 3.dp,
                shape = keyShape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.7f)
            )
            .clip(keyShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        bgColor.copy(alpha = (bgColor.alpha * 1.15f).coerceAtMost(1f)),
                        bgColor.copy(alpha = (bgColor.alpha * 0.85f).coerceAtLeast(0.1f))
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.40f),
                        borderColor,
                        Color.Black.copy(alpha = 0.5f)
                    )
                ),
                shape = keyShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = textColor),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Üst ışıltı
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.07f), Color.Transparent)
                    )
                )
        )
        Text(
            text = text,
            fontSize = if (isCompact) 12.sp else 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

