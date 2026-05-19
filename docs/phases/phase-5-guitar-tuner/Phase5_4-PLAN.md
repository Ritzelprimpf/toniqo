# Phase 5.4 — Tuner UI

## Goal

Build the complete `TunerScreen` and supporting composables. This is the first real (non-placeholder) module screen in the app. By the end of 5.4, a user with a guitar can install the app, grant microphone access, pick a tuning, and tune the instrument end-to-end. The audio pipeline (`MicrophoneAudioSource` from 5.2, `DetectTunedStringUseCase` and `TunerViewModel` from 5.3) is unchanged in behavior — 5.4 wires it into a UI.

Phase 5.4 also closes documentation debt accumulated through 5.1–5.3: the chromatic-mode addendum to `APP_SPECIFICATION.md`, the §14 Q1/Q2 resolutions in `DESIGN.md`, and the missing formal entries in `DECISIONS.md`.

## Scope

- Replace the Phase 4 `TunerScreen` placeholder with the full screen per `DESIGN.md` §8.1 and the four reference mockups (Idle, Flat, In tune, Sharp).
- Build the screen's composable parts as reusable components under `tuner/presentation/ui/components/`:
  - Needle gauge (Compose `Canvas`).
  - Detected-note hero.
  - Status line (status word + cents, side-by-side).
  - Readout well with Detected/Target Hz pair.
  - String selector row (note pills, checkmarks, semantic outline).
  - Preset chip row with mode popover and `MIC LIVE` indicator.
  - Reference-pitch header kicker.
  - Preset picker (ModalBottomSheet).
  - Tuner settings sheet (432 Hz toggle + auto-advance toggle).
  - Permission-denied card.
- Implement microphone permission flow: first-launch request via Activity Result API; permanent-denial fallback to system app settings.
- Add three new actions to `TunerViewModel`: `onEnterChromaticMode()`, `onAutoAdvanceChanged(enabled: Boolean)`, `onReferencePitchChanged(hz: Double)`.
- Extend `TunerPreferences` with `autoAdvanceEnabled: Flow<Boolean>` and `referencePitchHz: Flow<Double>`.
- Wire the `SharedFlow<TunerEvent>` to a haptic effect (`StringTuned`, `AllStringsTuned`) and to the §8.1 "all strings tuned" mint success ring (320 ms in / 1200 ms hold / 320 ms out per §9).
- Honour reduced-motion (§9): needle drops to 80 ms linear, success ring becomes an instant state change.
- Build against both dark and light palettes (no extra effort if tokens are consumed correctly — verify visually).
- Minimal Compose UI tests for state-to-UI mapping.
- Documentation updates (`APP_SPECIFICATION.md`, `DESIGN.md`, `DECISIONS.md`).

## Out of Scope

- No changes to `DetectTunedStringUseCase` or `YinPitchDetector`.
- No changes to the sustained-tone state machine or auto-advance hold durations.
- No instrumented (`androidTest`) coverage — left to a later hardening pass. Real-device audio paths from 5.2 are verified here by the user.
- No analytics, no error reporting beyond surfacing `CAPTURE_FAILED` in the UI.
- No light-mode trigger UI (settings panel for theme choice). §14 Q5 remains open; the screen renders correctly in both palettes, but how the user *selects* a palette is not part of 5.4.
- No metronome/key-finder/chord-finder work.

## Reading Order Before Starting

1. `APP_SPECIFICATION.md` → "Module: Guitar Tuner" (including the chromatic-mode addendum once written in this phase).
2. `IMPLEMENTATION_NOTES.md` → "Audio", "Conventions".
3. `CLAUDE.md` → §3, §4, §7, §14.
4. `DECISIONS.md` → all entries.
5. `DESIGN.md` → §2 (palettes), §2.4 (cents → colour), §6 (components), §8.1 (tuner spec), §9 (motion), §10 (surfaces), §13 (accessibility), §14 (open questions — note Q1 and Q2 are being closed here).
6. `Phase5-PLAN.md`.
7. The completed sub-phase plans: `Phase5_1-PLAN.md`, `Phase5_2-PLAN.md`, `Phase5_3-PLAN.md`.
8. The four reference mockups (`Idle/Listening`, `Flat`, `In tune`, `Sharp`).
9. This file.

## Decisions Locked In For 5.4

These are settled before implementation begins. Each gets a formal entry in `DECISIONS.md` as part of this phase's documentation step.

### Inherited (re-stating for visibility — already resolved upstream)

- ✅ **Needle gauge** as the v1 readout. (`DESIGN.md` §8.1)
- ✅ **Cents → colour mapping.** (`DESIGN.md` §2.4)
- ✅ **A4 = 432 Hz toggle UI placement:** sun-icon button (top-right of the tuner screen) opens a settings sheet. (`Phase5-PLAN.md`; formally recorded in `DECISIONS.md` here.)
- ✅ **Permission-denied screen:** single `ToniqoCard` with mic icon, explanation, primary "Grant access" button. (`Phase5-PLAN.md`; formally recorded in `DECISIONS.md` here.)
- ✅ **Lifecycle/capture rule:** `WhileSubscribed(5000)` from 5.3; collected with `collectAsStateWithLifecycle` in the screen. 5-second grace on backgrounding is accepted.

### New for 5.4

- ✅ **Mode toggle UI placement:** a `DropdownMenu` anchored to the preset chip (`6-STRING · DROP ▾`). Two items: `Preset` (current) and `Chromatic`. The chip's chevron is the affordance hint. Tapping `Chromatic` calls `onEnterChromaticMode()`. Tapping `Preset` while in chromatic mode restores the previous preset string index (see "Chromatic re-entry policy" below). Tapping `Preset` while already in preset mode dismisses the popover with no action.
- ✅ **Auto-advance toggle:** lives in the settings sheet alongside the 432 Hz toggle. Persisted via `TunerPreferences.autoAdvanceEnabled` (new field, defaults to `true`). When auto-advance is off, sustained-in-tune still triggers `TunerEvent.StringTuned` (so the haptic and check mark fire), but the use case does **not** auto-increment `currentStringIndex` after the 200 ms hold — the user must tap the next string manually.
- ✅ **Reference pitch persistence:** also persisted via `TunerPreferences.referencePitchHz` (new field, defaults to `440.0`). Toggling it in the settings sheet retunes all targets live.
- ✅ **Preset picker surface:** Material 3 `ModalBottomSheet` opened by tapping the preset chip's left half (the `6-STRING · DROP` text, *not* the popover chevron — see "Preset chip interaction" below). Content is the grouped preset catalog from `Phase5_1-PLAN.md`: outer grouping by string count (6 / 7 / 8); inner grouping by category (Standard / Open / Dropped). Tapping a preset closes the sheet and calls `onPresetSelected(presetId)`.
- ✅ **Preset chip interaction model:** the chip is a compound affordance.
  - Tap on the **label** (left side, "6-STRING · DROP"): opens the preset picker sheet.
  - Tap on the **chevron** (right side, `▾`): opens the mode popover.
  - The chevron has its own 44×44 dp tap target. The label area takes the rest. Both behaviors are wrapped so the entire chip remains 26 dp tall per §6.2 (the tap targets extend invisibly).
- ✅ **Chromatic re-entry policy (Option 2):** when the user explicitly enters chromatic mode via the popover, the ViewModel captures `currentStringIndex` into a private field `previousPresetStringIndex: Int?`. When the user taps `Preset` in the popover while in chromatic mode, the ViewModel restores that index (preset mode at that string, `tunedStringIndices` *not* restored — keeping the snapshot minimal). The snapshot is cleared on: the automatic post-success chromatic transition (success has its own start-fresh semantics), `onPresetSelected` (different tuning), and `onStringSelected` (explicit choice overrides). `tunedStringIndices` is intentionally not restored to keep the snapshot minimal and the "I'm peeking" intent clear: the user gets back the *position*, not the entire session.
- ✅ **`SharedFlow<TunerEvent>` collection pattern:** lifecycle-aware via `repeatOnLifecycle(Lifecycle.State.STARTED)` inside a single `LaunchedEffect`. Prevents duplicate-haptic on rotation and prevents replay-after-resume issues that a `LaunchedEffect(Unit) { events.collect { ... } }` would have. See "Event handling" below for the exact pattern.
- ✅ **Needle gauge implementation:** pure Compose `Canvas`, all elements drawn per-frame (sweep arc, ticks, sweet-spot arc, needle, pivot cap). Correctness over micro-optimisation; a future `GraphicsLayer`-based cache is profile-driven, not pre-emptive.
- ✅ **Glow scope:** the only on-screen glow on the tuner screen is the **6 dp needle drop-shadow glow** in the current semantic colour when not idle (already permitted by §8.1). The detected-note hero **letter** has no glow — the mockups show it as flat white. This corrects the "faint semantic glow" wording in the §8.1 prose. The mint dot inside the `MIC LIVE` indicator is rendered at full saturation with no additional halo (it is a 1.25 dp filled circle, not a glow source).
- ✅ **Mockup-derived additions to §8.1:**
  - **Readout-well Hz pair.** Inside the well, bottom row: two `mono.micro` labels ("DETECTED", "TARGET") with `Tq.Type.Body`-equivalent numeric values (e.g., "108.86 Hz" / "110.00 Hz") below them. The detected value reads `— Hz` (em-dash) when no pitch is detected.
  - **Status-line composition.** Status word in current semantic colour (`mono.micro` kicker, uppercase). Cents value in `fg.tertiary` (same style, same line). Format: `FLAT  −18¢`, `IN TUNE  +2¢`, `SHARP  +15¢`. When idle, only `LISTENING —` appears, the entire line in `fg.quaternary`. The em-dash is a literal trailing glyph, not a cents placeholder.
  - **String-pill secondary label.** Each string pill shows the note letter at `Tq.Type.H1` size and the octave digit below in `Tq.Type.NumericM`-equivalent / `fg.tertiary`. The check mark for tuned strings is a 9 dp `check` glyph in `signal.mint` at the top-right corner (already in §8.1).
- ✅ **Reduced-motion exception scope:** the reduced-motion behavior is per §9. For the tuner specifically: needle settle drops from 200 ms cubic-bezier to 80 ms linear; the success-ring fades are replaced with instant-on / instant-off at the same hold duration (1200 ms hold preserved). The check-mark appearance on string-pills uses the same 80 ms instant-vs-fade rule.
- ✅ **First-launch permission flow:** the screen does *not* request permission automatically on first composition. Instead, the permission-denied card is shown with "Grant access" as a primary button. Tapping it invokes the `RequestPermission` launcher. On `false` callback (denied), the launcher is invoked again on the next button tap until Android starts returning denied-with-`shouldShowRequestPermissionRationale = false` (permanent denial). After that point, the button's behavior changes to opening `ACTION_APPLICATION_DETAILS_SETTINGS` for the app's package. The card's body copy stays the same throughout; only the button's action shifts. This avoids surprising the user with a permission dialog before they've engaged with anything, while keeping recovery seamless.
- ✅ **Compose UI test scope:** minimal screen-level tests covering (a) idle state renders the listening line, (b) detection state renders the correct status word and cents in the correct colour, (c) tuned-string pill shows the check mark, (d) permission-denied card renders with the expected button label, (e) preset picker sheet opens on chip label tap, (f) mode popover opens on chip chevron tap. No needle-position pixel asserts.

## State Model — additions to 5.3

Phase 5.4 keeps `TunerUiState` mostly as 5.3 left it, but adds a small number of fields surfaced from `TunerPreferences` and one snapshot for the chromatic re-entry behavior:

```kotlin
data class TunerUiState(
    // ── unchanged from 5.3 ────────────────────────────────────────────────
    val mode: TunerMode = TunerMode.PRESET,
    val availablePresets: Map<Int, Map<TunerCategory, List<TunerPreset>>> = emptyMap(),
    val selectedPreset: TunerPreset? = null,
    val currentStringIndex: Int = 0,
    val targetNote: Note? = null,
    val targetFrequencyHz: Double? = null,
    val detectedFrequencyHz: Double? = null,
    val detectedNote: Note? = null,
    val centsOffTarget: Double? = null,
    val status: TuningStatus = TuningStatus.IDLE,
    val tunedStringIndices: Set<Int> = emptySet(),
    val referencePitchHz: Double = 440.0,
    // ── new in 5.4 ────────────────────────────────────────────────────────
    val autoAdvanceEnabled: Boolean = true,
)
```

`previousPresetStringIndex: Int?` is held **inside the ViewModel** as a private mutable field, *not* on `TunerUiState`. It is an implementation detail of the chromatic re-entry logic; the UI never reads it. Keeping it off the state class keeps that class describing what the user can see.

## File Plan

### New files under `tuner/presentation/ui/`

```
tuner/presentation/ui/
├── TunerScreen.kt                          # The screen composable; replaces the Phase 4 placeholder.
└── components/
    ├── NeedleGauge.kt                      # Canvas-based gauge composable.
    ├── DetectedNoteHero.kt                 # Note letter + octave subscript.
    ├── StatusLine.kt                       # Status word + cents value, side-by-side.
    ├── ReadoutWell.kt                      # The dark inset well wrapping hero / gauge / Hz pair.
    ├── HzReadoutPair.kt                    # The DETECTED / TARGET Hz row.
    ├── StringSelectorRow.kt                # Row of note pills.
    ├── StringPill.kt                       # Single note pill (note letter + octave + check mark).
    ├── PresetChipRow.kt                    # Preset chip + mode popover + MIC LIVE indicator.
    ├── PresetChip.kt                       # Compound chip: label tap → picker, chevron tap → popover.
    ├── ReferencePitchKicker.kt             # The `TUNER · A4 = 440 HZ` kicker line at the top.
    ├── SuccessRing.kt                      # The "all strings tuned" mint border ring.
    ├── PresetPickerSheet.kt                # ModalBottomSheet content for preset selection.
    ├── TunerSettingsSheet.kt               # ModalBottomSheet content for 432 Hz + auto-advance.
    └── PermissionDeniedCard.kt             # The single-card permission-denied state.
```

### Supporting utilities

```
tuner/presentation/util/
├── TunerPermissionHandling.kt              # Permission request + settings-fallback helpers.
└── TunerHaptics.kt                         # Wrapper around the system HapticFeedback API.

tuner/presentation/mapping/
└── TuningStatusExt.kt                      # `TuningStatus.toSignalColor()` and related mappings.
                                            # Implements the §2.4 contract once, used everywhere.
```

### Modified files

- `tuner/data/TunerPreferences.kt` — add `autoAdvanceEnabled: Flow<Boolean>`, `referencePitchHz: Flow<Double>`, plus setters.
- `tuner/data/TunerPreferencesImpl.kt` — implement the new fields against DataStore.
- `tuner/presentation/viewmodel/TunerUiState.kt` — add `autoAdvanceEnabled: Boolean`.
- `tuner/presentation/viewmodel/TunerViewModel.kt` — add `onEnterChromaticMode()`, `onAutoAdvanceChanged(enabled: Boolean)`, `onReferencePitchChanged(hz: Double)`, `previousPresetStringIndex` snapshot logic, auto-advance gating, and the existing `onPresetSelected`-handles-restore behavior for chromatic re-entry.
- `tuner/presentation/viewmodel/TunerViewModel.kt` — also: combine `TunerPreferences.autoAdvanceEnabled` and `referencePitchHz` flows into `TunerUiState` (initialization + ongoing).
- `ui/navigation/AppNavHost.kt` (or wherever the tuner route is bound) — swap the placeholder for the real `TunerScreen`.
- `AndroidManifest.xml` — confirm `<uses-permission android:name="android.permission.RECORD_AUDIO" />` is present (added in 5.2; verify still there).

### Tests (under `app/src/test/`)

- `TunerViewModelTest` — extend the existing 5.3 test class with cases for the new actions and re-entry behavior. **Do not duplicate the suite** — add new test methods to the same class.
- `TuningStatusExtTest` — pure unit test of `TuningStatus.toSignalColor()` and `cents → status` derivation if exposed as a helper.

### Tests (under `app/src/androidTest/` — Compose UI tests, minimal)

- `TunerScreenTest` — six tests covering the I1 scope listed in "Decisions Locked In".

## Implementation Details

### Reference pitch kicker — top of the screen

```
●  TUNER · A4 = 440 HZ                                                    ☼
Drop D
```

- Top-left: a 6 dp filled circle in `signal.mint` (the same dot used in `MIC LIVE`) followed by the kicker text "TUNER · A4 = 440 HZ" or "TUNER · A4 = 432 HZ" in `Tq.Type.Kicker` (mono micro, +0.16 em tracking, uppercase) at `fg.secondary`.
- Below the kicker, the preset display name in `Tq.Type.H1` at `fg.primary`. The mockups show "Drop D" — this is `selectedPreset?.displayName`.
- Top-right: a 36 dp `btn.icon-round` with the `sun` glyph (the existing `settings`-class icon from §7's icon set; if the set ships a `sun` icon, use that; otherwise use the existing `settings` icon — the §14 Q1 wording says "sun-icon button is already in the mockup but unbound"). Tapping it opens the settings sheet.

> **Icon-set check.** §7 lists the available icons: `tuner, metronome, key, chord, more, settings, info, play, pause, plus, minus, check, chevron-right, chevron-down, tap, mic, search`. No `sun`. Use `settings` for now; if the design intent is a dedicated sun icon, raise it as a §14 follow-up (not a 5.4 blocker — `settings` is a sensible stand-in). **Record the substitution in `DECISIONS.md`.**

### Readout well

The dark inset card wrapping (top-to-bottom): detected-note hero, status line, needle gauge, Hz readout pair.

- Background: `Tq.Color.BgInset` per §6.6 ("readout wells: background `bg.inset`, radius `r.xl` (18 dp), padding 20 dp / 12 dp").
- Border: `Tq.Color.LineFaint` 1 dp hairline.
- Internal padding: `Tq.Sp.s5` (20 dp) on left/right, `Tq.Sp.s3` (12 dp) on top/bottom.
- The success ring (see "Success state" below) is drawn as a sibling overlay using `Modifier.drawWithCache { ... }` rather than as a border on the well — easier to animate in and out without affecting layout.

### Needle gauge — `NeedleGauge.kt`

Pure Compose `Canvas` composable.

**Inputs:**
```kotlin
@Composable
fun NeedleGauge(
    cents: Double?,            // null → idle (centred, faded)
    semanticColor: Color,      // mint / cyan / amber / fg.quaternary when idle
    modifier: Modifier = Modifier,
)
```

**Geometry** (per §8.1):
- Canvas size: 280 × 150 dp.
- Origin (pivot): bottom-centre, i.e. `(140 dp, 150 dp)`.
- Radius: 120 dp.
- Sweep: from −60° to +60° (measured from the vertical "0 cents" position), corresponding to ±50 cents (clamped). Conversion: `angleDegrees = (cents.coerceIn(-50.0, +50.0) / 50.0) × 60.0`.
- Sweet-spot arc: −6° to +6° rendered in `signal.mint`, 2 dp stroke, with a 4 dp glow when the *needle's current state* is in-tune (i.e. when `semanticColor` resolves to mint). When the needle is in any other state, the sweet-spot arc is still drawn but at reduced alpha (~50%) — it's a permanent reference, just toned down so the eye doesn't get pulled to it during flat/sharp states. (Mockup-derived; the in-tune frame shows the sweet-spot arc lit, the flat/sharp frames show a dimmer version of it.)
- Tick marks: every 10° of needle angle → every ~8.33 cents. **Simpler interpretation per §8.1's "every 10 cents":** ticks every 12° of needle angle. Ticks 1 dp stroke, length 6 dp, `fg.tertiary`. Major ticks every 30 cents → every 36°, length 10 dp, `fg.secondary`. The centre tick (0 cents, 0°) is mint, length 10 dp, with the small mint T-bar visible in the mockups.
- Needle: 2 dp stroke from pivot to `radius - 4 dp`, current semantic colour, with 6 dp drop-shadow glow when not idle. (The mockups make the glow soft and broad — implement via `Canvas { drawLine(...); drawLine(thickerAlphaBlurredVersion) }` or `Modifier.shadow(elevation = 6.dp, shape = ...)` if a cleaner approach works in Compose's Canvas. Spike both at implementation time; document the choice in the file's KDoc.)
- Pivot cap: 5 dp diameter filled circle, `bg.elev3`, with a 0.8 dp `line` border, and a 2 dp filled inner dot in the current semantic colour.
- "−50" and "+50" labels: `mono.micro` (~10 sp), `fg.tertiary`, positioned just outside the arc at its bottom-left and bottom-right.

**Motion:**
- Needle position is animated to its target angle via `animateFloatAsState` with `tween(durationMillis = 200, easing = CubicBezierEasing(0.4f, 1.2f, 0.5f, 1f))`.
- Under reduced motion: `tween(durationMillis = 80, easing = LinearEasing)`. The composable reads the reduced-motion flag via a helper `LocalReducedMotion.current` (define in `ui/theme/` if not already present — small composable-local utility wrapping `Settings.Global.TRANSITION_ANIMATION_SCALE`).

**Idle state:**
- `cents = null` → needle drawn at 0° in `fg.quaternary`, stroke unchanged, no glow.

**Clamping:**
- The needle clamps at ±60° (±50 cents). Cents values beyond ±50 in the status line are still shown raw (e.g. `−87¢`), per the 5.3 decision that the UI clamps the *needle* and the *status line* shows raw values.

### Detected-note hero — `DetectedNoteHero.kt`

```kotlin
@Composable
fun DetectedNoteHero(
    note: Note?,                  // null → em-dash
    semanticColor: Color,         // unused on the letter (kept here for future / consistency)
    modifier: Modifier = Modifier,
)
```

- Note letter: `NonScalingText` (per §13.1) at 64 dp via the `display.l` style (`Tq.Type.DisplayL`), `fg.primary`. No glow.
- Octave subscript: a small mono digit at 18 dp / `fg.tertiary`, baseline aligned to the bottom third of the letter. Use Compose's `BasicText` with explicit baseline offset, or two `Text`s in a `Row` with `Alignment.Bottom`.
- When `note == null`: render a single em-dash `"—"` at the letter's size in `fg.quaternary`, no subscript.

The `semanticColor` parameter is plumbed through but currently unused on the letter (mockups show flat white). Keep the parameter so if the design later wants a coloured letter, it's a one-line change in this file. Document the no-op in the function's KDoc.

### Status line — `StatusLine.kt`

```kotlin
@Composable
fun StatusLine(
    status: TuningStatus,
    centsOffTarget: Double?,
    modifier: Modifier = Modifier,
)
```

- Layout: `Row` with `Arrangement.spacedBy(Tq.Sp.s2)`, vertically centred.
- Status word: `Tq.Type.Kicker`, uppercase. Colour comes from `status.toSignalColor()`:
  - `FLAT` → `signal.cyan` → "FLAT"
  - `IN_TUNE` → `signal.mint` → "IN TUNE"
  - `SHARP` → `signal.amber` → "SHARP"
  - `LISTENING` → `fg.quaternary` → "LISTENING"
  - `IDLE` → `fg.quaternary` → "LISTENING" (treated identically for UI; idle is a backend artefact)
  - `ALL_STRINGS_TUNED` → `signal.mint` → "ALL TUNED"
  - `PERMISSION_DENIED`, `CAPTURE_FAILED` → not displayed (the screen renders the permission card / error card instead, replacing the readout well; no status line)
- Cents value: same `Tq.Type.Kicker` style, colour `fg.tertiary`. Format: `"+15¢"`, `"−18¢"`, `"+2¢"`. Use the proper minus glyph `−` (U+2212), not a hyphen.
- When `centsOffTarget == null` (idle/listening with no detection): a single em-dash `"—"` in `fg.quaternary` instead of the cents value.

### Hz readout pair — `HzReadoutPair.kt`

A `Row` with `SpaceBetween` arrangement:
- Left: a `Column` with kicker label "DETECTED" (`fg.tertiary`) and value `"108.86 Hz"` or `"— Hz"` (`fg.primary` / `Tq.Type.Body`).
- Right: a `Column` with kicker label "TARGET" and value `"110.00 Hz"` or `"— Hz"`.

Formatting: `"%.2f Hz".format(detected ?: target ?: 0.0)`. When the value is `null` (no detection or no target), render `"— Hz"` instead.

### String selector — `StringSelectorRow.kt` + `StringPill.kt`

`StringSelectorRow`:
- Row of `StringPill`s, one per string in `selectedPreset.notes`.
- Horizontal gap: `Tq.Sp.s2` (8 dp; §8.1 says "6 dp gap" — that's a §6.3-or-§8.1 conflict, §8.1 is more specific; use 6 dp here directly via `Tq.Sp` if a 6 dp token exists, else `6.dp` with a comment).

> **Spacing note.** §8.1 specifies 6 dp gap. The `Tq.Sp` scale jumps `s1 = 4`, `s2 = 8` — no 6 dp token. Use the literal `6.dp` with an inline comment citing §8.1, and consider raising whether to add a 6 dp step to `Tq.Sp` as a future tidy-up (not 5.4 scope).

`StringPill`:
- Size: 38 × 38 dp visible (per §6.3); minimum tap target 44 × 44 dp enforced via `Modifier.minimumInteractiveComponentSize()` or `Modifier.size(44.dp).clickable { ... }` with the visible content in an inner 38 dp box. The mockup's pills look closer to ~54 dp tall — §8.1 says "54 dp tall, equal flex" for the row, which overrides §6.3's 38 dp default for *the tuner's* string-selector specifically. **Use 54 dp height per §8.1, full equal-flex width.**
- Background: `bg.elev2` for inactive, `bg.elev2` with semantic-color outline (3 dp halo at 12% alpha + 2 dp border) for the active string.
- Content:
  - Note letter at `Tq.Type.H2` (or whatever the screenshot's "D / A / G" sizing maps to — 17 sp semibold is the §3 H2 size), `fg.primary`. The note letter colour matches the current semantic colour *only* when this pill is the currently active string and the detection is non-idle.
  - Octave digit below the letter at `Tq.Type.NumericM` (15 sp medium mono) or `mono.micro` (10 sp), `fg.tertiary`. Mockups read closer to the larger 15 sp size — verify in-build and adjust to the size that matches.
- Check mark: a 9 dp `check` glyph in `signal.mint` at the top-right corner of the pill, 4 dp inset from each edge. Shown when `stringIndex in tunedStringIndices`.
- Tap: invokes `onStringSelected(stringIndex)`.

### Preset chip row — `PresetChipRow.kt`

A `Row` with `SpaceBetween`:
- Left: `PresetChip` (compound: label + chevron).
- Right: `MIC LIVE` indicator — a 6 dp mint dot followed by `Tq.Type.Kicker` "MIC LIVE" in `fg.secondary`.

### `PresetChip.kt` — compound affordance

```kotlin
@Composable
fun PresetChip(
    preset: TunerPreset?,
    mode: TunerMode,
    onLabelClick: () -> Unit,           // open preset picker
    onChevronClick: () -> Unit,         // open mode popover
    expanded: Boolean,                   // controls mode popover visibility
    onDismissMenu: () -> Unit,
    onExitChromaticMode: () -> Unit,
    onSelectChromaticMode: () -> Unit,
    modifier: Modifier = Modifier,
)
```

- Internally: a single `Row` styled per §6.2 (26 dp tall, h-padding 10 dp, pill radius, `bg.elev2`, `fg.secondary`, `Tq.Type.Kicker`).
- The label text reads `"6-STRING · DROP"` (string count + category label) from `preset.stringCount` and `preset.category.displayName`. Both visible in `fg.secondary` per §6.2.
- The chevron is `chevron-down` from the icon set, 14 dp, `fg.tertiary`.
- Two invisible 44 × 44 dp tap targets overlap the chip: one covering everything *except* the chevron, one covering just the chevron. The chevron's tap target is at minimum 44 × 44 dp even though the visible chevron is 14 dp; this is enforced by an inner padding scheme rather than scaling the chip itself.
- `DropdownMenu` is anchored to the chevron's tap target box. Two items: "Preset" (with a `check` glyph if `mode == PRESET`) and "Chromatic" (with a `check` glyph if `mode == CHROMATIC`). Tapping either dismisses the menu and invokes the relevant callback.

### Preset picker sheet — `PresetPickerSheet.kt`

Material 3 `ModalBottomSheet`. Contents:

```
[ ── grabber ── ]

SELECT TUNING                                                                  ✕
─────────────────────────────────────────────────────────────────────────

[ 6-STRING | 7-STRING | 8-STRING ]   ← segmented control / tab row

  STANDARD
  ┌──────────────────────────────────────────────────┐
  │ E Standard               E2 A2 D3 G3 B3 E4    ●  │  ← selected indicator
  │ Eb Standard              Eb2 Ab2 Db3 Gb3 …       │
  │ D Standard               D2 G2 C3 F3 A3 D4       │
  │ C# Standard              C#2 F#2 B2 E3 G#3 C#4   │
  └──────────────────────────────────────────────────┘

  OPEN
  ┌──────────────────────────────────────────────────┐
  │ Open D                   D2 A2 D3 F#3 A3 D4      │
  │ ...                                              │
  └──────────────────────────────────────────────────┘

  DROPPED
  ┌──────────────────────────────────────────────────┐
  │ Drop D                   D2 A2 D3 G3 B3 E4    ●  │
  │ Drop C#                  ...                     │
  │ ...                                              │
  └──────────────────────────────────────────────────┘
```

- Sheet background: `bg.elev1`, top radius `r.xl` (18 dp), no top border.
- Tab row: §6.8 segmented control. Three segments. Tapping switches the displayed string-count group. Initial selection follows the currently selected preset's string count.
- Category headers: `Tq.Type.Kicker`, `fg.tertiary`, ~12 dp top padding above each group.
- Row: `Tq.Type.Body` `fg.primary` for the display name on the left; a `mono.micro` `fg.tertiary` summary of the note list on the right; a small mint dot indicator on the far right when this is the currently selected preset.
- Row tap: dismisses the sheet (animated) and calls `onPresetSelected(presetId)`.
- Close button (✕): top-right of the sheet header, dismisses without selection.

> The note-list summary on each row is purely visual confirmation — the user already chose the tuning by name. Show all notes for 6-string (fits comfortably), abbreviate 8-string to "F#1 … E4" form (first + last) to keep the row to one line.

### Tuner settings sheet — `TunerSettingsSheet.kt`

Smaller `ModalBottomSheet`, ~280 dp tall.

```
[ ── grabber ── ]

TUNER SETTINGS                                                                 ✕
─────────────────────────────────────────────────────────────────────────

  Reference pitch                                              A4 = 440 Hz
                                                          [ 440 ▏ 432 ]   ← segmented control

  Auto-advance strings                                              ( ●)   ← M3 Switch
  Advance automatically when a string is in tune.
```

- "Reference pitch" row: label `Tq.Type.Body` `fg.primary`, current value `Tq.Type.Body` `fg.secondary` on the right, segmented control below.
- "Auto-advance strings" row: label + Material 3 `Switch` on the right. A `Tq.Type.Body` `fg.tertiary` description sits below the row.
- Both controls call back into the ViewModel (`onReferencePitchChanged`, `onAutoAdvanceChanged`). The sheet observes the same `uiState` so the controls reflect persisted values.

### Permission-denied card — `PermissionDeniedCard.kt`

Replaces the entire readout-well area when `status == PERMISSION_DENIED`. Sits in the same vertical position so the screen layout doesn't shift.

```
┌────────────────────────────────────────────────┐
│                                                │
│                  [mic-slash icon, 28 dp]       │
│                                                │
│              Microphone access needed          │
│                                                │
│   Toniqo listens to your guitar to detect      │
│   pitch. We don't record or store audio —      │
│   everything stays on your device.             │
│                                                │
│        ┌──────────────────────────┐            │
│        │      Grant access        │            │
│        └──────────────────────────┘            │
│                                                │
└────────────────────────────────────────────────┘
```

- Card: §6.6 — `bg.elev1`, hairline border, `r.lg` (16 dp), padding `Tq.Sp.s4` (16 dp).
- Icon: 28 dp; use `mic` from the icon set with a 1.25 dp diagonal stroke overlay to indicate "denied". (Alternatively, ship a dedicated `mic-slash` icon if it exists in the set — §7 doesn't list it, so for v1 use the slash-overlay approach. Record in `DECISIONS.md`.)
- Heading: `Tq.Type.H2`, `fg.primary`, centred.
- Body: `Tq.Type.Body`, `fg.secondary`, centred, max 3 lines.
- Button: `btn.primary` 40 dp variant, `signal.mint` background. Label `"Grant access"`.
- The string selector row and preset chip remain visible below the card — the user can still browse tunings before they grant access.

The component receives a single `onGrantAccess: () -> Unit` callback. The decision of "request permission" vs "open settings" lives in the calling screen (`TunerScreen`), which has the `ActivityResultLauncher` and the `shouldShowRequestPermissionRationale` check.

### `TunerScreen.kt` — composition

```kotlin
@Composable
fun TunerScreen(
    viewModel: TunerViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Permission launcher
    val context = LocalContext.current
    val activity = context.findActivity()        // helper in TunerPermissionHandling.kt
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* state is observed via TunerPreferences / capture pipeline; no direct write here */ },
    )

    // Event handling
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel.events, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is TunerEvent.StringTuned -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    TunerEvent.AllStringsTuned -> { /* SuccessRing reads its own trigger from a remembered state */ }
                    TunerEvent.EnteredChromaticMode -> { /* no-op for now; reserved for future */ }
                }
            }
        }
    }

    // Sheets — visibility held locally
    var presetSheetOpen by rememberSaveable { mutableStateOf(false) }
    var settingsSheetOpen by rememberSaveable { mutableStateOf(false) }
    var modeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(/* …no top bar; we draw the kicker manually inside the body… */) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = Tq.Sp.s4)) {
            ReferencePitchKicker(
                referencePitchHz = uiState.referencePitchHz,
                presetDisplayName = uiState.selectedPreset?.displayName ?: "—",
                onSettingsClick = { settingsSheetOpen = true },
            )
            PresetChipRow(
                preset = uiState.selectedPreset,
                mode = uiState.mode,
                expanded = modeMenuExpanded,
                onLabelClick = { presetSheetOpen = true },
                onChevronClick = { modeMenuExpanded = true },
                onDismissMenu = { modeMenuExpanded = false },
                onExitChromaticMode = { viewModel.onExitChromaticMode(); modeMenuExpanded = false },
                onSelectChromaticMode = { viewModel.onEnterChromaticMode(); modeMenuExpanded = false },
            )
            Spacer(Modifier.height(Tq.Sp.s3))

            when (uiState.status) {
                TuningStatus.PERMISSION_DENIED -> PermissionDeniedCard(
                    onGrantAccess = {
                        handleGrantAccess(activity, permissionLauncher)   // helper
                    },
                )
                TuningStatus.CAPTURE_FAILED -> CaptureFailedCard(onRetry = { /* future */ })
                else -> ReadoutWell {
                    DetectedNoteHero(note = uiState.detectedNote, semanticColor = uiState.status.toSignalColor())
                    StatusLine(status = uiState.status, centsOffTarget = uiState.centsOffTarget)
                    NeedleGauge(cents = uiState.centsOffTarget, semanticColor = uiState.status.toSignalColor())
                    HzReadoutPair(
                        detectedHz = uiState.detectedFrequencyHz,
                        targetHz = uiState.targetFrequencyHz,
                    )
                }
            }

            Spacer(Modifier.height(Tq.Sp.s4))
            StringSelectorRow(
                preset = uiState.selectedPreset,
                currentStringIndex = uiState.currentStringIndex,
                tunedStringIndices = uiState.tunedStringIndices,
                activeSemanticColor = uiState.status.toSignalColor(),
                mode = uiState.mode,
                onStringTap = viewModel::onStringSelected,
            )
        }

        SuccessRing(
            visible = uiState.status == TuningStatus.ALL_STRINGS_TUNED,
            // ring is drawn over the readout well via a Popup or AlignedBox; details in the component file
        )

        if (presetSheetOpen) {
            PresetPickerSheet(
                grouped = uiState.availablePresets,
                selectedPresetId = uiState.selectedPreset?.id,
                onDismiss = { presetSheetOpen = false },
                onSelect = { id -> viewModel.onPresetSelected(id); presetSheetOpen = false },
            )
        }
        if (settingsSheetOpen) {
            TunerSettingsSheet(
                referencePitchHz = uiState.referencePitchHz,
                autoAdvanceEnabled = uiState.autoAdvanceEnabled,
                onReferencePitchChanged = viewModel::onReferencePitchChanged,
                onAutoAdvanceChanged = viewModel::onAutoAdvanceChanged,
                onDismiss = { settingsSheetOpen = false },
            )
        }
    }
}
```

This is illustrative — the real file resolves the wiring concerns (`SuccessRing` placement, the readout well's overlay behavior, the haptic call site). The pseudocode is in the plan to anchor the intent.

### Event handling — exact pattern

```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
LaunchedEffect(viewModel.events, lifecycleOwner) {
    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
            // ...
        }
    }
}
```

This is the **required** pattern. The keys `(viewModel.events, lifecycleOwner)` ensure the effect is re-keyed only on actual identity changes; the inner `repeatOnLifecycle(STARTED)` ensures collection pauses on `STOPPED` and resumes on `STARTED` without re-running the outer block (which would replay buffered events on a `SharedFlow` configured with `replay = 0`, *but* without `repeatOnLifecycle` would also re-collect from scratch on every recomposition keyed on a less-stable input). Document this rationale in the file.

### ViewModel additions

```kotlin
// In TunerViewModel

private var previousPresetStringIndex: Int? = null

fun onEnterChromaticMode() {
    val current = _uiState.value
    if (current.mode == TunerMode.CHROMATIC) return
    previousPresetStringIndex = current.currentStringIndex
    _uiState.update { it.copy(
        mode = TunerMode.CHROMATIC,
        tunedStringIndices = emptySet(),
        // currentStringIndex retained for the restore; UI ignores it in CHROMATIC
    ) }
    tunerInput.value = TunerInput(
        mode = TunerMode.CHROMATIC,
        targetNote = null,
        referencePitchHz = current.referencePitchHz,
    )
    viewModelScope.launch { _events.emit(TunerEvent.EnteredChromaticMode) }
}

fun onExitChromaticMode() {
    val current = _uiState.value
    if (current.mode == TunerMode.PRESET) return
    val restoreIndex = previousPresetStringIndex ?: 0
    previousPresetStringIndex = null   // consume the snapshot
    val preset = current.selectedPreset ?: return
    _uiState.update { it.copy(
        mode = TunerMode.PRESET,
        currentStringIndex = restoreIndex,
        tunedStringIndices = emptySet(),
        targetNote = preset.notes[restoreIndex],
        targetFrequencyHz = preset.notes[restoreIndex].frequencyHz(current.referencePitchHz),
        status = TuningStatus.LISTENING,
    ) }
    tunerInput.value = TunerInput(
        mode = TunerMode.PRESET,
        targetNote = preset.notes[restoreIndex],
        referencePitchHz = current.referencePitchHz,
    )
}

fun onAutoAdvanceChanged(enabled: Boolean) {
    viewModelScope.launch { preferences.setAutoAdvanceEnabled(enabled) }
    // The persisted flow re-emits and combines into uiState via the existing flow combine.
}

fun onReferencePitchChanged(hz: Double) {
    viewModelScope.launch { preferences.setReferencePitchHz(hz) }
    // The combined flow updates uiState.referencePitchHz, which the next tunerInput
    // emission picks up via the existing pipeline. To retune immediately, also re-emit:
    val current = _uiState.value
    val preset = current.selectedPreset
    if (preset != null && current.mode == TunerMode.PRESET) {
        tunerInput.value = TunerInput(
            mode = TunerMode.PRESET,
            targetNote = preset.notes[current.currentStringIndex],
            referencePitchHz = hz,
        )
    }
}
```

Also: the existing `onPresetSelected` and `onStringSelected` clear `previousPresetStringIndex` (per Option 2's clear-on-explicit-choice rule). The auto-transition after the last string also clears it. Add these clears in 5.4's diff.

**Auto-advance gating** — the auto-advance currently fires unconditionally on `isSustainedInTune`. Change in `TunerViewModel`'s detection-event handler: when `isSustainedInTune` fires, always emit `TunerEvent.StringTuned` and add to `tunedStringIndices`. The increment-after-200-ms then runs only if `uiState.autoAdvanceEnabled` is `true`. When `false`, the haptic still fires, the pill still shows the check mark, but the user has to tap the next string.

### `TunerPreferences` additions

```kotlin
interface TunerPreferences {
    val lastUsedPresetId: Flow<String?>
    val autoAdvanceEnabled: Flow<Boolean>
    val referencePitchHz: Flow<Double>

    suspend fun setLastUsedPresetId(id: String)
    suspend fun setAutoAdvanceEnabled(enabled: Boolean)
    suspend fun setReferencePitchHz(hz: Double)
}
```

`TunerPreferencesImpl` adds two new DataStore keys: `auto_advance_enabled` (Boolean, default `true`) and `reference_pitch_hz` (Double, default `440.0`). Defaults come from `.map { it[KEY] ?: DEFAULT }`.

In the ViewModel's `init`, the `combine(...)` that builds the initial state now also reads these two flows. The state stays subscribed for the ViewModel's lifetime.

### Permission flow helper — `TunerPermissionHandling.kt`

```kotlin
fun handleGrantAccess(
    activity: Activity?,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
) {
    if (activity == null) return                            // defensive; should never happen in normal flow
    val canShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.RECORD_AUDIO,
    )
    val isPermanentlyDenied = !canShowRationale && !hasPermission(activity)
    if (isPermanentlyDenied) {
        openAppSettings(activity)
    } else {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

private fun hasPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
```

The `isPermanentlyDenied` heuristic has a known caveat on first launch: before the user has ever been asked, both `shouldShowRequestPermissionRationale` returns `false` *and* `hasPermission` returns `false`. Under the simple form above, that case routes to `openAppSettings` — which is wrong on first launch. The fix is to track a one-bit "has the user been asked at least once" flag in `TunerPreferences` (`hasRequestedAudioPermission: Flow<Boolean>`). The launcher's `onResult` sets it to `true` the first time it's called.

> Add `hasRequestedAudioPermission` as a third new field on `TunerPreferences`. Default `false`. The settings-fallback condition becomes `hasRequestedAtLeastOnce && !canShowRationale && !hasPermission`.

### Reduced-motion helper

```kotlin
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f
    }
}
```

Lives in `ui/util/Motion.kt`. Consumed by `NeedleGauge`, `SuccessRing`, and any future motion-sensitive component. The `remember` is intentional — we don't need to react to runtime toggles of the developer-mode setting; reading once on composition is sufficient.

## Tests

### Unit tests — `TunerViewModelTest` (extensions to the 5.3 suite)

Add cases:

- **`onEnterChromaticMode captures previous string index.`** From `mode = PRESET, currentStringIndex = 3`, calling `onEnterChromaticMode()` results in `mode = CHROMATIC` and internally `previousPresetStringIndex == 3`.
- **`onExitChromaticMode restores previous string index.`** Setup: enter chromatic from string 4. Call `onExitChromaticMode`. `mode = PRESET`, `currentStringIndex = 4`, `tunedStringIndices = emptySet()`. `previousPresetStringIndex` is cleared internally (verify via a follow-up `onEnterChromaticMode` from a different string captures the new value, not the stale one).
- **`onExitChromaticMode without a prior snapshot lands on string 0.`** Setup: ViewModel is freshly created, `mode = PRESET, currentStringIndex = 0`. Force `mode = CHROMATIC` via the post-success transition (advance through all strings). Now `previousPresetStringIndex` should be `null`. Calling `onExitChromaticMode` restores `currentStringIndex = 0`.
- **`Auto-success transition clears previous string index.`** Enter chromatic from string 3 (snapshot captures 3). Then trigger a manual preset re-selection (which clears the snapshot). Verify the snapshot is `null`.
- **`onPresetSelected clears the chromatic snapshot.`** Enter chromatic from string 3. Call `onPresetSelected("six_string_drop_d")`. The snapshot is `null`; subsequent `onExitChromaticMode` would land on string 0 (but the new preset already put the user in preset mode at string 0, so this is mostly a state-consistency check).
- **`onStringSelected in chromatic mode clears the chromatic snapshot.`** Enter chromatic from string 3 (snapshot = 3). Call `onStringSelected(1)`. `mode = PRESET, currentStringIndex = 1`, snapshot = `null`.
- **`onAutoAdvanceChanged persists.`** Calling `onAutoAdvanceChanged(false)` results in fake preferences receiving `setAutoAdvanceEnabled(false)`. `uiState.autoAdvanceEnabled` reflects `false` after the persisted flow re-emits.
- **`Auto-advance disabled: sustained-in-tune emits StringTuned but does not advance.`** Setup `autoAdvanceEnabled = false`, use case emits `Detection(isSustainedInTune = true)` for string 0. `tunedStringIndices` includes 0, `TunerEvent.StringTuned(0)` is emitted, but `currentStringIndex` stays 0. Advancing virtual time past `STRING_LOCK_HOLD_MS` does not change `currentStringIndex`.
- **`onReferencePitchChanged retunes targets.`** Setup preset mode at string 0 of `six_string_standard_e` (E2 at 82.41 Hz @ 440 Hz). Call `onReferencePitchChanged(432.0)`. `uiState.referencePitchHz == 432.0`. `uiState.targetFrequencyHz` ≈ 80.92 Hz (E2 at 432 Hz reference).
- **`Initial state reads autoAdvanceEnabled and referencePitchHz from preferences.`** Fake preferences returns `autoAdvanceEnabled = false, referencePitchHz = 432.0`. Initial `uiState` reflects both.

### Pure helper test — `TuningStatusExtTest`

If `TuningStatus.toSignalColor()` is an extension function, unit-test it against the §2.4 mapping. Inputs: every `TuningStatus` value. Outputs: the documented colour. This is trivial but valuable as a contract test for §2.4's "single source of truth" rule.

### Compose UI tests — `TunerScreenTest` (under `androidTest/`)

Six tests, each driving the screen with a fake ViewModel that exposes a `MutableStateFlow<TunerUiState>` and a `MutableSharedFlow<TunerEvent>`:

1. **Idle renders the listening line.** Initial state: `status = LISTENING, detectedFrequencyHz = null`. Assert: a node with text matching "LISTENING" exists; a node with "— Hz" exists for the detected readout.
2. **Detection renders status word and cents.** State: `status = FLAT, centsOffTarget = -18.0`. Assert: "FLAT" and "−18¢" both present.
3. **Tuned string shows check mark.** State: `tunedStringIndices = setOf(0, 1)`. Assert: pills at index 0 and 1 expose a content description including "tuned"; pill at index 2 does not.
4. **Permission denied renders the card.** State: `status = PERMISSION_DENIED`. Assert: card heading "Microphone access needed" present; button "Grant access" present; readout-well content (e.g. the needle gauge) is not.
5. **Preset chip label opens picker.** Initial state: a preset is loaded. Click the chip label. Assert: a sheet with heading "Select tuning" is composed. (Use `onNodeWithTag` and a stable tag on the chip's label area.)
6. **Preset chip chevron opens mode popover.** Click the chip chevron. Assert: a `DropdownMenu` with items "Preset" and "Chromatic" is composed.

These tests run on the JVM via Robolectric *or* on an emulator via `androidTest`. **Use `androidTest` with Compose's `createComposeRule()`**, since 5.4 establishes the first real screen-level test pattern for the project; Robolectric introduces a parallel runtime that complicates Hilt + Compose. Document the choice in `DECISIONS.md`.

> Adding Compose UI test dependencies (`androidx.compose.ui:ui-test-junit4`, `androidx.compose.ui:ui-test-manifest`) to `libs.versions.toml` and `build.gradle.kts`. Both are baseline-justified for any phase that ships UI tests.

### What we explicitly don't test

- Needle position pixel asserts. Drawing semantics are visually verified by the user on a real device.
- Real audio-pipeline integration. 5.3 already covered the state-machine plumbing; 5.4's screen tests use a fake ViewModel.
- Permission launcher integration. The launcher is provided by the framework; we test the screen's *display* in the denied state, not Android's flow.

## Documentation Updates (cleanup items 1–3)

These run **in parallel** with the code work — not as a separate hand-off step. The agent must touch these files in the same change set as the code that depends on them.

### `APP_SPECIFICATION.md`

Add a new subsection "Operating Modes" under "Module: Guitar Tuner", positioned after "Tuning Flow". Draft text:

> ### Operating Modes
>
> The tuner operates in one of two modes within a session.
>
> **Preset mode** is the default when a tuning is selected. The tuner targets the lowest string of the chosen preset and advances through the strings in order. The current string can be jumped to by tapping its pill in the string selector; auto-advance then continues from the tapped string forward.
>
> **Chromatic mode** can be entered explicitly via the mode selector on the preset chip, or automatically after all strings of the current preset have been confirmed in tune. In chromatic mode the tuner does not target a specific string — it identifies the nearest equal-tempered note to whatever pitch is detected and shows cents-off relative to that note. Auto-advance does not apply. The user returns to preset mode either by tapping the "Preset" item in the mode selector (which restores the string they were on before entering chromatic mode, if the entry was user-initiated; otherwise resets to the lowest string) or by tapping any string in the string selector (which jumps directly to that string).

Also add a brief mention under "Permissions" of the in-app pre-prompt-then-card flow:

> If `RECORD_AUDIO` permission is not granted when the tuner is opened, the readout area is replaced with a permission-explainer card and a "Grant access" button. The button triggers the system permission prompt on first use; after a permanent denial, it opens the system app-settings screen instead. The card's body copy stays consistent across both states.

### `DESIGN.md`

Three edits:

1. **§8.1 corrections and additions.**
   - Replace the wording "Detected-note hero ... both with a faint semantic glow when not idle" with "Detected-note hero ... rendered in `fg.primary` with no glow. The semantic colour is conveyed by the needle and status line." (Mockup-grounded.)
   - Add a new subsection "Hz readout pair" describing the DETECTED / TARGET row below the needle gauge.
   - Add a new subsection "Settings sheet" describing the bottom-sheet contents (reference pitch segmented control, auto-advance switch).
   - Add a new subsection "Permission-denied state" describing the card layout.
   - Update the "Reference pitch chip" wording: "The toggle to change it is in the tuner settings sheet, opened by tapping the sun-icon button in the top-right corner."

2. **§14 Q1 and Q2 closed.**
   - Q1 → `Resolved (2026-05-19): sun-icon button (top-right) opens the tuner settings sheet, which contains the 432 Hz toggle alongside the auto-advance toggle.`
   - Q2 → `Resolved (2026-05-19): single ToniqoCard with a 28 dp mic icon (slash-overlaid), an explanatory heading and body, and a primary "Grant access" button. The button requests permission on first tap; after permanent denial, opens system app settings.`
   - Q3, Q4, Q5 remain open and unchanged.

3. **§7 icon-set note.**
   - Add a small note that no `sun` glyph ships in the v1 set; the tuner uses the `settings` glyph for the top-right button. If a dedicated sun icon is desired, raise it as a new §14 question.

### `DECISIONS.md`

Append (in this order, all dated to the day of 5.4 work):

1. **A4 = 432 Hz toggle UI placement.** Decision: sun-icon button (top-right of the tuner screen) opens a settings sheet containing the 432 Hz toggle. Alternatives: long-press the kicker; chip next to the preset chip. Rationale: discoverability and grouping with other tuner preferences.

2. **Microphone permission-denied screen design.** Decision: single `ToniqoCard` with mic-slash icon, explanation, primary "Grant access" button. Alternatives: a full-screen takeover; an inline banner. Rationale: matches the design system's card surface and keeps the rest of the screen accessible (preset chip, string selector still browsable).

3. **Mode toggle UI placement.** Decision: `DropdownMenu` anchored to the preset chip's chevron; preset picker opens from the chip's label area. Alternatives: top-bar segmented control; settings sheet item; no UI affordance at all. Rationale: discoverable, doesn't add a row, gives the chevron in the mockup an explicit meaning.

4. **Chromatic re-entry policy.** Decision: capture `currentStringIndex` on user-initiated `PRESET → CHROMATIC`; restore on `PRESET` selection from the mode menu. Clear on auto-success transition, preset change, or explicit string tap. `tunedStringIndices` is not restored. Alternatives: always restore; always start at string 0; restore including check marks. Rationale: matches the "I'm peeking, take me back" intent without making chromatic mode a saved-state mechanism.

5. **Auto-advance toggle persistence.** Decision: lives in the settings sheet, persisted via `TunerPreferences.autoAdvanceEnabled`, default `true`. Alternatives: session-only; on-screen toggle. Rationale: predicted second tenant of `TunerPreferences`; sticky preference matches user expectation.

6. **Reference pitch persistence.** Decision: persisted via `TunerPreferences.referencePitchHz`, default `440.0`. Alternatives: session-only. Rationale: same as above; 432 Hz users want it to stick.

7. **Preset picker surface.** Decision: Material 3 `ModalBottomSheet` with a segmented control (6 / 7 / 8) and grouped category sections. Alternatives: full-screen route; inline carousel; popover. Rationale: standard Material 3 affordance, scales to 25+ presets without nav cost, matches the dark surface system.

8. **`SharedFlow<TunerEvent>` collection pattern.** Decision: `LaunchedEffect(events, lifecycleOwner) { lifecycleOwner.repeatOnLifecycle(STARTED) { events.collect { ... } } }`. Alternatives: `LaunchedEffect(Unit) { events.collect { ... } }` (replays on rotation); `collectAsStateWithLifecycle` (wrong shape for one-shot events). Rationale: prevents duplicate haptic on configuration change; pauses on `STOPPED` to avoid invisible event consumption.

9. **Needle gauge implementation.** Decision: pure Compose `Canvas`, all elements drawn per-frame. Alternatives: pre-rendered `GraphicsLayer` for static elements; SVG asset. Rationale: simpler code, sufficient performance at the redraw rates involved; optimisation is profile-driven.

10. **DESIGN.md §8.1 prose corrections.** Decision: hero letter has no glow; the needle's 6 dp drop-shadow glow is the only glow on the screen. Mockups are authoritative when they differ from prose. Alternatives: amend §10 to permit a text-glow exception. Rationale: mockups don't show a hero glow; no new exception needed.

11. **Sun-icon substitution.** Decision: use the `settings` glyph for the tuner's top-right button until a dedicated `sun` glyph is added to the icon set. Alternatives: ship a one-off sun SVG. Rationale: stay within the §7 icon-set discipline; raise the question if a sun is wanted.

12. **Compose UI test infrastructure.** Decision: use `androidTest` with `createComposeRule()` and `androidx.compose.ui:ui-test-junit4`. Alternatives: Robolectric. Rationale: Robolectric introduces a parallel runtime that complicates Hilt + Compose; `androidTest` is the standard Android Compose pattern. Cost: tests require an emulator/device to run.

13. **`hasRequestedAudioPermission` preference.** Decision: track a `Boolean` in `TunerPreferences` to distinguish "never asked" from "permanently denied". Alternatives: an SDK-version-specific API (none reliable across versions); a separate `getPermissionFlow` helper that synthesises the state. Rationale: simplest, smallest surface, robust across Android versions.

## Steps

1. **Documentation cleanup first**, so the rest of the work is grounded:
   1. Append the chromatic-mode subsection to `APP_SPECIFICATION.md`.
   2. Apply the §8.1 corrections and additions to `DESIGN.md`, close §14 Q1 and Q2, add the §7 icon-set note.
   3. Append the 13 entries above to `DECISIONS.md`.
2. **`TunerPreferences` extension**: add three new fields (`autoAdvanceEnabled`, `referencePitchHz`, `hasRequestedAudioPermission`) and their setters. Update `TunerPreferencesImpl`. Update tests (the fake gains the new fields).
3. **`TunerUiState` extension**: add `autoAdvanceEnabled: Boolean`.
4. **`TunerViewModel` updates**: add the three new actions, the `previousPresetStringIndex` snapshot, the auto-advance gating, and the combine that brings the new prefs into `uiState`. Update existing tests as needed; add the new test cases.
5. **Mapping helper**: `TuningStatus.toSignalColor()` extension in `tuner/presentation/mapping/TuningStatusExt.kt`. Unit-test it.
6. **Reduced-motion helper**: `rememberReducedMotion()` in `ui/util/Motion.kt`.
7. **Components** (in dependency order):
   - `ReferencePitchKicker.kt`
   - `HzReadoutPair.kt`
   - `StatusLine.kt`
   - `DetectedNoteHero.kt`
   - `NeedleGauge.kt`
   - `ReadoutWell.kt`
   - `StringPill.kt`
   - `StringSelectorRow.kt`
   - `PresetChip.kt`
   - `PresetChipRow.kt`
   - `SuccessRing.kt`
   - `PermissionDeniedCard.kt`
   - `PresetPickerSheet.kt`
   - `TunerSettingsSheet.kt`
8. **Permission handling**: `TunerPermissionHandling.kt`.
9. **Haptics wrapper**: `TunerHaptics.kt`.
10. **`TunerScreen.kt`**: compose the parts; wire the launcher; wire the event collection; wire the sheet visibility states; wire the success-ring trigger.
11. **Navigation wire-up**: replace the Phase 4 `TunerScreen` placeholder in the route definition.
12. **Compose UI tests**: `TunerScreenTest`, six cases. Add the test dependencies to the catalog and module build file.
13. **Manual visual verification (user)**: run on a device, walk through Idle → Listening → Flat → In tune → Sharp → All strings tuned → Chromatic mode → Preset mode (restore) → Settings (432 Hz toggle, auto-advance toggle) → Permission denied flow.
14. **Hand off** to the user with a summary itemising new files, modified files, the new dependencies (Compose UI test artifacts), the documentation files touched, and a checklist of the manual verification scenarios.

## Completion Criteria

See `Phase5_4-REQUIREMENTS.md`.
