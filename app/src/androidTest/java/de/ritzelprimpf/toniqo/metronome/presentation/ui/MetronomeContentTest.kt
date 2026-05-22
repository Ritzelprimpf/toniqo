package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import de.ritzelprimpf.toniqo.metronome.domain.model.tempoDescriptorFor
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.MetronomeUiState
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MetronomeContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launch(
        state: MetronomeUiState = MetronomeUiState(),
        onPlayToggled: () -> Unit = {},
        onBpmChanged: (Int) -> Unit = {},
        onBpmIncrement: () -> Unit = {},
        onBpmDecrement: () -> Unit = {},
        onTimeSignatureChanged: (Int, Int) -> Unit = { _, _ -> },
        onSubdivisionChanged: (Subdivision) -> Unit = {},
        onTapTempo: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ToniqoTheme {
                MetronomeContent(
                    state = state,
                    onPlayToggled = onPlayToggled,
                    onBpmChanged = onBpmChanged,
                    onBpmIncrement = onBpmIncrement,
                    onBpmDecrement = onBpmDecrement,
                    onTimeSignatureChanged = onTimeSignatureChanged,
                    onSubdivisionChanged = onSubdivisionChanged,
                    onTapTempo = onTapTempo,
                )
            }
        }
    }

    @Test
    fun `BPM value is rendered`() {
        launch(state = MetronomeUiState(config = MetronomeConfig(bpm = 137)))
        composeTestRule.onNodeWithText("137").assertIsDisplayed()
    }

    @Test
    fun `tempo descriptor Moderato shown for bpm 120`() {
        launch(state = MetronomeUiState(
            config = MetronomeConfig(bpm = 120),
            tempoDescriptor = tempoDescriptorFor(120),
        ))
        composeTestRule.onNodeWithText("MODERATO").assertIsDisplayed()
    }

    @Test
    fun `tempo descriptor Andante shown for bpm 90`() {
        launch(state = MetronomeUiState(
            config = MetronomeConfig(bpm = 90),
            tempoDescriptor = tempoDescriptorFor(90),
        ))
        composeTestRule.onNodeWithText("ANDANTE").assertIsDisplayed()
    }

    @Test
    fun `stopped state shows METRONOME STOPPED kicker`() {
        launch(state = MetronomeUiState(isPlaying = false))
        composeTestRule.onNodeWithText("METRONOME · STOPPED").assertIsDisplayed()
    }

    @Test
    fun `playing state shows METRONOME RUNNING kicker`() {
        launch(state = MetronomeUiState(isPlaying = true))
        composeTestRule.onNodeWithText("METRONOME · RUNNING").assertIsDisplayed()
    }

    @Test
    fun `beat indicator header shows 1-indexed current beat`() {
        launch(state = MetronomeUiState(
            currentBeat = 2,
            config = MetronomeConfig(timeSignatureNumerator = 4, timeSignatureDenominator = 4),
        ))
        composeTestRule.onNodeWithText("BEAT · 3 / 4").assertIsDisplayed()
    }

    @Test
    fun `4 over 4 time signature shows QUARTER NOTES label`() {
        launch(state = MetronomeUiState(
            config = MetronomeConfig(timeSignatureNumerator = 4, timeSignatureDenominator = 4),
        ))
        composeTestRule.onNodeWithText("QUARTER NOTES").assertIsDisplayed()
    }

    @Test
    fun `7 over 8 time signature shows EIGHTH NOTES label`() {
        launch(state = MetronomeUiState(
            config = MetronomeConfig(timeSignatureNumerator = 7, timeSignatureDenominator = 8),
        ))
        composeTestRule.onNodeWithText("EIGHTH NOTES").assertIsDisplayed()
    }

    @Test
    fun `beat indicator has correct segment count for 4 over 4`() {
        launch(state = MetronomeUiState(
            config = MetronomeConfig(timeSignatureNumerator = 4, timeSignatureDenominator = 4),
        ))
        composeTestRule.onAllNodesWithTag(BEAT_SEGMENT_TEST_TAG).assertCountEquals(4)
    }

    @Test
    fun `beat indicator has correct segment count for 7 over 8`() {
        launch(state = MetronomeUiState(
            config = MetronomeConfig(timeSignatureNumerator = 7, timeSignatureDenominator = 8),
        ))
        composeTestRule.onAllNodesWithTag(BEAT_SEGMENT_TEST_TAG).assertCountEquals(7)
    }

    @Test
    fun `stopped state shows Start button label`() {
        launch(state = MetronomeUiState(isPlaying = false))
        composeTestRule.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test
    fun `playing state shows Stop button label`() {
        launch(state = MetronomeUiState(isPlaying = true))
        composeTestRule.onNodeWithText("Stop").assertIsDisplayed()
    }

    @Test
    fun `tapping Start button invokes onPlayToggled`() {
        var toggled = false
        launch(state = MetronomeUiState(isPlaying = false), onPlayToggled = { toggled = true })
        composeTestRule.onNodeWithText("Start").performClick()
        assertTrue(toggled)
    }

    @Test
    fun `tapping decrement button invokes onBpmDecrement`() {
        var decremented = false
        launch(onBpmDecrement = { decremented = true })
        composeTestRule.onNode(hasContentDescription("Decrease BPM")).performClick()
        assertTrue(decremented)
    }

    @Test
    fun `tapping increment button invokes onBpmIncrement`() {
        var incremented = false
        launch(onBpmIncrement = { incremented = true })
        composeTestRule.onNode(hasContentDescription("Increase BPM")).performClick()
        assertTrue(incremented)
    }

    @Test
    fun `tapping TAP button invokes onTapTempo`() {
        var tapped = false
        launch(onTapTempo = { tapped = true })
        composeTestRule.onNodeWithText("TAP").performClick()
        assertTrue(tapped)
    }

    @Test
    fun `SIGNATURE kicker label is visible`() {
        launch()
        composeTestRule.onNodeWithText("SIGNATURE").assertIsDisplayed()
    }

    @Test
    fun `SUBDIVIDE kicker label is visible`() {
        launch()
        composeTestRule.onNodeWithText("SUBDIVIDE").assertIsDisplayed()
    }
}
