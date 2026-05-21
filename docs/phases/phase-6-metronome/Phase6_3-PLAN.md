# Phase 6.3 — Metronome ViewModel & State Management

## Goal

Wire the player and preferences from 6.2 into a complete, observable `MetronomeUiState` that will drive the UI built in 6.4. The ViewModel exposes a single `StateFlow<MetronomeUiState>` plus a small set of input methods (`onPlayToggled`, `onBpmChanged`, `onBpmIncrement`, `onBpmDecrement`, `onTimeSignatureChanged`, `onSubdivisionChanged`, `onTapTempo`). Transient errors flow through a separate one-shot event channel.

This is the orchestration layer. No Android UI dependencies — Compose lands in 6.4. All ViewModel work in 6.3 is unit-testable on the JVM using the fakes from 6.2.

## Scope

- Flesh out `MetronomeViewModel` (real body — Phase 2 was a stub).
- Flesh out `MetronomeUiState` from the Phase 2 placeholder.
- Implement `StartMetronomeUseCase` and `StopMetronomeUseCase` (real bodies — Phase 2 stubs).
- Wire persistence: read on init, write on every config change with BPM debouncing.
- Wire tap tempo: tap events into the calculator, resulting BPM through the same config-update path.
- Wire transient errors: `PlayerEvent.Failed` → a one-shot UI event.
- Replace the temporary debug harness from 6.2 — the placeholder screen now observes the real ViewModel state but still uses the simple Phase 4 placeholder UI (the proper screen is 6.4).
- Exhaustive unit tests for the ViewModel and the use cases.

## Out of Scope

- No Compose UI, no real `MetronomeScreen`, no dialogs, no snackbar — Phase 6.4.
- No haptics (Item 10 — deferred).
- No accent customization (Item 11 — deferred).
- No `MetronomePreferencesImpl` integration tests — covered by manual smoke testing in 6.2 and by the ViewModel tests using `FakeMetronomePreferences`.

## Reading Order Before Starting

1. `APP_SPECIFICATION.md` → "Module: Metronome"
2. `DESIGN.md` → §8.2
3. `DECISIONS.md` → all entries
4. `Phase6-Metronome-Decisions.md` → Items 4, 6, 9, 17, 19
5. The completed `Phase6_1-PLAN.md` and `Phase6_2-PLAN.md` for the pipeline shape
6. Phase 5.3's `Phase5_3-PLAN.md` for the analogous ViewModel/preferences pattern from the tuner
7. This file

## Decisions Locked In For 6.3

These are settled before implementation begins:

- ✅ **ViewModel scope:** Screen-scoped via `hiltViewModel()` in the Metronome destination. Lifetime tied to the screen, not the activity.
- ✅ **State shape:** Single `StateFlow<MetronomeUiState>` plus a separate `SharedFlow<MetronomeEvent>` for one-shot events (errors). No state-as-event mixing.
- ✅ **BPM write debouncing:** When BPM changes rapidly (e.g., slider drag), persistence writes are debounced — 200 ms after the last change. The player is updated immediately (so audio responds in real time), but DataStore is not hammered.
- ✅ **Player runs while ViewModel is active and `isPlaying = true`.** The ViewModel collects from `player.run(...)` inside `viewModelScope` only when `isPlaying` becomes true; cancels the collection job on stop.
- ✅ **Tap tempo BPM goes through the same path as other BPM changes:** `onTapTempo()` calls `tapTempoCalculator.onTap()`; if it returns a non-null BPM, `onBpmChanged(bpm)` is invoked.
- ✅ **+/− buttons are flat ±1.** No press-and-hold acceleration (Item 9).
- ✅ **Player failure event:** `PlayerEvent.Failed` → emit `MetronomeEvent.AudioUnavailable` on the event flow; set `isPlaying = false`; release player resources.
- ✅ **Persisted config read on init:** First emission of `MetronomePreferences.config` populates the ViewModel state. Subsequent persisted changes (from other ViewModel instances, in theory — practically none, but for robustness) also update state.

## State Model

### `MetronomeUiState` (refined from Phase 2)

```kotlin
data class MetronomeUiState(
    val isPlaying: Boolean = false,
    val config: MetronomeConfig = MetronomeConfig.DEFAULT,
    val currentBeat: Int = 0,                          // main-beat index, 0-based
    val tempoDescriptor: TempoDescriptor = TempoDescriptor.ALLEGRO,
    val isInitialLoadComplete: Boolean = false,        // false until prefs first emit
)
```

- `currentBeat` always refers to the main beat (subdivision ticks do not advance it).
- `tempoDescriptor` is derived from `config.bpm` and updated whenever BPM changes.
- `isInitialLoadComplete` is `false` for the very first state emission (before DataStore returns the persisted config); the UI in 6.4 will show a brief loading state or simply not animate transitions until this is true.

> **Refinement of Phase 2.** Phase 2 had `MetronomeUiState(isPlaying, config, currentBeat)`. New fields `tempoDescriptor` and `isInitialLoadComplete` are added. Record in `DECISIONS.md`.

### `MetronomeEvent` (new — one-shot events)

```kotlin
sealed interface MetronomeEvent {
    /** Audio playback could not start or was interrupted. UI shows a snackbar. */
    data object AudioUnavailable : MetronomeEvent
}
```

A `SharedFlow<MetronomeEvent>` (with replay = 0, extraBufferCapacity = 1) on the ViewModel surfaces these. The UI in 6.4 collects them in a `LaunchedEffect`.

## Implementation Details

### `metronome/presentation/viewmodel/MetronomeUiState.kt`

Update the existing Phase 2 file with the refined structure above.

### `metronome/presentation/viewmodel/MetronomeEvent.kt`

New file with the sealed interface.

### `metronome/domain/usecase/StartMetronomeUseCase.kt`

Replace the Phase 2 stub. Single responsibility: given a config and a `configFlow`, return the player's event flow.

```kotlin
internal class StartMetronomeUseCase @Inject constructor(
    private val player: MetronomePlayer,
) {
    operator fun invoke(
        initialConfig: MetronomeConfig,
        configFlow: Flow<MetronomeConfig>,
    ): Flow<PlayerEvent> = player.run(initialConfig, configFlow)
}
```

Genuinely thin — the use case mostly exists to keep ViewModel `@Inject` dependencies clean and to give a stable injection point if logic ever needs to be added.

### `metronome/domain/usecase/StopMetronomeUseCase.kt`

Phase 2's `StopMetronomeUseCase` becomes essentially a no-op in 6.3 because stopping is "cancel the collection job." Two options:

**Option A: Remove the use case.** Phase 2 had it as a stub; with the flow-based player, "stop" is a coroutine cancellation, not a function call.

**Option B: Keep it as a marker for symmetry.** Empty body or `cancelJob.cancel()`.

**Decision: Remove `StopMetronomeUseCase`.** Phase 2 had it as a stub because the imperative player API needed an explicit stop function. The new flow-based API doesn't, and a use case with no behavior is noise. Record in `DECISIONS.md`.

### `metronome/presentation/viewmodel/MetronomeViewModel.kt`

The orchestration core. Replaces the Phase 2 stub.

```kotlin
@HiltViewModel
internal class MetronomeViewModel @Inject constructor(
    private val preferences: MetronomePreferences,
    private val startMetronome: StartMetronomeUseCase,
    private val tapTempoCalculator: TapTempoCalculator,
) : ViewModel() {

    private val configFlow = MutableStateFlow(MetronomeConfig.DEFAULT)
    private val _uiState = MutableStateFlow(MetronomeUiState())
    val uiState: StateFlow<MetronomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MetronomeEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<MetronomeEvent> = _events.asSharedFlow()

    private var playerJob: Job? = null
    private var persistJob: Job? = null

    init {
        // Read persisted config on init.
        viewModelScope.launch {
            preferences.config.collect { persisted ->
                configFlow.value = persisted
                _uiState.update {
                    it.copy(
                        config = persisted,
                        tempoDescriptor = tempoDescriptorFor(persisted.bpm),
                        isInitialLoadComplete = true,
                    )
                }
            }
        }
    }

    fun onPlayToggled() {
        if (_uiState.value.isPlaying) stopPlayback() else startPlayback()
    }

    fun onBpmChanged(newBpm: Int) {
        val clamped = newBpm.coerceIn(BPM_MIN, BPM_MAX)
        updateConfig { it.copy(bpm = clamped) }
    }

    fun onBpmIncrement() = onBpmChanged(_uiState.value.config.bpm + 1)
    fun onBpmDecrement() = onBpmChanged(_uiState.value.config.bpm - 1)

    fun onTimeSignatureChanged(numerator: Int, denominator: Int) {
        if ((numerator to denominator) !in SUPPORTED_SIGNATURES) return
        updateConfig { it.copy(timeSignatureNumerator = numerator, timeSignatureDenominator = denominator) }
    }

    fun onSubdivisionChanged(subdivision: Subdivision) {
        updateConfig { it.copy(subdivision = subdivision) }
    }

    fun onTapTempo() {
        tapTempoCalculator.onTap()?.let { newBpm -> onBpmChanged(newBpm) }
    }

    private fun updateConfig(transform: (MetronomeConfig) -> MetronomeConfig) {
        val updated = transform(_uiState.value.config)
        configFlow.value = updated
        _uiState.update {
            it.copy(config = updated, tempoDescriptor = tempoDescriptorFor(updated.bpm))
        }
        schedulePersist(updated)
    }

    private fun schedulePersist(config: MetronomeConfig) {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            preferences.setConfig(config)
        }
    }

    private fun startPlayback() {
        if (playerJob?.isActive == true) return
        _uiState.update { it.copy(isPlaying = true, currentBeat = 0) }
        playerJob = viewModelScope.launch {
            startMetronome(initialConfig = configFlow.value, configFlow = configFlow)
                .catch { _events.tryEmit(MetronomeEvent.AudioUnavailable); _uiState.update { it.copy(isPlaying = false) } }
                .collect { event ->
                    when (event) {
                        PlayerEvent.Started -> { /* already set isPlaying=true above */ }
                        is PlayerEvent.BeatTick -> _uiState.update { it.copy(currentBeat = event.beatIndexInBar) }
                        is PlayerEvent.Failed -> {
                            _events.tryEmit(MetronomeEvent.AudioUnavailable)
                            _uiState.update { it.copy(isPlaying = false) }
                        }
                    }
                }
            // Flow completed (player flow closed): treat as a stop.
            _uiState.update { it.copy(isPlaying = false) }
        }
    }

    private fun stopPlayback() {
        playerJob?.cancel()
        playerJob = null
        tapTempoCalculator.reset()
        _uiState.update { it.copy(isPlaying = false) }
    }

    private companion object {
        const val PERSIST_DEBOUNCE_MS = 200L
    }
}
```

Notes on the design:
- `configFlow` (a `MutableStateFlow`) is the bridge between the ViewModel and the player. Updates flow through it both when the user changes config and when the player needs to be re-anchored.
- Persistence is debounced 200 ms after the last config change. Slider drags don't hammer DataStore.
- `tapTempoCalculator.reset()` on stop ensures the next session starts fresh.
- The `catch` block on the player flow handles any exceptional termination (the `Failed` event covers in-band errors; `catch` covers unexpected exceptions).

### `metronome/di/MetronomeModule.kt`

No new bindings — `MetronomeViewModel` is constructor-injected via Hilt, and all its dependencies (`MetronomePreferences`, `StartMetronomeUseCase`, `TapTempoCalculator`) are already bound from 6.2.

### Phase 4 placeholder update (interim)

The debug harness from 6.2 is replaced by a minimal "observe the ViewModel" version. The Phase 4 placeholder screen now:
- Observes `viewModel.uiState` via `collectAsStateWithLifecycle`.
- Shows the current BPM, time signature, and `isPlaying` as text.
- Has a Play / Stop button calling `viewModel.onPlayToggled()`.
- Has a +/− pair calling `viewModel.onBpmIncrement()` / `viewModel.onBpmDecrement()`.

This is still a placeholder, not the real screen — but it now exercises the real ViewModel. The `// TODO(6.4)` comment migrates from 6.2's harness onto this temporary view. 6.4 replaces it entirely.

## Tests

All tests live under `app/src/test/java/de/ritzelprimpf/toniqo/metronome/`.

### `MetronomeViewModelTest` (exhaustive)

Uses `FakeMetronomePreferences`, a fake `MetronomePlayer`, and a `TapTempoCalculator` with a fake `Clock`. Tests use `kotlinx-coroutines-test` (`runTest`, virtual time).

The fake player needs to be created for this phase — it lives in `app/src/test/java/de/ritzelprimpf/toniqo/metronome/fakes/FakeMetronomePlayer.kt`:

```kotlin
internal class FakeMetronomePlayer : MetronomePlayer {
    private val emissions = MutableSharedFlow<PlayerEvent>(replay = 0, extraBufferCapacity = 64)
    val emitted = mutableListOf<MetronomeConfig>()

    var receivedInitialConfig: MetronomeConfig? = null
    var receivedConfigUpdates: List<MetronomeConfig> = emptyList()

    override fun run(
        initialConfig: MetronomeConfig,
        configFlow: Flow<MetronomeConfig>,
    ): Flow<PlayerEvent> = flow {
        receivedInitialConfig = initialConfig
        emit(PlayerEvent.Started)
        launch { configFlow.toList(emitted) }
        emitAll(emissions)
    }

    suspend fun emit(event: PlayerEvent) = emissions.emit(event)
}
```

Tests:

- **Initial state loads from preferences.** Set fake prefs to `MetronomeConfig(bpm = 90, ..., subdivision = EIGHTHS)`. After ViewModel init, `uiState.value.config.bpm == 90`, `subdivision == EIGHTHS`, `isInitialLoadComplete == true`, `isPlaying == false`.
- **First launch defaults.** Fake prefs is empty (state flow at `DEFAULT`). ViewModel init populates `uiState` with `DEFAULT`. `tempoDescriptor == ALLEGRO` (BPM 120).
- **`onBpmChanged` updates state and persists (debounced).** Call with `bpm = 100`. Immediately, `uiState.value.config.bpm == 100`. After advancing virtual time by 200 ms, `prefs.config.value.bpm == 100`. Fewer than 200 ms after the last change, prefs is not yet updated.
- **`onBpmChanged` clamps to [1, 300].** Calling with 0 → state has 1. Calling with 999 → state has 300.
- **`onBpmIncrement` and `onBpmDecrement`.** Default BPM 120; increment → 121; decrement twice → 119.
- **Tempo descriptor updates on BPM change.** Start at BPM 120 (`ALLEGRO`). Change to 90 → `ANDANTE`. Change to 200 → `PRESTO`.
- **`onTimeSignatureChanged` updates state.** Valid signature → state updates; persist debounced.
- **`onTimeSignatureChanged` rejects unsupported signatures.** Call with (5, 8) → no state change. (5/8 is not in `SUPPORTED_SIGNATURES`.)
- **`onSubdivisionChanged` updates state.** All four values exercised.
- **`onPlayToggled` from stopped → playing.** Calling triggers player collection. `uiState.isPlaying == true`. Fake player received the right initial config.
- **`onPlayToggled` from playing → stopped.** Cancels player job. `isPlaying == false`.
- **Config changes mid-playback flow through to player.** Start playback. Call `onBpmChanged(100)`. The fake player's `emitted` list contains the updated config.
- **`PlayerEvent.BeatTick` updates `currentBeat`.** Start playback. Fake player emits `BeatTick(2)`. State has `currentBeat == 2`.
- **`PlayerEvent.Failed` triggers error event and stops playback.** Start playback. Fake player emits `Failed(AUDIO_TRACK_INIT_FAILED)`. State has `isPlaying == false`. The events flow has emitted `AudioUnavailable`.
- **Player flow completion stops playback.** Start playback. Fake player closes the flow (no emission). After a tick of the scheduler, `isPlaying == false`. No error event.
- **`onTapTempo` first tap returns null, state unchanged.** Fake clock at t=0. Call `onTapTempo()`. State BPM unchanged.
- **`onTapTempo` second tap updates BPM.** Fake clock advances 500 ms. Call `onTapTempo()` again. State BPM ≈ 120.
- **Stopping playback resets tap-tempo state.** Tap multiple times. Stop playback. Tap again. The first post-stop tap returns null (window was reset).

### `StartMetronomeUseCaseTest`

Trivial — verifies it delegates to `player.run(...)` with the given arguments. One test.

## Steps

1. Update `metronome/presentation/viewmodel/MetronomeUiState.kt` with the refined structure (`isInitialLoadComplete`, `tempoDescriptor`).
2. Create `metronome/presentation/viewmodel/MetronomeEvent.kt`.
3. Implement `metronome/domain/usecase/StartMetronomeUseCase.kt`.
4. **Remove** `metronome/domain/usecase/StopMetronomeUseCase.kt`. Record in `DECISIONS.md`.
5. Implement `metronome/presentation/viewmodel/MetronomeViewModel.kt`.
6. Create `metronome/fakes/FakeMetronomePlayer.kt` in the test source set.
7. Write `MetronomeViewModelTest` covering all cases above.
8. Write `StartMetronomeUseCaseTest`.
9. Update the Phase 4 metronome placeholder screen to consume the real ViewModel. Keep the `// TODO(6.4)` marker.
10. Remove the 6.2 debug harness button-only path; the new placeholder uses the ViewModel.
11. Update `DECISIONS.md` with: the `MetronomeUiState` field additions, the removal of `StopMetronomeUseCase`, the 200 ms persistence debounce, the `MetronomeEvent` one-shot channel design.
12. Hand off to the user with a summary.

## Completion Criteria

See `Phase6_3-REQUIREMENTS.md`.
