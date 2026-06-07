package de.ritzelprimpf.toniqo.chordfinder

import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.VoicingLookupResult
import de.ritzelprimpf.toniqo.chordfinder.fakes.FakeVoicingRepository
import de.ritzelprimpf.toniqo.chordfinder.fakes.stubVoicings
import de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel.ChordVoicingsViewModel
import de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel.VoicingTier
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.common.model.GuitarTuning
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import de.ritzelprimpf.toniqo.common.state.SelectedTuningStore
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

@OptIn(ExperimentalCoroutinesApi::class)
class ChordVoicingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private val amKey = ChordKey(rootPitchClass = 9, quality = ChordQuality.MINOR)
    private val amNoteNames = listOf("A", "C", "E")
    private val amName = "Am"

    // E♭ standard: each string one semitone below STANDARD_6
    private val ebStandard = GuitarTuning(
        id = "eb_standard",
        openNotes = listOf(
            Note(NoteName.DSharp, 2),
            Note(NoteName.GSharp, 2),
            Note(NoteName.CSharp, 3),
            Note(NoteName.FSharp, 3),
            Note(NoteName.ASharp, 3),
            Note(NoteName.DSharp, 4),
        ),
    )

    // Drop D: only 6th string different (not a uniform offset)
    private val dropD = GuitarTuning(
        id = "drop_d",
        openNotes = listOf(
            Note(NoteName.D, 2),   // tuned down 2 semitones
            Note(NoteName.A, 2),
            Note(NoteName.D, 3),
            Note(NoteName.G, 3),
            Note(NoteName.B, 3),
            Note(NoteName.E, 4),
        ),
    )

    private fun makeViewModel(
        repo: FakeVoicingRepository = FakeVoicingRepository(),
        store: SelectedTuningStore = SelectedTuningStore(),
    ) = ChordVoicingsViewModel(amKey, amName, amNoteNames, repo, store)

    // ── Standard tier ─────────────────────────────────────────────────────────────

    @Test
    fun `standard tuning yields tier STANDARD and passes through voicings`() = runTest {
        val standardVoicings = stubVoicings(3)
        val repo = FakeVoicingRepository().apply {
            on(GuitarTuning.STANDARD_6, VoicingLookupResult.Standard(standardVoicings))
        }
        val vm = makeViewModel(repo = repo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(VoicingTier.STANDARD, state.tier)
        assertEquals(standardVoicings, state.voicings)
        assertNull(state.offsetSemitones)
        assertFalse(state.isLoading)
    }

    @Test
    fun `standard tier exposes tuning label from the store`() = runTest {
        val store = SelectedTuningStore() // defaults to STANDARD_6 with label "E Standard"
        val repo = FakeVoicingRepository().apply {
            on(GuitarTuning.STANDARD_6, VoicingLookupResult.Standard(emptyList()))
        }
        val vm = makeViewModel(repo = repo, store = store)
        advanceUntilIdle()

        assertEquals("E Standard", vm.uiState.value.tuningLabel)
    }

    // ── Uniform offset tier ───────────────────────────────────────────────────────

    @Test
    fun `uniform offset tuning yields tier UNIFORM_OFFSET with negative offsetSemitones`() = runTest {
        val shiftedVoicings = stubVoicings(2)
        val repo = FakeVoicingRepository().apply {
            on(ebStandard, VoicingLookupResult.UniformOffset(shiftedVoicings, offsetSemitones = -1))
        }
        val store = SelectedTuningStore().apply { publish(ebStandard, "E♭ Standard") }
        val vm = makeViewModel(repo = repo, store = store)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(VoicingTier.UNIFORM_OFFSET, state.tier)
        assertEquals(shiftedVoicings, state.voicings)
        assertEquals(-1, state.offsetSemitones)
        assertEquals("E♭ Standard", state.tuningLabel)
        assertFalse(state.isLoading)
    }

    // ── Unsupported tier ──────────────────────────────────────────────────────────

    @Test
    fun `unsupported tuning yields tier UNSUPPORTED and shows standard voicings as fallback`() = runTest {
        val standardFallback = stubVoicings(4)
        val repo = FakeVoicingRepository().apply {
            on(dropD, VoicingLookupResult.Unsupported(dropD))
            on(GuitarTuning.STANDARD_6, VoicingLookupResult.Standard(standardFallback))
        }
        val store = SelectedTuningStore().apply { publish(dropD, "Drop D") }
        val vm = makeViewModel(repo = repo, store = store)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(VoicingTier.UNSUPPORTED, state.tier)
        assertEquals(standardFallback, state.voicings)
        assertNull(state.offsetSemitones)
        assertEquals("Drop D", state.tuningLabel)
        assertFalse(state.isLoading)
    }

    // ── Note names and root highlighting ─────────────────────────────────────────

    @Test
    fun `rootNoteName is the first entry of chordNoteNames`() = runTest {
        val vm = makeViewModel()
        // Initial state is set synchronously from constructor
        assertEquals("A", vm.uiState.value.rootNoteName)
    }

    @Test
    fun `noteNames contain all chord tones from the provided list`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        assertEquals(amNoteNames, vm.uiState.value.noteNames)
    }

    @Test
    fun `chordName is preserved from constructor arg`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        assertEquals(amName, vm.uiState.value.chordName)
    }

    // ── Loading state ─────────────────────────────────────────────────────────────

    @Test
    fun `isLoading is true before tuning store emits`() {
        val vm = makeViewModel()
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun `isLoading is false after lookup completes`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    // ── Tuning changes ────────────────────────────────────────────────────────────

    @Test
    fun `changing tuning in store triggers re-lookup and updates tier`() = runTest {
        val store = SelectedTuningStore() // starts at STANDARD_6
        val standardVoicings = stubVoicings(2)
        val shiftedVoicings = stubVoicings(1)
        val repo = FakeVoicingRepository().apply {
            on(GuitarTuning.STANDARD_6, VoicingLookupResult.Standard(standardVoicings))
            on(ebStandard, VoicingLookupResult.UniformOffset(shiftedVoicings, -1))
        }
        val vm = makeViewModel(repo = repo, store = store)
        advanceUntilIdle()

        assertEquals(VoicingTier.STANDARD, vm.uiState.value.tier)

        store.publish(ebStandard, "E♭ Standard")
        advanceUntilIdle()

        assertEquals(VoicingTier.UNIFORM_OFFSET, vm.uiState.value.tier)
        assertEquals(shiftedVoicings, vm.uiState.value.voicings)
    }
}
