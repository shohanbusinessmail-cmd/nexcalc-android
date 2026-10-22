package com.shohankhan.nexcalc

import com.shohankhan.nexcalc.feature.tools.MatrixToolViewModel
import com.shohankhan.nexcalc.math.MathException
import com.shohankhan.nexcalc.math.NumberFormatOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Matrix text parsing and display formatting used by the Tools workspace. */
@RunWith(RobolectricTestRunner::class)
class MatrixParsingTest {

    @Test
    fun `parses comma and whitespace separated rows`() {
        val matrix = MatrixToolViewModel.parseMatrix("1, 2\n3 4", "A")
        assertEquals(2, matrix.rows)
        assertEquals(2, matrix.columns)
        assertEquals(4.0, matrix[1, 1], 0.0)
    }

    @Test
    fun `rejects ragged rows`() {
        assertThrows(MathException::class.java) {
            MatrixToolViewModel.parseMatrix("1 2\n3", "A")
        }
    }

    @Test
    fun `rejects non numeric entries`() {
        assertThrows(MathException::class.java) {
            MatrixToolViewModel.parseMatrix("1 two\n3 4", "A")
        }
    }

    @Test
    fun `parses a column vector`() {
        assertEquals(listOf(1.0, 2.0), MatrixToolViewModel.parseVector("1\n2"))
    }

    @Test
    fun `dimension readout reflects the parsed matrix`() {
        assertEquals("2 × 3", MatrixToolViewModel.dimensionsOf("1 2 3\n4 5 6"))
        assertEquals("—", MatrixToolViewModel.dimensionsOf("1 2\n3"))
    }

    @Test
    fun `formatting aligns columns`() {
        val matrix = MatrixToolViewModel.parseMatrix("1 20\n300 4", "A")
        val text = MatrixToolViewModel.formatMatrix(matrix, NumberFormatOptions(useGrouping = false))
        val lines = text.lines()
        assertEquals(2, lines.size)
        assertEquals(lines[0].length, lines[1].length)
    }
}
