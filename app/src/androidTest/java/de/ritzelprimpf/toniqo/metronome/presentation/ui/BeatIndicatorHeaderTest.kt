package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BeatIndicatorHeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `beat counter is 1-indexed — currentBeat 0 shows Beat 1`() {
        composeTestRule.setContent {
            ToniqoTheme { BeatIndicatorHeader(currentBeat = 0, numerator = 4, denominator = 4) }
        }
        composeTestRule.onNodeWithText("BEAT · 1 / 4").assertIsDisplayed()
    }

    @Test
    fun `beat counter updates — currentBeat 3 shows Beat 4`() {
        composeTestRule.setContent {
            ToniqoTheme { BeatIndicatorHeader(currentBeat = 3, numerator = 4, denominator = 4) }
        }
        composeTestRule.onNodeWithText("BEAT · 4 / 4").assertIsDisplayed()
    }

    @Test
    fun `numerator 7 appears in the beat label`() {
        composeTestRule.setContent {
            ToniqoTheme { BeatIndicatorHeader(currentBeat = 0, numerator = 7, denominator = 8) }
        }
        composeTestRule.onNodeWithText("BEAT · 1 / 7").assertIsDisplayed()
    }

    @Test
    fun `denominator 4 shows QUARTER NOTES`() {
        composeTestRule.setContent {
            ToniqoTheme { BeatIndicatorHeader(currentBeat = 0, numerator = 4, denominator = 4) }
        }
        composeTestRule.onNodeWithText("QUARTER NOTES").assertIsDisplayed()
    }

    @Test
    fun `denominator 8 shows EIGHTH NOTES`() {
        composeTestRule.setContent {
            ToniqoTheme { BeatIndicatorHeader(currentBeat = 0, numerator = 7, denominator = 8) }
        }
        composeTestRule.onNodeWithText("EIGHTH NOTES").assertIsDisplayed()
    }
}
