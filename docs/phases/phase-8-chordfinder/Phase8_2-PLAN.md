# Phase 8.2 — Voicing Data, Tuning Model & Loader

## Goal

Implement Stage 2's runtime: the fretboard model, the curated chord-keyed voicing library for standard tuning, the loader that turns it into `Voicing`s, and the uniform-offset tuning transform (tier 2). No CAGED engine at runtime. By the end of 8.2, `VoicingRepository.lookup(chordKey, tuning)` returns the correct, validated voicings for tier-1 and tier-2 tunings, and reports tier-3 as unsupported.

## Scope

- `common/model/GuitarTuning.kt` — tuning model + standard-6 constant + uniform-offset classifier. *(In `common/` because the Tuner becomes its second consumer in 8.3 — the "promote on second consumer" trigger.)*
- `chordfinder/domain/model/` — `FretMark`, `Barre`, `VoicingCategory`, `ChordToneRole`, `Voicing`, `ChordKey`.
- `chordfinder/domain/VoicingTransposer.kt` — pure tier-2 fret-shift.
- `chordfinder/domain/repository/VoicingRepository.kt` — interface; `VoicingLookupResult`.
- `chordfinder/data/VoicingRepositoryImpl.kt` + `VoicingJsonParser.kt` — load, parse, cache, validate.
- The **curated JSON asset** `assets/chordfinder/voicings_standard_6.json` (12 roots × 4 triad qualities), curated from the Python generator's output (the generator is built separately under its own prompt).
- Exhaustive unit tests, including the **library validation test**.

## Out of Scope

- ViewModel, tuning source wiring, persistence → 8.3.
- Composables, navigation → 8.4 / 8.5.
- Seventh / inversion / non-uniform voicings → FUTURE_PLANS FP-1/2/3.

## Reading Order Before Starting

1. `Phase8-PLAN.md` → "Stage 2 — Voicing Resolution", "The Tuning Source", "Why not CAGED"
2. `FUTURE_PLANS.md` → FP-1, FP-2, FP-3 (the seams this must leave)
3. `CLAUDE.md` → §4 (SOLID), §8 (dependencies)
4. `Chord_Finder___C_voicings.png` (the visual target the data must support)
5. This file

## Decision To Take At The Start (then record)

- **JSON parser.** Default: Android's built-in **`org.json`** (no new dependency). Alternative: **kotlinx.serialization** (cleaner, but a new dependency → requires explicit approval per `CLAUDE.md` §8). Decide before writing `VoicingJsonParser`; the parsed-model tests are parser-agnostic.

## Decisions Locked In For 8.2

- ✅ **Data-driven, chord-keyed** (`rootPitchClass` + `ChordQuality`), never key/mode.
- ✅ **Root-position, triad qualities, standard 6-string** shipped data; tier-2 by transform.
- ✅ **Neutral position-based labels** (no CAGED names).
- ✅ **Variable string count** in the model from day one.
- ✅ **`bassDegree` = `ROOT`** always (FP-1 seam).
- ✅ **Offline generator is throwaway/dev-only**; the shipped artifact is the curated JSON; correctness is guarded by the validation test, not by testing the generator.

## Implementation Details

### `common/model/GuitarTuning.kt`

```kotlin
data class GuitarTuning(val id: String, val openNotes: List<Note>) {
    val stringCount: Int get() = openNotes.size
    /** Δ semitones if every string is the same offset from [base] and string counts match; else null. */
    fun uniformOffsetFrom(base: GuitarTuning): Int?
    companion object { val STANDARD_6: GuitarTuning /* E2 A2 D3 G3 B3 E4 */ }
}
```
`uniformOffsetFrom`: if counts differ → null; compute per-string semitone delta (`this - base`); if all equal → that Δ (negative = tuned down); else null. Uses `Note`'s absolute semitone index (octave-aware), so it relies on `Note` carrying octave + pitch class (already true from the tuner).

### `chordfinder/domain/model/` voicing types

```kotlin
sealed interface FretMark { object Open; object Muted; data class Fretted(val fret: Int) }
data class Barre(val fret: Int, val fromString: Int, val toString: Int)
enum class VoicingCategory { OPEN, BARRE, SHAPE }
enum class ChordToneRole { ROOT, THIRD, FIFTH, OTHER }
data class ChordKey(val rootPitchClass: Int, val quality: ChordQuality)

data class Voicing(
    val labelKey: Int,                 // neutral label resource (position-based)
    val marks: List<FretMark>,         // size = stringCount, low→high
    val fingers: List<Int?>,           // size = stringCount
    val barre: Barre?,
    val rootStringIndices: Set<Int>,
    val bassDegree: ChordToneRole,     // ROOT in v1
) {
    val category: VoicingCategory get() = when {
        marks.any { it is FretMark.Open } -> VoicingCategory.OPEN
        barre != null -> VoicingCategory.BARRE
        else -> VoicingCategory.SHAPE
    }
    val fretRange: IntRange get() = /* min..max over Fretted marks (+barre fret) */
    val baseFret: Int get() = fretRange.first
}
```
`category`, `fretRange`, `baseFret` are **derived** (cannot drift). `rootStringIndices` is supplied by the loader/generator (computed from tuning + chord root). A factory `Voicing.validated(...)` enforces the invariants below and throws on violation.

### Invariants (enforced + tested)

1. `marks.size == fingers.size == tuning.stringCount`.
2. Every sounded string's pitch class ∈ the chord's pitch classes; all chord tones present across sounded strings.
3. Lowest sounded string sounds the **root** (root-position).
4. Fret span ≤ `MAX_FRET_SPAN` (proposed 4); frets in `0..MAX_FRET` (proposed 15 region cap for diagrams; actual neck longer — cap is per-diagram window).
5. `rootStringIndices` exactly = sounded strings whose pitch class is the root.

### `chordfinder/domain/VoicingTransposer.kt`

```kotlin
object VoicingTransposer {
    /** Shift a movable voicing up by [deltaFrets] to preserve sounding pitch under a tuned-down instrument.
     *  Returns null for open-position voicings (cannot stay open) or if any fret leaves the playable window. */
    fun shift(voicing: Voicing, deltaFrets: Int, maxFret: Int): Voicing?
}
```
`deltaFrets` = `-uniformOffset` (tuned down Δ<0 → shift up |Δ|). If `voicing.category == OPEN` → null. Else add `deltaFrets` to every `Fretted.fret` and the barre fret; if any exceeds `maxFret` or drops below 1 → null; fingers unchanged; `rootStringIndices` unchanged.

### `chordfinder/domain/repository/VoicingRepository.kt`

```kotlin
sealed interface VoicingLookupResult {
    data class Standard(val voicings: List<Voicing>) : VoicingLookupResult
    data class UniformOffset(val voicings: List<Voicing>, val offsetSemitones: Int) : VoicingLookupResult
    data class Unsupported(val tuning: GuitarTuning) : VoicingLookupResult   // tier 3
}
interface VoicingRepository {
    suspend fun lookup(chord: ChordKey, tuning: GuitarTuning): VoicingLookupResult
}
```
Impl logic: tier 1 (`tuning == STANDARD_6`) → `Standard`. Tier 2 (`uniformOffsetFrom(STANDARD_6) != null`) → transpose each standard voicing via `VoicingTransposer`, drop nulls, → `UniformOffset`. Else → `Unsupported`. Ordering: ascending `baseFret`; cap `MAX_VOICINGS` (proposed 5). Loading is cached after first read.

### Curated JSON asset

`assets/chordfinder/voicings_standard_6.json` — developer-readable, chord-keyed:

```json
{
  "tuningId": "standard_6",
  "version": 1,
  "chords": [
    { "rootPitchClass": 0, "quality": "MAJOR",
      "voicings": [
        { "frets": ["x",3,2,0,1,0], "fingers": [0,3,2,0,1,0], "barre": null },
        { "frets": [8,10,10,9,8,8], "fingers": [1,3,4,2,1,1], "barre": {"fret":8,"from":0,"to":5} }
      ] }
  ]
}
```
`frets`: `"x"` muted, `"o"` or `0` open, integer fretted. The loader computes `rootStringIndices`, derives `category`/`fretRange`, and validates. Cover **all 12 roots × {MAJOR, MINOR, DIMINISHED, AUGMENTED}**; a few voicings each (dim/aug fewer). The agent authors this from the offline generator's output, then prunes for playability.

### Curated data input (from the Python generator)

The candidate voicings are produced **outside this sub-phase** by a standalone **Python script** (`tools/voicing-generator/`, written under its own implementation prompt). It is throwaway, dev-only, never part of the Android build, and emits the exact JSON schema above. 8.2's job is to **curate** that output (prune awkward/unplayable entries by hand) into the shipped `assets/chordfinder/voicings_standard_6.json`, then load and validate it. There is no generator code in the app; correctness of the shipped data is guarded by the **library validation test**, not by testing the generator.

## Tests

### `GuitarTuningTest`
- `STANDARD_6` open notes correct; `stringCount == 6`.
- `uniformOffsetFrom`: E♭-standard → −1; D-standard → −2; standard → 0; Drop D → null; a 7-string tuning → null.

### `VoicingTest`
- `category` derivation (open → OPEN, barre → BARRE, else SHAPE); `fretRange`/`baseFret` derivation; `validated(...)` throws on each broken invariant.

### `VoicingTransposerTest`
- Movable voicing +2 → all frets +2, fingers identical, barre +2, roots unchanged.
- Open-position voicing → null.
- Off-window shift → null.

### `VoicingRepositoryImplTest` (with a fake/asset)
- Tier 1 standard → `Standard`, ascending baseFret, ≤ cap.
- Tier 2 (Δ=−1) → `UniformOffset(offset=-1)`, open voicings dropped, frets shifted.
- Tier 3 (Drop D) → `Unsupported`.

### `VoicingLibraryValidationTest` (the safety net — loads the shipped JSON)
- Every entry parses; `marks.size == 6`; every voicing passes all five invariants for its `ChordKey`; every chord key (12 roots × 4 qualities) is present with ≥ 1 voicing; C MAJOR includes a near-nut open voicing and at least one barre voicing.

## Steps

1. Decide + record the JSON parser. 2. `GuitarTuning` + tests. 3. Voicing model types + `validated` + tests. 4. `VoicingTransposer` + tests. 5. `VoicingRepository` interface + result type. 6. Take the Python generator's JSON output; author + **curate** it into the shipped asset. 7. `VoicingJsonParser` + `VoicingRepositoryImpl` + tests, incl. the library validation test. 8. `strings.xml` neutral labels + category tags. 9. Append 8.2 decisions. 10. Hand off.

## Completion Criteria

See `Phase8_2-REQUIREMENTS.md`.
