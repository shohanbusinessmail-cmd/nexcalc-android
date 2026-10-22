package com.shohankhan.nexcalc

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.shohankhan.nexcalc.feature.calculator.backspace
import com.shohankhan.nexcalc.feature.calculator.insertAtCursor
import com.shohankhan.nexcalc.feature.calculator.negate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Cursor-aware editing behaviour used by the Standard and Scientific calculators. */
@RunWith(RobolectricTestRunner::class)
class ExpressionEditingTest {

    @Test
    fun `inserts at the caret and keeps the caret after the token`() {
        val value = TextFieldValue("2+3", selection = TextRange(1))
        val updated = value.insertAtCursor("×")
        assertEquals("2×+3", updated.text)
        assertEquals(TextRange(2), updated.selection)
    }

    @Test
    fun `replaces a non-empty selection`() {
        val value = TextFieldValue("12+34", selection = TextRange(0, 2))
        val updated = value.insertAtCursor("7")
        assertEquals("7+34", updated.text)
        assertEquals(TextRange(1), updated.selection)
    }

    @Test
    fun `backspace deletes the character before the caret`() {
        val value = TextFieldValue("12+3", selection = TextRange(4))
        assertEquals("12+", value.backspace().text)
    }

    @Test
    fun `backspace deletes a selection without touching the caret`() {
        val value = TextFieldValue("12345", selection = TextRange(1, 4))
        assertEquals("15", value.backspace().text)
    }

    @Test
    fun `backspace at the start is a no-op`() {
        val value = TextFieldValue("abc", selection = TextRange(0))
        assertEquals("abc", value.backspace().text)
    }

    @Test
    fun `negate toggles a plain number`() {
        val positive = TextFieldValue("42")
        val negative = positive.negate()
        assertEquals("-42", negative.text)
        assertEquals("42", negative.negate().text)
    }

    @Test
    fun `negate wraps an expression in parentheses`() {
        val value = TextFieldValue("2+3*4")
        assertEquals("-(2+3*4)", value.negate().text)
    }

    @Test
    fun `negate inserts a minus when the draft is empty`() {
        val value = TextFieldValue("")
        assertEquals("-", value.negate().text)
    }
}
