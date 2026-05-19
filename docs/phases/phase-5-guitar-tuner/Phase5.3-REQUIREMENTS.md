# Phase 5.3 — Requirements & Acceptance Criteria

Phase 5.3 produces a complete tuner pipeline driven by a ViewModel. No UI is built; the placeholder from Phase 4 remains. The phase is complete when both checklists pass.

## Agent Responsibilities

### Dependencies

- [ ] `androidx.datastore:datastore-preferences` added to `libs.versions.toml` and referenced from `app/build.gradle.kts`.

### `tuner/data/TunerPreferences.kt`

- [ ] Interface declares `val lastUsedPresetId: Flow<String?>` and `suspend fun setLastUsedPresetId(id: String)`.
- [ ] `TunerPreferencesImpl` is a constructor-injected class with `@ApplicationContext context`.
- [ ] Backed by `androidx.datastore.preferences.preferencesDataStore(name = "tuner_preferences")`.
- [ ] Single persisted key: `last_used_preset_id` (string).
- [ ] Bound to the interface in `tuner/di/TunerModule.kt`.

### `tuner/domain/model/TuningStatus.kt`

- [ ] Enum gains `PERMISSION_DENIED` and `CAPTURE_FAILED` in addition to the Phase 2 values (`IDLE, LISTENING, IN_TUNE, FLAT, SHARP, ALL_STRINGS_TUNED`).
- [ ] Each value has a KDoc comment explaining what it represents and what UI affordance it drives.
- [ ] Addition recorded in `DECISIONS.md`.

### `tuner/domain/model/TunerMode.kt`

- [ ] Enum with two values: `PRESET, CHROMATIC`.
- [ ] KDoc explains each.

### `tuner/domain/model/TunerInput.kt`

- [ ] Data class with `mode: TunerMode`, `targetNote: Note?`, `referencePitchHz: Double`.
- [ ] `targetNote` is null only when `mode == CHROMATIC`. KDoc states this contract; runtime enforcement happens in `DetectTunedStringUseCase`.

### `tuner/domain/usecase/DetectionEvent.kt`

- [ ] Sealed interface with: `object Listening`, `data class Detection(...)`, `object PermissionDenied`, `data class Failed(reason)`.
- [ ] `Detection` carries: `detectedFrequencyHz`, `detectedNote`, `targetNote`, `targetFrequencyHz`, `centsOff`, `isSustainedInTune`.

### `tuner/domain/usecase/DetectTunedStringUseCase.kt`

- [ ] Replaces the Phase 2 stub. Constructor injects `MicrophoneAudioSource` and `PitchDetector`.
- [ ] Single public method: `fun execute(input: TunerInput): Flow<DetectionEvent>`.
- [ ] The flow internally collects from `MicrophoneAudioSource.samples()` and uses `PitchDetector.detectPitch` to convert sample buffers into pitches.
- [ ] Uses `MusicTheory.centsBetween` for cents calculation.
- [ ] In PRESET mode, target comes from `input.targetNote`. In CHROMATIC mode, target is computed each frame via `MusicTheory.frequencyToNote(detected, input.referencePitchHz)`; frames where this returns `null` are skipped.
- [ ] Sustained-tone window: `ArrayDeque<Boolean>` of capacity 6. `isSustainedInTune` is true iff the window is full AND `count { it } >= 5`. Null detections push `false` and emit no `Detection` event for that frame.
- [ ] Constants: `SUSTAINED_WINDOW_SIZE = 6`, `SUSTAINED_MIN_IN_TOLERANCE = 5`, `IN_TUNE_TOLERANCE_CENTS = 5.0`. Named, not inline.
- [ ] Cents output is **raw** (not clamped to ±50). Range: `[-1200, +1200]` for in-range detection.
- [ ] Each `execute` call returns a fresh flow with a fresh window — no state leaks between invocations.
- [ ] Capture events map correctly: `CaptureEvent.PermissionDenied → DetectionEvent.PermissionDenied`, `CaptureEvent.Failed → DetectionEvent.Failed`, `CaptureEvent.Listening → DetectionEvent.Listening`.

### `tuner/presentation/viewmodel/TunerUiState.kt`

- [ ] Refactored to match the spec in `Phase5.3-PLAN.md` "State Model" section.
- [ ] All new fields have sensible defaults: `mode = PRESET`, `availablePresets = emptyMap()`, `selectedPreset = null`, `currentStringIndex = 0`, target/detection fields null, `status = IDLE`, `tunedStringIndices = emptySet()`, `referencePitchHz = 440.0`.
- [ ] Shape change recorded in `DECISIONS.md`.

### `tuner/presentation/viewmodel/TunerEvent.kt`

- [ ] Sealed interface with: `StringTuned(stringIndex)`, `AllStringsTuned`, `EnteredChromaticMode`.
- [ ] KDoc explains why this is a SharedFlow concern (single-consumption side-effects, not state).

### `tuner/presentation/viewmodel/TunerViewModel.kt`

- [ ] Replaces the Phase 2 stub. Constructor-injects `TunerPresetRepository`, `TunerPreferences`, `DetectTunedStringUseCase`.
- [ ] Exposes `val uiState: StateFlow<TunerUiState>` and `val events: SharedFlow<TunerEvent>`.
- [ ] Initialization (in `init { ... }` via `viewModelScope.launch`): loads presets, reads last-used preset ID, resolves to the saved preset or the default (`"six_string_standard_e"`), and emits initial `uiState`.
- [ ] If the saved preset ID no longer exists in the catalog, falls back to the default without crashing.
- [ ] Pipeline is built via `flatMapLatest` over a `MutableStateFlow<TunerInput?>`. When the input is `null`, no use case collection happens (mic stays off).
- [ ] `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialState)` for the final `StateFlow`.
- [ ] Public functions: `onPresetSelected(presetId: String)`, `onStringSelected(stringIndex: Int)`.
- [ ] No public `start()` / `stop()` methods.
- [ ] `onPresetSelected`:
  - [ ] Resolves the preset; if not found, no-op (or update state to an error — at minimum, doesn't crash).
  - [ ] Sets `mode = PRESET`, `selectedPreset = preset`, `currentStringIndex = 0`, `tunedStringIndices = emptySet()`, target derived from `preset.notes[0]`.
  - [ ] Persists via `preferences.setLastUsedPresetId` fire-and-forget (`viewModelScope.launch`).
- [ ] `onStringSelected`:
  - [ ] Sets `mode = PRESET`, `currentStringIndex = stringIndex`, `tunedStringIndices = emptySet()`, target derived from `selectedPreset.notes[stringIndex]`.
  - [ ] Does **not** modify `selectedPreset` or call `setLastUsedPresetId`.
- [ ] Auto-advance:
  - [ ] When `DetectionEvent.Detection(isSustainedInTune = true)` is observed, immediately add `currentStringIndex` to `tunedStringIndices` and emit `TunerEvent.StringTuned(currentStringIndex)`.
  - [ ] After `STRING_LOCK_HOLD_MS = 200`, if there is a next string, increment `currentStringIndex` and update target. If it was the last string, set `status = ALL_STRINGS_TUNED`, emit `TunerEvent.AllStringsTuned`, then after `ALL_TUNED_HOLD_MS = 1200` transition to `mode = CHROMATIC` and emit `TunerEvent.EnteredChromaticMode`.
  - [ ] These delays use `viewModelScope.launch { delay(...) }`, cancelable so that user actions (preset change, string tap) during a hold abort cleanly.
- [ ] Detection mapping respects the IN_TUNE-is-sustained rule: a transient in-tolerance frame is `LISTENING`, not `IN_TUNE`. `IN_TUNE` is only set when `isSustainedInTune` is true.
- [ ] Chromatic mode operation: the use case provides target each frame via `MusicTheory.frequencyToNote`; `uiState.targetNote` and `uiState.detectedNote` reflect that.

### `tuner/di/TunerModule.kt`

- [ ] Existing bindings preserved.
- [ ] New binding: `TunerPreferences` → `TunerPreferencesImpl`.

### Tests

- [ ] `DetectTunedStringUseCaseTest` exists and covers every case listed in `Phase5.3-PLAN.md` "Tests" section:
  - [ ] Permission denied propagates.
  - [ ] Listening propagates.
  - [ ] In-tune in PRESET mode after 6 in-tolerance buffers.
  - [ ] Glitch budget: 5-of-6 still sustains.
  - [ ] Two glitches reset.
  - [ ] Null counts as out-of-tolerance; no `Detection` emitted for null frame.
  - [ ] Two consecutive nulls reset the window.
  - [ ] Chromatic mode resolves target via `frequencyToNote`.
  - [ ] Chromatic with frequencyToNote returning null skips emission.
  - [ ] Raw cents are not clamped (test with -100 cents).
  - [ ] Capture failure propagates.
- [ ] `TunerViewModelTest` exists and covers:
  - [ ] First-launch default preset.
  - [ ] Restore last-used preset.
  - [ ] Fallback when persisted preset no longer exists.
  - [ ] `onPresetSelected` updates state and persists.
  - [ ] Re-selecting the current preset still resets session state.
  - [ ] `onStringSelected` jumps target and exits chromatic mode.
  - [ ] Sustained-in-tune triggers auto-advance after 200 ms and emits `StringTuned` event.
  - [ ] Last-string lock triggers `ALL_STRINGS_TUNED` and (after 1200 ms) transitions to chromatic mode with the right events.
  - [ ] Permission denied sets `status = PERMISSION_DENIED`.
  - [ ] Capture failure sets `status = CAPTURE_FAILED`.
  - [ ] `tunedStringIndices` reset on preset change, string selection, and chromatic transition.
- [ ] Test fakes are hand-written under `app/src/test/java/.../tuner/fakes/`:
  - [ ] `FakeMicrophoneAudioSource` — emits a configurable list of `CaptureEvent`s on collection.
  - [ ] `FakePitchDetector` — configurable to return a list of frequencies per call.
  - [ ] `FakeTunerPresetRepository` — returns a small in-memory preset catalog suitable for testing.
  - [ ] `FakeTunerPreferences` — in-memory `MutableStateFlow<String?>`-backed implementation.
- [ ] Tests use `kotlinx-coroutines-test` `runTest` with virtual time for the `delay(...)` calls (the 200 ms and 1200 ms holds must be exercised by advancing virtual time, not real time).
- [ ] All Phase 5.1 and 5.2 tests still pass.

### Documentation Updates

- [ ] `DECISIONS.md` gains entries for:
  - Sustained-tone rule: sliding window 6, threshold 5, with the glitch-budget rationale.
  - Auto-advance hold duration: 200 ms (`STRING_LOCK_HOLD_MS`).
  - All-strings-tuned hold duration: 1200 ms (`ALL_TUNED_HOLD_MS`).
  - Default first-launch preset: `six_string_standard_e`.
  - DataStore dependency added.
  - Two-mode operation (`TunerMode.PRESET` / `CHROMATIC`).
  - Re-entry rules from chromatic mode (any preset tap, any string tap).
  - `TuningStatus` gains `PERMISSION_DENIED` and `CAPTURE_FAILED`.
  - `TunerUiState` shape refinement.
- [ ] If chromatic mode behavior is not already mentioned in `APP_SPECIFICATION.md`, an addendum subsection is added to "Module: Guitar Tuner" describing it. (The spec currently does not mention chromatic mode; this is new behavior surfaced during 5.3 planning.)

### Code Quality

- [ ] No magic numbers. Window size, threshold, tolerance cents, hold durations, default preset ID all named constants.
- [ ] No `TODO("...")` remains in any 5.3-touched file.
- [ ] No `runBlocking` outside of test code.
- [ ] All public types and methods have KDoc.
- [ ] No `GlobalScope`. All coroutine launches use `viewModelScope`.
- [ ] No fire-and-forget side effects that could outlive the ViewModel (the preferences write uses `viewModelScope`, which is cancelled with the ViewModel — acceptable here because DataStore writes are short and the worst case is a missed persistence on a hard kill).

### Handoff

- [ ] Summary message to the user lists files added, modified, removed, and:
  - A reminder that the tuner placeholder from Phase 4 is unchanged; 5.3 produces no UI.
  - A note that end-to-end audio verification is still deferred to Phase 5.4.
  - Anything to double-check after Gradle sync (DataStore is the only new dependency).

## User Responsibilities (Verification in Android Studio)

- [ ] After applying changes, **File → Sync Project with Gradle Files** completes without errors.
- [ ] **Build → Make Project** completes successfully.
- [ ] **Run → Run All Tests** reports all tests green, including the new `DetectTunedStringUseCaseTest` and `TunerViewModelTest` suites.
- [ ] App launches on Android 12+ emulator/device. Phase 4 tuner placeholder still appears.
- [ ] No Hilt binding errors in Logcat at launch (particularly: `TunerPreferences` binds correctly).

## Decision Log

- [ ] All decisions listed under "Documentation Updates" are recorded in `DECISIONS.md` before the phase is marked complete.
