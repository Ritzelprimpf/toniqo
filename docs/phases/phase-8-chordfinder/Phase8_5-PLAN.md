# Phase 8.5 — UI: Voicings Screen & FretboardDiagram

## Goal

Build the voicings detail screen and the new, reusable, variable-string-count `FretboardDiagram` component, then wire list → detail navigation. By the end of 8.5 the Chord Finder is complete end-to-end: tap a chord, see its playable voicings drawn correctly for the active tuning. Also write the `FretboardDiagram` spec into `DESIGN.md` §6.

## Scope

- `ui/components/FretboardDiagram.kt` — Compose `Canvas` diagram (reusable design-system component).
- `chordfinder/presentation/ui/ChordVoicingsScreen.kt` + voicing card + `VoicingRenderModel` mapper.
- Navigation: add the list and voicings routes to the app `NavHost`; pass `ChordKey` + display name.
- `DESIGN.md` §6 — new component entry for `FretboardDiagram`, derived from `Chord_Finder___C_voicings.png`.
- `strings.xml` — VOICINGS kicker, `%d SHAPES`, ROOT legend, TUNING indicator, `OPEN/BARRE/SHAPE` tags, `FR %d–%d`, tier-3 note.
- Previews; unit test for the pure render-model mapper.

## Out of Scope

- Any change to voicing data/logic (done in 8.2/8.3).
- Non-uniform tuning rendering beyond the tier-3 note (FP-3).

## Reading Order Before Starting

1. `Phase8-PLAN.md` → "Display Rules" (voicings screen), "Stage 2"
2. `DESIGN.md` → §6, §2, §3 (and note §6 currently lacks this component)
3. `Chord_Finder___C_voicings.png` (the authoritative visual — minus CAGED names)
4. `Phase8_2-PLAN.md` (the `Voicing` model), `Phase8_3-PLAN.md` (`ChordVoicingsViewModel`)
5. This file

## Decisions Locked In For 8.5

- ✅ **Variable string count** (6/7/8) supported by the component from day one.
- ✅ **Neutral labels** (index + `FR` range + category tag); **no CAGED names**.
- ✅ **Root dots mint**, other dots neutral; `o`/`x` above the nut; barre as a rounded bar; position label `Nfr` when `baseFret > 1`.
- ✅ **2-column grid**; **read-only tuning indicator**; tier-3 shows standard voicings + a "shown for standard tuning" note (the v1 fallback; an in-screen tuning picker remains deferred).
- ✅ **New `DESIGN.md` §6 component entry** authored from the mockup.

## Implementation Details

### `ui/components/FretboardDiagram.kt`

A stateless `Canvas` composable driven by a render model — never the domain `Voicing` directly:

```kotlin
data class FretboardRenderModel(
    val stringCount: Int,
    val fretWindow: Int,               // visible frets, e.g. 5
    val positionLabel: String?,        // "3fr" when not at the nut, else null
    val showNut: Boolean,              // true only at the nut region
    val dots: List<Dot>,               // string, fretWithinWindow, finger?, isRoot
    val barre: BarreSpan?,             // fretWithinWindow, fromString, toString
    val openStrings: Set<Int>,         // 'o'
    val mutedStrings: Set<Int>,        // 'x'
)
```
Drawing rules (parameterise all sizes via `DESIGN` tokens): vertical string lines (count = `stringCount`), horizontal fret lines (`fretWindow + 1`), a thick nut line when `showNut`; finger dots as filled circles with the finger number centred (root dots use the mint token, others the neutral dot token); `o`/`x` glyphs above their strings; the barre as a rounded rectangle spanning its strings at its fret; the `Nfr` label to the left when `positionLabel != null`. Must lay out correctly for 6, 7, and 8 strings (width scales with `stringCount`). No interactivity, no animation.

### `VoicingRenderModel` mapper (pure, tested)

`fun Voicing.toRenderModel(): FretboardRenderModel` — windows the frets around `baseFret`, sets `showNut`/`positionLabel` (`baseFret <= 1` → nut, no label; else label `"${baseFret}fr"`), converts `marks` to dots/open/muted, maps `rootStringIndices` to `isRoot`, carries the barre. Pure, no Android Canvas — fully unit-testable.

### `ChordVoicingsScreen`

Per `Chord_Finder___C_voicings.png` minus shape names: back arrow + kicker `VOICINGS · {CHORD}`; title `{Root} {Quality}` with note pills (root pill mint); a row with `{n} SHAPES` + status dot, the `ROOT` legend (mint dot + label), and the **tuning indicator** (e.g. `TUNING · E♭ STD`, from `state.tuningLabel`); a `LazyVerticalGrid(columns = Fixed(2))` of voicing cards. Each card: index `01…`, the `FretboardDiagram`, the `FR x–y` range, the `OPEN/BARRE/SHAPE` tag chip. When `state.tier == UNIFORM_OFFSET`, optionally annotate the header with the offset (e.g. "−1 semitone"). When `state.tier == UNSUPPORTED`, show the "shown for standard tuning" note above the grid (still rendering the standard voicings).

### Navigation

Add to the app `NavHost`: a Chord Finder list route and a `chordVoicings/{rootPc}/{quality}/{name}` route. `ChordFinderScreen`'s `onChordSelected` navigates with the `ChordKey` + display name; `ChordVoicingsScreen` obtains its `ChordVoicingsViewModel` (Hilt, nav-arg-aware) and a back action. Keep the bottom-nav `CHORD` tab behaviour intact.

### `DESIGN.md` §6 addition

Add a "Fretboard Diagram" component entry: anatomy (strings, frets, nut, position label), dot spec (size, finger numeral, mint-root vs neutral), `o`/`x` markers, barre rendering, variable string count, and the tokens used. Reference `Chord_Finder___C_voicings.png` as the source and note the deliberate omission of CAGED shape names.

## Tests

- `VoicingRenderModelMapperTest` — nut vs `Nfr` selection; dot/open/muted conversion; root flags; barre mapping; correct windowing for a high-position voicing; 6/7/8-string inputs produce the right `stringCount`.
- Optional Compose UI tests: grid shows `{n}` cards; back navigation works; tier-3 note appears for an unsupported tuning.

## Steps

1. `FretboardRenderModel` + mapper + tests. 2. `FretboardDiagram` Canvas + previews (open chord, barre, high position, 7-string). 3. Voicing card + `ChordVoicingsScreen` + previews (standard, uniform-offset, unsupported). 4. Navigation routes + wire `onChordSelected` + back. 5. `strings.xml`. 6. `DESIGN.md` §6 component entry. 7. Append 8.5 decision to `DECISIONS.md`. 8. End-to-end check against the mockup. 9. Hand off (phase complete).

## Completion Criteria

See `Phase8_5-REQUIREMENTS.md`.
