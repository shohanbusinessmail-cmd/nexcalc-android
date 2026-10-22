package com.shohankhan.nexcalc.core.data

import com.shohankhan.nexcalc.math.AngleMode
import com.shohankhan.nexcalc.math.NumberFormatOptions

/** Theme selection exposed in Settings. Values are stored by name for forward compatibility. */
enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

/** Accent palettes of the NEXCALC design system. */
enum class AccentColor(val storageKey: String) {
    BLUE("blue"),
    TEAL("teal"),
    VIOLET("violet");

    companion object {
        fun fromKey(key: String?): AccentColor = entries.firstOrNull { it.storageKey == key } ?: BLUE
    }
}

/**
 * User-visible preferences. Defaults are the documented product defaults:
 * 30-day history retention, degree angle mode, 10 decimal places, English number formatting.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.BLUE,
    val dynamicColor: Boolean = false,
    val decimalPrecision: Int = 10,
    val significantDigits: Int = 10,
    val useGrouping: Boolean = true,
    val decimalSeparator: Char = '.',
    val groupingSeparator: Char = ',',
    val showTrailingZeros: Boolean = false,
    val angleMode: AngleMode = AngleMode.DEGREES,
    val haptics: Boolean = true,
    val animations: Boolean = true,
    val historyRetentionDays: Int = 30
) {
    /** Bridges user settings to the engine's display-only formatting policy. */
    val numberFormat: NumberFormatOptions
        get() = NumberFormatOptions(
            decimalPrecision = decimalPrecision.coerceIn(0, 15),
            significantDigits = significantDigits.coerceIn(1, 15),
            decimalSeparator = decimalSeparator,
            groupingSeparator = groupingSeparator,
            useGrouping = useGrouping,
            showTrailingZeros = showTrailingZeros
        )

    val retentionLabel: String
        get() = if (historyRetentionDays < 0) "Never" else if (historyRetentionDays == 1) "1 day" else "$historyRetentionDays days"
}
