# Phase 8.3 — ViewModel, State, Seeding & Tuning Source

## Goal

Wire Stage 1 and Stage 2 into reactive state, persist the user's selection, seed the initial selection from Key Finder's top result, and make Chord Finder respect the tuner's current tuning. By the end of 8.3 the module is fully functional **without** its real screens (drivable from tests): selecting root/mode/toggle produces the chord list; selecting a chord produces its voicings for the active tuning; the seed and persistence rules hold.

## Scope

- `common/state/LatestKeyResultStore.kt` — app-scoped holder of Key Finder's top `ScaleMatch`.
- `common/state/SelectedTuningStore.kt` — app-scoped holder of the current `GuitarTuning`.
- Minimal **additive** writes: `KeyFinderViewModel` publishes its top result; the tuner publishes its selected tuning (via a preset→`GuitarTuning` mapper).
- `chordfinder/data/ChordFinderSelectionRepository.kt` — persists `{rootPitchClass, scaleType, includeSeventhChords, hasUserSelection}`.
- `chordfinder/presentation/viewmodel/ChordFinderViewModel.kt` + `ChordFinderUiState.kt` — list + selection + seed.
- `chordfinder/presentation/viewmodel/ChordVoicingsViewModel.kt` + `ChordVoicingsUiState.kt` — voicings for a selected chord + active tuning.
- `chordfinder/di/ChordFinderModule.kt` — bindings.
- Unit tests for all logic.

## Out of Scope

- Composables, navigation routes → 8.4 / 8.5.
- Voicing computation/transform itself (done in 8.2; consumed here).

## Reading Order Before Starting

1. `Phase8-PLAN.md` → "The Tuning Source", "Key Finder → Chord Finder Seeding"
2. `Phase8_1-PLAN.md`, `Phase8_2-PLAN.md` (the engines this composes)
3. The Phase 6 metronome persistence decision in `DECISIONS.md` (reuse that persistence mechanism — likely DataStore Preferences)
4. The Phase 7.3/7.4 Key Finder ViewModel for state-shape precedent
5. This file

## Decisions Locked In For 8.3

- ✅ **Seed once, then user-owned**, exactly per `Phase8-PLAN.md`.
- ✅ **Two app-scoped in-memory stores** (`@Singleton`), `StateFlow`-based; Key Finder and the tuner are the writers, Chord Finder the reader.
- ✅ **Selection persisted** with the same mechanism the metronome uses; a `hasUserSelection` flag distinguishes "seedable" from "user-owned".
- ✅ **Tuning inherited** from the tuner (read-only this phase; no in-Chord-Finder picker — deferred to an 8.5 decision).
- ✅ **Two ViewModels** (list, voicings) for SRP.

## Implementation Details

### `common/state/LatestKeyResultStore.kt`

```kotlin
@Singleton
class LatestKeyResultStore @Inject constructor() {
    private val _topResult = MutableStateFlow<ScaleMatch?>(null)
    val topResult: StateFlow<ScaleMatch?> = _topResult.asStateFlow()
    fun publish(top: ScaleMatch?) { _topResult.value = top }
}
```
`KeyFinderViewModel` calls `publish(results.firstOrNull())` whenever it recomputes results. This is the only edit to Key Finder; it does not change Key Finder's own behaviour.

### `common/state/SelectedTuningStore.kt`

```kotlin
@Singleton
class SelectedTuningStore @Inject constructor() {
    private val _tuning = MutableStateFlow(GuitarTuning.STANDARD_6)
    val tuning: StateFlow<GuitarTuning> = _tuning.asStateFlow()
    fun publish(tuning: GuitarTuning) { _tuning.value = tuning }
}
```
The tuner publishes its active preset, mapped to `GuitarTuning` by a `TuningPresetMapper` (lives in the tuner module, since it knows preset internals; outputs the shared `common` `GuitarTuning`). Default before the tuner is touched is `STANDARD_6`. This makes the tuner the second consumer of `GuitarTuning` — the reason it lives in `common/` (recorded).

### `chordfinder/data/ChordFinderSelectionRepository.kt`

Interface in domain, impl in data, backed by the metronome's persistence mechanism. Stores `rootPitchClass: Int`, `scaleType: ScaleType` (by stable name), `includeSeventhChords: Boolean`, `hasUserSelection: Boolean`. Debounced writes if the mechanism warrants.

### `ChordFinderViewModel` + `ChordFinderUiState`

```kotlin
data class ChordFinderUiState(
    val rootPitchClass: Int,
    val scaleType: ScaleType,
    val includeSeventhChords: Boolean,
    val title: String,                 // "{Root} {ModeLabel}" via ScaleSpeller + ScaleType label
    val chords: List<DegreeChord>,
)
```
Construction / first collection — **the seed algorithm**:
1. If `repository.hasUserSelection` → load persisted `{root, type, sevenths}`.
2. Else if `latestKeyResultStore.topResult.value != null` → seed `root = top.candidate.rootPitchClass`, `type = top.candidate.type`, `sevenths = false`.
3. Else → `A` (pitch class 9) + `ScaleType.AEOLIAN`, `sevenths = false`.

Intents: `setRoot`, `setScaleType`, `toggleSevenths`, `selectChord(degreeChord)`. Any of the first three → recompute chords via `FindChordsUseCase`, persist, set `hasUserSelection = true`. The seed reads `topResult` **once** at init; later Key Finder changes never override a user-owned selection. `selectChord` exposes the chosen `ChordKey` + display name for navigation (consumed in 8.5).

### `ChordVoicingsViewModel` + `ChordVoicingsUiState`

Created per selected chord (nav arg: `ChordKey` + display name). Reads `selectedTuningStore.tuning`, calls `voicingRepository.lookup(chordKey, tuning)` (off the main thread), maps to UI state:

```kotlin
data class ChordVoicingsUiState(
    val chordName: String,
    val noteNames: List<String>,       // root flagged for mint pill
    val tuningLabel: String,
    val tier: VoicingTier,             // STANDARD | UNIFORM_OFFSET | UNSUPPORTED
    val voicings: List<Voicing>,
    val offsetSemitones: Int?,         // when UNIFORM_OFFSET
)
```
For `Unsupported` (tier 3), the v1 fallback is to surface `tier = UNSUPPORTED` and still expose the **standard** voicings with a "shown for standard tuning" note (the screen decides presentation in 8.5).

### `ChordFinderModule`

Provide/bind: `FindChordsUseCase`, `VoicingRepository`, `ChordFinderSelectionRepository`, and the two `@Singleton` stores. Ensure the stores are app-scoped singletons shared with Key Finder and the tuner.

## Tests (MockK + hand-written fakes)

### `ChordFinderViewModelTest`
- **Seed from Key Finder:** no persisted selection + store top = {root=2 (D), DORIAN} → state seeds D Dorian; chords match `FindChordsUseCase` for that input.
- **Seed fallback:** no persisted + store empty → A Aeolian.
- **Persisted wins:** `hasUserSelection = true` with stored {root=7 (G), LYDIAN} → state uses it, ignores a non-null store top.
- **Seed reads once:** after seeding from the store, a later `publish` on the store does **not** change state.
- **User edit is sticky:** `setRoot`/`setScaleType` persists, sets `hasUserSelection`, and a subsequent store `publish` is ignored.
- **Toggle sevenths** rebuilds chords (triad vs seventh symbols/notes).
- **Title** formatting via `ScaleSpeller` + scale-type label.

### `ChordVoicingsViewModelTest`
- Standard tuning → `tier = STANDARD`, voicings passed through.
- Δ=−1 tuning → `tier = UNIFORM_OFFSET`, `offsetSemitones = -1`, voicings transformed (fake repo returns `UniformOffset`).
- Drop D → `tier = UNSUPPORTED`, standard voicings still exposed with the flag.
- Note names expose the root for mint highlighting.

### Store + writer tests
- `LatestKeyResultStore.publish` updates `topResult`.
- `KeyFinderViewModel` publishes `results.firstOrNull()` on recompute (parity test: Key Finder behaviour otherwise unchanged).
- `SelectedTuningStore.publish` updates `tuning`; `TuningPresetMapper` maps a known preset to the correct `GuitarTuning` (e.g. E♭ standard → Δ=−1 from `STANDARD_6`).

## Steps

1. The two stores. 2. Key Finder publish hook + parity test. 3. `TuningPresetMapper` + tuner publish hook + test. 4. `ChordFinderSelectionRepository` (+ impl on the metronome's persistence). 5. `ChordFinderViewModel` + seed algorithm + tests. 6. `ChordVoicingsViewModel` + tests. 7. `ChordFinderModule` bindings. 8. Append 8.3 decisions. 9. Hand off.

## Completion Criteria

See `Phase8_3-REQUIREMENTS.md`.
