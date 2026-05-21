# Phase 6.4 — Requirements & Acceptance Criteria

Phase 6.4 produces the actual metronome UI — the final user-visible deliverable for Phase 6. The phase is complete when the agent checklist, the user automated checks, and the manual QA pass all succeed.

## Agent Responsibilities

### `metronome/presentation/ui/MetronomeScreen.kt`

- [ ] Top-level `@Composable` consuming `MetronomeViewModel` via `hiltViewModel()`.
- [ ] Observes `uiState` with `collectAsStateWithLifecycle`.
- [ ] Collects `viewModel.events` in a `LaunchedEffect`; on `AudioUnavailable`, shows the snackbar with `R.string.metronome_error_audio_unavailable`.
- [ ] Wraps content in a `Scaffold` with a `SnackbarHost`.
- [ ] Uses `KeepScreenOnWhilePlaying(isPlaying = state.isPlaying)`.
- [ ] Delegates layout to a stateless `MetronomeContent` composable.

### `metronome/presentation/ui/MetronomeContent.kt`

- [ ] Stateless: takes `MetronomeUiState` plus all interaction lambdas as parameters.
- [ ] Layout matches `DESIGN.md` §8.2 and the approved mockups: status kicker → page title → tempo card → beat indicator header → beat indicator → signature/subdivide row → bottom row (tap + start/stop pill).
- [ ] Spacings and dimensions per `DESIGN.md` §8.2 and the mockups.
- [ ] No hardcoded colors, typography, or sizes — all values come from the `Tq.*` design tokens established in Phase 3.
- [ ] BPM display is clickable; click opens `BpmInputDialog`.
- [ ] Slider `valueRange = 1f..300f`, `steps = 298`, snaps to integer BPMs.
- [ ] +/− buttons trigger flat ±1 changes; no press-and-hold.
- [ ] Time signature dropdown items are exactly the 8 supported signatures from `MetronomeConfig.SUPPORTED_SIGNATURES`.
- [ ] Subdivide dropdown shows all four `Subdivision` values with strings from `strings.xml`.
- [ ] Page title "Metronome" rendered per app-shell convention (title style from Phase 3).

### `metronome/presentation/ui/MetronomeStatusKicker.kt`

- [ ] `@Composable` taking `isPlaying: Boolean`.
- [ ] Renders a row in `Tq.type.mono.micro` with secondary text color.
- [ ] When `isPlaying = true`: shows the pulsing mint dot followed by `R.string.metronome_status_running` ("Metronome · Running").
- [ ] When `isPlaying = false`: no dot; shows `R.string.metronome_status_stopped` ("Metronome · Stopped").
- [ ] Pulsing dot uses ~1-second alpha cycle (full ↔ ~30%). Reuses any existing `PulsingDot` primitive; otherwise adds one in `ui/components/`.

### `metronome/presentation/ui/TempoCard.kt`

- [ ] Single visually-grouped container — rounded corners, subtle border, slight elevation — per `DESIGN.md` §8.2 (or §6.6 if a shared card primitive exists from Phase 4).
- [ ] Contains, in order: "TEMPO" kicker label → BPM display → tempo descriptor → slider row.
- [ ] Slider row layout: [`−` button (36dp circle) | `Slider` (1fr) | `+` button (36dp circle)].
- [ ] Stateless — takes `bpm`, `tempoDescriptor`, and interaction lambdas as parameters.
- [ ] No padding / corner / border literals — all from design tokens.

### `metronome/presentation/ui/BeatIndicatorHeader.kt`

- [ ] `@Composable` taking `currentBeat: Int`, `numerator: Int`, `denominator: Int`.
- [ ] Renders a row with `SpaceBetween` arrangement.
- [ ] Left side: formatted `R.string.metronome_beat_header_format` ("Beat · %1$d / %2$d") with `currentBeat + 1` (1-indexed display) and `numerator`.
- [ ] Right side: beat-unit label — `R.string.metronome_beat_unit_quarter_notes` if `denominator == 4`, `R.string.metronome_beat_unit_eighth_notes` if `denominator == 8`.
- [ ] Both texts use `Tq.type.mono.micro` and secondary text color.
- [ ] Beat counter always displays at least 1 (even when stopped at `currentBeat = 0`, the formatted value is "Beat · 1 / N").
- [ ] Helper to map denominator to label res id is co-located in this file (or extension on `MetronomeConfig`); errors loudly on unsupported denominators (should never happen given `SUPPORTED_SIGNATURES`).

### `metronome/presentation/ui/KeepScreenOnWhilePlaying.kt`

- [ ] `@Composable` that takes `isPlaying: Boolean`.
- [ ] Adds `FLAG_KEEP_SCREEN_ON` on the window when `isPlaying == true`.
- [ ] Clears the flag in `onDispose` (covers both key change to false AND composable leaving the composition).
- [ ] Uses a `findActivity()` helper that walks `ContextWrapper` chain.
- [ ] Returns gracefully (no crash) if no `Activity` is found.

### `metronome/presentation/ui/BeatIndicator.kt`

- [ ] Renders `numerator` segments equally weighted in a row.
- [ ] Segment 0 = beat 1; gets the 4dp mint dot when unlit and the mint glow (12dp) when lit.
- [ ] Other lit segments use mint at 35% alpha over `bg.elev2`.
- [ ] Unlit segments use `bg.elev1` with `line.faint` border.
- [ ] Color transitions animated with `animateColorAsState`, `tween(80, LinearEasing)`.
- [ ] **Reduced-motion exception:** The 80ms transitions run regardless of system reduced-motion preference. KDoc explains why.
- [ ] All segments unlit when `isPlaying = false`.

### `metronome/presentation/ui/BpmDisplay.kt`

- [ ] BPM rendered via `NonScalingText` at `Tq.type.display.xl` (96dp mono, font-size-setting-immune).
- [ ] Tempo descriptor below, `Tq.type.mono.micro`, secondary color.
- [ ] `TempoDescriptor` enum is mapped to a string resource via an extension property `labelResId`.
- [ ] Clickable surface invokes `onClick` lambda.

### `metronome/presentation/ui/BpmInputDialog.kt`

- [ ] Material 3 `AlertDialog` with a single text field.
- [ ] Text input filters non-digit characters and limits to 3 characters.
- [ ] Keyboard type is `KeyboardType.Number` with `ImeAction.Done`.
- [ ] OK button enabled iff `text.toIntOrNull() in 1..300`.
- [ ] OK or keyboard "Done" action calls `onConfirm` with the parsed value.
- [ ] Cancel or dismiss invokes `onDismiss` without committing.
- [ ] No inline error messages while typing — just OK enabled/disabled state.
- [ ] Initial value populated from the current BPM.

### `metronome/presentation/ui/TimeSignatureDropdown.kt` + `SubdivideDropdown.kt`

- [ ] Pill-style dropdowns, 44dp height, side-by-side at 1fr each.
- [ ] Mono kicker labels above each, both backed by string resources:
  - Time signature: `R.string.metronome_signature_label` ("Signature").
  - Subdivide: `R.string.metronome_subdivide_label` ("Subdivide") — verb form per Item 23c.
- [ ] Time signature dropdown items: only the 8 supported pairs from `SUPPORTED_SIGNATURES`. No 5/8 or other unsupported combinations exposed.
- [ ] Subdivide dropdown items: all four enum values with the noun-form display strings ("None", "Eighth notes", "Sixteenth notes", "Eighth triplets").

### `metronome/presentation/ui/PlayStopButton.kt`

- [ ] Pill shape (60dp tall, flex 1 width).
- [ ] **Stopped state:** ▶ `play` icon followed by "Start" text. Mint primary background, 24dp glow.
- [ ] **Running state:** ⏸ `pause` icon followed by "Stop" text. `bg.elev3` background, no glow.
- [ ] Icon and text both visible; icon sits to the left of the text with spacing per `DESIGN.md` §8.2 (raise the conflict if unspecified).
- [ ] Text strings: `R.string.metronome_start` ("Start") and `R.string.metronome_stop` ("Stop").
- [ ] Icon `contentDescription` is `null` (the visible text label is what screen readers announce — setting both would cause TalkBack to read the icon twice).
- [ ] Triggers `onClick` on press.

### `metronome/presentation/ui/TapTempoButton.kt`

- [ ] 60dp circle, `bg.elev2` background.
- [ ] `tap` icon centered (or above a "TAP" label per `DESIGN.md` §8.2).
- [ ] Uppercase "TAP" in `Tq.type.mono.micro`.
- [ ] Triggers `onTapTempo` on each press (no debounce; each press is a tap).

### Resources

- [ ] `res/values/strings.xml` contains every string listed under "Resources" in `Phase6_4-PLAN.md`. New strings added in this phase:
  - `metronome_status_running`, `metronome_status_stopped`
  - `metronome_tempo_label`, `metronome_signature_label`, `metronome_subdivide_label`
  - `metronome_beat_header_format` (positional format args: `%1$d / %2$d`)
  - `metronome_beat_unit_quarter_notes`, `metronome_beat_unit_eighth_notes`
  - `metronome_start`, `metronome_stop`, `metronome_tap_tempo`, `metronome_bpm_dialog_title`
  - `metronome_error_audio_unavailable`
  - `tempo_adagio`, `tempo_andante`, `tempo_moderato`, `tempo_allegro`, `tempo_presto`
  - `subdivision_none`, `subdivision_eighths`, `subdivision_sixteenths`, `subdivision_triplets`
- [ ] Reused strings (`action_ok`, `action_cancel`) verified to already exist.
- [ ] Required icons (`ic_play`, `ic_pause`, `ic_tap`) present as vector drawables. If `tap` was missing from `DESIGN.md` §7, the conflict was raised before assuming.
- [ ] Kicker labels in `strings.xml` are stored in normal case (e.g., "Tempo", "Signature", "Subdivide") — uppercasing comes from the `mono.micro` text style, not from hardcoded all-caps.

### Navigation

- [ ] `metronome_route` (existing from Phase 4) now resolves to the new `MetronomeScreen`. The interim 6.3 placeholder is removed.

### Tests

- [ ] `MetronomeContentTest` covers all behaviors listed under "Tests" in `Phase6_4-PLAN.md`: BPM display rendering, tempo descriptor lookup, status kicker reflects `isPlaying`, beat header shows current beat (1-indexed) and beat-unit label, beat indicator segment count, current beat highlighting, beat-1 marker when stopped, play/pause icon + text swap, all interaction lambdas wired, dropdown contents, subdivide kicker label.
- [ ] `BpmInputDialogTest` covers: initial value population, non-digit filtering, 3-digit cap, OK enabled/disabled logic, OK and Cancel actions.
- [ ] `BeatIndicatorTest` covers: numerator → segment count; correct segment lit; all unlit when stopped.
- [ ] `BeatIndicatorHeaderTest` covers: beat counter is 1-indexed; counter updates with `currentBeat`; numerator visible in "/ N"; denominator → correct beat-unit label.
- [ ] `MetronomeStatusKickerTest` covers: running state text + dot present; stopped state text + no dot.
- [ ] Tests use `createComposeRule()`; ViewModel-free tests run on JVM where possible.

### Documentation Updates

- [ ] `APP_SPECIFICATION.md` updated per Item 22 of the decision log:
  - Tempo descriptor labels with BPM boundary table.
  - Persistence statement.
  - Lifecycle/stop-on-leave statement.
  - Tap tempo user-facing description.
  - Time-signature / beat-unit clarification.
  - Subdivision multiplier explanation + EIGHTHS-in-/8 no-op.
  - Screen-on behaviour.
- [ ] `DESIGN.md` §8.2 updated:
  - **Start/Stop button:** icon + "Start" / "Stop" text (revised Item 18).
  - **Page status kicker** at top of screen (Item 23a).
  - **Beat indicator header row** with `BEAT · X / N` left and beat-unit label right (Item 23b).
  - **Tempo card** as a single visually-grouped container (Item 23d).
  - **"SUBDIVIDE" verb-form kicker** for the subdivision dropdown (Item 23c).
- [ ] `DECISIONS.md` contains entries for (consolidated from prior sub-phases if already present):
  - Synthesis over assets.
  - Anchor-based scheduling.
  - Strict screen-lifecycle binding.
  - No accent customization in v1; forward-compatible extension via optional `accentPattern`.
  - **Start/Stop button revised** to icon + text after mockup review (revised Item 18).
  - **UI structure additions from mockup review** (status kicker, beat header, tempo card, subdivide kicker label — Item 23).
- [ ] Any final adjustments to `ClickParameters` from manual QA recorded in `DECISIONS.md`.

### Code Quality

- [ ] No `TODO("Not yet implemented")`, no `TODO(6.4)` markers remaining anywhere in the metronome module.
- [ ] All composables have KDoc comments.
- [ ] No hardcoded strings, colors, dimensions, or typography — everything comes from resources or design tokens.
- [ ] No `@Preview` composables containing real ViewModel calls; previews use hand-crafted state.

### Handoff

- [ ] Summary lists all files added, modified, and removed.
- [ ] Summary calls out: "Phase 6 is complete. The metronome is a fully functional feature."
- [ ] Summary lists manual QA items the user is asked to perform (see below).
- [ ] Summary notes that any audio adjustments made during QA are tracked in `DECISIONS.md`.

## User Responsibilities (Verification + Manual QA)

### Build & test gates

- [ ] **Gradle sync** completes without errors.
- [ ] **Build → Make Project** completes successfully.
- [ ] **Run → Run All Tests** reports all tests green (unit + Compose tests).
- [ ] App launches on an Android 12+ device or emulator without exceptions in Logcat.

### Visual / design QA

- [ ] Layout matches `DESIGN.md` §8.2 and the approved mockups: status kicker position, tempo card grouping, beat header row, beat indicator height, dropdown row, button placements.
- [ ] **Status kicker** at top reads "Metronome · Running" with pulsing mint dot while playing; "Metronome · Stopped" with no dot while stopped.
- [ ] **Tempo card** renders as a single visually-grouped container (rounded corners, subtle border) wrapping BPM, descriptor, and slider.
- [ ] **Beat indicator header** shows "Beat · X / N" on the left and "Quarter notes" or "Eighth notes" on the right.
- [ ] **Beat-unit label** correctly reflects denominator: 4/4 → "Quarter notes"; 6/8 → "Eighth notes".
- [ ] **Subdivide dropdown** kicker label reads "Subdivide" (verb form).
- [ ] **Start/Stop pill** shows ▶ icon + "Start" text when stopped; ⏸ icon + "Stop" text when running.
- [ ] Colors and typography match the design tokens (mint glow on beat 1; `bg.elev1` for unlit segments; mono fonts for BPM and tempo descriptor).
- [ ] Reduced-motion setting on the device does NOT disable the beat flash animation (intentional override).
- [ ] BPM display stays at fixed 96dp size regardless of system font-size setting (sanity check the `NonScalingText`).

### Interaction QA

- [ ] **Start / Stop:** Tap the play pill (shows ▶ + "Start"). It swaps to ⏸ + "Stop". Metronome ticks. Tap again. It swaps back to ▶ + "Start". Metronome stops.
- [ ] **Status kicker:** While stopped, top kicker reads "Metronome · Stopped" with no dot. Start playback — kicker reads "Metronome · Running" and the leading mint dot pulses.
- [ ] **Beat counter advances:** In 4/4, the "Beat · X / 4" label cycles 1 → 2 → 3 → 4 → 1 as the metronome plays. Counter matches the lit segment.
- [ ] **+/−:** Each tap changes BPM by exactly 1. Audio responds on the next beat.
- [ ] **Slider:** Dragging the slider updates BPM live. Audio responds smoothly (anchor-based; no glitches). After releasing, the persisted value reflects the final position within ~200 ms.
- [ ] **Tap-to-type:** Tap the BPM number. Dialog opens with current value. Type a new value, OK enables once valid. OK commits. Cancel discards.
- [ ] **Tap tempo:** Tap the TAP button repeatedly at ~120 BPM. BPM display converges to ~120 over 3–5 taps. Stop tapping for 3 seconds, then tap once — no BPM change (new session). Tap a second time and BPM updates.
- [ ] **Time signature:** Open dropdown. All 8 signatures present. Select 3/4. Beat indicator now has 3 segments. Audio plays in 3.
- [ ] **Subdivision:** Open dropdown. All 4 values present. Select EIGHTHS in 4/4. Subdivision clicks audible between main beats.
- [ ] **EIGHTHS in /8 no-op:** Set 6/8 + EIGHTHS. Audio is identical to 6/8 + NONE. (Documented expected behaviour.)

### Lifecycle QA

- [ ] **Screen-on:** Start the metronome. Wait the system display timeout (e.g., 30s). Screen stays on.
- [ ] **Screen-on releases on stop:** Stop the metronome. Wait the system display timeout. Screen sleeps as normal.
- [ ] **Tab navigation:** Start. Switch to Tuner tab. Metronome stops. Return to metronome tab. Settings preserved, `isPlaying = false`.
- [ ] **App background:** Start. Press Home. Metronome stops. Reopen the app. Settings preserved, `isPlaying = false`.
- [ ] **Audio focus loss:** Start the metronome. From another app, play music. Metronome stops.
- [ ] **Persistence across launch:** Set BPM 92, 3/4, EIGHTHS. Stop. Force-close the app. Relaunch. Open metronome tab. BPM = 92, signature = 3/4, subdivision = EIGHTHS. State is stopped.

### Audio quality QA

- [ ] Beat 1 is clearly accented (higher pitch / louder).
- [ ] Subdivision clicks are quieter than main beats and have a distinct pitch.
- [ ] No clicks-on-clicks artifacts, no distortion, no clipping at any tempo.
- [ ] Cadence is steady at 60, 120, 240 BPM (listen for 30 seconds each; no audible drift).
- [ ] Sixteenth subdivisions at fast tempos (e.g., 180 BPM × 4 = 12 clicks/sec) still sound clean.
- [ ] If any aspect sounds wrong, the relevant `ClickParameters` constant gets adjusted; the change is recorded in `DECISIONS.md`.

### Error path QA

- [ ] Snackbar appears if `AudioTrack` initialization fails (hard to provoke on a healthy device; observed as needed).

## Decision Log

- [ ] All `APP_SPECIFICATION.md`, `DESIGN.md`, and `DECISIONS.md` updates from Item 22 of the decision log are committed before the phase is marked complete.
- [ ] Any final synthesizer parameter tweaks from manual QA are recorded in `DECISIONS.md`.

## Phase 6 Completion

When this requirements document is fully satisfied:

- [ ] **Phase 6 is complete.** The metronome is a fully functional feature of Toniqo.
- [ ] The `Phase6-Metronome-Decisions.md` planning log can be archived (or kept for reference; not deleted).
- [ ] The next phase target is the Key Finder module (Phase 7).
