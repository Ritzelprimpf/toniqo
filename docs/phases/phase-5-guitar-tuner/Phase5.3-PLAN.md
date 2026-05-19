# Phase 5.3 — Tuner Logic & ViewModel

## Goal

Wire the audio capture and pitch detection from 5.2 into a working tuner pipeline. By the end of 5.3, the ViewModel exposes a complete, observable `TunerUiState` that drives the UI to be built in 5.4. The pipeline implements: target-pitch comparison, the sustained-tone state machine, auto-advance, the preset-mode → chromatic-mode transition, manual string selection, preset persistence, and graceful handling of permission denial.

This is the first sub-phase that introduces meaningful behavior. Most of the testing burden lives here — the use case and ViewModel are pure logic and must be exhaustively covered.

## Scope

- Implement `DetectTunedStringUseCase` (real body — Phase 2 was a stub).
- Implement `TunerViewModel` (real body — Phase 2 was a stub).
- Flesh out `TunerUiState` from the Phase 2 placeholder.
- Introduce `TunerMode` (preset vs. chromatic) and refine `TuningStatus`.
- Introduce `TunerPreferences` (DataStore-backed persistence for last-used preset).
- Wire everything via Hilt.
- Exhaustive unit tests for the use case, ViewModel, and preferences.

## Out of Scope

- No UI. `TunerScreen` stays as the Phase 4 placeholder until 5.4.
- No permission-request flow. The ViewModel observes `CaptureEvent.PermissionDenied` and surfaces it as `TuningStatus.PERMISSION_DENIED`; 5.4 builds the request-and-recover UI.
- No reference-pitch toggle. `MusicTheory.centsBetween` is called with A4 = 440 Hz; the 432 Hz toggle is a 5.4 concern. The use case takes `referencePitchHz` as a parameter so changing it later is trivial.
- No analytics, no logging beyond Logcat-level debug, no error reporting.

## Reading Order Before Starting

1. `APP_SPECIFICATION.md` → "Module: Guitar Tuner"
2. `DESIGN.md` → §8.1 (Tuner) and §2.4 (cents → color mapping)
3. `DECISIONS.md` → all entries
4. `Phase5-PLAN.md` → "Decisions Already Resolved" and the 5.3 open questions
5. The completed `Phase5.1-PLAN.md` and `Phase5.2-PLAN.md` for the pipeline shape
6. This file

## Decisions Locked In For 5.3

- ✅ **Sustained-tone rule:** Sliding window of 6 consecutive pitch detections. The string is considered "in tune" when **at least 5 of the last 6 detections** were within ±5 cents of the target. A `null` from the detector counts as out-of-tolerance. Two consecutive nulls reset the window. Constants in the use case: `SUSTAINED_WINDOW_SIZE = 6`, `SUSTAINED_MIN_IN_TOLERANCE = 5`.
- ✅ **Cents output range:** Use case emits raw cents in `[-1200, +1200]`. The UI is responsible for visual clamping (the needle pegs at ±50; the cents value is still readable in the status line). Out-of-range detection (cents outside ±1200, or detector returned `null` for a reason other than silence) is reported as the status without a cents value.
- ✅ **Auto-advance:** When the sustained-in-tune condition is met, the use case emits `TuningStatus.IN_TUNE` and **holds it for 200 ms** before advancing to the next string. During this hold, the `tunedStringIndices` set already includes the just-tuned string (so the UI can render the check mark immediately).
- ✅ **Mic listening rule:** Lifecycle-aware **AND** preset-selected. The capture pipeline runs iff (the tuner screen is in `STARTED` state) AND (a preset is selected). The ViewModel exposes a `Flow<TunerUiState>` whose upstream is a `Flow<CaptureEvent>` whose lifetime is governed by the collector — so simply collecting with `collectAsStateWithLifecycle` in the screen gets the lifecycle half for free.
- ✅ **Default preset:** First-launch default is `six_string_standard_e`. On every subsequent launch, the last-used preset (stored in DataStore) is restored.
- ✅ **Preset persistence:** `androidx.datastore:datastore-preferences` (new dependency, baseline-justified). Single value persisted: `last_used_preset_id: String`.
- ✅ **Per-string state representation:** `tunedStringIndices: Set<Int>` in `TunerUiState`. Strings are added to the set when they pass the sustained-tone check. The set is reset when (a) a preset is selected (even if it's the same one — see below) or (b) the user enters chromatic mode.
- ✅ **Two-mode operation:** `TunerMode.PRESET` and `TunerMode.CHROMATIC`.
  - Preset mode targets `preset.notes[currentStringIndex]`. Auto-advance fires on each in-tune string. After the last string, the use case emits `TuningStatus.ALL_STRINGS_TUNED` for 1.2 s (per `DESIGN.md` §8.1's success state hold) and then transitions to chromatic mode.
  - Chromatic mode computes the target from `MusicTheory.frequencyToNote(detectedHz)` per detection. No auto-advance. `currentStringIndex` is meaningless; UI shows the detected-note hero based on the chromatic target.
- ✅ **Returning to preset mode:** Two paths:
  - Tapping any preset (including the currently selected one) re-arms preset mode at `currentStringIndex = 0` with an empty `tunedStringIndices` set. The success animation is re-armed.
  - Tapping a specific string in the string selector also re-arms preset mode, targeting that string (`currentStringIndex = tappedIndex`). The `tunedStringIndices` set is reset and auto-advance proceeds from that string forward. Selecting a string lower than already-tuned ones means the user has explicitly chosen to re-tune; the auto-advance order is index-by-index from the tap point.
- ✅ **Reference pitch:** 440 Hz hardcoded into the ViewModel for 5.3. The use case accepts it as a parameter to keep 5.4's job small.

## State Model

### `TuningStatus` (enum)
```
IDLE                  // No preset and no chromatic detection running
LISTENING             // Capture active, no fundamental detected yet
FLAT                  // Detected pitch < target by more than 5 cents
IN_TUNE               // Detected pitch within ±5 cents, sustained
SHARP                 // Detected pitch > target by more than 5 cents
ALL_STRINGS_TUNED     // Transitional state (~1.2 s) before flipping to chromatic
PERMISSION_DENIED     // Capture cannot proceed; UI shows permission card
CAPTURE_FAILED        // Non-permission audio error
```

A `TuningStatus.IN_TUNE` is emitted only after the sustained-tone window is satisfied. The transient frame-by-frame "this buffer happens to be within ±5 cents" is **not** `IN_TUNE` — it's still `FLAT` / `SHARP` / `LISTENING` depending on context. This is important: the UI's needle position is driven by the raw cents value, the *status color* and the auto-advance trigger are driven by the sustained flag.

> **Refinement of Phase 2.** Phase 2 had `IDLE, LISTENING, IN_TUNE, FLAT, SHARP, ALL_STRINGS_TUNED`. The two new values (`PERMISSION_DENIED`, `CAPTURE_FAILED`) are necessary for the use case to expose what `MicrophoneAudioSource` can emit. Record the addition in `DECISIONS.md`.

### `TunerMode` (enum)
```
PRESET     // Tuning to a specific string of the selected preset
CHROMATIC  // Tuning to whatever note is nearest the detected pitch
```

### `TunerUiState` (data class)

```kotlin
data class TunerUiState(
    val mode: TunerMode = TunerMode.PRESET,
    val availablePresets: Map<Int, Map<TunerCategory, List<TunerPreset>>> = emptyMap(),
    val selectedPreset: TunerPreset? = null,
    val currentStringIndex: Int = 0,                  // Meaningful only in PRESET mode
    val targetNote: Note? = null,                     // Either preset[i] or chromatic nearest
    val targetFrequencyHz: Double? = null,
    val detectedFrequencyHz: Double? = null,
    val detectedNote: Note? = null,                   // Computed for display; same as targetNote in CHROMATIC, may be null in PRESET when far off
    val centsOffTarget: Double? = null,               // Raw cents, [-1200, +1200], null when no detection
    val status: TuningStatus = TuningStatus.IDLE,
    val tunedStringIndices: Set<Int> = emptySet(),
    val referencePitchHz: Double = 440.0,
)
```

`detectedNote` deserves explanation: in `PRESET` mode it is the *nearest* note to whatever the user is playing (computed via `MusicTheory.frequencyToNote`), so the UI's detected-note hero can show "you're playing an F#" even when the target is E. This is the standard tuner behavior — you want to know what you're playing, not just whether it matches.

### `TunerEvent` (sealed interface) — one-shot effects

Some events are too transient or too "fire and forget" to fit in `TunerUiState`. The ViewModel exposes a `SharedFlow<TunerEvent>` for these:

```kotlin
sealed interface TunerEvent {
    data class StringTuned(val stringIndex: Int) : TunerEvent      // Haptic + fade-in mint ring
    object AllStringsTuned : TunerEvent                            // Drives the 1.2s success ring per DESIGN.md §8.1
    object EnteredChromaticMode : TunerEvent                       // For any chromatic-entry UI affordance
}
```

This is the only place `SharedFlow` is used; everything else is `StateFlow`. Single-event consumption (haptic, animation triggers) doesn't fit `StateFlow`'s replay semantics.

## Implementation Details

### `tuner/data/TunerPreferences.kt`

A small DataStore-backed component for tuner-specific persistence. Currently one field; designed to grow.

```kotlin
interface TunerPreferences {
    val lastUsedPresetId: Flow<String?>
    suspend fun setLastUsedPresetId(id: String)
}

class TunerPreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : TunerPreferences {
    private val Context.dataStore by preferencesDataStore(name = "tuner_preferences")

    override val lastUsedPresetId: Flow<String?> = context.dataStore.data
        .map { it[LAST_USED_PRESET_ID_KEY] }

    override suspend fun setLastUsedPresetId(id: String) {
        context.dataStore.edit { it[LAST_USED_PRESET_ID_KEY] = id }
    }

    private companion object {
        val LAST_USED_PRESET_ID_KEY = stringPreferencesKey("last_used_preset_id")
    }
}
```

Bound via Hilt in `tuner/di/TunerModule.kt`.

> **New dependency.** Add `androidx.datastore:datastore-preferences` to `libs.versions.toml`. Confirm the latest stable version at implementation time. Record the addition in `DECISIONS.md`.

### `tuner/domain/usecase/DetectTunedStringUseCase.kt`

The heart of 5.3. Phase 2 was a stub. Real implementation now:

```kotlin
class DetectTunedStringUseCase @Inject constructor(
    private val microphoneAudioSource: MicrophoneAudioSource,
    private val pitchDetector: PitchDetector,
) {
    /**
     * Returns a flow of pipeline events for the given tuning configuration.
     * The flow runs the microphone, runs the detector, runs the sustained-tone
     * state machine, and emits events the ViewModel can map into UI state.
     *
     * Lifetime is bound to the collector's scope.
     */
    fun execute(input: TunerInput): Flow<DetectionEvent>
}

data class TunerInput(
    val mode: TunerMode,
    val targetNote: Note?,                  // null in chromatic mode
    val referencePitchHz: Double,
)

sealed interface DetectionEvent {
    object Listening : DetectionEvent
    data class Detection(
        val detectedFrequencyHz: Double,
        val detectedNote: Note?,             // From MusicTheory.frequencyToNote
        val targetNote: Note,                // In CHROMATIC, this equals detectedNote (rounded)
        val targetFrequencyHz: Double,
        val centsOff: Double,
        val isSustainedInTune: Boolean,
    ) : DetectionEvent
    object PermissionDenied : DetectionEvent
    data class Failed(val reason: String) : DetectionEvent
}
```

The use case body:
1. Collect from `microphoneAudioSource.samples()`.
2. Map `CaptureEvent.PermissionDenied` → `DetectionEvent.PermissionDenied`. Map `CaptureEvent.Failed` → `DetectionEvent.Failed`. Map `CaptureEvent.Listening` → `DetectionEvent.Listening`.
3. For each `CaptureEvent.Samples`:
   - Call `pitchDetector.detectPitch(buffer, sampleRateHz = 44100)`.
   - If `null`: feed `null` into the sustained-tone window (counts as out-of-tolerance) and don't emit a `Detection` event (UI stays on `LISTENING`).
   - If a frequency: resolve `targetNote`:
     - `PRESET` mode: use `input.targetNote!!`
     - `CHROMATIC` mode: call `MusicTheory.frequencyToNote(detectedHz, input.referencePitchHz)`. If that returns `null`, skip emission for this buffer (treat as detection failure).
   - Compute `centsOff = MusicTheory.centsBetween(targetHz, detectedHz)`.
   - Feed `abs(centsOff) <= 5.0` into the sustained-tone window.
   - Emit `Detection(...)` with the sustained-in-tune boolean.

Sustained-tone window is implemented as an `ArrayDeque<Boolean>` of capacity `SUSTAINED_WINDOW_SIZE`. Each new sample pushes a boolean; capacity is enforced by removing from the front. `isSustainedInTune` is `count { it } >= SUSTAINED_MIN_IN_TOLERANCE` AND `size == SUSTAINED_WINDOW_SIZE`. (Don't trigger before the window is full — would create a false positive on the very first in-tune detection.)

The use case is **stateless across `execute(input)` calls**. Each call returns a fresh flow with a fresh sustained-tone window. The ViewModel restarts the flow whenever target changes (new string, new preset, mode switch).

### `tuner/presentation/viewmodel/TunerViewModel.kt`

Phase 2 had a stub. Real implementation now.

Constructor-injected dependencies:
- `TunerPresetRepository` (5.1)
- `TunerPreferences` (5.3, new)
- `DetectTunedStringUseCase` (5.3)

The ViewModel exposes:
- `val uiState: StateFlow<TunerUiState>`
- `val events: SharedFlow<TunerEvent>`

Public functions (actions the UI dispatches):
- `fun onPresetSelected(presetId: String)` — switches preset, resets to preset mode at string 0.
- `fun onStringSelected(stringIndex: Int)` — jumps to a specific string in preset mode (or re-enters preset mode from chromatic).

The ViewModel does **not** expose a `start()` / `stop()` — capture activity is purely lifecycle-driven via `collectAsStateWithLifecycle` in the screen (5.4).

#### Initialization

On construction:
1. Load presets via `TunerPresetRepository.getPresetsGrouped()` (suspend; use `viewModelScope.launch`).
2. Read `TunerPreferences.lastUsedPresetId.first()` (suspending).
3. Resolve the preset: `repository.getPresetById(savedId) ?: repository.getPresetById("six_string_standard_e")`.
4. Update `uiState` to reflect the loaded preset at string index 0.

#### Pipeline wiring

The detection flow is built and exposed as a hot `StateFlow<TunerUiState>` whose upstream is `flatMapLatest`'d over the current `TunerInput`. Pattern:

```kotlin
private val tunerInput = MutableStateFlow<TunerInput?>(null)

private val pipelineEvents = tunerInput
    .flatMapLatest { input ->
        if (input == null) flowOf(DetectionEvent.Listening) // no-op while idle
        else useCase.execute(input)
    }

// Combined into uiState via stateIn(viewModelScope, WhileSubscribed(5000), initial)
```

The `WhileSubscribed(5000)` keeps the upstream running for 5 seconds after the last collector unsubscribes — guards against momentary detachment (e.g., screen rotation). Critically: when `tunerInput` is null (no preset selected) and there are no subscribers, no audio source is collected → no microphone is active. This is what makes the "preset-selected guard" of the listening rule work mechanically.

#### Mapping `DetectionEvent` to `TunerUiState`

For each event, update the corresponding `TunerUiState` fields:
- `Listening` → `status = LISTENING`, clear detection fields.
- `Detection(...)` with `isSustainedInTune = false` → derive status: `FLAT` if `centsOff < -5`, `SHARP` if `centsOff > 5`, else `LISTENING` (in-tolerance but not yet sustained).
- `Detection(...)` with `isSustainedInTune = true` → `status = IN_TUNE`. **Add `currentStringIndex` to `tunedStringIndices`.** Emit `TunerEvent.StringTuned(currentStringIndex)`. Schedule auto-advance after 200 ms.
- `PermissionDenied` → `status = PERMISSION_DENIED`, clear detection fields.
- `Failed` → `status = CAPTURE_FAILED`, clear detection fields.

#### Auto-advance logic

When `isSustainedInTune` fires:
1. Add the index to `tunedStringIndices` immediately (UI shows the check mark).
2. Emit `TunerEvent.StringTuned(index)`.
3. After 200 ms (`viewModelScope.launch { delay(STRING_LOCK_HOLD_MS); ... }`):
   - If there is a next string in the preset: increment `currentStringIndex`, update `targetNote` / `targetFrequencyHz`, restart the use case with the new target.
   - If this was the last string: emit `status = ALL_STRINGS_TUNED`, emit `TunerEvent.AllStringsTuned`, after 1200 ms transition to chromatic mode (`mode = CHROMATIC`, clear `tunedStringIndices`, emit `TunerEvent.EnteredChromaticMode`).

Constants: `STRING_LOCK_HOLD_MS = 200`, `ALL_TUNED_HOLD_MS = 1200`.

#### Persistence side-effect

When `onPresetSelected` is invoked, fire-and-forget `viewModelScope.launch { preferences.setLastUsedPresetId(id) }` after updating the state. Don't `await` — UI shouldn't block on disk.

### `tuner/di/TunerModule.kt`

Update existing module with the new bindings:
- `TunerPreferences` → `TunerPreferencesImpl`

The Phase 2 bindings for `TunerPresetRepository`, `PitchDetector`, and `MicrophoneAudioSource` (from 5.2) all stay.

## Tests

### `DetectTunedStringUseCaseTest`

The use case is a pure function from `(TunerInput, Flow<CaptureEvent>) → Flow<DetectionEvent>`. Tests inject a fake `MicrophoneAudioSource` (a hand-written test double, per `IMPLEMENTATION_NOTES.md` preference for fakes over mocks at repository boundaries) and a fake `PitchDetector` whose `detectPitch` can be programmed.

Test cases:
- **Permission denied propagates.** Fake source emits `PermissionDenied`. Use case emits `DetectionEvent.PermissionDenied` once and completes.
- **Listening propagates.** Fake source emits `Listening`. Use case emits `DetectionEvent.Listening` once.
- **In-tune in PRESET mode after 5/6 windows.** Feed 6 buffers with detector returning frequencies all within ±5 cents. Sixth emission is `Detection(isSustainedInTune = true)`. First five are `isSustainedInTune = false`.
- **Glitch budget.** Feed 5 in-tolerance + 1 out-of-tolerance (cents = 20). Sixth emission's `isSustainedInTune` is still `true` (5 of 6 in tolerance).
- **Two glitches reset.** Feed 4 in-tolerance + 2 out-of-tolerance. None of the emissions have `isSustainedInTune = true` until 5 of any 6 in the rolling window are in tolerance again.
- **Null detection counts as out-of-tolerance.** Feed 5 in-tolerance + 1 null buffer (detector returns `null`). The null buffer is **not** emitted (UI stays on LISTENING for that frame). Window still records "out". Next in-tolerance hit produces `isSustainedInTune = true` (since the window is back to 5 of 6).
- **Two consecutive nulls reset window.** Feed 4 in-tolerance + 2 consecutive nulls. After the nulls, the next detection has `isSustainedInTune = false`.
- **Chromatic mode resolves target.** Set `TunerInput(mode = CHROMATIC, targetNote = null, ...)`. Detector emits 196.0 Hz (≈ G3). Use case emits `Detection` with `targetNote = G3`, `targetFrequencyHz ≈ 196.0` Hz, and the correct cents.
- **Chromatic with frequencyToNote returning null is skipped.** Detector emits 0.5 Hz (out of MusicTheory range). Use case emits nothing for that frame.
- **Raw cents are not clamped.** Feed a 100 cents flat detection. Emitted `centsOff = -100.0` (not −50.0).
- **Capture failure propagates.** Fake source emits `Failed("…")`. Use case emits `DetectionEvent.Failed`.

### `TunerViewModelTest`

Inject fake repository, fake preferences, and fake use case. Test the ViewModel's state transitions in isolation from real detection.

Test cases:
- **Initial state loads default preset on first launch.** Preferences returns `null` from `lastUsedPresetId`. Initial `uiState.selectedPreset.id == "six_string_standard_e"`.
- **Initial state restores last-used preset.** Preferences returns `"six_string_drop_d"`. Initial `uiState.selectedPreset.id == "six_string_drop_d"`.
- **Initial state falls back if persisted ID no longer exists.** Preferences returns `"old_preset_that_was_removed"`. Initial `uiState.selectedPreset.id == "six_string_standard_e"`.
- **`onPresetSelected` updates state and persists.** Calling `onPresetSelected("seven_string_standard_b")` results in `uiState.selectedPreset.id == "seven_string_standard_b"`, `currentStringIndex = 0`, `tunedStringIndices.isEmpty()`, `mode = PRESET`. Fake preferences received `setLastUsedPresetId("seven_string_standard_b")`.
- **`onPresetSelected` with the currently selected preset still resets the session.** Re-selecting the same preset clears `tunedStringIndices` and resets to string 0.
- **`onStringSelected` jumps target and re-arms preset mode from chromatic.** From `mode = CHROMATIC`, calling `onStringSelected(2)` flips `mode = PRESET`, `currentStringIndex = 2`, `tunedStringIndices = emptySet()`.
- **Sustained-in-tune triggers auto-advance.** Use case emits `Detection(isSustainedInTune = true)`. After `STRING_LOCK_HOLD_MS`, `currentStringIndex` incremented and the use case is restarted with the new target. `TunerEvent.StringTuned(0)` is emitted exactly once.
- **Auto-advance on last string triggers ALL_STRINGS_TUNED.** When the last string locks, `status = ALL_STRINGS_TUNED`, `TunerEvent.AllStringsTuned` emitted, after `ALL_TUNED_HOLD_MS` the state flips to `mode = CHROMATIC`, `tunedStringIndices = emptySet()`, and `TunerEvent.EnteredChromaticMode` is emitted.
- **Permission denied surfaces in state.** Use case emits `PermissionDenied`. `status = PERMISSION_DENIED`. Capture-related fields cleared.
- **Capture failure surfaces in state.** Use case emits `Failed`. `status = CAPTURE_FAILED`.
- **`tunedStringIndices` reset paths.** Verify that the set is reset on: preset change, manual string selection, mode transition to chromatic.

Tests use `kotlinx-coroutines-test` (`runTest`, `TestDispatcher`, virtual time) — already a transitive of the Compose/lifecycle test artifacts.

### `TunerPreferencesImplTest`

Verifying DataStore behavior typically requires a `Context`, putting it in `androidTest/`. For 5.3, the easier route:
- The **interface** `TunerPreferences` is unit-testable trivially via a fake.
- The **implementation** `TunerPreferencesImpl` is a thin DataStore wrapper. Skipped at the unit-test level; covered by the ViewModel integration smoke test on a real device in 5.4.

Document this in a top-of-file comment on `TunerPreferencesImplTest` if you stub the file out at all, or just don't create it. (Personal preference: don't create empty test files. The fake is what matters.)

## Steps

1. Add `androidx.datastore:datastore-preferences` to `libs.versions.toml`.
2. Create `tuner/data/TunerPreferences.kt` (interface) and `TunerPreferencesImpl.kt`.
3. Update `TuningStatus` enum to add `PERMISSION_DENIED` and `CAPTURE_FAILED`. Record in `DECISIONS.md`.
4. Create `tuner/domain/model/TunerMode.kt` (enum).
5. Refactor `TunerUiState` per the spec above (new fields, defaults). The Phase 2 fields stay; new ones are added. Record the shape change in `DECISIONS.md`.
6. Create `tuner/domain/model/TunerInput.kt` and `tuner/domain/usecase/DetectionEvent.kt`.
7. Replace the Phase 2 stub `DetectTunedStringUseCase.kt` with the real implementation.
8. Create `tuner/presentation/viewmodel/TunerEvent.kt` (sealed interface).
9. Replace the Phase 2 stub `TunerViewModel.kt` with the real implementation.
10. Update `tuner/di/TunerModule.kt` to bind `TunerPreferences`.
11. Write the test suite (`DetectTunedStringUseCaseTest`, `TunerViewModelTest`, plus fakes: `FakeMicrophoneAudioSource`, `FakePitchDetector`, `FakeTunerPresetRepository`, `FakeTunerPreferences`).
12. Update `DECISIONS.md` with: sustained-tone rule (1-of-6 glitch budget), auto-advance hold duration (200 ms), all-tuned hold duration (1200 ms), default preset on first launch, DataStore dependency, two-mode operation (preset/chromatic), and the precise re-entry rules from chromatic to preset mode.
13. Hand off to the user with a summary.

## Completion Criteria

See `Phase5.3-REQUIREMENTS.md`.
