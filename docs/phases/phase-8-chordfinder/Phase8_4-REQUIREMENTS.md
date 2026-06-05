# Phase 8.4 — Requirements & Acceptance Criteria

8.4 delivers the chord list screen. Most acceptance is visual (user-verified against the mockups); the pure colour mapper is unit-tested.

## Agent Responsibilities

### `ChordFinderScreen` / `ChordFinderContent`
- [ ] Kicker `CHORD FINDER · DIATONIC`, title `{Root} {ModeLabel}`, info affordance — per §8.4.
- [ ] Root dropdown (12, respelled for the active mode) and Mode dropdown (all 14 `ScaleType`s); widths per §8.4 (Mode `flex 1.4`, Root `flex 1`, 42dp).
- [ ] `{n} CHORDS` count + `TRIADS / 7THS` segmented toggle wired to the ViewModel.
- [ ] `LazyColumn` renders one `ChordDegreeRow` per `DegreeChord`.
- [ ] Stateful wrapper collects state lifecycle-aware and forwards chord selection to `onChordSelected`.

### `ChordDegreeRow`
- [ ] Roman numeral coloured per quality; quality abbreviation beneath; chord symbol; note pills; trailing chevron.
- [ ] Entire row clickable, ≥ 48dp tap target.

### `DegreeColor`
- [ ] Exhaustive `when` mapping the four qualities to the correct `Tq` tokens (mint/cyan/amber/violet), no `else`.

### Design fidelity
- [ ] Uses only `DESIGN.md` tokens (no raw hex/sp/dp literals where a token exists); matches the spacing, type, and styling of §8.4 and the two mockups.

### Tests
- [ ] `DegreeColorTest` passes. Any optional Compose UI tests pass.

### Documentation Updates
- [ ] A `DECISIONS.md` entry only if a design ambiguity was resolved (e.g. info-sheet content). No spec change otherwise.

### Code Quality
- [ ] No `TODO()`. KDoc on public composables. All text in `strings.xml`. Stateless content separated from the Hilt wrapper. Previews for triads + sevenths, light + dark.

### Handoff
- [ ] Summary lists files; notes navigation is stubbed via `onChordSelected` pending 8.5.

## User Responsibilities (Verification in Android Studio)
- [ ] Build + run on device; the list screen matches `Chord_Finder___triads.png` and (toggled) `…7ths.png`.
- [ ] Root/Mode/toggle update the list correctly; tapping a row triggers the (temporary) callback without crashing.
- [ ] Run All Tests succeeds.

## Decision Log
- [ ] Any design-interpretation entry recorded; otherwise none required.
