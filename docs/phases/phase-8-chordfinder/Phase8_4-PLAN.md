# Phase 8.4 — UI: Chord List Screen

## Goal

Build the Chord Finder's first screen exactly as `DESIGN.md` §8.4 and `Chord_Finder___triads.png` / `…7ths.png` specify: kicker + title + info, the Root and Mode dropdowns, the `TRIADS / 7THS` toggle, and the scrollable list of degree-coloured chord rows with note pills. Tapping a row triggers navigation to the voicings screen (the route lands in 8.5; 8.4 exposes the callback).

## Scope

- `chordfinder/presentation/ui/ChordFinderScreen.kt` (stateful + stateless split).
- Row component `ChordDegreeRow.kt`; the two dropdowns; the segmented `TRIADS/7THS` control (reuse `DESIGN.md` §6.8 if a segmented control already exists from the metronome/tuner).
- `chordfinder/presentation/ui/DegreeColor.kt` — pure `ChordQuality → Tq` colour-token mapping.
- `strings.xml` — kicker, dropdown labels, toggle labels, `%d CHORDS`, quality abbreviations.
- Previews; a unit test for the pure colour mapper.

## Out of Scope

- The voicings screen, `FretboardDiagram`, navigation graph wiring → 8.5.
- Any new state logic (all comes from `ChordFinderViewModel`, 8.3).

## Reading Order Before Starting

1. `Phase8-PLAN.md` → "Display Rules" (list screen)
2. `DESIGN.md` → §8.4, §6 (dropdowns, chips, segmented control), §2.4 (degree colours), §2, §3
3. `Chord_Finder___triads.png`, `Chord_Finder___7ths.png`
4. `Phase8_3-PLAN.md` (the state this renders)
5. This file

## Decisions Locked In For 8.4

- ✅ **Two dropdowns** (Root + Mode), Mode wider (`flex 1.4`) than Root (`flex 1`), 42dp tall, per §8.4.
- ✅ **Degree colours**: MAJOR→mint, MINOR→cyan, DIMINISHED→amber, AUGMENTED→violet (`DESIGN.md` §2.4 tokens).
- ✅ **Root label respells with the mode** (uses `ScaleSpeller` root spelling for the active `ScaleType`).
- ✅ **Mode dropdown lists all 14 `ScaleType`s** with their `DESIGN`/`ScaleType` display labels (e.g. "Major · Ionian").

## Implementation Details

### Screen structure (stateless `ChordFinderContent(state, onIntent)`)

- **Header:** kicker `CHORD FINDER · DIATONIC`; title `state.title` (`{Root} {ModeLabel}`) in the display type; trailing info icon (tap → an info sheet/dialog; minimal placeholder acceptable, content can be a short explainer).
- **Selectors row:** Root dropdown (12 entries, labelled via `ScaleSpeller.rootName(pc, state.scaleType)`), Mode dropdown (14 `ScaleType`s). Each opens the app's existing dropdown/menu component; selection fires `onIntent(SetRoot/SetScaleType)`.
- **Count + toggle row:** `{n} CHORDS` (always 7) on the left with the small status dot; the `TRIADS / 7THS` segmented control on the right firing `onIntent(ToggleSevenths)`.
- **Chord list:** `LazyColumn` of `ChordDegreeRow` for each `DegreeChord`, with the §8.4 card styling and spacing.

### `ChordDegreeRow`

Left block: the Roman numeral in `DegreeColor.of(quality)` (Space Mono / numeral style per §3), the quality abbreviation (`MAJ/MIN/DIM/AUG`) beneath in muted type. A thin divider. Main block: chord `symbol` in Space Grotesk SemiBold; a row of **note pills** (each note name as a mini-tag per §6.2). Trailing: chevron. The whole row is clickable → `onIntent(SelectChord(degreeChord))`. Min tap target 48dp.

### `DegreeColor.kt`

```kotlin
object DegreeColor {
    fun of(quality: ChordQuality): Color = when (quality) {
        ChordQuality.MAJOR -> Tq.Mint
        ChordQuality.MINOR -> Tq.Cyan
        ChordQuality.DIMINISHED -> Tq.Amber
        ChordQuality.AUGMENTED -> Tq.Violet
    }
}
```
Exhaustive `when` (no `else`), so a future quality is a compile error. Uses the real `Tq` token names from `DESIGN.md` §2.

### Stateful wrapper

`ChordFinderScreen(viewModel: ChordFinderViewModel = hiltViewModel(), onChordSelected: (ChordKey, String) -> Unit)` collects state with lifecycle awareness and forwards `SelectChord` to `onChordSelected` (the nav callback wired in 8.5).

## Tests

- `DegreeColorTest` — each quality maps to the correct token; `when` is exhaustive.
- Compose UI tests are optional per `CLAUDE.md` (androidTest); if added, assert 7 rows render, the toggle switches symbols (e.g. `C` ↔ `Cmaj7`), and a row click invokes the callback.

## Steps

1. `DegreeColor` + test. 2. `strings.xml` entries. 3. `ChordDegreeRow` + preview. 4. Dropdowns + segmented toggle (reuse components). 5. `ChordFinderContent` + `ChordFinderScreen` + previews (triads and sevenths states; light/dark). 6. Visual check against both mockups. 7. Append any 8.4 note to `DECISIONS.md` (only if a design interpretation was made). 8. Hand off.

## Completion Criteria

See `Phase8_4-REQUIREMENTS.md`.
