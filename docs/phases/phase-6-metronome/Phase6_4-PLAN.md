# Phase 6.4 — Metronome UI

## Goal

Build the actual metronome screen. This is the visible deliverable — the user-facing metronome. By the end of 6.4, the metronome tab presents the full Compose UI from `DESIGN.md` §8.2, with all controls wired to `MetronomeViewModel`, screen-on management active during playback, and a snackbar handling transient errors.

The interim placeholder from 6.3 is replaced. The temporary `// TODO(6.4)` markers disappear. Manual UX testing against `DESIGN.md` §8.2 is the final acceptance gate.

## Scope

- Implement `MetronomeScreen` composable per `DESIGN.md` §8.2 and the approved mockups.
- Implement child composables:
  - Page status kicker (`METRONOME · RUNNING` / `· STOPPED` with optional pulsing dot).
  - Tempo card (groups BPM display, descriptor, slider, +/− buttons).
  - BPM display (96dp mono, fixed size — does not scale with font-size setting).
  - Tempo descriptor label.
  - BPM slider, +/− buttons.
  - Beat indicator header row (`BEAT · X / N` + beat-unit name).
  - Beat indicator segments (with accent + lit/unlit treatment).
  - Tap-tempo button.
  - Time signature dropdown.
  - Subdivide dropdown (with "SUBDIVIDE" verb-form kicker).
  - Start/Stop pill with play / pause icons and Start / Stop text labels.
  - BPM tap-to-type dialog.
- Wire `MetronomeViewModel` via `hiltViewModel()`.
- Implement `FLAG_KEEP_SCREEN_ON` lifecycle handling via `DisposableEffect` keyed on `isPlaying`.
- Snackbar host wired to `MetronomeEvent.AudioUnavailable`.
- All UI strings in `res/values/strings.xml`.
- Compose UI tests covering interactions and state transitions.
- Manual QA pass against `DESIGN.md` §8.2 and the mockups.
- Update `APP_SPECIFICATION.md` and `DESIGN.md` per Item 22 of the decision log.

## Out of Scope

- No haptics (Item 10 — deferred).
- No volume control (Item 12 — deferred).
- No accent customization (Item 11 — deferred).
- No background playback / foreground service (Item 5 — deferred).
- No new audio behavior, no new ViewModel methods. UI consumes the existing surface from 6.3.

## Reading Order Before Starting

1. `APP_SPECIFICATION.md` → "Module: Metronome"
2. `DESIGN.md` → §8.2 (Metronome) in detail
3. `DESIGN.md` → §6.1 (Tap targets, button heights), §6.4 (Bottom nav), §6.6 (Cards), §7 (Icons), §2 (Color tokens), §3 (Typography)
4. The two approved UI mockups (running 4/4, stopped 7/8) — the design authority for visible structure
5. `DECISIONS.md` → all entries
6. `Phase6-Metronome-Decisions.md` → Items 3, 6, 9, 14, 18 (revised), 19, 22, 23
7. The completed `Phase6_3-PLAN.md` for the ViewModel surface this UI consumes
8. This file

## Decisions Locked In For 6.4

These are settled before implementation begins:

- ✅ **Layout:** Exactly per `DESIGN.md` §8.2 and the approved mockups (running 4/4, stopped 7/8). No deviations without raising the conflict.
- ✅ **Page status kicker (Item 23a):** `mono.micro` line above the page title. `METRONOME · RUNNING` with a leading mint pulsing dot when `isPlaying = true`; `METRONOME · STOPPED` with no dot otherwise.
- ✅ **Tempo card (Item 23d):** BPM display + tempo descriptor + slider + −/+ buttons are wrapped in a single visually-grouped card (rounded corners, subtle border, slight elevation), not laid out as loose siblings.
- ✅ **Beat indicator header (Item 23b):** A `mono.micro` row above the beat indicator segments, split:
  - Left: `BEAT · X / N` where `X` is the 1-indexed current main beat (always at least 1 — when stopped, displays `1`), `N` is the time-signature numerator.
  - Right: `QUARTER NOTES` for /4 signatures, `EIGHTH NOTES` for /8 signatures. Derived from `timeSignatureDenominator`.
- ✅ **Beat indicator segments:** Mint glow on beat 1 (12dp glow per §8.2). Other lit beats use mint at 35% over `bg.elev2`. Unlit beats use `bg.elev1` with `line.faint` border. Beat-1 marker (4dp mint dot) sits inside its unlit cell.
- ✅ **Beat indicator animation:** 80ms linear transitions. **Overrides reduced-motion** — the beat indicator must flash even with reduced motion enabled. Documented in `DESIGN.md` §8.2.
- ✅ **BPM display:** 96dp mono, fixed size. Does **not** scale with the system font-size setting (uses `NonScalingText` from Phase 3).
- ✅ **Tempo descriptor:** Read-only `mono.micro` label below the BPM. Sourced from `TempoDescriptor` enum (string resources in `strings.xml`).
- ✅ **BPM slider:** Linear scale across 1–300, snaps to integer BPMs (`steps = 298`).
- ✅ **+/− buttons:** Flat ±1 per tap; no press-and-hold acceleration (Item 9).
- ✅ **Tap-tempo button:** 60dp circle, `bg.elev2`, with `tap` icon + uppercase "TAP" in `mono.micro`. Triggers `viewModel.onTapTempo()` on each press.
- ✅ **Time signature dropdown:** Pill-style 44dp. Mono kicker label "SIGNATURE" above.
- ✅ **Subdivide dropdown:** Pill-style 44dp. Mono kicker label **"SUBDIVIDE"** (verb form per Item 23c) above. Values displayed in noun form ("None", "Eighth notes", "Sixteenth notes", "Eighth triplets").
- ✅ **Start/Stop button (Item 18, revised):** Pill, 60dp tall, flex 1. **Icon + text combo.**
  - Stopped: ▶ `play` icon + "Start" text. Mint primary background with 24dp glow.
  - Running: ⏸ `pause` icon + "Stop" text. `bg.elev3` background, no glow.
- ✅ **BPM tap-to-type:** Tapping the BPM display opens a dialog with a number-pad input. OK disabled until value ∈ [1, 300]. Commit on OK/done; cancel discards.
- ✅ **Snackbar:** `SnackbarHost` collects `viewModel.events`; shows `metronome_error_audio_unavailable` on `AudioUnavailable`.
- ✅ **Screen-on:** `DisposableEffect` keyed on `isPlaying` adds/removes `FLAG_KEEP_SCREEN_ON` on the window.
- ✅ **Initial loading:** If `uiState.isInitialLoadComplete == false`, the screen renders without animating transitions (state is briefly the default while DataStore loads — typically a single frame).
- ✅ **Sun / theme-toggle icon (Item 23e):** Visible in the mockup at top-right. This is **app-shell scaffolding** — not part of `MetronomeScreen`. If absent from the app shell, that's tracked separately; Phase 6.4 does not implement it.

## Implementation Details

### `metronome/presentation/ui/MetronomeScreen.kt`

The top-level composable. Wires the ViewModel, lifecycle effects, and lays out child composables per `DESIGN.md` §8.2.

```kotlin
@Composable
internal fun MetronomeScreen(
    viewModel: MetronomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val audioUnavailableMessage = stringResource(R.string.metronome_error_audio_unavailable)

    // Audio-unavailable snackbar.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MetronomeEvent.AudioUnavailable -> snackbarHostState.showSnackbar(audioUnavailableMessage)
            }
        }
    }

    // Keep screen on while playing.
    KeepScreenOnWhilePlaying(isPlaying = state.isPlaying)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        MetronomeContent(
            state = state,
            onPlayToggled = viewModel::onPlayToggled,
            onBpmChanged = viewModel::onBpmChanged,
            onBpmIncrement = viewModel::onBpmIncrement,
            onBpmDecrement = viewModel::onBpmDecrement,
            onTimeSignatureChanged = viewModel::onTimeSignatureChanged,
            onSubdivisionChanged = viewModel::onSubdivisionChanged,
            onTapTempo = viewModel::onTapTempo,
            modifier = Modifier.padding(padding),
        )
    }
}
```

`MetronomeContent` is a separate composable taking plain state and lambdas — ViewModel-free, so it's previewable and testable in isolation.

### `metronome/presentation/ui/KeepScreenOnWhilePlaying.kt`

```kotlin
@Composable
internal fun KeepScreenOnWhilePlaying(isPlaying: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(isPlaying) {
        if (isPlaying) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
```

The `onDispose` block fires both on key change to `false` and on composable leaving the composition — covering all the lifecycle exits from Item 5.

### `metronome/presentation/ui/MetronomeContent.kt`

The actual visual layout. Stateless — takes `MetronomeUiState` plus lambdas.

Structure (refer to `DESIGN.md` §8.2 and the mockups for exact spacing):

```
Column (top-aligned, padded):
  Page Status Kicker  (mono.micro, with optional pulsing dot)
  Spacer
  Page Title "Metronome"  (display.lg or similar — comes from app shell convention)
  Spacer (24dp)
  Tempo Card  (rounded container, slight elevation)
    └ Column:
        Kicker "TEMPO" (mono.micro, centered)
        BPM Display (96dp mono, NonScalingText) — clickable, opens BPM dialog
        Tempo descriptor (mono.micro, centered)
        Spacer
        Row: [- button (36dp circle) | Slider (1fr) | + button (36dp circle)]
  Spacer (20dp)
  Beat Indicator Header Row  (mono.micro)
    └ Row: [Left: "BEAT · X / N"  |  Right: "QUARTER NOTES" / "EIGHTH NOTES"]
  Spacer (8dp)
  Beat Indicator Row (44dp height, N segments per timeSignatureNumerator)
  Spacer (20dp)
  Row [SignatureDropdown (1fr) | SubdivideDropdown (1fr)]
    └ Each dropdown has its mono.micro kicker label above it
      ("SIGNATURE" and "SUBDIVIDE" respectively)
  Spacer (24dp; expandable so the Start/Stop pill anchors near the bottom)
  Row [Tap Tempo (60dp circle) | Start/Stop Pill (60dp tall, flex 1)]
```

Real layout values come from `DESIGN.md` §8.2 and the mockups; if any are missing, raise the conflict per the project's design conflict rule.

### `metronome/presentation/ui/MetronomeStatusKicker.kt`

The status indicator at the top of the screen. Mono.micro line with an optional leading pulsing dot.

```kotlin
@Composable
internal fun MetronomeStatusKicker(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (isPlaying) {
            PulsingDot(color = Tq.color.signal.mint, modifier = Modifier.size(8.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = stringResource(
                if (isPlaying) R.string.metronome_status_running
                else R.string.metronome_status_stopped
            ),
            style = Tq.type.mono.micro,
            color = Tq.color.text.secondary,
        )
    }
}
```

`PulsingDot` is a small helper that alpha-animates between full and ~30% over a ~1-second cycle (using `rememberInfiniteTransition` + `tween`). If a similar primitive already exists from the tuner (e.g., for the listening state), reuse it; otherwise add it under `ui/components/`.

### `metronome/presentation/ui/TempoCard.kt`

A single visually-grouped container holding the tempo controls.

```kotlin
@Composable
internal fun TempoCard(
    bpm: Int,
    tempoDescriptor: TempoDescriptor,
    onBpmDisplayClick: () -> Unit,
    onSliderValueChange: (Int) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Tq.color.bg.elev1, shape = RoundedCornerShape(20.dp))
            .border(width = 1.dp, color = Tq.color.line.faint, shape = RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.metronome_tempo_label),  // "TEMPO"
            style = Tq.type.mono.micro,
            color = Tq.color.text.secondary,
        )
        BpmDisplay(bpm = bpm, tempoDescriptor = tempoDescriptor, onClick = onBpmDisplayClick)
        Spacer(Modifier.height(12.dp))
        BpmSliderRow(
            bpm = bpm,
            onSliderValueChange = onSliderValueChange,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
        )
    }
}
```

Exact corner radius, border, and elevation values come from `DESIGN.md` §8.2 or §6.6 (shared card primitive from Phase 4). The 20dp here is illustrative — use design tokens.

### `metronome/presentation/ui/BeatIndicatorHeader.kt`

The two-part header row above the beat indicator segments.

```kotlin
@Composable
internal fun BeatIndicatorHeader(
    currentBeat: Int,                  // 0-indexed main beat from state
    numerator: Int,
    denominator: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(
                R.string.metronome_beat_header_format,
                currentBeat + 1,        // 1-indexed for display
                numerator,
            ),
            style = Tq.type.mono.micro,
            color = Tq.color.text.secondary,
        )
        Text(
            text = stringResource(beatUnitLabelResId(denominator)),
            style = Tq.type.mono.micro,
            color = Tq.color.text.secondary,
        )
    }
}

private fun beatUnitLabelResId(denominator: Int): Int = when (denominator) {
    4 -> R.string.metronome_beat_unit_quarter_notes
    8 -> R.string.metronome_beat_unit_eighth_notes
    else -> error("Unsupported denominator: $denominator")
}
```

The denominator-to-label helper can also live as an extension on `MetronomeConfig` if preferred.

### `metronome/presentation/ui/BeatIndicator.kt`

```kotlin
@Composable
internal fun BeatIndicator(
    numerator: Int,
    currentBeat: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(numerator) { index ->
            BeatSegment(
                isBeatOne = index == 0,
                isLit = isPlaying && currentBeat == index,
            )
        }
    }
}

@Composable
private fun BeatSegment(isBeatOne: Boolean, isLit: Boolean) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isLit && isBeatOne -> Tq.color.signal.mint
            isLit -> Tq.color.signal.mint.copy(alpha = 0.35f).compositeOver(Tq.color.bg.elev2)
            else -> Tq.color.bg.elev1
        },
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "beat-segment-bg",
    )
    val glow = if (isLit && isBeatOne) 12.dp else 0.dp

    Box(
        Modifier
            .weight(1f)
            .height(44.dp)
            .glow(color = Tq.color.signal.mint, radius = glow)  // existing primitive from Phase 3
            .background(backgroundColor, shape = RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = Tq.color.line.faint, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (isBeatOne && !isLit) {
            Box(
                Modifier
                    .size(4.dp)
                    .background(Tq.color.signal.mint, shape = CircleShape)
            )
        }
    }
}
```

The exact glow API depends on what Phase 3 set up; this is pseudocode-ish but the visual rules are exact.

> **Reduced motion note.** `animateColorAsState` ignores the system reduced-motion preference. This is intentional — the beat indicator must flash even with reduced motion enabled. Documented in `DESIGN.md` §8.2 and in this file's `Decisions Locked In For 6.4` section.

### `metronome/presentation/ui/BpmDisplay.kt`

```kotlin
@Composable
internal fun BpmDisplay(
    bpm: Int,
    tempoDescriptor: TempoDescriptor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        NonScalingText(
            text = bpm.toString(),
            style = Tq.type.display.xl,  // 96dp mono fixed
        )
        Text(
            text = stringResource(tempoDescriptor.labelResId),
            style = Tq.type.mono.micro,
            color = Tq.color.text.secondary,
        )
    }
}

// Extension on the enum file (or a companion lookup):
internal val TempoDescriptor.labelResId: Int
    get() = when (this) {
        TempoDescriptor.ADAGIO -> R.string.tempo_adagio
        TempoDescriptor.ANDANTE -> R.string.tempo_andante
        TempoDescriptor.MODERATO -> R.string.tempo_moderato
        TempoDescriptor.ALLEGRO -> R.string.tempo_allegro
        TempoDescriptor.PRESTO -> R.string.tempo_presto
    }
```

### `metronome/presentation/ui/BpmInputDialog.kt`

```kotlin
@Composable
internal fun BpmInputDialog(
    initialBpm: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialBpm.toString()) }
    val parsedValue = text.toIntOrNull()
    val isValid = parsedValue != null && parsedValue in BPM_MIN..BPM_MAX

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.metronome_bpm_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { newText -> text = newText.filter { it.isDigit() }.take(3) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (isValid) onConfirm(parsedValue!!)
                }),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(parsedValue!!) },
                enabled = isValid,
            ) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
```

### `metronome/presentation/ui/TimeSignatureDropdown.kt`, `SubdivisionDropdown.kt`

Pill-style 44dp dropdowns matching `DESIGN.md` §8.2. Use `ExposedDropdownMenuBox` from Material 3, styled to the pill shape.

Items in the time signature dropdown: exactly the 8 supported signatures from `MetronomeConfig.SUPPORTED_SIGNATURES`. Display as "2/4", "3/4", etc.

Items in the subdivision dropdown: all four `Subdivision` enum values. Display strings (from `strings.xml`):
- NONE → "None"
- EIGHTHS → "Eighth notes"
- SIXTEENTHS → "Sixteenth notes"
- TRIPLETS → "Eighth triplets"

### `metronome/presentation/ui/PlayStopButton.kt`

```kotlin
@Composable
internal fun PlayStopButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isPlaying) Tq.color.bg.elev3 else Tq.color.signal.mint
    val contentColor = if (isPlaying) Tq.color.text.primary else Tq.color.bg.elev0
    val glow = if (isPlaying) 0.dp else 24.dp
    val labelRes = if (isPlaying) R.string.metronome_stop else R.string.metronome_start
    val iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play

    Row(
        modifier
            .height(60.dp)
            .glow(color = Tq.color.signal.mint, radius = glow)
            .background(backgroundColor, shape = RoundedCornerShape(30.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,  // text label suffices for accessibility
            tint = contentColor,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(labelRes),
            style = Tq.type.label.large,  // exact token TBD from DESIGN.md
            color = contentColor,
        )
    }
}
```

Notes:
- Icon `contentDescription` is `null` because the visible text label is what screen readers will announce; setting both would cause TalkBack to read the icon twice (e.g., "play, Start").
- The exact text style for "Start" / "Stop" comes from `DESIGN.md` §8.2; if it's not specified, raise the conflict before guessing.

### `res/values/strings.xml`

Add:

- `metronome_status_running` — "Metronome · Running"
- `metronome_status_stopped` — "Metronome · Stopped"
- `metronome_tempo_label` — "Tempo" (rendered uppercase via type style)
- `metronome_signature_label` — "Signature"
- `metronome_subdivide_label` — "Subdivide"
- `metronome_beat_header_format` — `Beat · %1$d / %2$d` (positional format args: current beat, numerator)
- `metronome_beat_unit_quarter_notes` — "Quarter notes"
- `metronome_beat_unit_eighth_notes` — "Eighth notes"
- `metronome_start` — "Start"
- `metronome_stop` — "Stop"
- `metronome_tap_tempo` — "Tap"
- `metronome_bpm_dialog_title` — "Set BPM"
- `metronome_error_audio_unavailable` — "Audio playback unavailable. Please try again."
- `tempo_adagio` — "Adagio"
- `tempo_andante` — "Andante"
- `tempo_moderato` — "Moderato"
- `tempo_allegro` — "Allegro"
- `tempo_presto` — "Presto"
- `subdivision_none` — "None"
- `subdivision_eighths` — "Eighth notes"
- `subdivision_sixteenths` — "Sixteenth notes"
- `subdivision_triplets` — "Eighth triplets"
- `action_ok` — "OK" (may already exist; verify)
- `action_cancel` — "Cancel" (may already exist; verify)

Kicker labels (`SUBDIVIDE`, `SIGNATURE`, `TEMPO`) are uppercased by their `mono.micro` text style, not by hardcoding all-caps in the string resource.

### Icons

`DESIGN.md` §7 should already list `play`, `pause`, and `tap`. If `tap` is missing, raise the conflict — don't invent an icon silently.

### Phase 4 placeholder cleanup

- Delete the interim `MetronomeContent` text-and-buttons placeholder body created in 6.3.
- The new `MetronomeScreen` from this phase replaces it entirely.
- Navigation route `metronome_route` (from Phase 4) now points to the new `MetronomeScreen`.

## Tests

UI tests use Compose Testing (`createComposeRule`, `setContent`) — these run as instrumented tests if any Android dependency requires it, but `MetronomeContent` is `ViewModel`-free, so most can run as JVM Compose tests via `createComposeRule()` without Android-specific setup.

### `MetronomeContentTest`

Tests the stateless content composable with hand-crafted `MetronomeUiState`.

- **BPM display renders the value.** Pass state with `bpm = 137`. Find a node with text "137".
- **Tempo descriptor renders.** State with `bpm = 120` shows "Allegro"; state with `bpm = 90` shows "Andante".
- **Status kicker reflects `isPlaying`.** Running → "Metronome · Running" + pulsing dot present. Stopped → "Metronome · Stopped" + no dot.
- **Beat indicator header shows the current beat.** State with `numerator = 4, currentBeat = 2` → "Beat · 3 / 4" (1-indexed for display).
- **Beat unit label follows denominator.** Denominator 4 → "Quarter notes". Denominator 8 → "Eighth notes".
- **Beat indicator shows the correct number of segments.** State with 4/4 has 4 segments; with 7/8 has 7.
- **Current beat is highlighted.** With `currentBeat = 2` and `isPlaying = true`, the third segment (index 2) is lit.
- **Beat 1 marker shows when stopped.** With `isPlaying = false`, the first segment shows the 4dp mint dot.
- **Play button shows "Start" + play icon when stopped.** Text "Start" present; play icon visible.
- **Stop button shows "Stop" + pause icon when running.** Text "Stop" present; pause icon visible.
- **Tapping Play invokes the lambda.** Click the play button; verify `onPlayToggled` was called.
- **Tapping + invokes the lambda.** Click the + button; verify `onBpmIncrement`.
- **Tapping − invokes the lambda.** Same for `onBpmDecrement`.
- **Tapping the BPM display invokes the dialog-open lambda.** Click the BPM number; verify the corresponding handler.
- **Tapping the TAP button invokes `onTapTempo`.**
- **Slider position reflects BPM.** With `bpm = 60`, the slider's position is at ~20% (60/300).
- **Time signature dropdown shows all 8 options when expanded.**
- **Subdivide dropdown shows all 4 options when expanded.**
- **Subdivide kicker label reads "Subdivide".**

### `BpmInputDialogTest`

- **Initial value populates the text field.** Open with `initialBpm = 120`; field shows "120".
- **Non-digit input is filtered.** Typing "12a3" results in "123".
- **Input clamped to 3 digits.** Typing "1234" results in "123".
- **OK is disabled for invalid input.** Field cleared → OK disabled. Field "999" → OK disabled. Field "0" → OK disabled.
- **OK enabled for valid input.** Field "60" → OK enabled. Field "300" → OK enabled.
- **OK invokes `onConfirm` with the parsed value.**
- **Cancel invokes `onDismiss`.**

### `BeatIndicatorTest`

- **Number of segments matches numerator.** 4 → 4 segments. 7 → 7 segments.
- **`currentBeat` lights the right segment.** `numerator = 4, currentBeat = 2, isPlaying = true` → only the third segment is in the "lit" visual state.
- **All segments unlit when stopped.** `isPlaying = false` → no lit segment regardless of `currentBeat`.

### `BeatIndicatorHeaderTest`

- **Beat counter is 1-indexed.** `currentBeat = 0, numerator = 4` → "Beat · 1 / 4".
- **Beat counter updates.** `currentBeat = 3, numerator = 4` → "Beat · 4 / 4".
- **Numerator visible.** `numerator = 7` → "/ 7" in the label.
- **Denominator 4 → "Quarter notes".**
- **Denominator 8 → "Eighth notes".**

### `MetronomeStatusKickerTest`

- **Running state.** `isPlaying = true` → "Metronome · Running" text rendered; pulsing dot present.
- **Stopped state.** `isPlaying = false` → "Metronome · Stopped" text rendered; no dot.

### Manual QA (no unit test)

These are user-side acceptance criteria:
- Layout matches the approved mockups and `DESIGN.md` §8.2.
- Colors and typography match the design tokens.
- Animations feel right (80 ms beat flash is snappy but not jarring; status-kicker dot pulses gently).
- The metronome runs end-to-end on a real device.

## Steps

1. Add icon resources (`ic_play`, `ic_pause`, `ic_tap`) if missing from Phase 4's icon set. If `tap` is genuinely missing from `DESIGN.md` §7, raise the conflict.
2. Add the new string resources to `res/values/strings.xml`.
3. Create or reuse a `PulsingDot` primitive in `ui/components/` (check for an existing one first).
4. Create `metronome/presentation/ui/MetronomeStatusKicker.kt`.
5. Create `metronome/presentation/ui/TempoCard.kt`.
6. Create `metronome/presentation/ui/MetronomeScreen.kt`.
7. Create `metronome/presentation/ui/MetronomeContent.kt`.
8. Create `metronome/presentation/ui/KeepScreenOnWhilePlaying.kt`.
9. Create `metronome/presentation/ui/BeatIndicatorHeader.kt`.
10. Create `metronome/presentation/ui/BeatIndicator.kt`.
11. Create `metronome/presentation/ui/BpmDisplay.kt`.
12. Create `metronome/presentation/ui/BpmInputDialog.kt`.
13. Create `metronome/presentation/ui/TimeSignatureDropdown.kt`.
14. Create `metronome/presentation/ui/SubdivideDropdown.kt`.
15. Create `metronome/presentation/ui/PlayStopButton.kt` (icon + text per revised Item 18).
16. Create `metronome/presentation/ui/TapTempoButton.kt` (if separated from `MetronomeContent`).
17. Update the navigation graph so `metronome_route` points to `MetronomeScreen` instead of the 6.3 placeholder.
18. Remove the interim text-based placeholder content from 6.3.
19. Write `MetronomeContentTest`, `BpmInputDialogTest`, `BeatIndicatorTest`, `BeatIndicatorHeaderTest`, `MetronomeStatusKickerTest`.
20. Update `APP_SPECIFICATION.md` per Item 22 of the decision log:
    - Add the five tempo descriptor labels with locked BPM boundaries.
    - Add the persistence behaviour statement.
    - Add the lifecycle/stop-on-leave statement.
    - Add the tap-tempo user-facing behaviour.
    - Add the time-signature / beat-unit clarification (/4 = quarter, /8 = eighth).
    - Add the subdivision multiplier table and EIGHTHS-in-/8 no-op.
    - Add the screen-on behaviour statement.
21. Update `DESIGN.md` §8.2:
    - Start/Stop button content: icon + "Start" / "Stop" text (revised Item 18).
    - Page status kicker, beat indicator header (`BEAT · X / N` + beat-unit label), tempo card as a grouped container, "SUBDIVIDE" verb-form kicker label (Item 23).
22. Update `DECISIONS.md` per Items 22 and the revised Item 18 / new Item 23:
    - Synthesis over assets.
    - Anchor-based scheduling.
    - Strict screen-lifecycle binding.
    - No accent customization in v1; forward-compatible extension via optional `accentPattern`.
    - Start/Stop button revised to icon + text after mockup review.
    - UI structure additions from mockup review (status kicker, beat header, tempo card, subdivide kicker label).
    (Some of these may have been added in earlier sub-phases. Verify and consolidate.)
23. Manual QA pass on a real device. Test against the smoke-test list in `Phase6_4-REQUIREMENTS.md`. Adjust click parameters (in `ClickParameters` from 6.1) if any sound still seems wrong; record in `DECISIONS.md`.
24. Hand off to the user with a final summary.

## Completion Criteria

See `Phase6_4-REQUIREMENTS.md`.
