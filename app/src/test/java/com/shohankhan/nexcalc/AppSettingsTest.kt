package com.shohankhan.nexcalc

import com.shohankhan.nexcalc.core.data.AccentColor
import com.shohankhan.nexcalc.core.data.AppSettings
import com.shohankhan.nexcalc.core.data.ThemeMode
import com.shohankhan.nexcalc.math.AngleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `default settings match the documented product defaults`() {
        val settings = AppSettings()
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(AccentColor.BLUE, settings.accent)
        assertEquals(AngleMode.DEGREES, settings.angleMode)
        assertEquals(30, settings.historyRetentionDays)
        assertEquals(10, settings.decimalPrecision)
        assertFalse(settings.showTrailingZeros)
    }

    @Test
    fun `number format maps display settings without affecting parsing`() {
        val settings = AppSettings(
            decimalPrecision = 4,
            significantDigits = 8,
            decimalSeparator = ',',
            groupingSeparator = '.',
            useGrouping = false,
            showTrailingZeros = true
        )
        val format = settings.numberFormat
        assertEquals(4, format.decimalPrecision)
        assertEquals(8, format.significantDigits)
        assertEquals(',', format.decimalSeparator)
        assertEquals('.', format.groupingSeparator)
        assertFalse(format.useGrouping)
        assertEquals(true, format.showTrailingZeros)
    }

    @Test
    fun `retention labels are readable`() {
        assertEquals("30 days", AppSettings(historyRetentionDays = 30).retentionLabel)
        assertEquals("1 day", AppSettings(historyRetentionDays = 1).retentionLabel)
        assertEquals("Never", AppSettings(historyRetentionDays = -1).retentionLabel)
    }

    @Test
    fun `unknown persisted keys fall back to defaults`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("nonsense"))
        assertEquals(AccentColor.BLUE, AccentColor.fromKey(null))
    }
}
