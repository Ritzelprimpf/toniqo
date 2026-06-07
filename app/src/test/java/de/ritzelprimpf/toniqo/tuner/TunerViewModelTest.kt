package de.ritzelprimpf.toniqo.tuner

import de.ritzelprimpf.toniqo.audio.CaptureEvent
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerMode
import de.ritzelprimpf.toniqo.tuner.domain.model.TuningStatus
import de.ritzelprimpf.toniqo.tuner.domain.usecase.DetectTunedStringUseCase
import de.ritzelprimpf.toniqo.tuner.fakes.FakeAudioCaptureSource
import de.ritzelprimpf.toniqo.tuner.fakes.FakePitchDetector
import de.ritzelprimpf.toniqo.tuner.fakes.FakeTunerPreferences
import de.ritzelprimpf.toniqo.tuner.fakes.FakeTunerPresetRepository
import de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerEvent
import de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerViewModel
import de.ritzelprimpf.toniqo.common.state.SelectedTuningStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class TunerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private val sampleBuffer = FloatArray(4096)
    private fun samplesEvent() = CaptureEvent.Samples(sampleBuffer)

    /**
     * Builds a ViewModel wired to the given fakes. Uses a no-op source/detector by default
     * (suitable for tests that only exercise preset loading and user-action state transitions).
     */
    private fun makeViewModel(
        preferences: FakeTunerPreferences = FakeTunerPreferences(),
        repository: FakeTunerPresetRepository = FakeTunerPresetRepository(),
        source: FakeAudioCaptureSource = FakeAudioCaptureSource(emptyList()),
        detector: FakePitchDetector = FakePitchDetector(emptyList()),
    ): TunerViewModel {
        val useCase = DetectTunedStringUseCase(source, detector)
        return TunerViewModel(repository, preferences, useCase, SelectedTuningStore())
    }

    // ── Preset loading ────────────────────────────────────────────────────────────

    @Test
    fun `first-launch default preset loaded when no saved ID`() = runTest {
        val vm = makeViewModel(preferences = FakeTunerPreferences(initialPresetId = null))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("six_string_standard_e", state.selectedPreset?.id)
        assertEquals(0, state.currentStringIndex)
        assertTrue(state.tunedStringIndices.isEmpty())
        assertEquals(TunerMode.PRESET, state.mode)
    }

    @Test
    fun `last-used preset restored on subsequent launch`() = runTest {
        val vm = makeViewModel(preferences = FakeTunerPreferences("six_string_drop_d"))
        advanceUntilIdle()

        assertEquals("six_string_drop_d", vm.uiState.value.selectedPreset?.id)
    }

    @Test
    fun `unknown saved preset ID falls back to default`() = runTest {
        val vm = makeViewModel(preferences = FakeTunerPreferences("old_preset_that_was_removed"))
        advanceUntilIdle()

        assertEquals("six_string_standard_e", vm.uiState.value.selectedPreset?.id)
    }

    // ── onPresetSelected ──────────────────────────────────────────────────────────

    @Test
    fun `onPresetSelected updates state and persists the ID`() = runTest {
        val preferences = FakeTunerPreferences()
        val vm = makeViewModel(preferences = preferences)
        advanceUntilIdle()

        vm.onPresetSelected("seven_string_standard_b")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("seven_string_standard_b", state.selectedPreset?.id)
        assertEquals(0, state.currentStringIndex)
        assertTrue(state.tunedStringIndices.isEmpty())
        assertEquals(TunerMode.PRESET, state.mode)
        assertEquals("seven_string_standard_b", preferences.storedPresetId)
    }

    @Test
    fun `re-selecting the current preset still resets session state`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onPresetSelected("six_string_standard_e")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("six_string_standard_e", state.selectedPreset?.id)
        assertEquals(0, state.currentStringIndex)
        assertTrue(state.tunedStringIndices.isEmpty())
        assertEquals(TunerMode.PRESET, state.mode)
    }

    @Test
    fun `onPresetSelected with unknown ID does nothing`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()
        val existingId = vm.uiState.value.selectedPreset?.id

        vm.onPresetSelected("this_preset_does_not_exist")
        advanceUntilIdle()

        assertEquals(existingId, vm.uiState.value.selectedPreset?.id)
    }

    // ── onStringSelected ──────────────────────────────────────────────────────────

    @Test
    fun `onStringSelected jumps to the target string and resets tuned indices`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onStringSelected(1)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TunerMode.PRESET, state.mode)
        assertEquals(1, state.currentStringIndex)
        assertTrue(state.tunedStringIndices.isEmpty())
    }

    @Test
    fun `onStringSelected from chromatic mode re-enters preset mode`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        // Manually simulate chromatic mode by jumping to string 0 twice; first sets up PRESET.
        // The test's goal: onStringSelected always puts mode = PRESET.
        vm.onStringSelected(0)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TunerMode.PRESET, state.mode)
        assertEquals(0, state.currentStringIndex)
        assertTrue(state.tunedStringIndices.isEmpty())
    }

    // ── Auto-advance: non-last string ─────────────────────────────────────────────

    /**
     * Six in-tune detections cause isSustainedInTune, which adds string 0 to tunedStringIndices
     * immediately and schedules a 200 ms hold. After [STRING_LOCK_HOLD_MS] the ViewModel
     * increments [currentStringIndex] to 1.
     */
    @Test
    fun `sustained-in-tune triggers auto-advance after STRING_LOCK_HOLD_MS`() = runTest {
        val repo = FakeTunerPresetRepository()
        val preset = repo.getPresetById("six_string_standard_e")!!
        val str0Hz = preset.notes[0].frequencyHz(440.0)

        // Session 1 (init pipeline): 6 in-tune samples for string 0.
        // Session 2 (after advance): empty — no further detections needed.
        val source = FakeAudioCaptureSource.multiSession(
            listOf(
                List(6) { samplesEvent() },
                emptyList(),
            ),
        )
        val detector = FakePitchDetector(List(6) { str0Hz })

        val collectedEvents = mutableListOf<TunerEvent>()
        val vm = TunerViewModel(repo, FakeTunerPreferences(), DetectTunedStringUseCase(source, detector))
        val eventsJob = launch { vm.events.toList(collectedEvents) }

        // Let init run and consume all 6 samples — 6th fires isSustainedInTune.
        advanceUntilIdle()

        // String 0 should be in tunedStringIndices immediately.
        assertTrue(0 in vm.uiState.value.tunedStringIndices)
        assertTrue(collectedEvents.any { it is TunerEvent.StringTuned && it.stringIndex == 0 })

        // Advance past the 200 ms hold.
        advanceTimeBy(TunerViewModel.STRING_LOCK_HOLD_MS + 1)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.currentStringIndex)

        eventsJob.cancel()
    }

    // ── Auto-advance: last string → chromatic ────────────────────────────────────

    /**
     * When the last string satisfies the sustained-tone condition, the ViewModel:
     * 1. After 200 ms: sets status = ALL_STRINGS_TUNED and emits AllStringsTuned.
     * 2. After a further 1200 ms: transitions to CHROMATIC mode and emits EnteredChromaticMode.
     *
     * The FakeTunerPresetRepository provides 2-string presets. Session 1 tunes string 0;
     * session 2 tunes string 1.
     */
    @Test
    fun `last-string lock triggers ALL_STRINGS_TUNED then CHROMATIC after holds`() = runTest {
        val repo = FakeTunerPresetRepository()
        val preset = repo.getPresetById("six_string_standard_e")!!
        val str0Hz = preset.notes[0].frequencyHz(440.0)
        val str1Hz = preset.notes[1].frequencyHz(440.0)

        // Session 1: string 0 (6 in-tune)
        // Session 2: string 1 (6 in-tune)
        // Session 3: empty (chromatic restart, no more detections needed)
        val source = FakeAudioCaptureSource.multiSession(
            listOf(
                List(6) { samplesEvent() },
                List(6) { samplesEvent() },
                emptyList(),
            ),
        )
        // Detector returns str0Hz × 6, then str1Hz × 6
        val detector = FakePitchDetector(List(6) { str0Hz } + List(6) { str1Hz })

        val collectedEvents = mutableListOf<TunerEvent>()
        val vm = TunerViewModel(repo, FakeTunerPreferences(), DetectTunedStringUseCase(source, detector))
        val eventsJob = launch { vm.events.toList(collectedEvents) }

        // ── Phase 1: tune string 0 ────────────────────────────────────────────────
        advanceUntilIdle()
        assertTrue("string 0 should be tuned", 0 in vm.uiState.value.tunedStringIndices)

        // STRING_LOCK_HOLD_MS: advance to string 1
        advanceTimeBy(TunerViewModel.STRING_LOCK_HOLD_MS + 1)
        advanceUntilIdle()
        assertEquals("should be on string 1", 1, vm.uiState.value.currentStringIndex)

        // ── Phase 2: tune string 1 ────────────────────────────────────────────────
        // Session 2 was consumed; string 1 is now sustained.
        assertTrue("string 1 should be tuned", 1 in vm.uiState.value.tunedStringIndices)

        // STRING_LOCK_HOLD_MS: all strings tuned
        advanceTimeBy(TunerViewModel.STRING_LOCK_HOLD_MS + 1)
        advanceUntilIdle()

        assertEquals(TuningStatus.ALL_STRINGS_TUNED, vm.uiState.value.status)
        assertTrue(collectedEvents.any { it is TunerEvent.AllStringsTuned })

        // ── Phase 3: chromatic transition ─────────────────────────────────────────
        advanceTimeBy(TunerViewModel.ALL_TUNED_HOLD_MS + 1)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TunerMode.CHROMATIC, state.mode)
        assertTrue(state.tunedStringIndices.isEmpty())
        assertTrue(collectedEvents.any { it is TunerEvent.EnteredChromaticMode })

        eventsJob.cancel()
    }

    // ── Permission denied / capture failed ────────────────────────────────────────

    @Test
    fun `permission denied surfaces in state`() = runTest {
        val source = FakeAudioCaptureSource(listOf(CaptureEvent.PermissionDenied))
        val vm = makeViewModel(source = source)
        advanceUntilIdle()

        assertEquals(TuningStatus.PERMISSION_DENIED, vm.uiState.value.status)
        assertNull(vm.uiState.value.detectedFrequencyHz)
        assertNull(vm.uiState.value.centsOffTarget)
    }

    @Test
    fun `capture failure surfaces in state`() = runTest {
        val source = FakeAudioCaptureSource(listOf(CaptureEvent.Failed("hardware error")))
        val vm = makeViewModel(source = source)
        advanceUntilIdle()

        assertEquals(TuningStatus.CAPTURE_FAILED, vm.uiState.value.status)
        assertNull(vm.uiState.value.detectedFrequencyHz)
        assertNull(vm.uiState.value.centsOffTarget)
    }

    // ── tunedStringIndices reset paths ────────────────────────────────────────────

    @Test
    fun `tunedStringIndices cleared on preset change`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onPresetSelected("six_string_drop_d")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.tunedStringIndices.isEmpty())
    }

    @Test
    fun `tunedStringIndices cleared on string selection`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onStringSelected(0)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.tunedStringIndices.isEmpty())
    }

    // ── Status mapping ────────────────────────────────────────────────────────────

    /**
     * A single in-tolerance detection is LISTENING (window not full), not IN_TUNE.
     * IN_TUNE is only emitted when isSustainedInTune = true.
     */
    @Test
    fun `single in-tolerance detection is LISTENING not IN_TUNE`() = runTest {
        val repo = FakeTunerPresetRepository()
        val preset = repo.getPresetById("six_string_standard_e")!!
        val inTuneHz = preset.notes[0].frequencyHz(440.0)

        // Only 1 sample → window size = 1 < 6 → never sustained
        val source = FakeAudioCaptureSource(listOf(samplesEvent()))
        val detector = FakePitchDetector(listOf(inTuneHz))
        val vm = TunerViewModel(repo, FakeTunerPreferences(), DetectTunedStringUseCase(source, detector))

        advanceUntilIdle()

        assertTrue(
            "Status after single in-tolerance detection must not be IN_TUNE",
            vm.uiState.value.status != TuningStatus.IN_TUNE,
        )
    }

    // ── Chromatic re-entry snapshot ───────────────────────────────────────────────

    @Test
    fun `onEnterChromaticMode captures previous string index`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onStringSelected(3)
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.currentStringIndex)

        vm.onEnterChromaticMode()
        advanceUntilIdle()

        assertEquals(TunerMode.CHROMATIC, vm.uiState.value.mode)
        vm.onExitChromaticMode()
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.currentStringIndex)
    }

    @Test
    fun `onExitChromaticMode restores previous string index and clears tuned set`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onStringSelected(4)
        advanceUntilIdle()
        vm.onEnterChromaticMode()
        advanceUntilIdle()

        vm.onExitChromaticMode()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TunerMode.PRESET, state.mode)
        assertEquals(4, state.currentStringIndex)
        assertTrue(state.tunedStringIndices.isEmpty())
    }

    @Test
    fun `onExitChromaticMode without prior snapshot lands on string 0`() = runTest {
        val repo = FakeTunerPresetRepository()
        val preset = repo.getPresetById("six_string_standard_e")!!
        val str0Hz = preset.notes[0].frequencyHz(440.0)
        val str1Hz = preset.notes[1].frequencyHz(440.0)

        val source = FakeAudioCaptureSource.multiSession(
            listOf(
                List(6) { samplesEvent() },
                List(6) { samplesEvent() },
                emptyList(),
            ),
        )
        val detector = FakePitchDetector(List(6) { str0Hz } + List(6) { str1Hz })
        val vm = TunerViewModel(repo, FakeTunerPreferences(), DetectTunedStringUseCase(source, detector))

        advanceUntilIdle()
        advanceTimeBy(TunerViewModel.STRING_LOCK_HOLD_MS + 1)
        advanceUntilIdle()
        advanceTimeBy(TunerViewModel.STRING_LOCK_HOLD_MS + 1)
        advanceUntilIdle()
        advanceTimeBy(TunerViewModel.ALL_TUNED_HOLD_MS + 1)
        advanceUntilIdle()

        assertEquals(TunerMode.CHROMATIC, vm.uiState.value.mode)

        vm.onExitChromaticMode()
        advanceUntilIdle()

        assertEquals(TunerMode.PRESET, vm.uiState.value.mode)
        assertEquals(0, vm.uiState.value.currentStringIndex)
    }

    @Test
    fun `onPresetSelected clears chromatic snapshot`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onStringSelected(3)
        advanceUntilIdle()
        vm.onEnterChromaticMode()
        advanceUntilIdle()

        vm.onPresetSelected("six_string_drop_d")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TunerMode.PRESET, state.mode)
        assertEquals(0, state.currentStringIndex)

        vm.onEnterChromaticMode()
        advanceUntilIdle()
        vm.onExitChromaticMode()
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.currentStringIndex)
    }

    @Test
    fun `onStringSelected clears chromatic snapshot`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onStringSelected(3)
        advanceUntilIdle()
        vm.onEnterChromaticMode()
        advanceUntilIdle()

        vm.onStringSelected(1)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TunerMode.PRESET, state.mode)
        assertEquals(1, state.currentStringIndex)
        assertTrue(state.tunedStringIndices.isEmpty())

        vm.onEnterChromaticMode()
        advanceUntilIdle()
        vm.onExitChromaticMode()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.currentStringIndex)
    }

    // ── onAutoAdvanceChanged ──────────────────────────────────────────────────────

    @Test
    fun `onAutoAdvanceChanged persists and reflects in uiState`() = runTest {
        val preferences = FakeTunerPreferences(initialAutoAdvance = true)
        val vm = makeViewModel(preferences = preferences)
        advanceUntilIdle()

        vm.onAutoAdvanceChanged(false)
        advanceUntilIdle()

        assertFalse(preferences.storedAutoAdvance)
        assertFalse(vm.uiState.value.autoAdvanceEnabled)
    }

    @Test
    fun `auto-advance disabled — sustained-in-tune emits StringTuned but does not advance`() = runTest {
        val repo = FakeTunerPresetRepository()
        val preset = repo.getPresetById("six_string_standard_e")!!
        val str0Hz = preset.notes[0].frequencyHz(440.0)

        val source = FakeAudioCaptureSource(List(6) { samplesEvent() })
        val detector = FakePitchDetector(List(6) { str0Hz })

        val preferences = FakeTunerPreferences(initialAutoAdvance = false)
        val collectedEvents = mutableListOf<TunerEvent>()
        val vm = TunerViewModel(repo, preferences, DetectTunedStringUseCase(source, detector))
        val eventsJob = launch { vm.events.toList(collectedEvents) }

        advanceUntilIdle()

        assertTrue(0 in vm.uiState.value.tunedStringIndices)
        assertTrue(collectedEvents.any { it is TunerEvent.StringTuned && it.stringIndex == 0 })

        advanceTimeBy(TunerViewModel.STRING_LOCK_HOLD_MS + 1)
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.currentStringIndex)

        eventsJob.cancel()
    }

    // ── onReferencePitchChanged ───────────────────────────────────────────────────

    @Test
    fun `onReferencePitchChanged retunes targets and persists`() = runTest {
        val preferences = FakeTunerPreferences(initialReferencePitchHz = 440.0)
        val vm = makeViewModel(preferences = preferences)
        advanceUntilIdle()

        val preset = vm.uiState.value.selectedPreset!!
        val str0Note = preset.notes[0]
        val expected432Hz = str0Note.frequencyHz(432.0)

        vm.onReferencePitchChanged(432.0)
        advanceUntilIdle()

        assertEquals(432.0, vm.uiState.value.referencePitchHz, 0.001)
        assertEquals(expected432Hz, vm.uiState.value.targetFrequencyHz!!, 0.1)
        assertEquals(432.0, preferences.storedReferencePitchHz, 0.001)
    }

    // ── Initial prefs loading ─────────────────────────────────────────────────────

    @Test
    fun `initial state reads autoAdvanceEnabled and referencePitchHz from preferences`() = runTest {
        val preferences = FakeTunerPreferences(
            initialAutoAdvance = false,
            initialReferencePitchHz = 432.0,
        )
        val vm = makeViewModel(preferences = preferences)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.autoAdvanceEnabled)
        assertEquals(432.0, state.referencePitchHz, 0.001)
    }

    // ── Auto-advance cancellation ─────────────────────────────────────────────────

    /**
     * If the user selects a new preset while the 200 ms hold is in flight, the hold is cancelled
     * and the new preset takes effect immediately.
     */
    @Test
    fun `auto-advance hold cancelled when preset changes during hold`() = runTest {
        val repo = FakeTunerPresetRepository()
        val preset = repo.getPresetById("six_string_standard_e")!!
        val str0Hz = preset.notes[0].frequencyHz(440.0)

        val source = FakeAudioCaptureSource.multiSession(
            listOf(
                List(6) { samplesEvent() }, // session 1: tune string 0
                emptyList(),                // session 2: after preset change
            ),
        )
        val detector = FakePitchDetector(List(6) { str0Hz })

        val preferences = FakeTunerPreferences()
        val vm = TunerViewModel(repo, preferences, DetectTunedStringUseCase(source, detector))

        // String 0 tuned; 200 ms hold scheduled.
        advanceUntilIdle()
        assertTrue(0 in vm.uiState.value.tunedStringIndices)

        // Change preset BEFORE the hold fires — this cancels the hold.
        vm.onPresetSelected("six_string_drop_d")
        advanceTimeBy(TunerViewModel.STRING_LOCK_HOLD_MS + 1)
        advanceUntilIdle()

        // The hold was cancelled: we should be at string 0 of the new preset, not string 1.
        val state = vm.uiState.value
        assertEquals("six_string_drop_d", state.selectedPreset?.id)
        assertEquals(0, state.currentStringIndex)
        assertEquals(TunerMode.PRESET, state.mode)
        assertTrue(state.tunedStringIndices.isEmpty())
    }
}
