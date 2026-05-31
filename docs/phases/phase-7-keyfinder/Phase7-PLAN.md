# Phase 7 — Key Finder Implementation

## Goal

Implement the Key Finder module fully. Given a set of notes the user assembles (by picking from a dropdown or by playing them into the microphone), the app identifies which scales/modes best match those notes, scored live as a percentage, ranked, and shown as the top results. By the end of this phase the Key Finder must be functional, tested, and production-quality on a real device.

This is the third real module, after the Guitar Tuner (Phase 5) and Metronome (Phase 6). Chord Finder follows as Phase 8.

## Approach

Phase 7 is divided into four sub-phases for incremental implementation and testing. Each sub-phase has its own `Phase7_N-PLAN.md` and `Phase7_N-REQUIREMENTS.md`. Sub-phases are completed in order — each builds on the previous. The sub-phases are sized so that each can be implemented in a single focused session without exhausting context.

## Sub-Phases

| Sub-Phase | Name | Description |
|---|---|---|
| 7.1 | Scale model & matching engine | Extend the shared music-theory layer with all 14 scale types, conventional spelling, and the scoring + ranking engine. Pure Kotlin, no Android, exhaustively unit-tested. |
| 7.2 | Shared audio & note detection | Promote the tuner's `AudioRecord` capture + YIN pitch detection into a shared `audio/` module, refactor the tuner to consume it, and build Key Finder's stable-note detector and `RECORD_AUDIO` handling. |
| 7.3 | ViewModel & state | The note-list state (add by dropdown, add by mic, remove, mark/unmark root, dedupe by pitch class), the ≥3-note gate, and live re-ranking on every change. |
| 7.4 | UI | `KeyFinderScreen` per `DESIGN.md` §8.3 (chip rail, add-note picker, mic affordance, ranked result cards), the self-contained result detail view, and the mic-permission-denied state. |

> Sub-phase plans and requirements for all four sub-phases are written up front in this phase (`Phase7_1` … `Phase7_4`). They may be refined at the start of each sub-phase if an earlier sub-phase surfaces something, but the intent is captured now.

## Reference Material

Before starting any sub-phase, read:

- `APP_SPECIFICATION.md` → "Module: Key Finder"
- `DESIGN.md` → §8.3 (Key Finder), §6.2 (Chips), §6.3 (Note pills), §2 (colour), §3 (type)
- `IMPLEMENTATION_NOTES.md` → "Music Theory Primitives" and "Audio"
- `DECISIONS.md` → all entries (especially the Phase 5 audio decisions and the Phase 7 entries this phase adds)
- `CLAUDE.md` → §3 (structure), §4 (SOLID), §6 (testing)
- This file, then the relevant `Phase7_N-PLAN.md` / `Phase7_N-REQUIREMENTS.md`

> **Conflict note for the agent.** `APP_SPECIFICATION.md` describes Key Finder against the 7 diatonic modes and a "% of input notes that belong to the mode" score, with a separate ranking step for the tonic. This phase **supersedes** both of those: the scale set is expanded to 14 types (below), and the score formula folds the tonic into the percentage (below). These supersessions are recorded in `DECISIONS.md` during 7.1. Where this plan and `APP_SPECIFICATION.md` disagree on Key Finder scoring or scale set, **this plan wins**; do not silently follow the older spec.

---

## The Scoring Model (authoritative)

All matching reduces user input to **pitch classes** (0–11, octave-agnostic, de-duplicated). For each candidate scale `S` (a root pitch class `r` plus its 7 member pitch classes):

```
n         = number of distinct input pitch classes
rootMarked = true if the user has marked one of their notes as the root
covered    = | inputPitchClasses ∩ S.pitchClasses |
rootBonus  = 1 if (rootMarked and S.root == markedRoot) else 0
points     = covered + rootBonus
maxPoints  = max(7, n) + (rootMarked ? 1 : 0)
score      = points / maxPoints                  // a fraction in [0, 1]
percent    = round(score * 100)                  // round half up
```

- A scale with `covered == 0` scores 0% and is **excluded** from results. (Because the marked root is always one of the input notes, `rootBonus == 1` implies `covered ≥ 1`, so no scale can surface on the bonus alone.)
- Every scale in the inventory is a **7-note scale**, so the denominator floor of 7 is uniform — there is no per-scale-size special case and no secondary tier. (This is why pentatonics were deliberately excluded.)

### Two result flags

- **`isFull`** (badge `FULL`): every input note is contained in the scale (`covered == n`). The scale accounts for everything the user played. This can be true at any percentage (e.g. 3 notes that all fit a scale → `FULL` at 43%).
- **`isRootMatch`** (badge `TONIC`): `rootMarked` and the scale is rooted on the marked note.

### Canonical worked examples (these become the engine's unit tests)

| Input pitch classes | Marked root | Candidate scale | covered | bonus | points | maxPoints | percent |
|---|---|---|---|---|---|---|---|
| C E G (n=3) | — | any scale containing C, E, G | 3 | 0 | 3 | 7 | **43%** |
| C E G (n=3) | C | a C-rooted scale containing C, E, G | 3 | 1 | 4 | 8 | **50%** |
| C E G (n=3) | C | A-rooted scale containing C, E, G (e.g. A Aeolian) | 3 | 0 | 3 | 8 | **38%** |
| C D E F G A B (n=7) | — | any of the 7 modes of C major | 7 | 0 | 7 | 7 | **100%** |
| C D E F G A B (n=7) | A | A Natural Minor (Aeolian) | 7 | 1 | 8 | 8 | **100%** |
| C D E F G A B (n=7) | A | C Major / other same-note sibling | 7 | 0 | 7 | 8 | **88%** |
| C D E F G A B + B♭ (n=8) | — | C Major (the B♭ is a stray) | 7 | 0 | 7 | 8 | **88%** |

The third and sixth rows are the point of the root: they are how the engine separates scales built from the *same* notes (A natural minor vs C major and their five siblings), which note containment alone can never do.

---

## Scale Inventory (14 scale types × 12 roots = 168 scales)

Each scale type carries a semitone interval pattern from its root (7 offsets), a primary display label, and a descriptive subtitle. `{r}` is the conventionally-spelled root (see "Conventional spelling" below). The diatonic patterns are identical to the existing `Mode` enum (single source of truth — see "Architecture notes").

| # | Scale type | Family | Pattern (semitones) | Primary label | Subtitle |
|---|---|---|---|---|---|
| 1 | Ionian | Diatonic | 0 2 4 5 7 9 11 | `{r} Major` | `{r} Ionian` |
| 2 | Dorian | Diatonic | 0 2 3 5 7 9 10 | `{r} Dorian` | `{r} minor with raised 6` |
| 3 | Phrygian | Diatonic | 0 1 3 5 7 8 10 | `{r} Phrygian` | `{r} minor with lowered 2` |
| 4 | Lydian | Diatonic | 0 2 4 6 7 9 11 | `{r} Lydian` | `{r} major with raised 4` |
| 5 | Mixolydian | Diatonic | 0 2 4 5 7 9 10 | `{r} Mixolydian` | `{r} major with lowered 7` |
| 6 | Aeolian | Diatonic | 0 2 3 5 7 8 10 | `{r} Natural Minor` | `{r} Aeolian` |
| 7 | Locrian | Diatonic | 0 1 3 5 6 8 10 | `{r} Locrian` | `{r} minor with lowered 2 & 5` |
| 8 | Harmonic Minor | Harmonic minor | 0 2 3 5 7 8 11 | `{r} Harmonic Minor` | `{r} minor with raised 7` |
| 9 | Phrygian Dominant | Harmonic minor | 0 1 4 5 7 8 10 | `{r} Phrygian Dominant` | `{r} major with lowered 2 & 6` |
| 10 | Locrian ♮6 | Harmonic minor | 0 1 3 5 6 9 10 | `{r} Locrian ♮6` | `{r} Locrian with a natural 6` |
| 11 | Melodic Minor | Melodic minor | 0 2 3 5 7 9 11 | `{r} Melodic Minor` | `{r} minor with raised 6 & 7` |
| 12 | Lydian Dominant | Melodic minor | 0 2 4 6 7 9 10 | `{r} Lydian Dominant` | `{r} Lydian with a lowered 7` |
| 13 | Altered | Melodic minor | 0 1 3 4 6 8 10 | `{r} Altered` | `{r} Super Locrian` |
| 14 | Dorian ♭2 | Melodic minor | 0 1 3 5 7 9 10 | `{r} Dorian ♭2` | `{r} Dorian with a lowered 2` |

Excluded deliberately (recorded in `DECISIONS.md`): pentatonic and blues scales; the remaining (non-curated) modes of harmonic and melodic minor; any double-harmonic / Byzantine parents.

The subtitle strings above are the intended defaults; they live in `res/values/strings.xml` and may be reworded without touching logic.

---

## Ranking & Display Rules

1. Compute `percent` for all 168 scales.
2. Exclude any with `covered == 0` (0%).
3. Sort by:
   - `percent` descending, then
   - **scale-type order** (the "common-first" order below), then
   - **root pitch class** ascending (C, C♯/D♭, D, … B).
4. Take the **top 7**.
5. The first result gets the mint-mixed "top match" treatment from `DESIGN.md` §8.3.

**Scale-type order (common-first), used as the tie-break:**

```
Major (Ionian) → Natural Minor (Aeolian) → Dorian → Phrygian → Lydian → Mixolydian → Locrian
→ Harmonic Minor → Phrygian Dominant → Locrian ♮6
→ Melodic Minor → Lydian Dominant → Altered → Dorian ♭2
```

**Match gate.** Results are only computed and shown when there are **≥ 3 distinct input pitch classes**. Below that, the screen shows the idle/empty state. Re-ranking is **live** — it recomputes on every add, remove, or root change, with no search/start button. (Exact mic debounce handled in 7.3.)

---

## Conventional Spelling Rule

Result roots and the notes shown in the detail view use **conventional key spelling**, not raw sharps. Two parts:

1. **Root spelling** comes from a deterministic per-pitch-class table chosen to minimise accidentals for that scale and to prefer the musically usual spelling (e.g. `B♭` not `A♯`, `F♯` not `G♭`, `E♭` not `D♯`). The table is specified in `Phase7_1-PLAN.md`.
2. **Within a scale**, the 7 degrees are spelled **one letter name per degree** (A–G each used once), with whatever accidental the interval pattern requires. This yields proper notation — e.g. A Harmonic Minor spells as `A B C D E F G♯` (the raised 7th lands on the G letter), never `A♭`.

Full correctness for the rarest enharmonic edge cases (double-flats in remote altered-scale roots) is acceptable to approximate; the requirement is "musically sensible, deterministic, and letter-correct per degree," asserted by the spelling tests in 7.1.

---

## Architecture & Package Notes

- **`common/model/ScaleType.kt`** (new) — the 14-type enum, each carrying its `ScaleFamily`, interval pattern, and label keys. It is a **superset** of the existing diatonic `Mode` enum. `Mode` is retained unchanged for the tuner and the upcoming Chord Finder. A 7.1 test asserts the 7 diatonic `ScaleType` patterns equal the corresponding `Mode` patterns, so the interval data has a single source of truth.
- **`common/util/ScaleSpeller.kt`** (new) — pure conventional-spelling utility (a top-level `object`, permitted by the `CLAUDE.md` §4 pure-utility exception).
- **`audio/` (new top-level module)** — a shared home for `AudioRecord` capture and YIN pitch detection, promoted out of `tuner/data/` in 7.2 so both Tuner and Key Finder consume it. This is a **deviation** from the feature-first layout in `CLAUDE.md` §3 (which names only `common/` and `ui/` as shared). It is justified because audio capture is an Android-dependent cross-feature concern that does not belong in the "pure music theory" `common/` package, and duplicating it into each module would violate DRY. Recorded in `DECISIONS.md` during 7.2.
- **`keyfinder/`** — standard feature-first Clean Architecture: `domain/` (scale catalog, `MatchScalesUseCase`, result models), `data/` (stable-note detector, repositories), `presentation/` (ViewModel, UI), `di/`.

---

## Decisions to Record in `DECISIONS.md`

These are settled by the planning conversation and must be appended (dated, append-only) during the sub-phase that implements them:

1. **Match-scoring formula** — `points / maxPoints` with `points = covered (+1 root match)` and `maxPoints = max(7, n) (+1 if root marked)`; stray notes lower the score; supersedes the `APP_SPECIFICATION.md` "% of input notes" definition. *(7.1)*
2. **Single root note**, set by tapping a note in the list; folds into the percentage and breaks same-note ties. *(7.1)*
3. **Scale inventory** — the 14 types above; pentatonics, blues, and the non-curated harmonic/melodic modes are out of scope. *(7.1)*
4. **Ranking** — score desc, then common-first scale-type order, then root pitch ascending; top 7 shown, 0% hidden; supersedes the `APP_SPECIFICATION.md` separate-tonic-ranking step. *(7.1)*
5. **Match gate** — ≥ 3 distinct pitch classes; live recompute, no action button. *(7.1 for the rule, 7.3 for the wiring)*
6. **Conventional enharmonic spelling** for result roots and detail notes. *(7.1)*
7. **`ScaleType` superset** added to `common/`; `Mode` retained. *(7.1)*
8. **Shared `audio/` module** — promote tuner pitch detection; structure deviation justified. *(7.2)*
9. **Result row opens a self-contained detail view** (scale notes + degrees); no cross-navigation into Chord Finder this phase. *(7.4)*

---

## Dependencies

No new third-party libraries. YIN already exists in the tuner and is only being relocated. Conventional spelling and matching are plain Kotlin.

## Completion Criteria

Phase 7 is complete when all sub-phases 7.1–7.4 meet their individual requirements **and** the Key Finder works end-to-end on a real device:

- The user can add notes from the dropdown and by playing them into the microphone, and remove any note.
- The user can mark exactly one note as the root, and unmark it.
- With ≥ 3 distinct notes, the ranked results appear and update live on every change, with the correct percentages, `FULL` / `TONIC` badges, and top-match styling per `DESIGN.md` §8.3.
- Tapping a result opens the detail view showing that scale's notes and degrees, conventionally spelled.
- The mic permission-denied state renders correctly and routes the user to grant access.
- The tuner still works exactly as before (audio promotion in 7.2 is parity-preserving).
- All unit tests pass (user-verified in Android Studio).
