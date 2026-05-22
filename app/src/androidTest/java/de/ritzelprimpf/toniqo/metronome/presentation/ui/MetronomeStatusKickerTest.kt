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
class MetronomeStatusKickerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `running state shows METRONOME RUNNING text`() {
        composeTestRule.setContent {
            ToniqoTheme { MetronomeStatusKicker(isPlaying = true) }
        }
        composeTestRule.onNodeWithText("METRONOME · RUNNING").assertIsDisplayed()
    }

    @Test
    fun `stopped state shows METRONOME STOPPED text`() {
        composeTestRule.setContent {
            ToniqoTheme { MetronomeStatusKicker(isPlaying = false) }
        }
        composeTestRule.onNodeWithText("METRONOME · STOPPED").assertIsDisplayed()
    }

    @Test
    fun `stopped state does not show running text`() {
        composeTestRule.setContent {
            ToniqoTheme { MetronomeStatusKicker(isPlaying = false) }
        }
        composeTestRule.onNodeWithText("METRONOME · RUNNING").assertDoesNotExist()
    }
}
