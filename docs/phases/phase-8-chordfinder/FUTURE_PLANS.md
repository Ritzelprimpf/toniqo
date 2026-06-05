# Toniqo — Future Plans

This document is the **deferred-scope backlog**, the deliberate counterpart to `DECISIONS.md`:

- `DECISIONS.md` records what we **decided to build**.
- `FUTURE_PLANS.md` records what we **decided to *defer*** — features considered, scoped out on purpose, with the reasoning preserved so we don't re-derive it later.

Nothing here is committed work. An item graduates into a real phase only when promoted to a `PhaseN-PLAN.md` / `PhaseN-REQUIREMENTS.md` pair (per `PROJECT_PLAN.md`). When that happens, mark the entry `→ promoted to Phase N` rather than deleting it.

Entries carry a stable ID (`FP-n`) so other documents can reference them.

---

## Background: the Chord Finder voicing architecture (v1, for context)

The deferred items below all build on the v1 voicing design from Phase 8, so the model matters:

- Voicings are **data, not runtime-computed**: a **curated, developer-readable JSON** library, **keyed by chord identity** (root pitch class + quality), **not** by key/mode. The chord engine computes mode membership in code; data is never per-mode.
- The shipped data is produced by a **throwaway offline generator** (tuning + chord notes → candidate positions) and then **human-curated** (prune awkward fingerings). The shipped artifact is static; the runtime loader is deterministic and fully tested.
- The `Voicing` model and the `FretboardDiagram` component support a **variable string count** (6/7/8) from day one.
- `GuitarTuning` is a **first-class parameter** of voicing resolution, and `Voicing.bassDegree` exists (always `ROOT` in v1) as an inversion seam.
- v1 ships **tier 1 (standard 6-string)** and **tier 2 (uniform offsets of standard — E♭/D/C♯ standard, etc.)**, the latter by shifting movable voicings to preserve sounding pitch.

These seams are what make the items below *additive* rather than rewrites.

---

## Index

| ID | Module | Item | Status | Notes |
|---|---|---|---|---|
| FP-1 | Chord Finder | Inversions & slash-chord voicings | Deferred from Phase 8 | Extra chord-keyed data or generator output via the `bassDegree` seam |
| FP-2 | Chord Finder | 7th-chord & extended voicings | Deferred from Phase 8 | Extra chord-keyed data; 7th theory already exists in 8.1 |
| FP-3 | Chord Finder | Tuning-adaptive voicings via a runtime generator | Deferred from Phase 8 | The big differentiator: non-uniform & arbitrary tunings |

---

## FP-1 — Chord Finder: inversions & slash-chord voicings

**Why it's here.** Phase 8 voicings are **root-position only** (lowest sounding string = root). Inversions and deliberate non-root basses were scoped out.

**What is deferred.**
- **First-inversion** (3rd in the bass), **second-inversion** (5th in the bass).
- **Slash chords** (a chosen bass note, possibly outside the triad).

**Design hooks already in place.** `Voicing.bassDegree` (`ROOT / THIRD / FIFTH / OTHER`) ships in v1, always `ROOT`. Inverted voicings populate the **same** model and render in the **same** `FretboardDiagram` with no schema change.

**How it would be built.** Either additional **curated chord-keyed entries** (inversions are a property of the chord, so they slot into the existing per-chord library), or output from the FP-3 generator. The diagram already supports it.

**Open questions when promoted.** Interleave inversions in the grid or behind an `ROOT-POS / INVERSIONS` filter? Slash chords imply a user-chosen bass — likely a separate "chord lookup" entry point rather than the diatonic list.

---

## FP-2 — Chord Finder: 7th-chord & extended voicings

**Why it's here.** The list screen's `TRIADS / 7THS` toggle already changes chord **names/pills** in Phase 8, but the **voicings screen renders triad shapes only**.

**What is deferred.**
- **Diatonic seventh voicings** (`maj7`, `m7`, `7`, `m7♭5`, plus the harmonic/melodic-minor sevenths `°7`, `mMaj7`, `maj7♯5`), so the toggle also drives the voicings screen.
- **Extensions further out** — 9/11/13, `sus2`/`sus4`, `add9`, altered dominants.

**How it would be built.** Additional **chord-keyed curated entries** (sevenths are just more chord identities in the same library), or FP-3 generator output. The **chord theory already exists** — `MusicTheory.buildSeventhChords` is fleshed out in 8.1; this is purely about fretboard *shapes* for those chords.

**Open question when promoted.** Order FP-1/FP-2 separately, or let a single FP-3 generator subsume all three at once.

---

## FP-3 — Chord Finder: tuning-adaptive voicings via a runtime generator

**The product's intended differentiator.** A chord finder that shows correct, playable voicings for **whatever tuning the player is actually in** — Drop D, DADGAD, open G/D/E, 7- and 8-string tunings, and arbitrary user tunings — not just standard and uniform detunes. Existing chord apps largely don't do this; it is a deliberate selling point.

**Why it's here / why v1 stops short.** Voicings depend on the **intervals between open strings**.
- **Uniform offsets** (every string moved the same amount, e.g. E♭/D standard) keep those intervals identical, so a curated standard voicing transposes by a fret shift. **This is in v1 (tier 2).**
- **Non-uniform tunings** (Drop D moves only the low string; DADGAD, opens, most 7-/8-string tunings) change the inter-string intervals, so grips genuinely differ — the same chord needs a *different shape, fingering, and mutes*. You cannot pre-curate this: the space of tunings is unbounded, so curated data is inherently per-tuning and can't cover arbitrary ones.

The only thing that adapts across arbitrary tunings is **generation**: given the tuning's open notes and a chord's pitch classes, compute the note at every string/fret, search for root-position combinations where every sounded string is a chord tone, score for playability, assign fingers, detect barres, dedupe, and rank — all **tuning-agnostic by construction**.

**How it would be built.** Promote the Phase 8 **throwaway offline generator into a real, tested runtime engine** that consumes the existing `GuitarTuning`, emits the existing `Voicing` model, and renders in the existing (variable-string-count) `FretboardDiagram` — so the data path and UI are already in place. New work is concentrated in: the search + playability scoring + finger-assignment logic, and its tests.

**Honest reservations (the reasons it isn't free).**
- **Playability is fuzzy.** "Is this grip reachable?" has no clean oracle, which fights the 100%-runtime-coverage bar. Mitigations: test the *hard* invariants deterministically (correct notes, root in bass, span within a max, frets in range, string count matches tuning) and treat the *ranking/aesthetics* as best-effort, since for arbitrary user tunings heuristic output is acceptable and expected.
- **No curation safety net** for arbitrary tunings — output is whatever the generator produces, unreviewed. (For *common named* non-standard tunings, a curated override layer on top of the generator could restore the human pass — same override pattern v1 uses for standard.)
- **Labels stay neutral** (position-based), already true in v1.

**Cross-module dependency.** Reuse the Tuner's preset model and the v1 `SelectedTuningStore` as the tuning source rather than duplicating tunings. Diagrams already render variable string counts, so 7-/8-string tunings need no UI change.

**Open questions when promoted.** Generate live on the device vs. ship pre-generated data for the *common named* non-standard tunings (Drop D, DADGAD, drop C, open G/D/E) and generate only for truly arbitrary ones? Curated override layer for the common ones? Where in the UI does the user pick a non-standard tuning — inherit from the Tuner only, or a Chord-Finder-local picker?

---

## Parking lot (unscoped, not yet worked through)

- _(none yet)_

---

## Template for new entries

```
## FP-n — <Module>: <short title>

**Why it's here.** What phase deferred it, and why.

**What is deferred.** The concrete capability.

**Design hooks already in place.** Shipped seams that make this additive.

**How it would be built.** Approach, reusing existing model/UI where possible.

**Open questions when promoted.** What still needs deciding.
```
