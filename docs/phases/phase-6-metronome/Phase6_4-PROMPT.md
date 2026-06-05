# Kickoff Prompt — Phase 6.4

You are starting **Phase 6.4 — Metronome UI** of the Toniqo Android app. This is the final sub-phase of Phase 6 and the user-visible deliverable.

## What you are doing

Building the actual Compose UI for the metronome screen, per `DESIGN.md` §8.2 and the approved mockups. By the end of 6.4, the metronome tab is a fully functional, polished feature — no more placeholders, no more debug harnesses.

You also write the canonical-document updates: `APP_SPECIFICATION.md`, `DESIGN.md`, and `DECISIONS.md` get the user-facing and architectural decisions from the Phase 6 planning sweep folded in.

Phases 6.1, 6.2, and 6.3 must be complete before starting 6.4. You consume `MetronomeViewModel` (with its full state surface including `tempoDescriptor` and `isInitialLoadComplete`), the `TempoDescriptor` enum, the `MetronomeEvent` channel, and all the design tokens from Phase 3.

## Read first, in this order

1. `CLAUDE.md` — house rules.
2. `APP_SPECIFICATION.md` → "Module: Metronome".
3. **`DESIGN.md` → §8.2 carefully** — this is the visual authority for the metronome screen. Also §6.1 (tap targets, button heights), §6.4 (bottom nav), §6.6 (cards), §7 (icons), §2 (color tokens), §3 (typography).
4. **The two approved UI mockups** (running 4/4 and stopped 7/8) — these are the design authority for visible structure and reveal elements that emerged during mockup review.
5. `DECISIONS.md` — all entries.
6. `Phase6-Metronome-Decisions.md` — Items 3, 6, 9, 14, **18 (revised)**, 19, 22, **23**.
7. `Phase6_3-PLAN.md` and the actual 6.3 code — the ViewModel surface this UI consumes.
8. **`Phase6_4-PLAN.md`** — the implementation plan.
9. **`Phase6_4-REQUIREMENTS.md`** — the acceptance checklist.

If anything in the plan conflicts with `DESIGN.md` or the mockups, **raise the conflict** — do not silently choose. Same rule applies if a value (corner radius, spacing, exact text style) is not specified anywhere.

## How to work

- Follow the **Steps** section of `Phase6_4-PLAN.md` in order.
- The mockups revealed several UI elements not originally in the spec — Item 23 of the decision log enumerates them. Build them: page status kicker (`METRONOME · RUNNING/STOPPED` with pulsing dot), beat indicator header (`BEAT · X / N` + beat-unit label), tempo card as grouped container, "SUBDIVIDE" verb-form kicker label.
- Item 18 was **revised** after mockup review — Start/Stop button is now icon + text ("Start" / "Stop"), not icon-only. The decision log entry explains the revision.
- The page title "Metronome" and the sun/theme-toggle icon at top-right are **app-shell scaffolding**, not metronome-specific. If they're already part of the app shell from Phase 3 / 4, integrate with them; if not, that's tracked separately and Phase 6.4 doesn't implement them.
- **`MetronomeContent` is stateless** — takes `MetronomeUiState` and lambdas, no `ViewModel` reference. Only `MetronomeScreen` (the top-level composable) touches the ViewModel via `hiltViewModel()`. This makes the content composable previewable and JVM-testable.
- The beat indicator's 80ms color transitions **override reduced-motion** — that's intentional and documented. Don't add a reduced-motion check that disables the flash; the flash is the whole point of the metronome.
- `KEEP_SCREEN_ON` is added via `DisposableEffect` keyed on `isPlaying`. `onDispose` covers both key change to false AND composable leaving the composition — same single rule, multiple lifecycle paths.
- All strings live in `res/values/strings.xml`. Kicker labels are stored in normal case; uppercasing comes from the `mono.micro` text style, not from hardcoded all-caps in the resource.
- Icon `contentDescription = null` on the Start/Stop pill — the visible text label is what TalkBack reads. Setting both would cause double-read.
- BPM display is wrapped in `NonScalingText` (from Phase 3) — fixed 96dp regardless of system font-size setting.
- Remove the interim placeholder from 6.3 — the nav graph now points `metronome_route` to the new `MetronomeScreen`.

## Documentation updates required (this is part of the work)

Per Item 22 + revised Item 18 + Item 23:

- **`APP_SPECIFICATION.md`** — add the seven user-facing additions listed in `Phase6_4-PLAN.md` step 20.
- **`DESIGN.md` §8.2** — add the design-layer additions listed in step 21 (revised Start/Stop content, status kicker, beat header, tempo card grouping, "SUBDIVIDE" kicker).
- **`DECISIONS.md`** — verify entries exist for the architecturally-significant items listed in step 22; consolidate if duplicated across sub-phases. Add entries for the revised Item 18 and the Item 23 additions if not already there.

## Hand-off

When you're done:
1. Hand-off summary lists all files added, modified, and removed.
2. State clearly: **"Phase 6 is complete. The metronome is a fully functional feature."**
3. List the manual QA items from `Phase6_4-REQUIREMENTS.md` → "User Responsibilities" — these are the final acceptance gate. The user verifies on a real device.
4. If the user reports an audio click sounds wrong during QA, adjust the relevant `ClickParameters` constant (from 6.1) and record the change in `DECISIONS.md`. Tuning starting values during 6.2/6.4 is expected.
5. **You do not run the build or the app yourself.**

When the user reports issues, fix them and hand off again. When all checkboxes in `Phase6_4-REQUIREMENTS.md` are satisfied, Phase 6 is done and Toniqo has a metronome.
