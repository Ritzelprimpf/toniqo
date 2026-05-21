# Phase 6.3 — Requirements & Acceptance Criteria

Phase 6.3 produces a fully-wired ViewModel and use case layer with a minimal Phase 4-style placeholder screen now backed by the real ViewModel. No real Compose UI yet — that's 6.4. The phase is complete when both the agent checklist and the user checklist pass.

## Agent Responsibilities

### `metronome/presentation/viewmodel/MetronomeUiState.kt`

- [ ] Phase 2 fields preserved: `isPlaying: Boolean`, `config: MetronomeConfig`, `currentBeat: Int`.
- [ ] New field: `tempoDescriptor: TempoDescriptor` defaulting to `tempoDescriptorFor(MetronomeConfig.DEFAULT.bpm)` (== `ALLEGRO`).
- [ ] New field: `isInitialLoadComplete: Boolean = false`.
- [ ] All fields have sensible defaults matching `MetronomeConfig.DEFAULT`.
- [ ] Field additions recorded in `DECISIONS.md`.

### `metronome/presentation/viewmodel/MetronomeEvent.kt`

- [ ] `sealed interface MetronomeEvent` with the one-shot event types.
- [ ] At minimum: `data object AudioUnavailable : MetronomeEvent`.
- [ ] No state-as-event mixing: `MetronomeUiState` does not carry an "error" field.

### `metronome/domain/usecase/StartMetronomeUseCase.kt`

- [ ] Replaces the Phase 2 stub. Constructor-injects `MetronomePlayer`.
- [ ] `operator fun invoke(initialConfig: MetronomeConfig, configFlow: Flow<MetronomeConfig>): Flow<PlayerEvent>` delegates to `player.run(...)`.
- [ ] Genuinely thin — no orchestration logic beyond the delegation.

### `metronome/domain/usecase/StopMetronomeUseCase.kt`

- [ ] **File removed.** Phase 2's stub use case is no longer needed: stopping is "cancel the player flow's collection job," done directly in the ViewModel.
- [ ] Removal recorded in `DECISIONS.md`.

### `metronome/presentation/viewmodel/MetronomeViewModel.kt`

- [ ] Annotated `@HiltViewModel`, constructor-injected.
- [ ] Exposes `val uiState: StateFlow<MetronomeUiState>`.
- [ ] Exposes `val events: SharedFlow<MetronomeEvent>` for one-shot events (replay = 0, `extraBufferCapacity = 1`).
- [ ] On `init`, collects `preferences.config` and populates state; sets `isInitialLoadComplete = true` after the first emission.
- [ ] Public methods:
  - `fun onPlayToggled()` — flips between starting and stopping playback.
  - `fun onBpmChanged(newBpm: Int)` — clamps to `[BPM_MIN, BPM_MAX]`, updates state, debounces persistence.
  - `fun onBpmIncrement()` — calls `onBpmChanged(currentBpm + 1)`.
  - `fun onBpmDecrement()` — calls `onBpmChanged(currentBpm - 1)`.
  - `fun onTimeSignatureChanged(numerator: Int, denominator: Int)` — validates against `SUPPORTED_SIGNATURES`; rejects unsupported pairs silently.
  - `fun onSubdivisionChanged(subdivision: Subdivision)`.
  - `fun onTapTempo()` — calls `tapTempoCalculator.onTap()`; routes any non-null result through `onBpmChanged`.
- [ ] On any config change, the ViewModel updates the shared `configFlow` (`MutableStateFlow`) that the player observes. Player updates apply on the next beat (per the player's re-anchor rule).
- [ ] On any config change, persistence is scheduled with a 200 ms debounce (named constant `PERSIST_DEBOUNCE_MS`). A subsequent change within the debounce window cancels the pending persist and schedules a new one.
- [ ] Tempo descriptor in state is updated immediately on every BPM change (no debounce).
- [ ] Starting playback launches a coroutine that collects from `startMetronome(...)` and updates state on each `PlayerEvent`.
- [ ] `PlayerEvent.Started` → no state change needed (`isPlaying` was already set to true before launching).
- [ ] `PlayerEvent.BeatTick` → `uiState.currentBeat` updated.
- [ ] `PlayerEvent.Failed` → emits `MetronomeEvent.AudioUnavailable`; sets `isPlaying = false`.
- [ ] Exception escaping the player flow (`catch` block) → emits `MetronomeEvent.AudioUnavailable`; sets `isPlaying = false`.
- [ ] Flow completion (player flow closes without emission) → sets `isPlaying = false`; no error event.
- [ ] Stopping playback cancels the player collection job and resets the tap-tempo calculator.
- [ ] No `@Provides` for things constructor-injectable.
- [ ] No magic numbers — only `PERSIST_DEBOUNCE_MS` as a named constant.

### `metronome/fakes/FakeMetronomePlayer.kt` (test source set)

- [ ] Lives under `app/src/test/java/de/ritzelprimpf/toniqo/metronome/fakes/`.
- [ ] Implements `MetronomePlayer`.
- [ ] Records the `initialConfig` it was called with.
- [ ] Records all values seen on `configFlow`.
- [ ] Exposes a way to emit arbitrary `PlayerEvent`s for tests.
- [ ] Internal visibility, not exposed in the main source set.

### Phase 4 Placeholder Screen Update (interim)

- [ ] The metronome placeholder now consumes `MetronomeViewModel` via `hiltViewModel()`.
- [ ] Observes `uiState` with `collectAsStateWithLifecycle`.
- [ ] Shows current BPM, time signature, subdivision, and `isPlaying` as plain text.
- [ ] Has a Play/Stop button calling `viewModel.onPlayToggled()`.
- [ ] Has +/− buttons calling `viewModel.onBpmIncrement()` / `viewModel.onBpmDecrement()`.
- [ ] `// TODO(6.4): replace with real screen` comment present.
- [ ] The 6.2 debug-harness scaffolding is removed; the placeholder now talks to the real ViewModel.

### Tests

- [ ] `MetronomeViewModelTest` covers all cases from `Phase6_3-PLAN.md` → "Tests":
  - Initial state loads from preferences (both populated and first-launch).
  - `onBpmChanged` updates state immediately and persists after debounce; clamps inputs.
  - `onBpmIncrement` / `onBpmDecrement`.
  - `tempoDescriptor` updates on BPM change.
  - `onTimeSignatureChanged` (valid and unsupported pairs).
  - `onSubdivisionChanged` for all four values.
  - `onPlayToggled` start and stop paths.
  - Config changes mid-playback flow through to player.
  - `BeatTick` updates `currentBeat`.
  - `Failed` triggers `AudioUnavailable` event and stops.
  - Flow completion stops playback without error.
  - Tap tempo paths (null on first tap; BPM on second tap; reset on stop).
- [ ] Tests use `runTest`, virtual time, `FakeMetronomePreferences`, `FakeMetronomePlayer`, and a `TapTempoCalculator` with a fake `Clock`.
- [ ] `StartMetronomeUseCaseTest` verifies delegation to `player.run(...)`.

### Documentation Updates

- [ ] `DECISIONS.md` gains entries for:
  - **`MetronomeUiState` field additions** — `tempoDescriptor` and `isInitialLoadComplete` added in 6.3.
  - **`StopMetronomeUseCase` removed** — superseded by coroutine cancellation in the ViewModel under the flow-based player API.
  - **200 ms BPM persistence debounce** — UI updates the state and player immediately, but DataStore writes are debounced to avoid churn during slider drags.
  - **`MetronomeEvent` one-shot channel** — transient errors flow through a `SharedFlow` separate from `MetronomeUiState`, avoiding state-as-event coupling.

### Code Quality

- [ ] No `TODO("Not yet implemented")` remains in any 6.3-touched file. The `TODO(6.4)` marker on the placeholder is distinct and allowed.
- [ ] All public types and methods have KDoc comments.
- [ ] No magic numbers — `PERSIST_DEBOUNCE_MS` is the only literal, and it's named.
- [ ] Use of `tryEmit` on the SharedFlow is intentional (events are dropped if the buffer overflows — acceptable for transient UI signals).

### Handoff

- [ ] Summary lists files added, modified, and removed (notably `StopMetronomeUseCase.kt`).
- [ ] Summary calls out: "the placeholder screen now talks to the real ViewModel, but it's still a placeholder — 6.4 replaces it with the proper Compose UI."

## User Responsibilities (Verification in Android Studio + on a Real Device)

- [ ] **Gradle sync** completes without errors.
- [ ] **Build → Make Project** completes successfully.
- [ ] **Run All Tests** reports all tests green.
- [ ] App launches on an Android 12+ device or emulator.

### Smoke tests on the interim placeholder

- [ ] **Initial config visible.** Open the metronome tab. The placeholder shows BPM 120 (default).
- [ ] **Play works.** Tap Play. Audio starts. `isPlaying` text reflects this.
- [ ] **+/− adjust BPM.** Tap + a few times. BPM updates in the text. Audio cadence adjusts on the next beat (no glitch).
- [ ] **Stop works.** Tap Stop. Audio stops. State reflects this.
- [ ] **Persistence.** Set BPM to e.g. 90 with the −/+ buttons. Stop playback. Force-close the app and relaunch. Open the metronome tab. BPM reads 90 (persisted across launches).
- [ ] **Lifecycle stop.** Start playback. Navigate to another tab. Audio stops. Return to the metronome tab. `isPlaying` is false; BPM still reads the value you set.
- [ ] **No exceptions in Logcat** during any of the above.

## Decision Log

- [ ] All decisions listed under "Documentation Updates" are recorded in `DECISIONS.md` before the phase is marked complete.
