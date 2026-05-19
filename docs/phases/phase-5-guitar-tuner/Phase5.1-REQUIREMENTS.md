# Phase 5.1 — Requirements & Acceptance Criteria

Phase 5.1 produces no user-visible functionality. Every requirement below is verifiable in the code or by running unit tests. The phase is complete when both the agent checklist and the user checklist pass.

## Agent Responsibilities

### `common/model/NoteName.kt`

- [ ] Enum has all 12 values: `C, CSharp, D, DSharp, E, F, FSharp, G, GSharp, A, ASharp, B`.
- [ ] `val semitonesFromC: Int` returns 0..11 in order.
- [ ] `val sharpName: String` returns the sharp-spelled display string.
- [ ] `val flatName: String` returns the flat-spelled display string. Identical to `sharpName` for natural notes; differs for the five accidentals.
- [ ] `companion object fun parse(input: String): NoteName?` accepts sharp and flat spellings, is case-insensitive, trims whitespace, returns `null` for invalid input.

### `common/model/Note.kt`

- [ ] `Note(name: NoteName, octave: Int)` data class with no other constructor params.
- [ ] `fun frequencyHz(referencePitchHz: Double = 440.0): Double` returns the equal-tempered frequency.
- [ ] `fun displayName(useFlats: Boolean = false): String` returns the note name + octave (e.g. `"C#4"`, `"Db4"`).
- [ ] `companion object fun parse(input: String): Note?` accepts any string `displayName` could produce; returns `null` for invalid input.
- [ ] `frequencyHz` is accurate to within 0.01 Hz of standard reference values at A4 = 440 Hz (verified by test).

### `common/model/Interval.kt`

- [ ] `Interval(semitones: Int)` data class.
- [ ] Companion object exposes named constants: `UNISON, MINOR_SECOND, MAJOR_SECOND, MINOR_THIRD, MAJOR_THIRD, PERFECT_FOURTH, TRITONE, PERFECT_FIFTH, MINOR_SIXTH, MAJOR_SIXTH, MINOR_SEVENTH, MAJOR_SEVENTH, OCTAVE`.

### `common/model/Mode.kt`

- [ ] Enum has all 7 diatonic modes: `Ionian, Dorian, Phrygian, Lydian, Mixolydian, Aeolian, Locrian`.
- [ ] Each carries `intervalsFromRoot: IntArray` matching the standard semitone pattern.
- [ ] Each has a `displayName: String` property — `Ionian` displays as `"Ionian (Major)"`, `Aeolian` as `"Aeolian (Natural Minor)"`, others as their bare name.

### `common/model/ChordQuality.kt`

- [ ] Enum values: `MAJOR, MINOR, DIMINISHED, AUGMENTED, MAJOR_SEVENTH, MINOR_SEVENTH, DOMINANT_SEVENTH, HALF_DIMINISHED, DIMINISHED_SEVENTH`.
- [ ] `intervalsFromRoot: IntArray` correct for every value.
- [ ] `symbol: String` returns the conventional chord-notation modifier (empty string for `MAJOR`, `"m"` for `MINOR`, `"maj7"`, `"m7"`, `"7"`, `"m7♭5"`, etc.).

### `common/model/Scale.kt`

- [ ] `Scale(root: Note, mode: Mode)` — the Phase 2 `intervals` field is removed. Change recorded in `DECISIONS.md`.
- [ ] `val notes: List<Note>` is derived from `root` and `mode`, with correct octave wrapping (notes whose pitch class would be below the previous one rise an octave).

### `common/model/Chord.kt`

- [ ] `Chord(root: Note, quality: ChordQuality)` — the Phase 2 explicit `notes` field is removed; it is now a derived val.
- [ ] `val notes: List<Note>` is derived from `root` + `quality.intervalsFromRoot`.
- [ ] `fun displayName(): String` returns `"${root.displayName()}${quality.symbol}"`.

### `common/util/MusicTheory.kt`

- [ ] `noteToFrequency(note: Note, referencePitchHz: Double = 440.0): Double` — implemented.
- [ ] `frequencyToNote(frequencyHz: Double, referencePitchHz: Double = 440.0): Note?` — implemented. Uses sharp spelling. Returns `null` for `<= 0`, `NaN`, `Infinity`, or pitches outside C0–B9.
- [ ] `centsBetween(referenceFrequencyHz: Double, detectedFrequencyHz: Double): Double` — new method, implemented. (Interface addition recorded in `DECISIONS.md`.)
- [ ] `buildScale(root: Note, mode: Mode): Scale` — implemented (trivial delegating call; documented).
- [ ] `buildTriads(scale: Scale): List<Chord>` — implemented; returns 7 chords, one per scale degree.
- [ ] `buildSeventhChords(scale: Scale): List<Chord>` — implemented; returns 7 diatonic seventh chords. For C Ionian this is `[Cmaj7, Dm7, Em7, Fmaj7, G7, Am7, Bm7♭5]`.

### `tuner/domain/model/TunerCategory.kt`

- [ ] Existing enum unchanged: `STANDARD, OPEN, DROPPED`.
- [ ] `val urlSlug: String` added.

### `tuner/data/TunerPresets.kt`

- [ ] Single file holding the full catalog from `APP_SPECIFICATION.md` — every 6/7/8-string Standard/Open/Dropped preset is present.
- [ ] All preset IDs follow the `<stringcount>_<category>_<descriptor>` snake_case pattern and are globally unique.
- [ ] `TunerPresets.all: List<TunerPreset>` and `TunerPresets.grouped: Map<Int, Map<TunerCategory, List<TunerPreset>>>` are both exposed.
- [ ] Each preset's `notes.size == stringCount`.
- [ ] Each preset's notes are listed lowest-string-first.

### `tuner/domain/repository/TunerPresetRepository.kt`

- [ ] Interface gains `suspend fun getPresetsGrouped(): Map<Int, Map<TunerCategory, List<TunerPreset>>>`. Change recorded in `DECISIONS.md`.
- [ ] Existing `getPresets()` and `getPresetById(id)` signatures unchanged.

### `tuner/data/TunerPresetRepositoryImpl.kt`

- [ ] Replaces the Phase 2 `TODO()` body. Real returns for all three methods.
- [ ] Continues to be the Hilt binding target for `TunerPresetRepository`.

### Tests

- [ ] `common/model/` — at least one test class per type covering the cases listed in `Phase5.1-PLAN.md` "Tests" section. All round-trip and reference-value checks pass.
- [ ] `MusicTheory*Test` — frequency math, cents math, scale building, triad building, seventh-chord building all tested per the canonical examples in `Phase5.1-PLAN.md`.
- [ ] `TunerPresetsTest` — ID uniqueness, note-count consistency, grouped-map structure all asserted.
- [ ] `TunerPresetRepositoryImplTest` — `getPresetById` round-trips, unknown ID returns `null`, `getPresetsGrouped()` matches the static catalog.

### Documentation Updates

- [ ] `DECISIONS.md` gains entries (one per decision, dated, append-only) for:
  - Seventh-chord harmonization is diatonic.
  - `frequencyToNote` defaults to sharp spelling; context-aware deferred.
  - `centsBetween` added to the `MusicTheory` interface.
  - `getPresetsGrouped()` added to `TunerPresetRepository`.
  - `Scale` no longer carries an explicit `intervals` field.
  - `Chord` no longer carries an explicit `notes` field.
- [ ] `FUTURE_IMPROVEMENTS.md` exists at the repo root and contains an entry titled "Context-aware enharmonic spelling for frequencyToNote" describing the future change.

### Code Quality

- [ ] No `TODO("Not yet implemented")` remains in any 5.1-touched file.
- [ ] All public types and methods have KDoc comments.
- [ ] No magic numbers in the math — A4 reference, octave size (12), cents-per-octave (1200), etc. are named constants either in `MusicTheory` or in a `common/util/MusicTheoryConstants.kt` companion.
- [ ] No hardcoded note names as strings inside any algorithm — algorithms work in semitone offsets and convert to `Note` at the boundary.

### Handoff

- [ ] Summary message to the user lists files added, modified, removed, and anything to double-check after Gradle sync (e.g. "the `TunerPresetRepository` interface signature changed — Hilt should re-bind automatically, but verify in Logcat that no DI error occurs on app launch").

## User Responsibilities (Verification in Android Studio)

- [ ] After applying the changes, **File → Sync Project with Gradle Files** completes without errors.
- [ ] **Build → Make Project** completes successfully.
- [ ] **Run → Run All Tests** (or `./gradlew test`) reports all tests green.
- [ ] App still launches on an Android 12+ emulator/device. The tuner placeholder screen from Phase 4 still appears (5.1 produces no UI changes; this is a sanity check that the data-layer changes haven't broken DI or app startup).
- [ ] No exceptions in Logcat during launch — particularly no Hilt binding errors.

## Decision Log

- [ ] All decisions listed under "Documentation Updates" are recorded in `DECISIONS.md` before the phase is marked complete.
