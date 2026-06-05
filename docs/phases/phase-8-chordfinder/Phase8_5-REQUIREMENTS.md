# Phase 8.5 — Requirements & Acceptance Criteria

8.5 completes the module end-to-end. Visual fidelity is user-verified; the render-model mapper is unit-tested.

## Agent Responsibilities

### `FretboardDiagram` (`ui/components/`)
- [ ] Stateless `Canvas` driven by `FretboardRenderModel` (never the domain `Voicing`).
- [ ] Draws strings, frets, nut/position label, finger dots with numerals, `o`/`x` markers, and barre.
- [ ] Root dots use the mint token; other dots the neutral token.
- [ ] Lays out correctly for **6, 7, and 8** strings.
- [ ] No interactivity, no animation; all sizes via `DESIGN` tokens.

### `VoicingRenderModel` mapper
- [ ] Pure (no Android Canvas); windows frets around `baseFret`; selects nut vs `Nfr`; maps marks/roots/barre correctly.

### `ChordVoicingsScreen`
- [ ] Back arrow + `VOICINGS · {CHORD}` kicker; title + note pills (root mint); `{n} SHAPES`; `ROOT` legend; tuning indicator.
- [ ] 2-column grid of cards (index, diagram, `FR x–y`, `OPEN/BARRE/SHAPE` tag).
- [ ] Uniform-offset header annotation; tier-3 "shown for standard tuning" note above the still-rendered standard voicings.
- [ ] No CAGED shape names anywhere.

### Navigation
- [ ] List → voicings route wired with `ChordKey` + display name; back returns to the list; bottom-nav `CHORD` tab intact.

### Tests
- [ ] `VoicingRenderModelMapperTest` covers `Phase8_5-PLAN.md` → "Tests" (incl. 6/7/8-string and high-position windowing). Optional Compose UI tests pass.

### Documentation Updates
- [ ] `DESIGN.md` §6 gains the `FretboardDiagram` component entry derived from the mockup (with the CAGED-name omission noted).
- [ ] `DECISIONS.md` records the §6 addition and the tier-3 fallback presentation.

### Code Quality
- [ ] No `TODO()`. KDoc on public composables/mapper. Text in `strings.xml`. Stateless component + Hilt wrapper split. Previews for open/barre/high-position/7-string and standard/uniform-offset/unsupported states.

### Handoff
- [ ] Summary lists files; states Phase 8 is complete end-to-end.

## User Responsibilities (Verification in Android Studio)
- [ ] Build + run on device. End-to-end: pick a key → list is correct → tap a chord → voicings grid matches `Chord_Finder___C_voicings.png` (minus shape names).
- [ ] In standard tuning the diagrams are correct; set the tuner to E♭ standard → the indicator updates and diagrams shift to sound correct (open voicings handled); set Drop D → the tier-3 note shows.
- [ ] On a fresh install with Key Finder used, the Chord Finder opens seeded to Key Finder's top result; after a manual change it stays put.
- [ ] Run All Tests succeeds; Tuner/Metronome/Key Finder unaffected.

## Decision Log
- [ ] The §6 component and tier-3 fallback entries are recorded before the phase closes.
