# Phase 8 — Chord Finder Implementation

## Goal

Implement the Chord Finder module fully. The user picks a **root** and a **mode** from two dropdowns at the top; the app lists every diatonic chord of that key, by scale degree, with Roman numerals, qualities, and note pills, and a `TRIADS / 7THS` toggle. Tapping a chord opens a second screen showing that chord's playable **guitar voicings** as a scrollable 2-column grid of fretboard diagrams, **respecting the user's current tuning** for standard and uniformly-detuned tunings. By the end of this phase the Chord Finder must be functional, tested, and production-quality on a real device.

This is the fourth and final core module, after the Guitar Tuner (Phase 5), Metronome (Phase 6), and Key Finder (Phase 7).

## Approach

Phase 8 is divided into five sub-phases, completed in order; each has its own `Phase8_N-PLAN.md` and `Phase8_N-REQUIREMENTS.md` and fits a single focused session.

The module has **two independent stages**:

1. A **chord engine** (8.1): *scale → the list of chords*. Pure music theory. Works for every scale type.
2. **Voicing resolution** (8.2): *one chord (+ current tuning) → its fretboard diagrams*. In v1 this is **curated data + a tuning transform**, not a runtime generator.

The scale you pick is consumed only by the chord engine; voicing resolution only ever receives an already-built individual chord plus the tuning. This separation is why supporting all 14 scale types costs voicing resolution nothing.

### Why not CAGED (recorded, so the reasoning isn't lost)

An earlier draft used a CAGED shape-transposition engine. It was **abandoned** because it is a dead end for the tuning-aware future the product wants: CAGED shapes are defined by standard tuning's open-string intervals, the real tuning-adaptive feature (FP-3) is a from-scratch fretboard **generator** that uses no CAGED shapes at all, and a CAGED runtime engine would be thrown away rather than extended. Voicings are therefore **data-driven** from day one, on a model the future generator can populate without a rewrite.

## Sub-Phases

| Sub-Phase | Name | Description |
|---|---|---|
| 8.1 | Diatonic chord engine | *Scale → ordered diatonic chords.* Build triads and seventh chords by stacking thirds on each degree of any of the 14 `ScaleType`s; derive quality and Roman numeral from the actual intervals; spell notes via the existing `ScaleSpeller`. Pure Kotlin, exhaustively tested. Supersedes the Phase 2 `Mode`-based stub. |
| 8.2 | Voicing data, tuning model & loader | New fretboard model (`GuitarTuning`, `FretMark`, `Voicing`, variable string count); the chord-keyed **curated JSON** voicing library for standard tuning, produced by a **throwaway offline generator** and human-curated; the runtime loader + validation; and the **uniform-offset** tuning classification + fret-shift transform (tier 2). |
| 8.3 | ViewModel, state, seeding & tuning source | Root/mode selection (persisted), `TRIADS/7THS` toggle, the chord list, selected-chord → voicings, the one-time **seed** from Key Finder's top result (A-minor fallback), and reading the **tuner's current tuning** to choose the tier-1/tier-2 path. |
| 8.4 | UI — chord list screen | `ChordFinderScreen` per `DESIGN.md` §8.4 and `Chord_Finder___triads.png` / `…7ths.png`: the two dropdowns, the toggle, degree-coloured chord rows with note pills, the tap-to-voicings affordance. |
| 8.5 | UI — voicings screen + `FretboardDiagram` | The new reusable, **variable-string-count** `FretboardDiagram` composable (spec written into `DESIGN.md` §6), the 2-column voicings grid, a tuning indicator, and list→detail navigation. |

> Sub-phase plans and requirements for all five are written up front. They may be refined at the start of each sub-phase, but the intent is captured now.

## Reference Material

Before starting any sub-phase, read:

- `APP_SPECIFICATION.md` → "Module: Chord Finder" (and the conflict note below)
- `DESIGN.md` → §8.4, §6, §2.4 (degree colours), §2 (colour), §3 (type)
- `IMPLEMENTATION_NOTES.md` → "Music Theory Primitives"
- `DECISIONS.md` → all entries, especially Phase 7.1 (`ScaleType` / `ScaleSpeller`) and the Phase 8 entries this phase adds
- `FUTURE_PLANS.md` → FP-1, FP-2, FP-3 — what is deliberately *out*, so the model leaves the right seams
- `CLAUDE.md` → §3, §4, §6, §8 (dependencies), §14
- This file, then the relevant `Phase8_N-PLAN.md` / `Phase8_N-REQUIREMENTS.md`
- The mockups: `Chord_Finder___triads.png`, `Chord_Finder___7ths.png`, `Chord_Finder___C_voicings.png`

> **Conflict note for the agent.** `APP_SPECIFICATION.md` scopes Chord Finder to the **7 diatonic modes**, **triads + a 7th toggle**, and **no fretboard voicings**. This phase **supersedes** that on: (1) Chord Finder targets the **14 `ScaleType`s** Key Finder supports; (2) the module gains a **guitar-voicings screen** that **respects the current tuning** (standard + uniform offsets in v1). The 7th *toggle* on the list screen is retained; 7th-chord *voicings* are deferred (FP-2). Where this plan and `APP_SPECIFICATION.md` disagree, **this plan wins**. Recorded in `DECISIONS.md` during 8.1.

---

## Stage 1 — The Chord Engine (authoritative)

### Inputs and outputs

Input: a **root pitch class** (0–11), a **`ScaleType`** (one of the 14), and `includeSeventhChords`. Output: an ordered `ChordFinderResult` of 7 `DegreeChord`s, one per scale degree, in degree order I…vii.

### How each chord is derived

The engine does **not** assume the major scale's quality pattern; it derives each chord from the *actual* scale, so harmonic minor, melodic minor, and every mode come out correct automatically:

1. Spell the scale's 7 notes in degree order via `ScaleSpeller.scaleNoteNames(root, type)` (letter-per-degree, from Phase 7.1).
2. For degree *i* (0-based) the **triad** is degrees *i*, *i*+2, *i*+4 (mod 7) — 1st, 3rd, 5th stacked in thirds; the **seventh chord** adds degree *i*+6.
3. **Quality is read from the intervals** (semitones above the root):

| 3rd | 5th | Triad quality |
|---|---|---|
| 4 | 7 | **Major** |
| 3 | 7 | **Minor** |
| 3 | 6 | **Diminished** |
| 4 | 8 | **Augmented** |

These four are the only triads two stacked thirds can produce.

4. **Seventh quality** combines the triad with the 7th interval. The full set the 14 types can produce:

| Triad | 7th | Chord | Suffix |
|---|---|---|---|
| Major | 11 | major seventh | `maj7` |
| Minor | 10 | minor seventh | `m7` |
| Major | 10 | dominant seventh | `7` |
| Diminished | 10 | half-diminished | `m7♭5` |
| Diminished | 9 | diminished seventh | `°7` |
| Minor | 11 | minor-major seventh | `mMaj7` |
| Augmented | 11 | augmented-major seventh | `maj7♯5` |

The last three appear because we support the harmonic/melodic-minor families (e.g. A harmonic minor: i = `AmMaj7`, III+ = `Cmaj7♯5`, vii° = `G♯°7`). No special-casing — quality is interval-derived.

### Roman numerals, names, spelling

Roman numeral case/symbol from triad quality: uppercase major/augmented, lowercase minor/diminished; `°` diminished, `+` augmented. Quality abbreviation (`MAJ/MIN/DIM/AUG`) beneath. Names and notes come from the spelled scale degrees, so they are letter-correct for free. Triad suffixes match the mockup (`C / Dm / Bdim`); degree colours per `DESIGN.md` §2.4 (mint major, cyan minor, amber diminished, violet augmented).

### Why all 14 scale types are safe

A triad is two stacked thirds → only major, minor, diminished, or augmented, all in scope. So the scale can never produce a chord the voicing layer can't represent. **A harmonic minor** → `Am, B°, C+, Dm, E, F, G♯°`: all four qualities, all coverable.

---

## Stage 2 — Voicing Resolution (authoritative)

### v1 scope

- **Triad qualities only** (maj/min/dim/aug). Seventh-chord voicings → FP-2.
- **Root-position only** (lowest sounding string = root). Inversions/slash → FP-1.
- **Tuning tiers 1–2 only:**
  - **Tier 1 — standard 6-string** (E2 A2 D3 G3 B3 E4): curated diagrams ship and render directly.
  - **Tier 2 — uniform offsets of standard 6-string** (E♭/D/C♯/… standard): handled by transform (below).
  - **Tier 3 — non-uniform / other string counts** (Drop D, DADGAD, opens, 7-/8-string): **not in v1** → FP-3 (runtime generator). The model and diagram are built to accept them, but no v1 data or generation serves them.

### Data model — curated, chord-keyed, tuning-parameterized

Voicings are **data, not computed at runtime** in v1. The library is **keyed by chord identity (root pitch class + triad quality)** — never by key/mode, so each shape is stored once and curated once. Stage 1 decides which chords belong to a mode; the data never encodes that.

```kotlin
data class GuitarTuning(
    val id: String,                   // e.g. "standard_6", "eb_standard_6"
    val openNotes: List<Note>,        // low → high; size = string count
) {
    val stringCount: Int
    /** Δ in semitones if this is a uniform offset of [base] with equal string count, else null. */
    fun uniformOffsetFrom(base: GuitarTuning): Int?
}

data class Voicing(
    val labelKey: Int,                // neutral, position-based label (see "Labels")
    val baseFret: Int,                // lowest occupied fret (1 = nut region)
    val marks: List<FretMark>,        // one per string, low → high; size = stringCount
    val fingers: List<Int?>,          // one per string, finger 1–4 or null
    val barre: Barre?,                // barre fret + string span, or null
    val rootStringIndices: Set<Int>,  // strings sounding the root → mint dots
    val fretRange: IntRange,          // for the "FR x–y" label
    val category: VoicingCategory,    // OPEN | BARRE | SHAPE — DERIVED, not stored
    val bassDegree: ChordToneRole,    // ROOT in v1 — the FP-1 seam
)

sealed interface FretMark {
    data object Open : FretMark
    data object Muted : FretMark
    data class Fretted(val fret: Int) : FretMark
}
enum class VoicingCategory { OPEN, BARRE, SHAPE }
enum class ChordToneRole { ROOT, THIRD, FIFTH, OTHER }   // only ROOT used in v1
```

`marks`/`fingers` are **variable length = string count**, so 7-/8-string data (FP-3) needs no model change. `category` is derived (contains `Open` → OPEN; else has `Barre` → BARRE; else SHAPE). `bassDegree` is always `ROOT` now so an FP-1 source can populate the same model.

### Labels (CAGED names dropped)

The mockup's "A-shape / E-shape" names are **not used** — they are a standard-tuning-only idea and meaningless under the future model. Voicings use **neutral, position-based labels** (e.g. the index `01…` plus the `FR x–y` range and the `OPEN/BARRE/SHAPE` tag). This is a deliberate divergence from `Chord_Finder___C_voicings.png`, recorded in `DECISIONS.md`.

### Producing the v1 data (throwaway offline generator + curation)

The curated standard-tuning JSON is produced **once, offline**, by a **throwaway dev-only generator** (not shipped runtime code): given standard tuning + a chord's pitch classes, it enumerates fretboard positions, keeps root-position playable candidates, and dumps **developer-readable JSON** keyed by chord. A developer then **prunes** awkward/unplayable entries by hand and the curated file ships as an app asset. Because the generator is offline and its output is human-reviewed, its fuzzy "is this playable?" heuristic does **not** need runtime test coverage — the **shipped artifact is static data**, and the runtime loader is deterministic. (This generator is also the first draft of the FP-3 runtime engine.)

### Runtime behaviour

- **Loader (data layer):** parse the curated JSON asset into `Voicing`s, keyed by chord. A **validation test** asserts every entry: notes ⊆ the chord, root in the bass, `marks` size = tuning string count, frets in range, `category`/`fretRange`/`rootStringIndices` consistent.
- **Tier 1:** look up the chord, render directly.
- **Tier 2 (uniform offset Δ):** the user selected a chord by name and expects diagrams that **sound** as that chord. With the instrument tuned down Δ semitones, each curated standard voicing is **shifted up Δ frets** to preserve the sounding pitch. **Movable (fully-fretted) voicings transpose cleanly. Open-position voicings cannot shift while staying open, so they are shown only at Δ = 0 and omitted otherwise.** Voicings whose shifted position runs off the fretboard are filtered out. *(Stated as decided per the product owner's tuning model; flag if you'd rather preserve grips over sounding pitch.)*
- **Tier 3:** out of v1 — show the standard-tuning voicings with a clear "shown for standard tuning" note, **or** disable the grid with a pointer to FP-3. *(Exact fallback confirmed in 8.5.)*

### Ordering & cap

Order by ascending `baseFret` (near-nut first). Cap at `MAX_VOICINGS` (proposed 5). Diminished/augmented naturally yield fewer. *(Finalised in 8.2.)*

### JSON parser dependency

Loading JSON needs a parser. Options: **kotlinx.serialization** (clean, but a **new dependency → requires approval** per `CLAUDE.md` §8) or Android's built-in **`org.json`** (no new dependency, more manual). Decide at the start of 8.2.

---

## The Tuning Source (cross-module, authoritative)

Chord Finder must know the **current tuning**, which is owned by the Tuner. Phase 8 introduces a small app-scoped, in-memory **`SelectedTuningStore`** holding the tuner's currently-selected preset (as a `GuitarTuning`), exposed as a `StateFlow`. The Tuner writes its selection there; Chord Finder reads it. If the Tuner has not been used this session, the default is **standard 6-string**. Chord Finder **inherits** this tuning and shows a **read-only tuning indicator** on the voicings screen. *(Whether Chord Finder also offers an in-screen tuning override picker is deferred to an 8.5 decision; inherit-only is the v1 default.)* This is the second cross-module coupling Phase 8 adds (alongside `LatestKeyResultStore`); recorded in `DECISIONS.md` during 8.3.

---

## Key Finder → Chord Finder Seeding (authoritative)

**Seed once, then user-owned.** Chord Finder persists its own last-used `{root, mode}`. The Key Finder result chooses the **initial** selection only when there is no persisted selection yet:

- Persisted user selection exists → use it; ignore Key Finder.
- Else Key Finder has a current top result → seed `{root, mode}` from it **1:1** (root pitch class + scale type — direct, since Chord Finder supports all 14 types).
- Else → default to **A natural minor (A Aeolian)**.

After any manual Root/Mode change, the selection is persisted and **never** overwritten by Key Finder again.

**Mechanism.** An app-scoped, in-memory **`LatestKeyResultStore`** (`StateFlow<ScaleMatch?>`), written by `KeyFinderViewModel`, read once by `ChordFinderViewModel` for the seed. In-memory suffices — the seed is a convenience; Chord Finder's persisted selection is the durable state. Recorded in `DECISIONS.md` during 8.3.

---

## Display Rules

**List screen** (`DESIGN.md` §8.4 + triads/7ths mockups): kicker `CHORD FINDER · DIATONIC`, title `{Root} {ModeLabel}`, info icon; Root + Mode dropdowns (Mode `flex 1.4`, Root `flex 1`, 42dp); `{n} CHORDS` + `TRIADS / 7THS` toggle; one row per degree — Roman numeral coloured per §2.4, quality abbreviation beneath, chord name in Space Grotesk SemiBold, note pills, trailing chevron.

**Voicings screen** (`Chord_Finder___C_voicings.png`, minus CAGED names): back arrow + kicker `VOICINGS · {CHORD}`; title `{Root} {Quality}` with note pills (root pill mint); `{n} SHAPES` + a `ROOT` legend; a **read-only tuning indicator** (e.g. `TUNING · E♭ STD`); a scrollable **2-column grid** of cards, each with an index (`01…`), the `FretboardDiagram`, the `FR x–y` range, and the `OPEN/BARRE/SHAPE` tag. Root dots mint, others neutral, per the §6 diagram spec added in 8.5.

---

## Architecture & Package Notes

- **`chordfinder/`** — feature-first Clean Architecture: `domain/` (chord engine, models, use cases), `data/` (JSON loader, selection persistence, `ChordFinderService` impl), `presentation/` (ViewModel, two screens), `di/`.
- **Reuse from `common/`** — `Note`, `NoteName`, `ScaleType`, `ScaleFamily`, `ScaleSpeller`, `MusicTheory`. Chord engine spells via `ScaleSpeller`.
- **Phase 2 stubs** — `MusicTheory.buildTriads/buildSeventhChords` fleshed out in 8.1; `ChordFinderInput` changes from `Mode` to `ScaleType` (recorded supersession).
- **Fretboard model** (`GuitarTuning`, `FretMark`, `Voicing`) lives in `chordfinder/domain/` for now; promote to `common/` only when a second consumer appears (the Phase 7.2 "promote on second consumer" precedent). The **curated JSON** ships as a `chordfinder` asset.
- **Offline generator** — a dev-only/throwaway artifact (e.g. a standalone JVM `main`), **not** in the shipped app graph; it produces the JSON that is then curated and committed.
- **`SelectedTuningStore`** and **`LatestKeyResultStore`** — app-scoped singletons (shared location), the only cross-module couplings Phase 8 adds.
- **`FretboardDiagram`** — reusable, variable-string-count composable in `ui/components/`, drawn with Compose `Canvas`; its visual spec is written into `DESIGN.md` §6.

---

## Decisions to Record in `DECISIONS.md`

1. **14 `ScaleType`s**, not 7 modes — *supersedes* `APP_SPECIFICATION.md`. *(8.1)*
2. **Interval-derived** quality & Roman numerals; the seven seventh qualities enumerated. *(8.1)*
3. **Guitar voicings added** to the module — *supersedes* `APP_SPECIFICATION.md`. *(8.1)*
4. **No CAGED runtime engine; voicings are curated, chord-keyed data**; the why-not-CAGED rationale. *(8.2)*
5. **Voicing scope:** root-position, triad qualities, tiers 1–2 (standard + uniform offset). Inversions/slash → FP-1, 7ths → FP-2, non-uniform/other tunings → FP-3. *(8.2)*
6. **Uniform-offset transform:** shift movable voicings by Δ to preserve sounding pitch; omit open voicings off-standard; filter off-neck. *(8.2)*
7. **CAGED shape names dropped** for neutral position-based labels (divergence from the mockup). *(8.2)*
8. **Variable string count** baked into `Voicing` + `FretboardDiagram` from day one. *(8.2 / 8.5)*
9. **`Voicing.bassDegree` seam** retained (always `ROOT`) for FP-1. *(8.2)*
10. **JSON parser choice** (kotlinx.serialization vs `org.json`). *(8.2)*
11. **`SelectedTuningStore`** cross-module tuning source; Chord Finder inherits the tuner's tuning. *(8.3)*
12. **Key Finder seeding** (seed once, 1:1, A-minor fallback, then user-owned) via **`LatestKeyResultStore`**. *(8.3)*
13. **`FretboardDiagram` design spec** added to `DESIGN.md` §6. *(8.5)*

---

## Dependencies

No new libraries are required by the chord engine or the tuning transform. A **JSON parser** may add one dependency (decision 10) — approve before adding. The fretboard diagram is drawn with Compose `Canvas`.

## Completion Criteria

Phase 8 is complete when 8.1–8.5 meet their requirements **and** the Chord Finder works end-to-end on a real device:

- Picking any Root + Mode (any of the 14 types) lists the 7 diatonic chords with correct numerals, qualities, colours, names, and pills.
- The `TRIADS / 7THS` toggle switches names/pills correctly, including the exotic sevenths from harmonic/melodic minor.
- Tapping a chord opens the voicings grid with correct, playable, root-position diagrams (accurate dots, o/x, barres, position labels, mint-root highlighting, category tags).
- In **standard** tuning the grid is correct; in a **uniform-offset** tuning the diagrams are shifted to sound correct and open voicings are handled per the rule; the indicator shows the active tuning.
- On first open with no prior selection, Chord Finder seeds from Key Finder's top result (or A minor); after any manual change the selection persists and Key Finder no longer overrides it.
- The Tuner, Metronome, and Key Finder are unaffected (the two new stores are additive).
- All unit tests pass (user-verified in Android Studio).
