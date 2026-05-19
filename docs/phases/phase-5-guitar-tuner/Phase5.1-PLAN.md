# Phase 5.1 — Tuning Data & Music Theory

## Goal

Implement the foundation layer for the Guitar Tuner: the music-theory math, the `Note`/`Scale`/`Chord`/`Mode` types fleshed out with real behaviour, and the full preset catalog. This is **pure Kotlin** — no Android dependencies, no audio, no UI, no ViewModel work. Everything in this sub-phase is unit-testable in isolation.

By the end of 5.1, the tuner's data and theory layer is complete and tested. Phases 5.2 (pitch detection), 5.3 (ViewModel), and 5.4 (UI) consume what 5.1 produces — they should never need to add a method to `MusicTheory` or change a preset.

## Scope

- Flesh out the `common/model/` types (`Note`, `Interval`, `Scale`, `Chord`, `Mode`, `NoteName`, `ChordQuality`) so derived properties actually compute.
- Implement every method on the `MusicTheory` object (currently `TODO()` stubs from Phase 2).
- Implement the full preset catalog from `APP_SPECIFICATION.md` as hardcoded Kotlin constants.
- Implement `TunerPresetRepositoryImpl` against those constants.
- Comprehensive unit tests for everything above.

## Out of Scope

- Anything involving `AudioRecord`, microphones, or audio processing → Phase 5.2.
- `PitchDetector` is left as a stub from Phase 2 → Phase 5.2.
- `DetectTunedStringUseCase` and `TunerViewModel` are left as stubs from Phase 2 → Phase 5.3.
- `TunerScreen` UI work → Phase 5.4.
- Context-aware enharmonic spelling (sharps vs. flats based on key context) → tracked in `FUTURE_IMPROVEMENTS.md`.

## Reading Order Before Starting

1. `APP_SPECIFICATION.md` → "Module: Guitar Tuner" (full preset list, reference pitch, tolerance)
2. `IMPLEMENTATION_NOTES.md` → "Music Theory Primitives"
3. `DECISIONS.md` → all entries (especially the 2026-05-17 reference-pitch and preset-storage decisions)
4. `Phase5-PLAN.md` → "Decisions Already Resolved" and the 5.1 open questions
5. This file

## Decisions Locked In For 5.1

These are settled before implementation begins:

- ✅ **Preset exposure:** Pre-grouped — `Map<Int, Map<TunerCategory, List<TunerPreset>>>` where the outer key is `stringCount`. The repository also exposes a flat `getPresetById(id)` lookup.
- ✅ **Preset storage location:** A single file `tuner/data/TunerPresets.kt` holds the full catalog.
- ✅ **Seventh-chord harmonization:** Diatonic 7ths — each scale degree gets its musically correct seventh quality (Cmaj7, Dm7, Em7, Fmaj7, G7, Am7, Bm7♭5 for C Ionian, etc.).
- ✅ **Preset ID format:** Structural — `<stringcount>_<category>_<descriptor>` in snake_case (e.g. `six_string_standard_e`, `six_string_drop_d`, `seven_string_open_b`, `eight_string_drop_d`).
- ✅ **Enharmonic spelling for `frequencyToNote`:** Sharp spelling (`C#`, `D#`, `F#`, `G#`, `A#`). Context-aware spelling deferred — see `FUTURE_IMPROVEMENTS.md`.
- ✅ **Out-of-range frequencies:** `frequencyToNote()` returns `null` for frequencies that don't map to a sensible Note (silent / non-musical input).

## Implementation Details

### `common/model/` — flesh out the types

The Phase 2 versions are stubs (data classes with `TODO()` in derived properties). For 5.1, every member of every type below must have a real, tested implementation.

#### `NoteName` enum

Stays as the 12-value enum from Phase 2 (`C`, `CSharp`, `D`, `DSharp`, `E`, `F`, `FSharp`, `G`, `GSharp`, `A`, `ASharp`, `B`). Add:

- `val semitonesFromC: Int` — `C` = 0, `CSharp` = 1, …, `B` = 11.
- `val sharpName: String` — the sharp-spelled display string (`"C"`, `"C#"`, `"D"`, …).
- `val flatName: String` — the flat-spelled display string (`"C"`, `"Db"`, `"D"`, `"Eb"`, …). For the seven natural notes the two are identical.
- `companion object fun parse(input: String): NoteName?` — accepts both spellings (`"C#"`, `"Db"`, case-insensitive, trims whitespace). Returns `null` on invalid input.

#### `Note` data class

`name: NoteName, octave: Int`.

- `fun frequencyHz(referencePitchHz: Double = 440.0): Double` — real implementation using equal temperament: `f = referencePitchHz × 2^(semitonesFromA4 / 12)` where `semitonesFromA4` is computed from `name` and `octave` (A4 = 0).
- `fun displayName(useFlats: Boolean = false): String` — e.g. `"E2"`, `"C#4"`, or with flats `"Db4"`. Octave appended without separator.
- `companion object fun parse(input: String): Note?` — accepts `"E2"`, `"C#4"`, `"Db4"`, etc. Returns `null` on invalid input.

#### `Interval` data class

`semitones: Int`. Add the companion-object named constants from Phase 2 (`UNISON` through `OCTAVE` plus the standard internal intervals).

#### `Mode` enum

Each constant carries its diatonic interval pattern from the root as `IntArray` (7 values, each is the semitone offset of that scale degree from the tonic):

| Mode       | Pattern (semitones from tonic)       |
|------------|--------------------------------------|
| Ionian     | 0, 2, 4, 5, 7, 9, 11                 |
| Dorian     | 0, 2, 3, 5, 7, 9, 10                 |
| Phrygian   | 0, 1, 3, 5, 7, 8, 10                 |
| Lydian     | 0, 2, 4, 6, 7, 9, 11                 |
| Mixolydian | 0, 2, 4, 5, 7, 9, 10                 |
| Aeolian    | 0, 2, 3, 5, 7, 8, 10                 |
| Locrian    | 0, 1, 3, 5, 6, 8, 10                 |

Add a `displayName: String` property — e.g. `Ionian` → `"Ionian (Major)"`, `Aeolian` → `"Aeolian (Natural Minor)"`, the others use their bare mode name.

#### `Scale` data class

`root: Note, mode: Mode`. The `notes: List<Note>` field becomes a **derived val** (computed once, in `init` or as a `val notes = ...` initializer). Each scale degree note has the correct octave — start from `root.octave`, wrap up an octave when the next semitone offset crosses 12 from the previous one.

> Note: this is a refinement of Phase 2's `Scale(root, intervals, notes)`. The `intervals` field is removed because it's redundant with `mode`. If Phase 2 declared `intervals` as a constructor parameter, change the signature — record this as a non-trivial decision in `DECISIONS.md`.

#### `ChordQuality` enum

`MAJOR, MINOR, DIMINISHED, AUGMENTED, MAJOR_SEVENTH, MINOR_SEVENTH, DOMINANT_SEVENTH, HALF_DIMINISHED, DIMINISHED_SEVENTH`.

Add:
- `val intervalsFromRoot: IntArray` — semitone offsets of every chord tone from the root. E.g. `MAJOR = [0, 4, 7]`, `MINOR_SEVENTH = [0, 3, 7, 10]`, `HALF_DIMINISHED = [0, 3, 6, 10]`.
- `val symbol: String` — the appended modifier in chord notation: `MAJOR = ""`, `MINOR = "m"`, `DIMINISHED = "dim"`, `AUGMENTED = "aug"`, `MAJOR_SEVENTH = "maj7"`, `MINOR_SEVENTH = "m7"`, `DOMINANT_SEVENTH = "7"`, `HALF_DIMINISHED = "m7♭5"`, `DIMINISHED_SEVENTH = "dim7"`.

#### `Chord` data class

`root: Note, quality: ChordQuality`. The `notes: List<Note>` field becomes a derived val computed from `root` + `quality.intervalsFromRoot`, with octave wrapping as in `Scale`.

- `fun displayName(): String` — `"${root.displayName()}${quality.symbol}"`, e.g. `"Cmaj7"`, `"Dm7"`, `"Bm7♭5"`.

### `common/util/MusicTheory.kt` — implement every method

All five methods from Phase 2 get real implementations.

#### `noteToFrequency(note: Note, referencePitchHz: Double = 440.0): Double`
Delegates to `Note.frequencyHz(referencePitchHz)`. Kept as a top-level utility for the times when callers have a `Note` and don't want to think about which method to call.

#### `frequencyToNote(frequencyHz: Double, referencePitchHz: Double = 440.0): Note?`
Computes the nearest equal-tempered semitone to A4. Returns the resulting `Note` using **sharp spelling**.

Returns `null` when:
- `frequencyHz <= 0` (non-musical input)
- The computed octave is outside `[0, 9]` (covers C0 ≈ 16 Hz through B9 ≈ 15800 Hz; well beyond any guitar's range and any realistic mic input)
- `frequencyHz.isNaN() || frequencyHz.isInfinite()`

#### `centsBetween(referenceFrequencyHz: Double, detectedFrequencyHz: Double): Double`
**New method, not in Phase 2.** Computes the cents offset of `detectedFrequencyHz` from `referenceFrequencyHz` using `1200 × log2(detected / reference)`. Positive = sharp, negative = flat. Phase 5.3 needs this; cleaner to land it here with the rest of the music-theory math.

> If adding this changes the `MusicTheory` interface, record the addition in `DECISIONS.md`.

#### `buildScale(root: Note, mode: Mode): Scale`
Constructs a `Scale(root, mode)`. Since `Scale.notes` is derived, this is trivially `Scale(root, mode)` — but keep the method as the public-facing builder so callers don't import the data class directly. KDoc the equivalence.

#### `buildTriads(scale: Scale): List<Chord>`
Returns 7 `Chord` instances, one per scale degree. For each degree:
1. Take the degree's scale tone as the chord root.
2. Compute the third and fifth above it from the *scale's* notes (not chromatically) — i.e. skip every other scale tone.
3. Classify the resulting triad by counting semitones between root–third and third–fifth:
   - 4 + 3 → `MAJOR`
   - 3 + 4 → `MINOR`
   - 3 + 3 → `DIMINISHED`
   - 4 + 4 → `AUGMENTED` (won't occur in standard diatonic modes but include the case for robustness)

#### `buildSeventhChords(scale: Scale): List<Chord>`
Same as `buildTriads` but stacks one more scale-tone third on top of each triad. Classify by the four interval gaps:
- 4+3+4 → `MAJOR_SEVENTH`
- 3+4+3 → `MINOR_SEVENTH`
- 4+3+3 → `DOMINANT_SEVENTH`
- 3+3+4 → `HALF_DIMINISHED`
- 3+3+3 → `DIMINISHED_SEVENTH` (unused diatonically, included for completeness)

The result for C Ionian must be: `[Cmaj7, Dm7, Em7, Fmaj7, G7, Am7, Bm7♭5]`. This is the canonical test case.

### `tuner/domain/model/TunerCategory`

Phase 2 enum: `STANDARD, OPEN, DROPPED`. Add:
- `val urlSlug: String` — lower-snake form for ID generation (`"standard"`, `"open"`, `"dropped"`).

### `tuner/data/TunerPresets.kt`

A single file holding the entire catalog. Structure:

```kotlin
internal object TunerPresets {
    val all: List<TunerPreset> = listOf(
        // 6-string Standard
        preset("six_string_standard_e", "E Standard", 6, STANDARD, "E2 A2 D3 G3 B3 E4"),
        preset("six_string_standard_eb", "Eb Standard (Half Step Down)", 6, STANDARD, "Eb2 Ab2 Db3 Gb3 Bb3 Eb4"),
        // ...all entries from APP_SPECIFICATION.md...
    )

    val grouped: Map<Int, Map<TunerCategory, List<TunerPreset>>> =
        all.groupBy { it.stringCount }
           .mapValues { (_, list) -> list.groupBy { it.category } }
}
```

A private `preset(id, displayName, stringCount, category, notesSpec)` helper parses the space-separated notes spec via `Note.parse(...)`. Crash loudly (`error(...)`) if parsing fails — these are compile-time constants, a failure means a typo in the catalog and should never reach production.

**The catalog must include every tuning listed in `APP_SPECIFICATION.md`** — all 6-string, 7-string, and 8-string entries across Standard/Open/Dropped categories. No omissions.

### `tuner/data/TunerPresetRepositoryImpl`

Replace Phase 2's stub body with:
- `suspend fun getPresets(): List<TunerPreset>` → returns `TunerPresets.all`
- `suspend fun getPresetById(id: String): TunerPreset?` → linear scan of `TunerPresets.all` matching by `id`
- `suspend fun getPresetsGrouped(): Map<Int, Map<TunerCategory, List<TunerPreset>>>` → returns `TunerPresets.grouped`

> The `getPresetsGrouped()` method is **new** vs. the Phase 2 interface signature. Update `TunerPresetRepository` to declare it. Record the interface change in `DECISIONS.md`.

The `suspend` keyword stays even though everything is in-memory — keeps the interface stable for a future migration (e.g. user-defined presets in Room).

## Tests

Tests in 5.1 are **exhaustive**, not token. Every method gets at least the following coverage. All tests live under `app/src/test/java/de/ritzelprimpf/toniqo/`.

### `common/model/`
- `NoteNameTest` — `parse` accepts every valid sharp and flat spelling and rejects invalid input; `semitonesFromC` is correct for all 12 values; `sharpName` and `flatName` agree for natural notes and differ for the 5 accidentals.
- `NoteTest` — `frequencyHz` matches known reference values to within 0.01 Hz: `A4 = 440.0`, `A3 = 220.0`, `C4 = 261.6256`, `E2 = 82.4069`, `E4 = 329.6276`. Also test with `referencePitchHz = 432.0`: `A4 = 432.0`. `parse` round-trips with `displayName` for every test case. Octave wrap correctness — `B3` to `C4` is one semitone, not eleven.
- `ScaleTest` — Building C Ionian yields `[C4, D4, E4, F4, G4, A4, B4]`. Building A Aeolian from `A3` yields `[A3, B3, C4, D4, E4, F4, G4]` (the C through G must rise an octave). Spot-check each mode against one canonical example.
- `ChordTest` — `Chord(C4, MAJOR).notes` is `[C4, E4, G4]`; `MINOR_SEVENTH` test; `HALF_DIMINISHED` test (verifies the symbol renders as `"m7♭5"`).

### `common/util/MusicTheory.kt`
- `MusicTheoryFrequencyTest` — `noteToFrequency` and `frequencyToNote` round-trip exactly for every standard guitar string across all presets. Out-of-range inputs return `null`. `NaN`, `Infinity`, `0.0`, negative numbers all return `null`. Test at reference 432 Hz separately.
- `MusicTheoryCentsTest` — `centsBetween(440.0, 440.0)` = 0. `centsBetween(440.0, 466.16)` ≈ +100 (one semitone sharp). Symmetric: flipping arguments negates the result. Spot-check ±5, ±50, ±1200 cents.
- `MusicTheoryScaleTest` — `buildScale(C4, Ionian).notes` is the C major scale; spot-check D Dorian, E Phrygian, G Mixolydian, A Aeolian, B Locrian.
- `MusicTheoryTriadsTest` — `buildTriads(C Ionian scale)` returns `[C, Dm, Em, F, G, Am, Bdim]` (verified by `displayName`). One test per mode confirms the standard quality sequence for that mode.
- `MusicTheorySeventhsTest` — **The canonical test:** `buildSeventhChords(C Ionian scale).map { it.displayName() }` equals `["Cmaj7", "Dm7", "Em7", "Fmaj7", "G7", "Am7", "Bm7♭5"]`. One test per mode.

### `tuner/data/`
- `TunerPresetsTest` — every preset's `id` is unique across the catalog. Every preset's `notes.size` equals its `stringCount`. Every preset's notes parse without error (will already crash at object-init if not, but worth an explicit assertion). The grouped map has exactly 3 outer keys (`6`, `7`, `8`) and each inner map has 1–3 category keys matching the categories actually present.
- `TunerPresetRepositoryImplTest` — `getPresetById("six_string_standard_e")` returns the right preset; unknown ID returns `null`; `getPresetsGrouped()` matches `TunerPresets.grouped`.

## Steps

1. Update `NoteName` (add `semitonesFromC`, `sharpName`, `flatName`, `parse`). Tests.
2. Update `Note` (`frequencyHz`, `displayName`, `parse`). Tests.
3. Update `Interval` with named constants. (Minor — no behaviour to test beyond construction.)
4. Update `Mode` (interval patterns, `displayName`). Tests.
5. Update `ChordQuality` (`intervalsFromRoot`, `symbol`). Tests.
6. Update `Scale` (derived `notes`). Tests.
7. Update `Chord` (derived `notes`, `displayName`). Tests.
8. Implement `MusicTheory.noteToFrequency`, `frequencyToNote`, `centsBetween`. Tests.
9. Implement `MusicTheory.buildScale`, `buildTriads`, `buildSeventhChords`. Tests.
10. Add `urlSlug` to `TunerCategory`.
11. Create `tuner/data/TunerPresets.kt` with the full catalog. Tests for catalog integrity.
12. Implement `TunerPresetRepositoryImpl` against the catalog. Update the `TunerPresetRepository` interface to add `getPresetsGrouped()`. Tests.
13. Update `DECISIONS.md` with: the seventh-chord policy, the sharp-spelling default for `frequencyToNote`, the `centsBetween` addition, the `getPresetsGrouped` interface addition, and the `Scale` signature change (removal of `intervals`).
14. Create `FUTURE_IMPROVEMENTS.md` (if it doesn't already exist) with an entry for context-aware enharmonic spelling.
15. Hand off to the user with a summary.

## Completion Criteria

See `Phase5.1-REQUIREMENTS.md`.
