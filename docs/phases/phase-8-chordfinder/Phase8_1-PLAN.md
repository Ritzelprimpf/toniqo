# Phase 8.1 — Diatonic Chord Engine

## Goal

Implement the entire logic core of Stage 1: given a root pitch class and a `ScaleType` (any of the 14), produce the 7 diatonic chords of that key, each with its scale degree, Roman numeral, triad quality, optional seventh quality, conventionally-spelled note names, and display symbol. **Pure Kotlin** — no Android, no voicings, no ViewModel, no UI. This is the only place chord-construction theory lives.

By the end of 8.1, `FindChordsUseCase(ChordFinderInput) → ChordFinderResult` returns the correct chords for every root × scale type, triads or sevenths, exhaustively unit-tested.

## Scope

- `common/model/ChordQuality.kt` — confirm/define the triad-quality enum (4 values).
- `chordfinder/domain/model/SeventhQuality.kt` — the 7 seventh qualities the 14 types can produce.
- `chordfinder/domain/model/DegreeChord.kt`, `ChordFinderResult.kt`, `ChordFinderInput.kt` (supersede the Phase 2 `Mode`-based stubs).
- `chordfinder/domain/ChordQualityResolver.kt` — pure interval → quality mapping.
- `chordfinder/domain/usecase/FindChordsUseCase.kt` — assembles the 7 chords.
- `strings.xml` entries for quality abbreviations and chord-symbol suffixes.
- Exhaustive unit tests.

## Out of Scope

- Guitar voicings, fretboard, tuning → 8.2.
- ViewModel, selection state, Key Finder seed → 8.3.
- Any composable → 8.4 / 8.5.

## Reading Order Before Starting

1. `Phase8-PLAN.md` → "Stage 1 — The Chord Engine"
2. `APP_SPECIFICATION.md` → "Module: Chord Finder" (and the conflict note in `Phase8-PLAN.md`)
3. `DECISIONS.md` → Phase 7.1 `ScaleType` / `ScaleSpeller` entries (this builds directly on them)
4. `IMPLEMENTATION_NOTES.md` → "Music Theory Primitives"
5. This file

## Decisions Locked In For 8.1

- ✅ **Targets `ScaleType` (14), not `Mode` (7).** `ChordFinderInput` carries `scaleType: ScaleType`. Supersedes the Phase 2 stub; recorded.
- ✅ **Quality is interval-derived**, never assumed from the major-scale pattern.
- ✅ **Spelling reuses `ScaleSpeller`.** Chord note names are the spelled scale degrees — no new spelling logic.
- ✅ **The Phase 2 `ChordFinderService` / `ChordFinderServiceImpl` stubs are removed**; the logic lives in `FindChordsUseCase`, mirroring Key Finder's `MatchScalesUseCase`. Recorded.
- ✅ **Seventh-quality set** is exactly the seven in `Phase8-PLAN.md` Stage 1.

## Implementation Details

### `common/model/ChordQuality.kt`

```kotlin
enum class ChordQuality { MAJOR, MINOR, DIMINISHED, AUGMENTED }
```
If a `ChordQuality` from Phase 2 already exists, reconcile to exactly these four; do not duplicate.

### `chordfinder/domain/model/SeventhQuality.kt`

An enum of the seven seventh types, each exposing a **string-resource key** for its symbol suffix (no display text baked in):

```kotlin
enum class SeventhQuality(@StringRes val suffixKey: Int) {
    MAJOR_SEVENTH(R.string.cf_suffix_maj7),         // maj7
    MINOR_SEVENTH(R.string.cf_suffix_m7),           // m7
    DOMINANT_SEVENTH(R.string.cf_suffix_dom7),      // 7
    HALF_DIMINISHED(R.string.cf_suffix_m7b5),       // m7♭5
    DIMINISHED_SEVENTH(R.string.cf_suffix_dim7),    // dim7
    MINOR_MAJOR_SEVENTH(R.string.cf_suffix_mmaj7),  // mMaj7
    AUGMENTED_MAJOR_SEVENTH(R.string.cf_suffix_maj7s5), // maj7♯5
}
```

### `chordfinder/domain/ChordQualityResolver.kt`

Pure `object`. Maps intervals (semitones above the root, each `0..11`) to qualities:

```kotlin
object ChordQualityResolver {
    fun triad(thirdInterval: Int, fifthInterval: Int): ChordQuality
    fun seventh(triad: ChordQuality, seventhInterval: Int): SeventhQuality
}
```

`triad`: (4,7)→MAJOR, (3,7)→MINOR, (3,6)→DIMINISHED, (4,8)→AUGMENTED. Any other pair is a programming error in scale data → throw `IllegalArgumentException` with a descriptive message (no silent default). `seventh`: per the `Phase8-PLAN.md` table — (MAJOR,11)→MAJOR_SEVENTH, (MINOR,10)→MINOR_SEVENTH, (MAJOR,10)→DOMINANT_SEVENTH, (DIMINISHED,10)→HALF_DIMINISHED, (DIMINISHED,9)→DIMINISHED_SEVENTH, (MINOR,11)→MINOR_MAJOR_SEVENTH, (AUGMENTED,11)→AUGMENTED_MAJOR_SEVENTH; anything else throws. All interval literals are named constants.

### `chordfinder/domain/model/DegreeChord.kt`

```kotlin
data class DegreeChord(
    val degree: Int,                  // 1..7
    val romanNumeral: String,         // e.g. "ii", "V", "vii°", "III+"
    val triadQuality: ChordQuality,
    val seventhQuality: SeventhQuality?,  // null when triads-only
    val rootName: String,             // spelled, e.g. "G♯"
    val noteNames: List<String>,      // 3 (triad) or 4 (seventh), spelled
    val symbol: String,               // e.g. "Dm", "Bdim", "Cmaj7♯5"
)
```

Roman numeral helper: base `["I","II","III","IV","V","VI","VII"]`, lower-cased for MINOR/DIMINISHED, `"°"` appended for DIMINISHED, `"+"` for AUGMENTED. Symbol = `rootName` + triad suffix (MAJOR `""`, MINOR `"m"`, DIMINISHED `"dim"`, AUGMENTED `"aug"`) when triads-only, or `rootName` + the seventh `suffixKey` text when sevenths. All suffix glyphs come from `strings.xml`.

### `chordfinder/domain/model/ChordFinderInput.kt` / `ChordFinderResult.kt`

```kotlin
data class ChordFinderInput(
    val rootPitchClass: Int,          // 0..11
    val scaleType: ScaleType,
    val includeSeventhChords: Boolean,
)
data class ChordFinderResult(val chords: List<DegreeChord>) // size 7, degree order
```

### `chordfinder/domain/usecase/FindChordsUseCase.kt`

```kotlin
class FindChordsUseCase {
    operator fun invoke(input: ChordFinderInput): ChordFinderResult
}
```

Behaviour:
1. Compute the scale's 7 pitch classes: `(input.rootPitchClass + offset) mod 12` for each offset in `input.scaleType.intervalsFromRoot`.
2. Spell the 7 notes: `ScaleSpeller.scaleNoteNames(input.rootPitchClass, input.scaleType)`.
3. For `i` in `0..6`:
   - triad indices = `i`, `(i+2) mod 7`, `(i+4) mod 7`; seventh index = `(i+6) mod 7`.
   - intervals = `(pc[idx] - pc[i] + 12) mod 12`.
   - `triadQuality = ChordQualityResolver.triad(thirdInterval, fifthInterval)`.
   - if `includeSeventhChords`, `seventhQuality = ChordQualityResolver.seventh(triadQuality, seventhInterval)` and `noteNames` = the 4 spelled names; else `seventhQuality = null`, 3 names.
   - build `romanNumeral`, `rootName = spelled[i]`, `symbol`.
4. Return the 7 `DegreeChord`s in degree order.

Pure, synchronous, **not** `suspend`. Constants: `SCALE_SIZE = 7`, the third/fifth/seventh step offsets (`2`, `4`, `6`), `PITCH_CLASSES = 12`.

## Tests

All under `app/src/test/.../`.

### `ChordQualityResolverTest`
- Each of the four valid triad pairs → correct quality; an invalid pair throws.
- Each of the seven valid seventh combinations → correct quality; an invalid combination throws.

### `FindChordsUseCaseTest`
- **C Ionian, triads** → `C, Dm, Em, F, G, Am, Bdim`; romans `I ii iii IV V vi vii°`; notes per `Chord_Finder___triads.png`.
- **C Ionian, sevenths** → `Cmaj7, Dm7, Em7, Fmaj7, G7, Am7, Bm7♭5`; 4 notes each; per `…7ths.png`.
- **A Aeolian, triads** → `Am, Bdim, C, Dm, Em, F, G`; romans `i ii° III iv v VI VII`.
- **A Harmonic Minor, triads** → `Am, Bdim, Caug, Dm, E, F, G♯dim`; **sevenths** → `AmMaj7, Bm7♭5, Cmaj7♯5, Dm7, E7, Fmaj7, G♯dim7`. (Exercises augmented + the three exotic sevenths + letter-correct `G♯` not `A♭`.)
- **A Melodic Minor, sevenths** → `AmMaj7, Bm7, Cmaj7♯5, D7, E7, F♯m7♭5, G♯m7♭5`.
- **Parameterised across all 12 roots × 14 types:** exactly 7 chords; each triad quality is one of the four; each chord's notes are 7-distinct-letter-correct (inherited from `ScaleSpeller`); no resolver throws (proves the 14 patterns only ever yield the four triads / seven sevenths).
- Roman-numeral casing/symbols asserted on a mixed example.

## Steps

1. `ChordQuality` (confirm/define). 2. `SeventhQuality` + string resources. 3. `ChordQualityResolver` + tests. 4. Models (`DegreeChord`, `ChordFinderInput`, `ChordFinderResult`). 5. `FindChordsUseCase` + tests (all fixtures). 6. `strings.xml` suffixes + quality abbreviations. 7. Append 8.1 decisions to `DECISIONS.md`. 8. Hand off with file summary.

## Completion Criteria

See `Phase8_1-REQUIREMENTS.md`.
