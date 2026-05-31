# Phase 7.1 — Scale Model & Matching Engine

## Goal

Implement the entire logic core of Key Finder: the 14-scale-type model, conventional spelling, and the scoring + ranking engine that turns a set of notes (plus an optional root) into a ranked list of scale matches. This is **pure Kotlin** — no Android, no audio, no ViewModel, no UI. Everything here is unit-testable in isolation, and it is the only place the matching rules live.

By the end of 7.1, given a list of `Note`s and an optional root pitch class, the engine returns the correct ranked `List<ScaleMatch>`. Phases 7.2 (audio), 7.3 (ViewModel), and 7.4 (UI) consume this without adding a single matching rule.

## Scope

- `common/model/ScaleFamily.kt` — new enum (`DIATONIC`, `HARMONIC_MINOR`, `MELODIC_MINOR`).
- `common/model/ScaleType.kt` — new enum, the 14 types from `Phase7-PLAN.md`, each carrying family, interval pattern, label keys, and the common-first ordinal.
- `common/util/ScaleSpeller.kt` — new pure utility for conventional spelling.
- `keyfinder/domain/model/` — `ScaleCandidate`, `ScaleMatch`, `KeyFinderInput`.
- `keyfinder/domain/` — `ScaleCatalog` (generates the 168 candidates once) and `MatchScalesUseCase` (score + rank + cap).
- Exhaustive unit tests for all of the above.

## Out of Scope

- `AudioRecord`, YIN, microphone, permissions → 7.2.
- Note-list state, the ≥3-note gate wiring, live recompute → 7.3.
- Any composable, screen, chip, or detail view → 7.4.
- Cross-navigation to Chord Finder → not in this phase at all.

## Reading Order Before Starting

1. `Phase7-PLAN.md` → "Scoring Model", "Scale Inventory", "Ranking & Display Rules", "Conventional Spelling Rule"
2. `APP_SPECIFICATION.md` → "Module: Key Finder" (and the conflict note in `Phase7-PLAN.md`)
3. `IMPLEMENTATION_NOTES.md` → "Music Theory Primitives"
4. `DECISIONS.md` → all entries (especially the Phase 5.1 `Note`/`Mode`/`Scale` decisions this builds on)
5. This file

## Decisions Locked In For 7.1

- ✅ **Score formula** exactly as in `Phase7-PLAN.md` → "Scoring Model". Percent is `round(score * 100)`, round half up, integer.
- ✅ **Pitch-class reduction.** Input is reduced to a `Set<Int>` of pitch classes 0–11 (octave-agnostic, de-duplicated) before scoring.
- ✅ **14 scale types**, patterns and labels per the `Phase7-PLAN.md` inventory table.
- ✅ **Diatonic single-source-of-truth.** `ScaleType`'s seven diatonic patterns must equal the existing `Mode` patterns; a test asserts this rather than the code duplicating the arrays silently.
- ✅ **Ranking** = percent desc → common-first scale-type ordinal → root pitch class asc. **Top 7**, exclude 0%.
- ✅ **Gate value** = 3 distinct pitch classes. The use case itself returns an empty list below the gate (the gate is enforced here, the *UI reaction* to it is 7.3/7.4).
- ✅ **Conventional spelling** via `ScaleSpeller`, letter-per-degree, root from the canonical table below.

## Implementation Details

### `common/model/ScaleFamily.kt`

```kotlin
enum class ScaleFamily { DIATONIC, HARMONIC_MINOR, MELODIC_MINOR }
```

### `common/model/ScaleType.kt`

An enum with 14 entries. Each carries:

- `val family: ScaleFamily`
- `val intervalsFromRoot: IntArray` — the 7 semitone offsets from the `Phase7-PLAN.md` table.
- `val rankOrder: Int` — the common-first ordinal (Major = 0, Natural Minor = 1, Dorian = 2, …, Dorian ♭2 = 13), used as the tie-break key.
- label resolution: each type exposes the **string resource keys** for its primary label and subtitle (the `{root}` placeholder is filled by the caller). Do **not** bake display text into the enum — only resource keys, so all user-visible text stays in `strings.xml`.

Add:

- `companion object val DIATONIC: List<ScaleType>` (the 7 diatonic, in case callers want just those).
- A KDoc note on each exotic type naming its parent and degree (e.g. Phrygian Dominant = mode 5 of harmonic minor) for maintainability.

> Do not reuse the existing `Mode` enum's `displayName` here — that one is tuner/Chord-Finder facing. `ScaleType` labels are Key-Finder facing and differ (e.g. `Mode.Aeolian` → "Aeolian (Natural Minor)"; `ScaleType.AEOLIAN` primary label → "{r} Natural Minor", subtitle "{r} Aeolian").

### `common/util/ScaleSpeller.kt`

A pure `object`. Public API:

```kotlin
object ScaleSpeller {
    /** Conventional display spelling of a scale's root pitch class for a given scale type. */
    fun rootName(rootPitchClass: Int, type: ScaleType): String

    /** The 7 conventionally-spelled note names of the scale, in degree order. */
    fun scaleNoteNames(rootPitchClass: Int, type: ScaleType): List<String>
}
```

**Algorithm.**

1. **Root letter + accidental** come from a canonical per-pitch-class table. Default table (root display):
   `0→C, 1→D♭, 2→D, 3→E♭, 4→E, 5→F, 6→F♯, 7→G, 8→A♭, 9→A, 10→B♭, 11→B`.
   This is the starting point for diatonic and melodic-minor scales. For scales whose conventional spelling is clearer with the opposite accidental on a specific root (documented edge cases — e.g. a root that would force many double-sharps), the table may carry a per-family override; keep overrides in a small documented map, not scattered in code.
2. **Degree spelling.** Starting from the root's **letter** (A–G), assign each of the 7 degrees the *next* letter cyclically (root letter, then the following letter, etc., wrapping G→A). For each degree, compute the accidental as the difference between the degree's actual pitch class and the natural pitch class of its assigned letter. Render `0→(natural), +1→♯, -1→♭, +2→𝄪, -2→♭♭`.
3. This guarantees exactly one of each letter A–G and the correct accidental per degree (so harmonic minor's raised 7th is `G♯` in A, and a Lydian's raised 4th is `F♯` in C, etc.).

Use the existing `NoteName` / `Note` types from `common/` where helpful, but the output is display `String`s. No magic chars inline — define `SHARP`, `FLAT`, `DOUBLE_SHARP`, `DOUBLE_FLAT` constants.

### `keyfinder/domain/model/ScaleCandidate.kt`

A single scale instance in the catalog (root + type, with its pitch-class set precomputed):

```kotlin
data class ScaleCandidate(
    val rootPitchClass: Int,          // 0..11
    val type: ScaleType,
) {
    val pitchClasses: Set<Int>        // derived: (rootPitchClass + each interval) mod 12
}
```

### `keyfinder/domain/model/KeyFinderInput.kt`

```kotlin
data class KeyFinderInput(
    val pitchClasses: Set<Int>,       // distinct, 0..11
    val rootPitchClass: Int?,         // null if no root marked
)
```

The caller (7.3) builds this from the user's `Note` list; the use case does not see octaves or duplicates.

### `keyfinder/domain/model/ScaleMatch.kt`

```kotlin
data class ScaleMatch(
    val candidate: ScaleCandidate,
    val percent: Int,                 // 0..100
    val isFull: Boolean,              // input ⊆ scale
    val isRootMatch: Boolean,         // root marked and scale rooted on it
    val rank: Int,                    // 1-based position in the displayed list
)
```

Display strings (primary label, subtitle, spelled note list) are **derived in the presentation layer** from `candidate` via `ScaleType` resource keys + `ScaleSpeller`; they are not stored on `ScaleMatch`. (Keeps the domain free of `Context`/resources.)

### `keyfinder/domain/ScaleCatalog.kt`

Builds the 168 `ScaleCandidate`s once (12 roots × 14 types) and exposes them as an immutable list. A pure `object` (no state beyond the constant list, no I/O). KDoc that the count is exactly 168 and why.

### `keyfinder/domain/usecase/MatchScalesUseCase.kt`

The heart of the module. Constructor-injected with the `ScaleCatalog` (inject the catalog as an interface or pass the list, so the use case is testable with a trimmed catalog).

```kotlin
class MatchScalesUseCase(private val catalog: ScaleCatalog) {
    operator fun invoke(input: KeyFinderInput): List<ScaleMatch>
}
```

Behaviour:

1. If `input.pitchClasses.size < MIN_NOTES_TO_MATCH` (= 3) → return `emptyList()`.
2. For each candidate, compute `covered`, `rootBonus`, `points`, `maxPoints`, `percent`, `isFull`, `isRootMatch` exactly per the formula.
3. Drop candidates with `covered == 0`.
4. Sort by `percent` desc → `type.rankOrder` asc → `rootPitchClass` asc.
5. Take the first 7, assign `rank = 1..7`.

All thresholds (`MIN_NOTES_TO_MATCH = 3`, `MAX_RESULTS = 7`, `SCALE_SIZE = 7`, `PITCH_CLASSES = 12`) are named constants, no inline literals.

> This is a pure function with no Android dependency. It is **not** a `suspend fun` — matching 168 small sets is microseconds; keep it synchronous. (7.3 decides whether to call it off the main thread; the math itself does not need coroutines.)

## Tests

Tests are **exhaustive**, not token. All under `app/src/test/java/de/ritzelprimpf/toniqo/`.

### `common/model/ScaleTypeTest`
- All 14 types present; each `intervalsFromRoot` has 7 entries, strictly ascending, first is 0, all in 0–11.
- The 7 diatonic types' patterns **equal** the corresponding `Mode.intervalsFromRoot` (the single-source-of-truth assertion).
- `rankOrder` is 0..13 with no duplicates and matches the common-first order in `Phase7-PLAN.md`.

### `common/util/ScaleSpellerTest`
Assert exact spellings (these are the canon):
- `C Major` → `C D E F G A B`; `G Major` → `G A B C D E F♯`; `F Major` → `F G A B♭ C D E`; `B♭ Major` → `B♭ C D E♭ F G A`; `F♯ Major` → `F♯ G♯ A♯ B C♯ D♯ E♯`.
- `A Natural Minor` → `A B C D E F G`; `A Harmonic Minor` → `A B C D E F G♯`; `A Melodic Minor` → `A B C D E F♯ G♯`.
- `E Phrygian Dominant` → `E F G♯ A B C D`; `C Lydian Dominant` → `C D E F♯ G A B♭`; `G Altered` → `G A♭ B♭ C♭ D♭ E♭ F`.
- Each spelled scale uses **7 distinct letter names** (regression guard for the letter-per-degree rule), verified for all 12 roots × 14 types in a parameterised loop.
- `rootName` returns the table value for each pitch class for a representative type.

### `keyfinder/domain/ScaleCatalogTest`
- Exactly 168 candidates; every (root, type) pair present exactly once.
- Every candidate's `pitchClasses` has exactly 7 distinct values and contains its root.

### `keyfinder/domain/usecase/MatchScalesUseCaseTest`
Drive every canonical example from `Phase7-PLAN.md` and the gate/ranking rules:
- **Gate:** 0, 1, 2 distinct notes → empty list. 3 → non-empty.
- **43%:** `{C,E,G}` no root → the scales containing C-E-G read 43%; one assertion pins an example scale's percent.
- **Root lifts and splits:** `{C,E,G}` root C → C-rooted containing scale = 50%, and a same-note scale rooted elsewhere (A Aeolian-style) = 38%, and the C-rooted one ranks above it.
- **Full C-major tie:** the 7 natural notes, no root → the seven modes of C major all read 100% and all carry `FULL`; ordering is Major first, then Natural Minor, then the rest per common-first.
- **Root breaks the tie:** same input, root A → A Natural Minor = 100% with `TONIC`+`FULL` and ranks #1; its six siblings = 88%.
- **Stray note:** the 7 natural notes + B♭, no root → C Major = 88% and is **not** `FULL` (the B♭ is a stray, `covered != n`).
- **Exclusion:** a scale sharing no notes with the input never appears.
- **Cap:** an input that matches many scales yields exactly 7 results with `rank` 1..7.
- **`isFull` at low percent:** `{C,E,G}` no root → a containing scale is `FULL` while still 43%.
- **Tie-break stability:** equal-percent results come out in common-first then root-ascending order, asserted on a constructed case.

## Steps

1. `ScaleFamily` enum. (Trivial.)
2. `ScaleType` enum with patterns, family, `rankOrder`, label resource keys. Test (including the `Mode` cross-check).
3. `ScaleSpeller` (root table + letter-per-degree). Tests (the spelling canon).
4. `ScaleCandidate`, `KeyFinderInput`, `ScaleMatch` models.
5. `ScaleCatalog` (168 candidates). Test.
6. `MatchScalesUseCase` (score, filter, rank, cap, gate). Tests (all canonical examples).
7. Add the `strings.xml` entries for the 14 primary labels and subtitles (with `%s` root placeholder).
8. Append the 7.1 decisions to `DECISIONS.md` (formula, single root, inventory, ranking, gate, spelling, `ScaleType` superset) — each as a dated, append-only entry; the four that supersede `APP_SPECIFICATION.md` must say so explicitly.
9. Hand off with a file summary.

## Completion Criteria

See `Phase7_1-REQUIREMENTS.md`.
