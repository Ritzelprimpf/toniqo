package de.ritzelprimpf.toniqo.metronome

import de.ritzelprimpf.toniqo.metronome.data.TapTempoCalculator
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.PlayerEvent
import de.ritzelprimpf.toniqo.metronome.domain.model.PlayerFailureReason
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import de.ritzelprimpf.toniqo.metronome.domain.model.TempoDescriptor
import de.ritzelprimpf.toniqo.metronome.domain.model.tempoDescriptorFor
import de.ritzelprimpf.toniqo.metronome.domain.repository.MetronomePlayer
import de.ritzelprimpf.toniqo.metronome.domain.usecase.StartMetronomeUseCase
import de.ritzelprimpf.toniqo.metronome.fakes.FakeClock
import de.ritzelprimpf.toniqo.metronome.fakes.FakeMetronomePlayer
import de.ritzelprimpf.toniqo.metronome.fakes.FakeMetronomePreferences
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.MetronomeEvent
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.MetronomeUiState
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.MetronomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MetronomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakePrefs = FakeMetronomePreferences()
    private val fakePlayer = FakeMetronomePlayer()
    private val fakeClock = FakeClock()

    private lateinit var viewModel: MetronomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = buildViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        prefs: FakeMetronomePreferences = fakePrefs,
        player: MetronomePlayer = fakePlayer,
    ) = MetronomeViewModel(
        preferences = prefs,
        startMetronome = StartMetronomeUseCase(player),
        tapTempoCalculator = TapTempoCalculator(fakeClock),
    )

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial state has defaults before preferences load`() {
        val state = viewModel.uiState.value

        assertFalse(state.isPlaying)
        assertEquals(MetronomeConfig.DEFAULT, state.config)
        assertEquals(MetronomeUiState.INITIAL_BEAT, state.currentBeat)
        assertEquals(tempoDescriptorFor(MetronomeConfig.DEFAULT_BPM), state.tempoDescriptor)
        assertFalse(state.isInitialLoadComplete)
    }

    // ─── Preferences wiring ───────────────────────────────────────────────────

    @Test
    fun `isInitialLoadComplete becomes true after preferences first emission`() = runTest {
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isInitialLoadComplete)
    }

    @Test
    fun `persisted config is loaded into uiState on init`() = runTest {
        val stored = MetronomeConfig(bpm = 90, subdivision = Subdivision.EIGHTHS)
        val vm = buildViewModel(prefs = FakeMetronomePreferences(stored))

        advanceUntilIdle()

        assertEquals(stored, vm.uiState.value.config)
    }

    // ─── Play / Stop ─────────────────────────────────────────────────────────

    @Test
    fun `onPlayToggled when stopped sets isPlaying true`() = runTest {
        advanceUntilIdle()

        viewModel.onPlayToggled()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isPlaying)
    }

    @Test
    fun `onPlayToggled when playing sets isPlaying false`() = runTest {
        advanceUntilIdle()

        viewModel.onPlayToggled() // start
        advanceUntilIdle()
        viewModel.onPlayToggled() // stop
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPlaying)
    }

    @Test
    fun `player flow completion sets isPlaying false without emitting error event`() = runTest {
        val emittedEvents = mutableListOf<MetronomeEvent>()
        val collectJob = launch { viewModel.events.toList(emittedEvents) }
        advanceUntilIdle()

        viewModel.onPlayToggled()
        advanceUntilIdle()

        fakePlayer.complete()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPlaying)
        assertTrue(emittedEvents.isEmpty())
        collectJob.cancel()
    }

    // ─── Beat ticks ──────────────────────────────────────────────────────────

    @Test
    fun `BeatTick event updates currentBeat in uiState`() = runTest {
        advanceUntilIdle()
        viewModel.onPlayToggled()
        advanceUntilIdle()

        fakePlayer.sendEvent(PlayerEvent.BeatTick(beatIndexInBar = 2))
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.currentBeat)
    }

    // ─── Error events ─────────────────────────────────────────────────────────

    @Test
    fun `PlayerEvent Failed emits AudioUnavailable and sets isPlaying false`() = runTest {
        val emittedEvents = mutableListOf<MetronomeEvent>()
        val collectJob = launch { viewModel.events.toList(emittedEvents) }
        advanceUntilIdle()

        viewModel.onPlayToggled()
        advanceUntilIdle()

        fakePlayer.sendEvent(PlayerEvent.Failed(PlayerFailureReason.AUDIO_FOCUS_DENIED))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPlaying)
        assertEquals(listOf(MetronomeEvent.AudioUnavailable), emittedEvents)
        collectJob.cancel()
    }

    @Test
    fun `player flow exception emits AudioUnavailable and sets isPlaying false`() = runTest {
        val throwingPlayer = object : MetronomePlayer {
            override fun run(
                initialConfig: MetronomeConfig,
                configFlow: Flow<MetronomeConfig>,
            ): Flow<PlayerEvent> = flow { throw RuntimeException("audio init failed") }
        }
        val vm = buildViewModel(player = throwingPlayer)

        val emittedEvents = mutableListOf<MetronomeEvent>()
        val collectJob = launch { vm.events.toList(emittedEvents) }
        advanceUntilIdle()

        vm.onPlayToggled()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isPlaying)
        assertEquals(listOf(MetronomeEvent.AudioUnavailable), emittedEvents)
        collectJob.cancel()
    }

    // ─── BPM changes ─────────────────────────────────────────────────────────

    @Test
    fun `onBpmChanged updates config and tempoDescriptor`() = runTest {
        advanceUntilIdle()

        viewModel.onBpmChanged(80)

        assertEquals(80, viewModel.uiState.value.config.bpm)
        assertEquals(TempoDescriptor.ANDANTE, viewModel.uiState.value.tempoDescriptor)
    }

    @Test
    fun `onBpmChanged clamps value below BPM_MIN to BPM_MIN`() = runTest {
        advanceUntilIdle()

        viewModel.onBpmChanged(MetronomeConfig.BPM_MIN - 1)

        assertEquals(MetronomeConfig.BPM_MIN, viewModel.uiState.value.config.bpm)
    }

    @Test
    fun `onBpmChanged clamps value above BPM_MAX to BPM_MAX`() = runTest {
        advanceUntilIdle()

        viewModel.onBpmChanged(MetronomeConfig.BPM_MAX + 1)

        assertEquals(MetronomeConfig.BPM_MAX, viewModel.uiState.value.config.bpm)
    }

    @Test
    fun `onBpmIncrement adds one to current BPM`() = runTest {
        advanceUntilIdle()

        val before = viewModel.uiState.value.config.bpm
        viewModel.onBpmIncrement()

        assertEquals(before + 1, viewModel.uiState.value.config.bpm)
    }

    @Test
    fun `onBpmDecrement subtracts one from current BPM`() = runTest {
        advanceUntilIdle()

        val before = viewModel.uiState.value.config.bpm
        viewModel.onBpmDecrement()

        assertEquals(before - 1, viewModel.uiState.value.config.bpm)
    }

    // ─── Tap tempo ────────────────────────────────────────────────────────────

    @Test
    fun `onTapTempo second tap after 500ms sets bpm to 120`() = runTest {
        advanceUntilIdle()

        viewModel.onTapTempo() // first tap — no BPM result yet
        fakeClock.advanceBy(500L * 1_000_000L) // 500 ms expressed in nanoseconds
        viewModel.onTapTempo() // second tap → 120 bpm

        assertEquals(120, viewModel.uiState.value.config.bpm)
    }

    // ─── Time signature ───────────────────────────────────────────────────────

    @Test
    fun `onTimeSignatureChanged with supported signature updates config`() = runTest {
        advanceUntilIdle()

        viewModel.onTimeSignatureChanged(numerator = 3, denominator = 4)

        assertEquals(3, viewModel.uiState.value.config.timeSignatureNumerator)
        assertEquals(4, viewModel.uiState.value.config.timeSignatureDenominator)
    }

    @Test
    fun `onTimeSignatureChanged with unsupported signature leaves config unchanged`() = runTest {
        advanceUntilIdle()

        val before = viewModel.uiState.value.config
        viewModel.onTimeSignatureChanged(numerator = 5, denominator = 8) // not in SUPPORTED_SIGNATURES

        assertEquals(before.timeSignatureNumerator, viewModel.uiState.value.config.timeSignatureNumerator)
        assertEquals(before.timeSignatureDenominator, viewModel.uiState.value.config.timeSignatureDenominator)
    }

    // ─── BPM persistence debounce ────────────────────────────────────────────

    @Test
    fun `bpm change updates ui state immediately but persists only after debounce delay`() = runTest {
        advanceUntilIdle()
        val initialBpm = fakePrefs.storedConfig.bpm

        viewModel.onBpmChanged(initialBpm + 50)

        // UI is updated immediately
        assertEquals(initialBpm + 50, viewModel.uiState.value.config.bpm)
        // DataStore not yet written — debounce has not fired
        assertEquals(initialBpm, fakePrefs.storedConfig.bpm)

        advanceTimeBy(201L)

        // Debounce window has elapsed; DataStore now reflects the new value
        assertEquals(initialBpm + 50, fakePrefs.storedConfig.bpm)
    }
}
