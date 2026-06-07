package de.ritzelprimpf.toniqo.chordfinder

import de.ritzelprimpf.toniqo.chordfinder.domain.repository.ChordFinderSelection
import de.ritzelprimpf.toniqo.chordfinder.domain.usecase.FindChordsUseCase
import de.ritzelprimpf.toniqo.chordfinder.fakes.FakeChordFinderSelectionRepository
import de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel.ChordFinderViewModel
import de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel.ChordNavEvent
import de.ritzelprimpf.toniqo.common.model.ScaleType
import de.ritzelprimpf.toniqo.common.state.LatestKeyResultStore
import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleCandidate
import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChordFinderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val findChords = FindChordsUseCase()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    // ── Factory helpers ───────────────────────────────────────────────────────────

    private fun makeViewModel(
        repo: FakeChordFinderSelectionRepository = FakeChordFinderSelectionRepository(),
        store: LatestKeyResultStore = LatestKeyResultStore(),
    ) = ChordFinderViewModel(findChords, repo, store)

    private fun scaleMatchFor(rootPc: Int, type: ScaleType) = ScaleMatch(
        candidate = ScaleCandidate(rootPc, type),
        percent = 100,
        isFull = true,
        isRootMatch = false,
        rank = 1,
    )

    // ── Seed algorithm ────────────────────────────────────────────────────────────

    @Test
    fun `no persisted selection and non-null store top seeds from Key Finder result`() = runTest {
        val store = LatestKeyResultStore().apply { publish(scaleMatchFor(2, ScaleType.DORIAN)) }
        val vm = makeViewModel(store = store)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2, state.rootPitchClass)
        assertEquals(ScaleType.DORIAN, state.scaleType)
        assertFalse(state.includeSeventhChords)
    }

    @Test
    fun `no persisted selection and empty store seeds to A Aeolian fallback`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ChordFinderSelection.DEFAULT_ROOT_PITCH_CLASS, state.rootPitchClass)
        assertEquals(ChordFinderSelection.DEFAULT_SCALE_TYPE, state.scaleType)
        assertFalse(state.includeSeventhChords)
    }

    @Test
    fun `hasUserSelection true restores persisted root and type ignoring Key Finder store`() = runTest {
        val persisted = ChordFinderSelection(
            rootPitchClass = 7,
            scaleType = ScaleType.LYDIAN,
            includeSeventhChords = false,
            hasUserSelection = true,
        )
        val store = LatestKeyResultStore().apply { publish(scaleMatchFor(2, ScaleType.DORIAN)) }
        val vm = makeViewModel(
            repo = FakeChordFinderSelectionRepository(persisted),
            store = store,
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(7, state.rootPitchClass)
        assertEquals(ScaleType.LYDIAN, state.scaleType)
    }

    @Test
    fun `seed reads store once — later publish on store does not change state`() = runTest {
        val store = LatestKeyResultStore()
        val vm = makeViewModel(store = store)
        advanceUntilIdle()

        // State is A Aeolian (fallback, store was null at init)
        assertEquals(ChordFinderSelection.DEFAULT_ROOT_PITCH_CLASS, vm.uiState.value.rootPitchClass)

        // Now publish to the store — ViewModel already initialised and doesn't collect from it
        store.publish(scaleMatchFor(0, ScaleType.IONIAN))

        assertEquals(
            "Store publish after init must not change seeded state",
            ChordFinderSelection.DEFAULT_ROOT_PITCH_CLASS,
            vm.uiState.value.rootPitchClass,
        )
    }

    // ── isInitialLoadComplete ────────────────────────────────────────────────────

    @Test
    fun `isInitialLoadComplete is false before init coroutine runs`() {
        val vm = makeViewModel()
        assertFalse(vm.uiState.value.isInitialLoadComplete)
    }

    @Test
    fun `isInitialLoadComplete is true after init coroutine completes`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isInitialLoadComplete)
    }

    // ── User intents ──────────────────────────────────────────────────────────────

    @Test
    fun `setRoot updates rootPitchClass and spelledRoot and persists as user-owned`() = runTest {
        val repo = FakeChordFinderSelectionRepository()
        val vm = makeViewModel(repo = repo)
        advanceUntilIdle()

        vm.setRoot(0) // C
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.rootPitchClass)
        assertEquals("C", vm.uiState.value.spelledRoot)
        assertTrue(repo.latestSaved.hasUserSelection)
        assertEquals(0, repo.latestSaved.rootPitchClass)
    }

    @Test
    fun `setScaleType updates scaleType and recomputes 7 chords`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.setScaleType(ScaleType.IONIAN)
        advanceUntilIdle()

        assertEquals(ScaleType.IONIAN, vm.uiState.value.scaleType)
        assertEquals(7, vm.uiState.value.chords.size)
    }

    @Test
    fun `toggleSevenths flips includeSeventhChords and rebuilds chords`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.includeSeventhChords)
        val triadSymbols = vm.uiState.value.chords.map { it.symbol }

        vm.toggleSevenths()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.includeSeventhChords)
        val seventhSymbols = vm.uiState.value.chords.map { it.symbol }
        // Symbols should differ because seventh chord symbols include quality suffixes
        assertFalse("Seventh chord symbols must differ from triad symbols", triadSymbols == seventhSymbols)
    }

    @Test
    fun `spelledRoot reflects ScaleSpeller output for the seeded root`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        // Fallback seeds A Aeolian; ScaleSpeller.rootName(9, AEOLIAN) == "A"
        assertEquals("A", vm.uiState.value.spelledRoot)
    }

    @Test
    fun `spelledRoot uses canonical flat spelling for D-flat`() = runTest {
        val repo = FakeChordFinderSelectionRepository(
            ChordFinderSelection(
                rootPitchClass = 1, // D♭
                scaleType = ScaleType.IONIAN,
                includeSeventhChords = false,
                hasUserSelection = true,
            ),
        )
        val vm = makeViewModel(repo = repo)
        advanceUntilIdle()

        assertEquals("D♭", vm.uiState.value.spelledRoot)
    }

    // ── selectChord emits nav event ───────────────────────────────────────────────

    @Test
    fun `selectChord emits NavigateToVoicings with correct chordKey and name`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        val events = mutableListOf<ChordNavEvent>()
        val collectJob = launch { vm.navEvents.collect { events.add(it) } }
        advanceUntilIdle()

        val firstChord = vm.uiState.value.chords.first()
        vm.selectChord(firstChord)
        advanceUntilIdle()

        assertEquals(1, events.size)
        val nav = events.single() as ChordNavEvent.NavigateToVoicings
        assertEquals(firstChord.symbol, nav.chordName)
        assertEquals(firstChord.noteNames, nav.noteNames)
        collectJob.cancel()
    }

    @Test
    fun `selectChord computes chordKey root from interval arithmetic`() = runTest {
        // A Aeolian degree 3 = C (rootPc 9 + interval 3 = 12 % 12 = 0)
        val vm = makeViewModel()
        advanceUntilIdle()

        val events = mutableListOf<ChordNavEvent>()
        val collectJob = launch { vm.navEvents.collect { events.add(it) } }
        advanceUntilIdle()

        val thirdDegree = vm.uiState.value.chords[2] // degree III in A Aeolian = C major
        vm.selectChord(thirdDegree)
        advanceUntilIdle()

        val nav = events.single() as ChordNavEvent.NavigateToVoicings
        assertEquals(0, nav.chordKey.rootPitchClass) // C = pitch class 0
        collectJob.cancel()
    }
}
