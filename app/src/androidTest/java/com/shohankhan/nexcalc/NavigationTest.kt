package com.shohankhan.nexcalc

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomBarOffersFourDestinations() {
        listOf(
            R.string.nav_standard,
            R.string.nav_scientific,
            R.string.nav_formulas,
            R.string.nav_more
        ).forEach { label ->
            compose.onNodeWithTag("bottom_$label").assertIsDisplayed()
        }
    }

    @Test
    fun scientificDestinationIsOneTapAway() {
        compose.onNodeWithTag("bottom_${R.string.nav_scientific}").performClick()
        compose.onNodeWithTag("top_bar_title").assertTextEquals("Scientific")
    }

    @Test
    fun formulasDestinationIsOneTapAway() {
        compose.onNodeWithTag("bottom_${R.string.nav_formulas}").performClick()
        compose.onNodeWithTag("top_bar_title").assertTextEquals("Formulas")
    }

    @Test
    fun quickSolveConverterAndToolsAreReachableFromMore() {
        compose.onNodeWithTag("bottom_${R.string.nav_more}").performClick()
        compose.onNodeWithText("Quick Solve").performClick()
        compose.onNodeWithTag("top_bar_title").assertTextEquals("Quick Solve")
        Espresso.pressBack()

        compose.onNodeWithText("Converters").performClick()
        compose.onNodeWithTag("top_bar_title").assertTextEquals("Converters")
        Espresso.pressBack()

        compose.onNodeWithText("Tools").performClick()
        compose.onNodeWithTag("top_bar_title").assertTextEquals("Tools")
    }

    @Test
    fun settingsAndAboutAreReachable() {
        compose.onNodeWithTag("bottom_${R.string.nav_more}").performClick()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("top_bar_title").assertTextEquals("Settings")
        Espresso.pressBack()
        compose.onNodeWithText("About").performClick()
        compose.onNodeWithTag("top_bar_title").assertTextEquals("About")
    }
}
