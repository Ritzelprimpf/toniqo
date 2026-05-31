# Phase 7.4 — Key Finder UI

## Goal

Build the `KeyFinderScreen` exactly to `DESIGN.md` §8.3 and the `Key Finder — result list` mockup, rendering the 7.3 `KeyFinderUiState`: the note chip rail with the tonic treatment and add button, the note picker, the mic affordance, the live ranked result cards with `TONIC`/`FULL` badges and top-match styling, the self-contained result detail view, and the mic permission-denied state. No new business logic — this layer only renders state and forwards intents.

By the end of 7.4 the module is complete and works end-to-end on a device.

## Scope

- `keyfinder/presentation/ui/KeyFinderScreen.kt` and its sub-composables.
- Note chip rail, add-note picker (dropdown), mic toggle, header (`NOTES · n` / `TONIC · x` / `N MATCHES` / `TONIC PREFERRED`).
- Result cards (rank, primary label, subtitle, badges, percent, chevron) with the first-result mint treatment.
- Result detail view (scale notes + degrees), self-contained.
- Mic permission-denied state.
- Idle/empty state (< 3 notes).
- Label/subtitle rendering via `ScaleType` resource keys + `ScaleSpeller`.

## Out of Scope

- Any change to scoring, ranking, detection, or state shape (owned by 7.1–7.3).
- Cross-navigation into Chord Finder.

## Reading Order Before Starting

1. `DESIGN.md` → §8.3 (Key Finder), §6.1–6.3 (buttons, chips, note pills), §6.6 (cards), §2 (colour), §3 (type), §7 (icons), §9 (motion), §13 (accessibility)
2. The `Key Finder — result list` mockup
3. `Phase7-PLAN.md` → "Ranking & Display Rules", "Conventional Spelling Rule"
4. `Phase7_3-PLAN.md` (the `KeyFinderUiState` contract)
5. This file

## Design Anchors (from `DESIGN.md` §8.3, do not invent values)

- **Input rail:** `bg.inset`, `r.md`, min-height 56dp, padded; chips wrap to a second row as needed (the mockup shows 6 + add on row 1, then the 7th + add).
- **Note chip:** 30dp tall, `kicker`-mono at 12sp. The **root** chip = 22% mint mix over `bg.elev2` plus a `· TONIC` mono suffix in mint.
- **Add-note button:** 30×30dp dashed-border circle with `plus` icon.
- **Result row:** 12dp padding, `r.md`. **First result** = 6% mint over `bg.elev1` with a mint-mixed border.
- **Row contents:** zero-padded 2-digit rank (mono), primary label, badges (`TONIC` mint-outlined, `FULL` neutral-outlined), `mono.micro` subtitle, percent on the right at `h2` in mint for the top match / `fg.primary` otherwise, then `chevron-right`.
- All colour/type/spacing come from the `Tq` tokens and `MaterialTheme.colorScheme.*`. No raw hex, sp, or dp literals outside tokens (`CLAUDE.md` §14, `DESIGN.md` §12).
- Tap targets ≥ 44×44dp even where visuals are smaller (chips, add button, chevron).

## Decisions To Lock At The Start Of 7.4

- [ ] **Add-note picker form.** The mockup shows a `+` opening note entry. Recommended: a compact bottom sheet / menu of the 12 pitch classes (chromatic), tap to add; already-present notes shown disabled. Confirm form (sheet vs inline grid).
- [ ] **Mic affordance placement.** A listen toggle (mic icon, `MIC LIVE`-style indicator when active, mirroring the tuner's mic indicator language from §8.1). Decide placement (e.g. in the input rail header next to the add button, or a toggle in the screen header). The icon set already includes `mic`.
- [ ] **Mark-root gesture.** How the user marks a chip as root. Recommended: tap a chip cycles its role, or long-press to mark root with tap-to-remove — pick one and keep it discoverable. Confirm.
- [ ] **Detail view form.** Sheet vs full screen. Recommended: a bottom sheet (lighter, keeps the list context), showing the scale's conventionally-spelled notes with scale-degree labels (1, ♭2/2, … 7), the primary label + subtitle, and the percent/badges. Confirm.
- [ ] **Remove-note gesture.** Recommended: an `×` affordance on the chip, or tap-to-remove from a chip menu. Confirm and keep consistent with the mark-root gesture so the two don't collide.

## Implementation Details

- **Screen skeleton:** header (kicker `KEY FINDER`, title `Match notes`, info button), `NOTES · n` / `TONIC · x` sub-header line, the input rail, the `N MATCHES` / `TONIC PREFERRED` results header, the scrollable result list (top 7), bottom nav (already global). The `TONIC PREFERRED` label shows only when a root is marked.
- **State source:** collect `KeyFinderUiState` via `collectAsStateWithLifecycle()`. The screen forwards taps to the ViewModel intents from 7.3 — no local state beyond ephemeral UI (sheet open/closed).
- **Card label rendering:** for each `ScaleMatch`, resolve the primary label and subtitle from `match.candidate.type`'s string-resource keys with the spelled root from `ScaleSpeller.rootName(...)`; the detail view uses `ScaleSpeller.scaleNoteNames(...)`. This is the one place display strings are built.
- **Idle/empty state (< 3 notes):** the results area shows a quiet prompt (e.g. "Add at least 3 notes") in `fg.tertiary`, per the design's restrained tone — no spinner, no empty illustration (`DESIGN.md` §12 forbids illustrations).
- **Reduced motion:** card press and any sheet enter follow `DESIGN.md` §9; respect the reduced-motion rules.
- **Mic permission-denied state:** a single `ToniqoCard` with the `mic` icon, an explanation, and a primary "Grant access" button that opens system app settings — mirror the tuner's permission-denied pattern (built in Phase 5) and reuse its component if one exists.
- **Live updates:** because the ViewModel re-ranks on every change, the list animates/recomposes as notes are added or the root changes; there is no search/start control anywhere on the screen.

## Tests

UI logic is thin, but cover what has branching:

- A small **label-mapping** test (can be a plain unit test on a pure mapping function): `ScaleMatch` → (primary label, subtitle) resolves to the right resource key + spelled root for representative types (Major, Natural Minor, Phrygian Dominant, Altered).
- **Compose UI tests** (encouraged from Phase 3 onward per `IMPLEMENTATION_NOTES.md`) for the key flows: adding 3 notes surfaces results; the first card has the top-match styling and mint percent; marking a root adds the `TONIC` badge to the matching card and shows `TONIC PREFERRED`; tapping a card opens the detail view with the correct notes; the < 3-note idle prompt shows; the permission-denied card renders when permission is absent.
- Snapshot/visual parity is user-verified against the mockup on device.

## Steps

1. Lock the start-of-phase UX decisions (picker, mic placement, mark-root + remove gestures, detail form); record in `DECISIONS.md`.
2. Build the note chip + root treatment + add button; the input rail.
3. Build the add-note picker and wire `addNoteFromPicker`.
4. Build the result card (rank, label, badges, subtitle, percent, chevron) + first-result treatment; render the top-7 list.
5. Header line (`NOTES · n`, `TONIC · x`, `N MATCHES`, `TONIC PREFERRED`) and idle/empty state.
6. Mic toggle + listening indicator; wire `startListening`/`stopListening`.
7. Mic permission-denied state (reuse tuner pattern).
8. Result detail view (notes + degrees) via `ScaleSpeller`.
9. The label-mapping unit test + Compose UI tests for the flows.
10. `DECISIONS.md` entry for the result-row → self-contained detail view, plus the UX gesture decisions.
11. Hand off; module is now end-to-end. Verify the full Phase 7 completion criteria from `Phase7-PLAN.md`.

## Completion Criteria

See `Phase7_4-REQUIREMENTS.md`. On completion, re-check the module-level "Completion Criteria" in `Phase7-PLAN.md`.
