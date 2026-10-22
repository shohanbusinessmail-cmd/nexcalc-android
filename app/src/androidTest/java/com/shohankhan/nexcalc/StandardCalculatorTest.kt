package com.shohankhan.nexcalc

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** End-to-end Standard calculator flow on a device or emulator. */
@RunWith(AndroidJUnit4::class)
class StandardCalculatorTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun key(label: String) {
        compose.onNodeWithContentDescription(label).performClick()
    }

    @Test
    fun operatorPrecedenceIsEvaluatedOnEquals() {
        key("2")
        key("+")
        key("3")
        key("×")
        key("4")
        key("=")
        compose.onNodeWithText("14").assertIsDisplayed()
    }

    @Test
    fun parenthesesChangeTheResult() {
        key("(")
        key("2")
        key("+")
        key("3")
        key(")")
        key("×")
        key("4")
        key("=")
        compose.onNodeWithText("20").assertIsDisplayed()
    }

    @Test
    fun divisionResultIsExactForWholeNumbers() {
        key("1")
        key("0")
        key("÷")
        key("2")
        key("=")
        compose.onNodeWithText("5").assertIsDisplayed()
    }

    @Test
    fun errorIsShownForDivisionByZero() {
        key("5")
        key("÷")
        key("0")
        key("=")
        compose.onNodeWithText("Division by zero is undefined.").assertIsDisplayed()
    }

    @Test
    fun standardScreenIsTheStartDestination() {
        compose.onNodeWithTag("top_bar_title").assertIsDisplayed()
        compose.onNodeWithText("Standard").assertIsDisplayed()
    }
}
