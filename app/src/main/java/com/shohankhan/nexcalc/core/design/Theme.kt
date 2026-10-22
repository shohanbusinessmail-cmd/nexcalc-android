package com.shohankhan.nexcalc.core.design

import android.animation.ValueAnimator
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.shohankhan.nexcalc.core.data.AccentColor
import com.shohankhan.nexcalc.core.data.AppSettings
import com.shohankhan.nexcalc.core.data.ThemeMode

/** Teal accent used by NEXCALC's own mark and success states. */
private val Teal = Color(0xFF0F766E)
private val TealLight = Color(0xFF5EEAD4)

/** Accent palettes: primary, container and on-container for light and dark surfaces. */
data class AccentPalette(
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color
)

private val BluePalette = AccentPalette(
    lightPrimary = Color(0xFF1D4ED8),
    lightOnPrimary = Color.White,
    lightContainer = Color(0xFFDBE4FF),
    lightOnContainer = Color(0xFF0B1E4B),
    darkPrimary = Color(0xFFA9C1FF),
    darkOnPrimary = Color(0xFF0A1A3A),
    darkContainer = Color(0xFF243B6B),
    darkOnContainer = Color(0xFFDBE4FF)
)

private val TealPalette = AccentPalette(
    lightPrimary = Color(0xFF0F766E),
    lightOnPrimary = Color.White,
    lightContainer = Color(0xFFC8F5EE),
    lightOnContainer = Color(0xFF00201C),
    darkPrimary = Color(0xFF5EEAD4),
    darkOnPrimary = Color(0xFF003731),
    darkContainer = Color(0xFF115E56),
    darkOnContainer = Color(0xFFC8F5EE)
)

private val VioletPalette = AccentPalette(
    lightPrimary = Color(0xFF6D28D9),
    lightOnPrimary = Color.White,
    lightContainer = Color(0xFFEADDFF),
    lightOnContainer = Color(0xFF25124B),
    darkPrimary = Color(0xFFD0BCFF),
    darkOnPrimary = Color(0xFF2C1858),
    darkContainer = Color(0xFF4A2D82),
    darkOnContainer = Color(0xFFEADDFF)
)

fun paletteFor(accent: AccentColor): AccentPalette = when (accent) {
    AccentColor.BLUE -> BluePalette
    AccentColor.TEAL -> TealPalette
    AccentColor.VIOLET -> VioletPalette
}

private fun lightScheme(accent: AccentPalette) = lightColorScheme(
    primary = accent.lightPrimary,
    onPrimary = accent.lightOnPrimary,
    primaryContainer = accent.lightContainer,
    onPrimaryContainer = accent.lightOnContainer,
    secondary = Color(0xFF4A5D7E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E3F6),
    onSecondaryContainer = Color(0xFF0F2036),
    tertiary = Teal,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8F5EE),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE6EBF3),
    onSurfaceVariant = Color(0xFF414A5C),
    outline = Color(0xFF6B7486),
    outlineVariant = Color(0xFFC6CDD9),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF2B3240),
    inverseOnSurface = Color(0xFFF1F4F9)
)

private fun darkScheme(accent: AccentPalette) = darkColorScheme(
    primary = accent.darkPrimary,
    onPrimary = accent.darkOnPrimary,
    primaryContainer = accent.darkContainer,
    onPrimaryContainer = accent.darkOnContainer,
    secondary = Color(0xFFB7C6E0),
    onSecondary = Color(0xFF22314A),
    secondaryContainer = Color(0xFF33445F),
    onSecondaryContainer = Color(0xFFD9E3F6),
    tertiary = TealLight,
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF115E56),
    onTertiaryContainer = Color(0xFFC8F5EE),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF111A2B),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF2A3446),
    onSurfaceVariant = Color(0xFFC1C9D6),
    outline = Color(0xFF8B94A5),
    outlineVariant = Color(0xFF3A4558),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    inverseSurface = Color(0xFFE6EAF2),
    inverseOnSurface = Color(0xFF2B3240)
)

@RequiresApi(Build.VERSION_CODES.S)
private fun dynamicScheme(dark: Boolean, context: android.content.Context): ColorScheme =
    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

private val NexTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 48.sp, lineHeight = 56.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 38.sp, lineHeight = 46.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp)
)

/** Monospaced style for expressions and results, keeping digits aligned. */
val MonospaceResult: TextStyle = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 38.sp)

/** Reduced-motion state: honours both the in-app switch and the platform animation scale. */
data class MotionPreference(val animationsEnabled: Boolean, val systemAnimationsEnabled: Boolean) {
    val allowMotion: Boolean get() = animationsEnabled && systemAnimationsEnabled
}

val LocalMotionPreference = staticCompositionLocalOf { MotionPreference(true, true) }

@Composable
fun NexCalcTheme(
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicScheme(dark, context)
        dark -> darkScheme(paletteFor(settings.accent))
        else -> lightScheme(paletteFor(settings.accent))
    }
    val systemAnimations = remember { ValueAnimator.areAnimatorsEnabled() }
    val motion = MotionPreference(settings.animations, systemAnimations)
    CompositionLocalProvider(LocalMotionPreference provides motion) {
        MaterialTheme(colorScheme = scheme, typography = NexTypography, content = content)
    }
}
