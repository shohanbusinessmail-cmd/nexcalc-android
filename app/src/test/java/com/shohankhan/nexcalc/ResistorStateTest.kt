package com.shohankhan.nexcalc

import com.shohankhan.nexcalc.feature.tools.ResistorUiState
import com.shohankhan.nexcalc.math.ResistorCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResistorStateTest {

    @Test
    fun `four band default decodes to one kiloohm`() {
        val result = ResistorCalculator.decode(ResistorUiState.defaultBands(4))
        assertEquals("1 kΩ", result.formattedResistance)
        assertEquals(5.0, result.tolerancePercent!!, 0.0)
    }

    @Test
    fun `five band default decodes to one kiloohm with one percent tolerance`() {
        val result = ResistorCalculator.decode(ResistorUiState.defaultBands(5))
        assertEquals("1 kΩ", result.formattedResistance)
        assertEquals(1.0, result.tolerancePercent!!, 0.0)
    }

    @Test
    fun `six band default includes a temperature coefficient`() {
        val result = ResistorCalculator.decode(ResistorUiState.defaultBands(6))
        assertEquals(6, result.bandCount)
        assertTrue(result.temperatureCoefficientPpm != null)
    }

    @Test
    fun `band count is validated`() {
        assertEquals(listOf(4, 5, 6), ResistorCalculator.supportedBandCounts)
    }
}
