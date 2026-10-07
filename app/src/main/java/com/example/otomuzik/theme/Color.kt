package com.example.otomuzik.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ── Statik Sabitler (ColorScheme tanımları için) ─────────────────────────────
val BaseCarBgDark = Color(0xFF0C0E12)
val BaseCarSurfaceDark = Color(0xFF161920)
val BaseCarSurfaceVariant = Color(0xFF202530)
val BaseCarBorder = Color(0xFF2C3342)

val BaseCarCyan = Color(0xFF00E5FF)
val BaseCarCyanGlow = Color(0x3300E5FF)
val CarAmber = Color(0xFFFF9100)
val CarGreen = Color(0xFF00E676)

val BaseCarTextPrimary = Color(0xFFFFFFFF)
val BaseCarTextSecondary = Color(0xFFA5B0C2)
val BaseCarTextMuted = Color(0xFF6B7587)

// Titanium & Mocha (Grey/Brown)
val MochaBrown = Color(0xFF5D4037)
val MochaBrownLight = Color(0xFF8D6E63)
val TitaniumGrey = Color(0xFF37474F)
val TitaniumGreyDark = Color(0xFF263238)
val MochaText = Color(0xFFD7CCC8)
val MochaBgLight = Color(0xFFEFEBE9)
val MochaSurfaceLight = Color(0xFFD7CCC8)

// Sport Red (Red/Dark Grey)
val SportRed = Color(0xFFD50000)
val SportRedDark = Color(0xFF9B0000)
val SportGreyBg = Color(0xFF212121)
val SportGreySurface = Color(0xFF424242)
val SportBgLight = Color(0xFFF5F5F5)
val SportSurfaceLight = Color(0xFFE0E0E0)

// Deep Blue (Navy/Silver)
val DeepNavy = Color(0xFF1A237E)
val DeepNavyLight = Color(0xFF3F51B5)
val SilverText = Color(0xFFCFD8DC)
val DeepBlueBgDark = Color(0xFF0D1117)
val DeepBlueBgLight = Color(0xFFE3F2FD)
val DeepBlueSurfaceLight = Color(0xFFBBDEFB)

// Light Mode Defaults
val LightBg = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE9ECEF)
val LightTextPrimary = Color(0xFF212529)
val LightTextSecondary = Color(0xFF495057)
val LightBorder = Color(0xFFDEE2E6)

// ── AKTİF TEMAYI DİNAMİK OLARAK TÜM UYGULAMAYA YANSITAN COMPOSE ERİŞİMCİLERİ ─
// Bu erişimciler sayesinde MainScreen, NowPlayingPanel, LibraryPanel, QueuePanel
// ve tüm iletişim pencereleri ayarlardaki renk/tema moduna (Açık/Koyu, Neon/Mocha/Kırmızı/Mavi)
// anında tepki verir ve tüm uygulama seçilen temaya bürünür.
val CarBgDark: Color
    @ReadOnlyComposable
    @Composable
    get() = MaterialTheme.colorScheme.background

val CarSurfaceDark: Color
    @ReadOnlyComposable
    @Composable
    get() = MaterialTheme.colorScheme.surface

val CarSurfaceVariant: Color
    @ReadOnlyComposable
    @Composable
    get() = MaterialTheme.colorScheme.surfaceVariant

val CarBorder: Color
    @ReadOnlyComposable
    @Composable
    get() = MaterialTheme.colorScheme.outline

val CarCyan: Color
    @ReadOnlyComposable
    @Composable
    get() = MaterialTheme.colorScheme.primary

val CarTextPrimary: Color
    @ReadOnlyComposable
    @Composable
    get() = MaterialTheme.colorScheme.onSurface

val CarTextSecondary: Color
    @ReadOnlyComposable
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val CarTextMuted: Color
    @ReadOnlyComposable
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

val CarCyanGlow: Color
    @ReadOnlyComposable
    @Composable
    get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
