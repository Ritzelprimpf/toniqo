# Implementation Prompt — Phase 8.2 (Chord Finder · Voicing Data, Tuning Model & Loader)

> Paste this to start a fresh implementation session for Phase 8.2. Phase 8.1 must be complete. The Python voicing generator (its own prompt) should have produced `voicings_standard_6.json` candidates for you to curate; if not, ask the user for them before authoring the asset by hand.

---

You are implementing **Phase 8.2 — Voicing Data, Tuning Model & Loader** of Toniqo. You write code; the user owns the build, device, and Git. Do not run Gradle or git.

**Before writing any code, read, in this order:**
1. `PROJECT_PLAN.md`, `CLAUDE.md`, `IMPLEMENTATION_NOTES.md`, `DECISIONS.md`.
2. `Phase8-PLAN.md` → "Stage 2 — Voicing Resolution", "The Tuning Source", and the "Why not CAGED" note.
3. `FUTURE_PLANS.md` → FP-1, FP-2, FP-3 — the seams this code must leave open.
4. `Phase8_2-PLAN.md` and `Phase8_2-REQUIREMENTS.md` — your scope.
5. `Chord_Finder___C_voicings.png` — the visual target the data must support.
6. Phase 8.1's output (`ChordQuality`).
7. Voicing JSON file in app/src/res/chords/

**Hard constraints:** as in 8.1 (Kotlin, SOLID, sealed state, `data class` models, no `!!`/magic numbers, strings in `strings.xml`, KDoc, tests alongside). **Stop and ask** on ambiguity.

**Decision to take first (then record in `DECISIONS.md`):** the JSON parser. Default to Android's built-in **`org.json`** (no new dependency). Only use **kotlinx.serialization** if the user explicitly approves the dependency (`CLAUDE.md` §8). Keep the parsed-model tests parser-agnostic.

**Your task this session (data + pure logic; no ViewModel, no UI):**
1. `common/model/GuitarTuning.kt` — `id`, `openNotes`, derived `stringCount`, `STANDARD_6` (E2 A2 D3 G3 B3 E4), and `uniformOffsetFrom(base)` (Δ only when string counts match and every string shares one offset; else null). It lives in `common/` because the Tuner becomes its second consumer in 8.3.
2. `chordfinder/domain/model/` — `FretMark` (sealed: Open/Muted/Fretted), `Barre`, `VoicingCategory`, `ChordToneRole`, `ChordKey`, and `Voicing` with **variable-length** `marks`/`fingers`, **derived** `category`/`fretRange`/`baseFret`, `bassDegree = ROOT`, and a `validated(...)` factory enforcing the five invariants.
3. `chordfinder/domain/VoicingTransposer.kt` — pure tier-2 shift (movable → frets+Δ, fingers/roots unchanged, barre shifted; open voicings → null; off-window → null).
4. `chordfinder/domain/repository/VoicingRepository.kt` + `VoicingLookupResult` (Standard / UniformOffset(offset) / Unsupported), and `chordfinder/data/VoicingRepositoryImpl.kt` + `VoicingJsonParser.kt` — load once + cache, order by ascending `baseFret`, cap at `MAX_VOICINGS`.
5. The **curated** `assets/chordfinder/voicings_standard_6.json` for all 12 roots × {MAJOR, MINOR, DIMINISHED, AUGMENTED}, prepared from the Python generator's candidates (prune unplayable ones).

**The library validation test is the heart of this.** It must load the **shipped** JSON and assert, for every entry: `marks.size == 6`; all five invariants (notes ⊆ chord, root in the bass, span/range valid, `rootStringIndices` exact); and full coverage (every chord key present with ≥1 voicing, C MAJOR including a near-nut open voicing and a barre voicing). Also test `GuitarTuning.uniformOffsetFrom` (E♭→−1, D→−2, standard→0, Drop D→null, 7-string→null), `Voicing` derivations + `validated` throwing, `VoicingTransposer`, and `VoicingRepositoryImpl` across the three tiers.

**When done:**
- Append the 8.2 decisions to `DECISIONS.md`: data-driven chord-keyed voicings + why-not-CAGED; the parser choice; the tier-2 transform rule (shift to preserve sounding pitch, omit open, filter off-neck); neutral labels; variable string count; the `bassDegree` seam.
- Summary: files + asset; note the runtime is deterministic data + transform; flag the parser dependency decision.
- Map the proposal to commits (e.g. `feat: guitar tuning model`, `feat: voicing model + transposer`, `feat: curated standard voicings + loader with validation`). Do not commit.

Confirm you have read the docs and have the generator's JSON (or will ask for it), then proceed file by file with tests.
