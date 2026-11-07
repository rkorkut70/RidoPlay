package com.example.otomuzik.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemePalette {
    NEON_CYAN, GREY_BROWN, SPORT_RED, DEEP_BLUE, WARM_AMBER, MINT_GREEN
}

// Neon Cyan (Default)
private val NeonCyanDarkColorScheme = darkColorScheme(
    primary = BaseCarCyan,
    onPrimary = Color(0xFF0C0E12),
    primaryContainer = Color(0xFF003542),
    onPrimaryContainer = BaseCarCyan,
    secondary = CarAmber,
    onSecondary = Color(0xFF0C0E12),
    tertiary = CarGreen,
    onTertiary = Color(0xFF0C0E12),
    background = BaseCarBgDark,
    onBackground = BaseCarTextPrimary,
    surface = BaseCarSurfaceDark,
    onSurface = BaseCarTextPrimary,
    surfaceVariant = BaseCarSurfaceVariant,
    onSurfaceVariant = BaseCarTextSecondary,
    outline = BaseCarBorder
)

private val NeonCyanLightColorScheme = lightColorScheme(
    primary = Color(0xFF00838F),
    onPrimary = LightSurface,
    primaryContainer = Color(0xFFB2EBF2),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = CarAmber,
    onSecondary = LightSurface,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

// Titanium & Mocha (Grey/Brown)
private val GreyBrownDarkColorScheme = darkColorScheme(
    primary = MochaBrownLight,
    onPrimary = TitaniumGreyDark,
    primaryContainer = MochaBrown,
    onPrimaryContainer = MochaText,
    secondary = TitaniumGrey,
    onSecondary = MochaText,
    background = TitaniumGreyDark,
    onBackground = MochaText,
    surface = TitaniumGrey,
    onSurface = MochaText,
    surfaceVariant = MochaBrown,
    onSurfaceVariant = MochaText,
    outline = MochaBrownLight
)

private val GreyBrownLightColorScheme = lightColorScheme(
    primary = MochaBrown,
    onPrimary = LightSurface,
    primaryContainer = MochaBrownLight,
    onPrimaryContainer = TitaniumGreyDark,
    secondary = TitaniumGrey,
    onSecondary = LightSurface,
    background = MochaBgLight,
    onBackground = TitaniumGreyDark,
    surface = MochaSurfaceLight,
    onSurface = TitaniumGreyDark,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TitaniumGrey,
    outline = MochaBrown
)

// Sport Red
private val SportRedDarkColorScheme = darkColorScheme(
    primary = SportRed,
    onPrimary = Color.White,
    primaryContainer = SportRedDark,
    onPrimaryContainer = Color.White,
    secondary = SportGreySurface,
    onSecondary = Color.White,
    background = SportGreyBg,
    onBackground = Color.White,
    surface = SportGreySurface,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF616161),
    onSurfaceVariant = Color.LightGray,
    outline = SportRed
)

private val SportRedLightColorScheme = lightColorScheme(
    primary = SportRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCDD2),
    onPrimaryContainer = SportRedDark,
    secondary = SportGreySurface,
    onSecondary = Color.White,
    background = SportBgLight,
    onBackground = SportGreyBg,
    surface = SportSurfaceLight,
    onSurface = SportGreyBg,
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = SportGreyBg,
    outline = SportRed
)

// Deep Blue
private val DeepBlueDarkColorScheme = darkColorScheme(
    primary = DeepNavyLight,
    onPrimary = DeepBlueBgDark,
    primaryContainer = DeepNavy,
    onPrimaryContainer = SilverText,
    secondary = SilverText,
    onSecondary = DeepNavy,
    background = DeepBlueBgDark,
    onBackground = SilverText,
    surface = Color(0xFF161B22),
    onSurface = SilverText,
    surfaceVariant = Color(0xFF21262D),
    onSurfaceVariant = Color.LightGray,
    outline = DeepNavyLight
)

private val DeepBlueLightColorScheme = lightColorScheme(
    primary = DeepNavy,
    onPrimary = Color.White,
    primaryContainer = DeepNavyLight,
    onPrimaryContainer = Color.White,
    secondary = DeepNavyLight,
    onSecondary = Color.White,
    background = DeepBlueBgLight,
    onBackground = DeepNavy,
    surface = DeepBlueSurfaceLight,
    onSurface = DeepNavy,
    surfaceVariant = Color(0xFF90CAF9),
    onSurfaceVariant = DeepNavy,
    outline = DeepNavy
)

// Warm Amber (Night Kehribar - Rido Launcher AccentAmber)
private val WarmAmberDarkColorScheme = darkColorScheme(
    primary = WarmAmber,
    onPrimary = Color(0xFF141004),
    primaryContainer = WarmAmberDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFFFD54F),
    onSecondary = Color(0xFF141004),
    background = AmberBgDark,
    onBackground = Color(0xFFFFF8E1),
    surface = AmberSurfaceDark,
    onSurface = Color(0xFFFFF8E1),
    surfaceVariant = Color(0xFF2B2516),
    onSurfaceVariant = Color(0xFFFFE082),
    outline = AmberBorder
)

private val WarmAmberLightColorScheme = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE082),
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = WarmAmber,
    onSecondary = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFFFB300)
)

// Mint Green (EV Electric - Rido Launcher AccentGreen)
private val MintGreenDarkColorScheme = darkColorScheme(
    primary = MintGreen,
    onPrimary = Color(0xFF041208),
    primaryContainer = MintGreenDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF69F0AE),
    onSecondary = Color(0xFF041208),
    background = MintBgDark,
    onBackground = Color(0xFFE8F5E9),
    surface = MintSurfaceDark,
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF1B3825),
    onSurfaceVariant = Color(0xFFA5D6A7),
    outline = MintBorder
)

private val MintGreenLightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = MintGreen,
    onSecondary = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFF4CAF50)
)

@Composable
fun OtoMuzikTheme(
    themeMode: String = "DARK",
    themePalette: String = "NEON_CYAN",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> true
    }

    val palette = try {
        ThemePalette.valueOf(themePalette)
    } catch (e: Exception) {
        ThemePalette.NEON_CYAN
    }

    val colorScheme = when (palette) {
        ThemePalette.NEON_CYAN -> if (darkTheme) NeonCyanDarkColorScheme else NeonCyanLightColorScheme
        ThemePalette.GREY_BROWN -> if (darkTheme) GreyBrownDarkColorScheme else GreyBrownLightColorScheme
        ThemePalette.SPORT_RED -> if (darkTheme) SportRedDarkColorScheme else SportRedLightColorScheme
        ThemePalette.DEEP_BLUE -> if (darkTheme) DeepBlueDarkColorScheme else DeepBlueLightColorScheme
        ThemePalette.WARM_AMBER -> if (darkTheme) WarmAmberDarkColorScheme else WarmAmberLightColorScheme
        ThemePalette.MINT_GREEN -> if (darkTheme) MintGreenDarkColorScheme else MintGreenLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
