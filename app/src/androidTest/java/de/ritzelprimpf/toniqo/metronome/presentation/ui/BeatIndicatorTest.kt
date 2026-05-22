package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BeatIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `numerator 4 produces 4 beat segments`() {
        composeTestRule.setContent {
            ToniqoTheme { BeatIndicator(numerator = 4, currentBeat = 0, isPlaying = false) }
        }
        composeTestRule.onAllNodesWithTag(BEAT_SEGMENT_TEST_TAG).assertCountEquals(4)
    }

    @Test
    fun `numerator 7 produces 7 beat segments`() {
        composeTestRule.setContent {
            ToniqoTheme { BeatIndicator(numerator = 7, currentBeat = 0, isPlaying = false) }
        }
        composeTestRule.onAllNodesWithTag(BEAT_SEGMENT_TEST_TAG).assertCountEquals(7)
    }

    @Test
    fun `numerator 12 produces 12 beat segments`() {
        composeTestRule.setContent {
            ToniqoTheme { BeatIndicator(numerator = 12, currentBeat = 0, isPlaying = false) }
        }
        composeTestRule.onAllNodesWithTag(BEAT_SEGMENT_TEST_TAG).assertCountEquals(12)
    }

    @Test
    fun `all segments show inactive state when stopped`() {
        composeTestRule.setContent {
            ToniqoTheme { BeatIndicator(numerator = 4, currentBeat = 0, isPlaying = false) }
        }
        composeTestRule.onAllNodes(
            hasTestTag(BEAT_SEGMENT_TEST_TAG).and(hasStateDescription("inactive")),
        ).assertCountEquals(4)
    }

    @Test
    fun `current beat segment is active when playing`() {
        composeTestRule.setContent {
            ToniqoTheme { BeatIndicator(numerator = 4, currentBeat = 2, isPlaying = true) }
        }
        composeTestRule.onAllNodes(
            hasTestTag(BEAT_SEGMENT_TEST_TAG).and(hasStateDescription("active")),
        ).assertCountEquals(1)
    }

    @Test
    fun `only 3 segments are inactive when playing at beat 2 of 4`() {
        composeTestRule.setContent {
            ToniqoTheme { BeatIndicator(numerator = 4, currentBeat = 2, isPlaying = true) }
        }
        composeTestRule.onAllNodes(
            hasTestTag(BEAT_SEGMENT_TEST_TAG).and(hasStateDescription("inactive")),
        ).assertCountEquals(3)
    }
}
