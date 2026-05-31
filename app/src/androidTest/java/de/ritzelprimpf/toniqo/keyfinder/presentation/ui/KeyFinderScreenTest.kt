package de.ritzelprimpf.toniqo.keyfinder.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.ritzelprimpf.toniqo.common.model.ScaleType
import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleCandidate
import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleMatch
import de.ritzelprimpf.toniqo.keyfinder.fakes.FakeKeyFinderScreenViewModel
import de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel.KeyFinderUiState
import de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel.NoteChip
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyFinderScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launch(
        state: KeyFinderUiState = KeyFinderUiState(),
        testMicPermDenied: Boolean = false,
    ): FakeKeyFinderScreenViewModel {
        val fake = FakeKeyFinderScreenViewModel(state)
        composeTestRule.setContent {
            ToniqoTheme {
                KeyFinderScreen(
                    viewModel = fake,
                    testMicPermissionDenied = testMicPermDenied,
                )
            }
        }
        return fake
    }

    // ── Idle state ──────────────────────────────────────────────────────────

    @Test
    fun `idle prompt shows when fewer than 3 notes`() {
        launch(KeyFinderUiState(notes = listOf(NoteChip(0, "C", false)), results = emptyList()))
        composeTestRule.onNodeWithText("Add at least 3 notes to see matches").assertIsDisplayed()
    }

    @Test
    fun `idle prompt shows when note list is empty`() {
        launch(KeyFinderUiState())
        composeTestRule.onNodeWithText("Add at least 3 notes to see matches").assertIsDisplayed()
    }

    // ── Results ─────────────────────────────────────────────────────────────

    @Test
    fun `3 notes with results hides idle prompt and shows match count`() {
        val results = buildResults(withRoot = false, withFull = false)
        launch(
            KeyFinderUiState(
                notes = threeNoteChips(withRoot = false),
                results = results,
                matchCount = results.size,
            )
        )
        composeTestRule.onNodeWithText("Add at least 3 notes to see matches").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("${results.size} MATCHES").assertIsDisplayed()
    }

    @Test
    fun `first result has mint percent (top match)`() {
        val results = buildResults(withRoot = false, withFull = true)
        launch(
            KeyFinderUiState(
                notes = threeNoteChips(withRoot = false),
                results = results,
                matchCount = results.size,
            )
        )
        // The first result shows "100%" (the top match's percent)
        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
    }

    @Test
    fun `FULL badge shown on full match`() {
        val results = buildResults(withRoot = false, withFull = true)
        launch(
            KeyFinderUiState(
                notes = threeNoteChips(withRoot = false),
                results = results,
                matchCount = results.size,
            )
        )
        composeTestRule.onNodeWithText("FULL").assertIsDisplayed()
    }

    // ── Root / TONIC ────────────────────────────────────────────────────────

    @Test
    fun `TONIC badge shown and TONIC PREFERRED rendered when root is marked`() {
        val results = buildResults(withRoot = true, withFull = false)
        launch(
            KeyFinderUiState(
                notes = threeNoteChips(withRoot = true),
                rootPitchClass = 0,
                results = results,
                matchCount = results.size,
            )
        )
        composeTestRule.onNodeWithText("TONIC").assertIsDisplayed()
        composeTestRule.onNodeWithText("TONIC PREFERRED").assertIsDisplayed()
    }

    @Test
    fun `TONIC PREFERRED not shown when no root is marked`() {
        val results = buildResults(withRoot = false, withFull = false)
        launch(
            KeyFinderUiState(
                notes = threeNoteChips(withRoot = false),
                rootPitchClass = null,
                results = results,
                matchCount = results.size,
            )
        )
        composeTestRule.onNodeWithText("TONIC PREFERRED").assertIsNotDisplayed()
    }

    // ── Detail view ─────────────────────────────────────────────────────────

    @Test
    fun `tapping a result card opens the detail sheet with the correct notes`() {
        val results = buildResults(withRoot = false, withFull = true)
        launch(
            KeyFinderUiState(
                notes = threeNoteChips(withRoot = false),
                results = results,
                matchCount = results.size,
            )
        )
        // The first result has "C Major" label
        composeTestRule.onNodeWithText("C Major").performClick()
        // Detail sheet should show the NOTES section header
        composeTestRule.onNodeWithText("NOTES").assertIsDisplayed()
        // C Major scale notes: C D E F G A B
        composeTestRule.onNodeWithText("C").assertIsDisplayed()
        composeTestRule.onNodeWithText("E").assertIsDisplayed()
        composeTestRule.onNodeWithText("G").assertIsDisplayed()
    }

    // ── Permission denied ────────────────────────────────────────────────────

    @Test
    fun `mic permission denied card renders when testMicPermissionDenied is true`() {
        launch(testMicPermDenied = true)
        composeTestRule.onNodeWithText("Microphone access needed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grant access").assertIsDisplayed()
    }

    @Test
    fun `mic permission denied card does not render by default`() {
        launch()
        composeTestRule.onNodeWithText("Microphone access needed").assertIsNotDisplayed()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun threeNoteChips(withRoot: Boolean) = listOf(
        NoteChip(0, "C", isRoot = withRoot),
        NoteChip(4, "E", false),
        NoteChip(7, "G", false),
    )

    /** Builds a minimal result list suitable for verifying UI rendering. */
    private fun buildResults(withRoot: Boolean, withFull: Boolean) = listOf(
        ScaleMatch(
            candidate = ScaleCandidate(rootPitchClass = 0, type = ScaleType.IONIAN),
            percent = 100,
            isFull = withFull,
            isRootMatch = withRoot,
            rank = 1,
        ),
        ScaleMatch(
            candidate = ScaleCandidate(rootPitchClass = 9, type = ScaleType.AEOLIAN),
            percent = 88,
            isFull = false,
            isRootMatch = false,
            rank = 2,
        ),
    )
}
