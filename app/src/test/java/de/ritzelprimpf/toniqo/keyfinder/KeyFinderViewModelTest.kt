package de.ritzelprimpf.toniqo.keyfinder

import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import de.ritzelprimpf.toniqo.common.model.ScaleType
import de.ritzelprimpf.toniqo.common.state.LatestKeyResultStore
import de.ritzelprimpf.toniqo.keyfinder.domain.usecase.MatchScalesUseCase
import de.ritzelprimpf.toniqo.keyfinder.fakes.FakeNoteDetector
import de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel.KeyFinderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [KeyFinderViewModel].
 *
 * Uses the **real** [MatchScalesUseCase] (pure, synchronous — makes percentage/rank
 * assertions concrete against the locked scoring formula) and a [FakeNoteDetector] whose
 * [FakeNoteDetector.detectedNotes] flow the test drives directly.
 *
 * [Dispatchers.Main] is replaced by a [StandardTestDispatcher] so [viewModelScope]-launched
 * coroutines (mic collection, detector stop) are controlled by [advanceUntilIdle].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KeyFinderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val useCase = MatchScalesUseCase()
    private val latestKeyResultStore = LatestKeyResultStore()
    private lateinit var fakeDetector: FakeNoteDetector
    private lateinit var viewModel: KeyFinderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDetector = FakeNoteDetector()
        viewModel = KeyFinderViewModel(useCase, fakeDetector, latestKeyResultStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun note(name: NoteName, octave: Int = 4) = Note(name, octave)

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial state is idle with no notes, no root, and no results`() {
        val state = viewModel.uiState.value

        assertTrue(state.notes.isEmpty())
        assertNull(state.rootPitchClass)
        assertFalse(state.isListening)
        assertTrue(state.results.isEmpty())
        assertEquals(0, state.matchCount)
    }

    // ─── addNoteFromPicker ────────────────────────────────────────────────────

    @Test
    fun `addNoteFromPicker adds a chip for the note's pitch class`() {
        viewModel.addNoteFromPicker(note(NoteName.E))

        val notes = viewModel.uiState.value.notes
        assertEquals(1, notes.size)
        assertEquals(NoteName.E.semitonesFromC, notes[0].pitchClass)
        assertEquals("E", notes[0].displayName)
        assertFalse(notes[0].isRoot)
    }

    @Test
    fun `results are empty with fewer than three distinct pitch classes`() {
        viewModel.addNoteFromPicker(note(NoteName.C))
        viewModel.addNoteFromPicker(note(NoteName.E))

        assertTrue(viewModel.uiState.value.results.isEmpty())
        assertEquals(0, viewModel.uiState.value.matchCount)
    }

    @Test
    fun `results are non-empty once three distinct pitch classes are present`() {
        viewModel.addNoteFromPicker(note(NoteName.C))
        viewModel.addNoteFromPicker(note(NoteName.E))
        viewModel.addNoteFromPicker(note(NoteName.G))

        assertFalse(viewModel.uiState.value.results.isEmpty())
    }

    @Test
    fun `chips appear in insertion order`() {
        viewModel.addNoteFromPicker(note(NoteName.G))
        viewModel.addNoteFromPicker(note(NoteName.C))
        viewModel.addNoteFromPicker(note(NoteName.E))

        val pcs = viewModel.uiState.value.notes.map { it.pitchClass }
        assertEquals(
            listOf(NoteName.G.semitonesFromC, NoteName.C.semitonesFromC, NoteName.E.semitonesFromC),
            pcs,
        )
    }

    // ─── Duplicate add is a no-op ─────────────────────────────────────────────

    @Test
    fun `adding same pitch class from different octaves is a no-op`() {
        viewModel.addNoteFromPicker(note(NoteName.E, octave = 2))
        viewModel.addNoteFromPicker(note(NoteName.E, octave = 4))

        assertEquals(1, viewModel.uiState.value.notes.size)
    }

    @Test
    fun `duplicate add does not change the display name of the existing chip`() {
        // Phase 7.4: addNoteFromPicker now uses ScaleSpeller.ROOT_DISPLAY_NAMES[pitchClass]
        // (canonical spelling), so CSharp (pitch class 1) displays as "D♭" not "C#".
        viewModel.addNoteFromPicker(note(NoteName.CSharp))  // first-seen; canonical name "D♭"
        viewModel.addNoteFromPicker(note(NoteName.CSharp))  // duplicate; no-op

        assertEquals("D♭", viewModel.uiState.value.notes.single().displayName)
    }

    // ─── removeNote ───────────────────────────────────────────────────────────

    @Test
    fun `removeNote removes the chip for the given pitch class`() {
        viewModel.addNoteFromPicker(note(NoteName.C))
        viewModel.addNoteFromPicker(note(NoteName.E))
        viewModel.removeNote(NoteName.E.semitonesFromC)

        val notes = viewModel.uiState.value.notes
        assertEquals(1, notes.size)
        assertEquals(NoteName.C.semitonesFromC, notes[0].pitchClass)
    }

    @Test
    fun `removing a note that is not present is a no-op`() {
        viewModel.addNoteFromPicker(note(NoteName.C))
        viewModel.removeNote(NoteName.G.semitonesFromC) // G not in list

        assertEquals(1, viewModel.uiState.value.notes.size)
    }

    @Test
    fun `removing the root note clears rootPitchClass`() {
        viewModel.addNoteFromPicker(note(NoteName.A))
        viewModel.addNoteFromPicker(note(NoteName.C))
        viewModel.addNoteFromPicker(note(NoteName.E))
        viewModel.toggleRoot(NoteName.A.semitonesFromC)
        assertEquals(NoteName.A.semitonesFromC, viewModel.uiState.value.rootPitchClass)

        viewModel.removeNote(NoteName.A.semitonesFromC)

        assertNull(viewModel.uiState.value.rootPitchClass)
    }

    @Test
    fun `removing a non-root note leaves rootPitchClass unchanged`() {
        viewModel.addNoteFromPicker(note(NoteName.A))
        viewModel.addNoteFromPicker(note(NoteName.C))
        viewModel.addNoteFromPicker(note(NoteName.E))
        viewModel.toggleRoot(NoteName.A.semitonesFromC)

        viewModel.removeNote(NoteName.C.semitonesFromC)

        assertEquals(NoteName.A.semitonesFromC, viewModel.uiState.value.rootPitchClass)
    }

    @Test
    fun `removing a note below the gate makes results empty again`() {
        viewModel.addNoteFromPicker(note(NoteName.C))
        viewModel.addNoteFromPicker(note(NoteName.E))
        viewModel.addNoteFromPicker(note(NoteName.G))
        assertFalse(viewModel.uiState.value.results.isEmpty())

        viewModel.removeNote(NoteName.G.semitonesFromC)

        assertTrue(viewModel.uiState.value.results.isEmpty())
    }

    // ─── toggleRoot ───────────────────────────────────────────────────────────

    @Test
    fun `toggleRoot marks the given note as root and sets isRoot on its chip`() {
        viewModel.addNoteFromPicker(note(NoteName.A))
        viewModel.toggleRoot(NoteName.A.semitonesFromC)

        val state = viewModel.uiState.value
        assertEquals(NoteName.A.semitonesFromC, state.rootPitchClass)
        assertTrue(state.notes.single().isRoot)
    }

    @Test
    fun `toggleRoot moves the root when a different note is chosen`() {
        viewModel.addNoteFromPicker(note(NoteName.A))
        viewModel.addNoteFromPicker(note(NoteName.C))
        viewModel.toggleRoot(NoteName.A.semitonesFromC)
        viewModel.toggleRoot(NoteName.C.semitonesFromC)

        val state = viewModel.uiState.value
        assertEquals(NoteName.C.semitonesFromC, state.rootPitchClass)
        val aChip = state.notes.first { it.pitchClass == NoteName.A.semitonesFromC }
        assertFalse(aChip.isRoot)
        val cChip = state.notes.first { it.pitchClass == NoteName.C.semitonesFromC }
        assertTrue(cChip.isRoot)
    }

    @Test
    fun `toggleRoot unsets the root when the same note is toggled twice`() {
        viewModel.addNoteFromPicker(note(NoteName.A))
        viewModel.toggleRoot(NoteName.A.semitonesFromC)
        viewModel.toggleRoot(NoteName.A.semitonesFromC) // toggle off

        val state = viewModel.uiState.value
        assertNull(state.rootPitchClass)
        assertFalse(state.notes.single().isRoot)
    }

    @Test
    fun `toggleRoot on a pitch class not in the list is a no-op`() {
        viewModel.addNoteFromPicker(note(NoteName.A))
        viewModel.toggleRoot(NoteName.C.semitonesFromC) // C not in list

        assertNull(viewModel.uiState.value.rootPitchClass)
    }

    @Test
    fun `toggleRoot changes ranking — marked root elevates matching scale to 100 percent`() {
        // Add C D E F G A B (all 7 pitch classes of C major / A natural minor etc.)
        listOf(NoteName.C, NoteName.D, NoteName.E, NoteName.F, NoteName.G, NoteName.A, NoteName.B)
            .forEach { viewModel.addNoteFromPicker(note(it)) }

        // Without root: all seven diatonic-same-set scales tie at 100%, Major sorts first
        val noRootTop = viewModel.uiState.value.results.first()
        assertEquals(ScaleType.IONIAN, noRootTop.candidate.type)
        assertEquals(100, noRootTop.percent)
        assertFalse(noRootTop.isRootMatch)

        // Mark root A: A Natural Minor should now be #1 at 100% with isRootMatch
        viewModel.toggleRoot(NoteName.A.semitonesFromC)

        val withRootTop = viewModel.uiState.value.results.first()
        assertEquals(ScaleType.AEOLIAN, withRootTop.candidate.type)
        assertEquals(NoteName.A.semitonesFromC, withRootTop.candidate.rootPitchClass)
        assertEquals(100, withRootTop.percent)
        assertTrue(withRootTop.isRootMatch)
    }

    // ─── clearAll ────────────────────────────────────────────────────────────

    @Test
    fun `clearAll empties notes, root, results, and matchCount`() {
        viewModel.addNoteFromPicker(note(NoteName.C))
        viewModel.addNoteFromPicker(note(NoteName.E))
        viewModel.addNoteFromPicker(note(NoteName.G))
        viewModel.toggleRoot(NoteName.C.semitonesFromC)

        viewModel.clearAll()

        val state = viewModel.uiState.value
        assertTrue(state.notes.isEmpty())
        assertNull(state.rootPitchClass)
        assertTrue(state.results.isEmpty())
        assertEquals(0, state.matchCount)
    }

    // ─── matchCount == results.size ───────────────────────────────────────────

    @Test
    fun `matchCount always equals results size`() {
        viewModel.addNoteFromPicker(note(NoteName.C))
        assertEquals(viewModel.uiState.value.results.size, viewModel.uiState.value.matchCount)

        viewModel.addNoteFromPicker(note(NoteName.E))
        assertEquals(viewModel.uiState.value.results.size, viewModel.uiState.value.matchCount)

        viewModel.addNoteFromPicker(note(NoteName.G))
        assertEquals(viewModel.uiState.value.results.size, viewModel.uiState.value.matchCount)

        viewModel.removeNote(NoteName.E.semitonesFromC)
        assertEquals(viewModel.uiState.value.results.size, viewModel.uiState.value.matchCount)
    }

    // ─── startListening / stopListening ──────────────────────────────────────

    @Test
    fun `startListening sets isListening and starts the detector`() = runTest {
        viewModel.startListening()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isListening)
        assertTrue(fakeDetector.isRunning)
    }

    @Test
    fun `startListening while already listening is a no-op`() = runTest {
        viewModel.startListening()
        advanceUntilIdle()
        viewModel.startListening() // second call; guard must prevent double-collection
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isListening)
    }

    @Test
    fun `mic emission while listening adds note to the chip list`() = runTest {
        viewModel.startListening()
        advanceUntilIdle()

        fakeDetector.emit(NoteName.E.semitonesFromC) // E = pitch class 4
        advanceUntilIdle()

        val notes = viewModel.uiState.value.notes
        assertEquals(1, notes.size)
        assertEquals(NoteName.E.semitonesFromC, notes[0].pitchClass)
        assertEquals("E", notes[0].displayName)
    }

    @Test
    fun `mic emission of an already-present pitch class is a no-op`() = runTest {
        viewModel.addNoteFromPicker(note(NoteName.E))
        viewModel.startListening()
        advanceUntilIdle()

        fakeDetector.emit(NoteName.E.semitonesFromC) // duplicate
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.notes.size)
    }

    @Test
    fun `multiple distinct mic emissions each add one chip`() = runTest {
        viewModel.startListening()
        advanceUntilIdle()

        fakeDetector.emit(NoteName.C.semitonesFromC)
        fakeDetector.emit(NoteName.E.semitonesFromC)
        fakeDetector.emit(NoteName.G.semitonesFromC)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.notes.size)
    }

    @Test
    fun `stopListening clears isListening and stops the detector`() = runTest {
        viewModel.startListening()
        advanceUntilIdle()

        viewModel.stopListening()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isListening)
        assertFalse(fakeDetector.isRunning)
    }

    @Test
    fun `emissions after stopListening are ignored`() = runTest {
        viewModel.startListening()
        advanceUntilIdle()
        viewModel.stopListening()
        advanceUntilIdle()

        fakeDetector.emit(NoteName.E.semitonesFromC)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notes.isEmpty())
    }

    @Test
    fun `stopListening when not listening is a no-op`() = runTest {
        viewModel.stopListening() // guard must not throw; detector was never started

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isListening)
        assertFalse(fakeDetector.isRunning)
    }

    // ─── End-to-end: full C major + root A ───────────────────────────────────

    /**
     * Canonical end-to-end assertion from `Phase7-PLAN.md` § Worked examples:
     *
     * Input: C D E F G A B (n=7), root A
     * - maxPoints = max(7,7) + 1 = 8
     * - A Natural Minor (Aeolian, root 9): covered=7, bonus=1, points=8, percent=100 → rank #1
     * - All six same-note siblings (e.g. C Major): covered=7, bonus=0, points=7, percent=88
     */
    @Test
    fun `full C major with root A ranks A Natural Minor first at 100 percent, siblings at 88 percent`() {
        listOf(NoteName.C, NoteName.D, NoteName.E, NoteName.F, NoteName.G, NoteName.A, NoteName.B)
            .forEach { viewModel.addNoteFromPicker(note(it)) }

        viewModel.toggleRoot(NoteName.A.semitonesFromC)

        val results = viewModel.uiState.value.results

        assertTrue(results.isNotEmpty())

        // #1 must be A Natural Minor at 100%
        val top = results.first()
        assertEquals(1, top.rank)
        assertEquals(NoteName.A.semitonesFromC, top.candidate.rootPitchClass)
        assertEquals(ScaleType.AEOLIAN, top.candidate.type)
        assertEquals(100, top.percent)
        assertTrue(top.isRootMatch)
        assertTrue(top.isFull)

        // The remaining results (all siblings) must each score 88%
        results.drop(1).forEach { match ->
            assertEquals(
                "Expected 88% for ${match.candidate.type} rooted at ${match.candidate.rootPitchClass}",
                88,
                match.percent,
            )
        }

        // matchCount must equal results.size
        assertEquals(results.size, viewModel.uiState.value.matchCount)
    }

    // ─── LatestKeyResultStore publish (parity test) ───────────────────────────

    @Test
    fun `recompute publishes the first result to LatestKeyResultStore`() {
        // Seeding with 3 notes triggers recompute; the top result should be published.
        viewModel.addNoteFromPicker(note(NoteName.C))
        viewModel.addNoteFromPicker(note(NoteName.E))
        viewModel.addNoteFromPicker(note(NoteName.G))

        val expectedTop = viewModel.uiState.value.results.firstOrNull()
        assertEquals(expectedTop, latestKeyResultStore.topResult.value)
    }

    @Test
    fun `clearing all notes publishes null to LatestKeyResultStore`() {
        viewModel.addNoteFromPicker(note(NoteName.C))
        viewModel.addNoteFromPicker(note(NoteName.E))
        viewModel.addNoteFromPicker(note(NoteName.G))

        viewModel.clearAll()

        assertNull(latestKeyResultStore.topResult.value)
    }

    @Test
    fun `store is updated on each recompute without affecting other Key Finder behaviour`() {
        viewModel.addNoteFromPicker(note(NoteName.A))
        viewModel.addNoteFromPicker(note(NoteName.C))
        viewModel.addNoteFromPicker(note(NoteName.E))

        val topAfterThree = latestKeyResultStore.topResult.value
        assertFalse("Store must hold a match after 3 notes", topAfterThree == null)

        viewModel.addNoteFromPicker(note(NoteName.G))

        val topAfterFour = latestKeyResultStore.topResult.value
        // Adding a note may change the top result, but the state count is still consistent
        assertEquals(viewModel.uiState.value.results.firstOrNull(), topAfterFour)
    }
}
