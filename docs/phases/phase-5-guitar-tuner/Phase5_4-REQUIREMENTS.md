# Phase 5.4 — Requirements & Acceptance Criteria

Phase 5.4 produces the complete tuner UI and ships the first end-to-end functional module. The phase is complete when both checklists pass.

## Agent Responsibilities

### Documentation cleanup (must land in the same change set as the code)

- [ ] `APP_SPECIFICATION.md` — new "Operating Modes" subsection under "Module: Guitar Tuner" describing preset and chromatic modes per `Phase5_4-PLAN.md` "Documentation Updates".
- [ ] `APP_SPECIFICATION.md` — "Permissions" subsection extended with the in-app pre-prompt-then-card flow description.
- [ ] `DESIGN.md` §8.1 — prose corrections (hero letter has no glow), new subsections for Hz readout pair, settings sheet, permission-denied state, and revised wording for the reference-pitch chip.
- [ ] `DESIGN.md` §14 — Q1 and Q2 marked resolved with the answers inline. Q3, Q4, Q5 remain open and unchanged.
- [ ] `DESIGN.md` §7 — note that no `sun` glyph ships in v1; the tuner uses `settings`.
- [ ] `DECISIONS.md` — 13 new entries appended per the plan's "DECISIONS.md" section. Each entry follows the existing template (Decision / Alternatives considered / Rationale; Consequences where relevant).

### Dependencies

- [ ] `androidx.compose.ui:ui-test-junit4` and `androidx.compose.ui:ui-test-manifest` added to `gradle/libs.versions.toml` and referenced from `app/build.gradle.kts` under `androidTestImplementation` and `debugImplementation` respectively.
- [ ] No other new third-party dependencies are introduced.

### `tuner/data/TunerPreferences.kt`

- [ ] Interface declares three new pairs (in addition to the 5.3 `lastUsedPresetId`):
  - [ ] `val autoAdvanceEnabled: Flow<Boolean>` and `suspend fun setAutoAdvanceEnabled(enabled: Boolean)`.
  - [ ] `val referencePitchHz: Flow<Double>` and `suspend fun setReferencePitchHz(hz: Double)`.
  - [ ] `val hasRequestedAudioPermission: Flow<Boolean>` and `suspend fun setHasRequestedAudioPermission(value: Boolean)`.
- [ ] Defaults via `.map { it[KEY] ?: DEFAULT }`: `autoAdvanceEnabled = true`, `referencePitchHz = 440.0`, `hasRequestedAudioPermission = false`.

### `tuner/data/TunerPreferencesImpl.kt`

- [ ] DataStore-backed implementation of the three new fields, with the same `tuner_preferences` store.
- [ ] Three new `Preferences.Key`s: `auto_advance_enabled` (Boolean), `reference_pitch_hz` (Double), `has_requested_audio_permission` (Boolean).
- [ ] Existing `lastUsedPresetId` behavior unchanged.

### `tuner/presentation/viewmodel/TunerUiState.kt`

- [ ] New field `val autoAdvanceEnabled: Boolean = true`.
- [ ] No other shape changes from 5.3. `previousPresetStringIndex` is **not** added to the state — it's a private ViewModel field.

### `tuner/presentation/viewmodel/TunerViewModel.kt`

- [ ] New private mutable field `private var previousPresetStringIndex: Int? = null`.
- [ ] New action `fun onEnterChromaticMode()`:
  - [ ] No-op if `mode == CHROMATIC`.
  - [ ] Captures `currentStringIndex` into `previousPresetStringIndex`.
  - [ ] Sets `mode = CHROMATIC, tunedStringIndices = emptySet()`.
  - [ ] Updates `tunerInput` to a `CHROMATIC` input.
  - [ ] Emits `TunerEvent.EnteredChromaticMode`.
- [ ] New action `fun onExitChromaticMode()`:
  - [ ] No-op if `mode == PRESET`.
  - [ ] Restores `currentStringIndex = previousPresetStringIndex ?: 0`.
  - [ ] Clears `previousPresetStringIndex`.
  - [ ] Sets `mode = PRESET, tunedStringIndices = emptySet()`.
  - [ ] Updates `targetNote` / `targetFrequencyHz` to the restored string.
  - [ ] Updates `tunerInput` to a `PRESET` input.
- [ ] New action `fun onAutoAdvanceChanged(enabled: Boolean)` persists via `preferences.setAutoAdvanceEnabled` (fire-and-forget within `viewModelScope`).
- [ ] New action `fun onReferencePitchChanged(hz: Double)`:
  - [ ] Persists via `preferences.setReferencePitchHz`.
  - [ ] Re-emits `tunerInput` immediately with the new reference (so targets retune live without waiting for the next sustained-in-tune cycle).
- [ ] `onPresetSelected` clears `previousPresetStringIndex` in addition to its 5.3 behavior.
- [ ] `onStringSelected` clears `previousPresetStringIndex` in addition to its 5.3 behavior.
- [ ] The auto-success transition (last string locked → `ALL_STRINGS_TUNED` → `CHROMATIC`) clears `previousPresetStringIndex`.
- [ ] Auto-advance gating: when `Detection(isSustainedInTune = true)` fires, `TunerEvent.StringTuned` is always emitted and `tunedStringIndices` is always updated. The increment-after-200-ms hold runs only if `uiState.autoAdvanceEnabled` is `true`. When `false`, `currentStringIndex` does not change.
- [ ] Initialization extended: the combine that produces the initial state now also reads `autoAdvanceEnabled` and `referencePitchHz` from `TunerPreferences`. The state continues to track these flows for the ViewModel's lifetime.
- [ ] No other 5.3 behavior is altered.

### `tuner/presentation/mapping/TuningStatusExt.kt`

- [ ] Extension function `fun TuningStatus.toSignalColor(): Color` returning the per-§2.4 colour:
  - [ ] `FLAT` → `Tq.Color.SignalCyan`
  - [ ] `IN_TUNE` → `Tq.Color.SignalMint`
  - [ ] `SHARP` → `Tq.Color.SignalAmber`
  - [ ] `LISTENING, IDLE, PERMISSION_DENIED, CAPTURE_FAILED` → `Tq.Color.FgQuaternary`
  - [ ] `ALL_STRINGS_TUNED` → `Tq.Color.SignalMint`
- [ ] KDoc cites §2.4 as the source of truth.

### `ui/util/Motion.kt`

- [ ] `@Composable fun rememberReducedMotion(): Boolean` reading `Settings.Global.TRANSITION_ANIMATION_SCALE`, returning `true` when scale is `0f`.
- [ ] KDoc explains the read-once behavior and links to §9.

### Components under `tuner/presentation/ui/components/`

For each component below, the requirement is: composable exists, is parameterized exactly as the plan specifies, consumes only `Tq.*` tokens and `MaterialTheme.colorScheme.*` (no `Color(0xFF...)`, no inline dp/sp literals outside the documented exceptions), passes the visual mockup test on the user's device, and has a KDoc.

- [ ] `ReferencePitchKicker.kt`
- [ ] `HzReadoutPair.kt`
- [ ] `StatusLine.kt`
- [ ] `DetectedNoteHero.kt`
- [ ] `NeedleGauge.kt`
- [ ] `ReadoutWell.kt`
- [ ] `StringPill.kt`
- [ ] `StringSelectorRow.kt`
- [ ] `PresetChip.kt`
- [ ] `PresetChipRow.kt`
- [ ] `SuccessRing.kt`
- [ ] `PermissionDeniedCard.kt`
- [ ] `PresetPickerSheet.kt`
- [ ] `TunerSettingsSheet.kt`

Per-component specific requirements:

- [ ] **`NeedleGauge`**: 280 × 150 dp canvas, pivot bottom-centre, ±60° sweep maps to ±50 cents (clamped), sweet-spot arc at ±5 cents in mint, ticks every ~12° (10 cents) with 30° major ticks, 2 dp needle stroke with 6 dp shadow glow in the semantic colour when not idle, 5 dp pivot cap with 2 dp inner dot, `tween(200 ms, cubic-bezier(0.4, 1.2, 0.5, 1))` animation. Idle state: needle at 0° in `fg.quaternary`, no glow.
- [ ] **`NeedleGauge`** under reduced motion: `tween(80 ms, LinearEasing)`.
- [ ] **`DetectedNoteHero`**: 64 dp `display.l` letter via `NonScalingText`, 18 dp octave subscript at `fg.tertiary`, em-dash em-style for `note == null`.
- [ ] **`StatusLine`**: status word in semantic colour via `TuningStatus.toSignalColor()`, cents value in `fg.tertiary`, format `"+15¢"` / `"−18¢"` with U+2212 minus, em-dash trailing glyph for `LISTENING`.
- [ ] **`HzReadoutPair`**: `"DETECTED"` / `"TARGET"` kicker labels, `Tq.Type.Body` values with `"%.2f Hz"` formatting, `"— Hz"` when `null`.
- [ ] **`StringPill`**: 54 dp tall, equal flex, 6 dp gap to siblings, semantic-outline active state with 3 dp halo, 9 dp mint check mark for tuned strings, tap target ≥ 44 × 44 dp.
- [ ] **`PresetChip`**: 26 dp tall, compound tap targets (label area opens picker; chevron area opens menu), each tap target ≥ 44 × 44 dp via padding extension, anchored `DropdownMenu` with `Preset` / `Chromatic` items showing a check on the current mode.
- [ ] **`SuccessRing`**: mint border ring around the readout well, 320 ms fade-in / 1200 ms hold / 320 ms fade-out. Reduced-motion variant: instant-on / hold / instant-off.
- [ ] **`PermissionDeniedCard`**: `bg.elev1` `ToniqoCard` shape, 28 dp `mic` icon with diagonal slash overlay (until a dedicated icon exists), `H2` heading, `body` description, `btn.primary` 40 dp variant with "Grant access" label.
- [ ] **`PresetPickerSheet`**: `ModalBottomSheet` with segmented control (6 / 7 / 8 string) and grouped category sections, selection indicator on the current preset, dismiss-on-select.
- [ ] **`TunerSettingsSheet`**: `ModalBottomSheet` with a "Reference pitch" segmented control (440 / 432) and an "Auto-advance strings" Material 3 `Switch`, both bound to the ViewModel.

### `tuner/presentation/util/`

- [ ] `TunerPermissionHandling.kt`:
  - [ ] `fun handleGrantAccess(activity: Activity?, permissionLauncher, hasRequestedBefore: Boolean): Action` (return an action enum or sealed type rather than touching Android directly, to keep it testable).
  - [ ] The "permanently denied" branch uses `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.
  - [ ] The "never asked" vs "permanently denied" disambiguation relies on `TunerPreferences.hasRequestedAudioPermission`, not on `shouldShowRequestPermissionRationale` alone.
- [ ] `TunerHaptics.kt`: a thin wrapper around `HapticFeedback` exposing a `tunedStringHaptic()` and `allTunedHaptic()`. Both use `HapticFeedbackType.LongPress` for v1.

### `tuner/presentation/ui/TunerScreen.kt`

- [ ] Replaces the Phase 4 placeholder. Uses `hiltViewModel()` to obtain the `TunerViewModel`.
- [ ] State collection via `collectAsStateWithLifecycle()`.
- [ ] Event collection via the **exact** pattern:
  ```kotlin
  LaunchedEffect(viewModel.events, lifecycleOwner) {
      lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
          viewModel.events.collect { event -> /* ... */ }
      }
  }
  ```
  No alternate patterns; deviating from this is a defect.
- [ ] Permission launcher via `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`. On result, calls `preferences.setHasRequestedAudioPermission(true)` (via a ViewModel action) so the "never asked" → "asked once" transition is recorded.
- [ ] Sheet visibility held in `rememberSaveable` for the preset picker and settings sheet; `remember` for the mode menu (transient).
- [ ] Status-driven content swap:
  - [ ] `PERMISSION_DENIED` → `PermissionDeniedCard` replaces the readout well.
  - [ ] `CAPTURE_FAILED` → a simple error card (single message + a passive "Try again" affordance that re-emits `tunerInput`; minimal scope for 5.4).
  - [ ] All other statuses → the readout well (hero / status line / gauge / Hz pair).
- [ ] String selector row is always shown below, regardless of status.
- [ ] Preset chip row is always shown above the well, regardless of status.
- [ ] Success ring is rendered only when `status == ALL_STRINGS_TUNED`.

### Navigation

- [ ] The tuner route in `ui/navigation/AppNavHost.kt` (or wherever it lives after Phase 4) is updated to compose the real `TunerScreen` instead of the placeholder.
- [ ] Other routes are untouched.

### Manifest

- [ ] `<uses-permission android:name="android.permission.RECORD_AUDIO" />` is present in `AndroidManifest.xml` (added in 5.2; verify still there).
- [ ] No `<uses-feature>` for microphone (we keep the app installable on devices that report no mic).

### Tests

- [ ] `TunerViewModelTest` extended with the cases listed in `Phase5_4-PLAN.md` "Tests" → "Unit tests". All existing 5.3 tests continue to pass.
- [ ] `TuningStatusExtTest` covers every `TuningStatus` value mapping to the documented colour.
- [ ] `TunerScreenTest` (under `androidTest/`) covers the six UI cases listed in the plan.
- [ ] Fakes (`FakeTunerPreferences`, etc.) updated to expose the three new prefs fields.
- [ ] All unit tests use `kotlinx-coroutines-test` `runTest` with virtual time for the delay-based assertions.

### Code Quality

- [ ] No `Color(0xFF...)` literals outside `ui/theme/Tq.kt`.
- [ ] No inline `dp` / `sp` literals outside `Tq.kt` except the one documented `6.dp` exception for the string-selector gap (with comment).
- [ ] No hardcoded user-facing strings — all in `res/values/strings.xml`.
- [ ] No magic numbers — all named constants in companion objects or top-level `object`s.
- [ ] No `TODO("...")` in any 5.4-touched file. The `EnteredChromaticMode` no-op in the event handler is fine as documented behavior.
- [ ] No `!!` outside of contracts the type system can't yet express (and document those with a one-line comment).
- [ ] No `GlobalScope`. All coroutine launches use `viewModelScope` or the appropriate `CoroutineScope` provided by Compose.
- [ ] All public composables, classes, and methods have KDoc.
- [ ] No leftover debug `Log.*` calls in 5.4-touched files.

### Handoff

- [ ] Summary message to the user lists:
  - [ ] Files added (component files, sheets, util files, test files).
  - [ ] Files modified (`TunerPreferences*`, `TunerUiState`, `TunerViewModel`, navigation file, manifest if changed, `libs.versions.toml`, `build.gradle.kts`).
  - [ ] Documentation files modified (`APP_SPECIFICATION.md`, `DESIGN.md`, `DECISIONS.md`).
  - [ ] New dependencies (Compose UI test artifacts).
  - [ ] A manual verification checklist (see User Responsibilities below).
- [ ] Any partial / known-not-yet-working code is flagged explicitly in the summary so the user doesn't accidentally commit broken code.
- [ ] Suggested commit shape — three commits total:
  1. **`docs: close Phase 5 documentation debt`** — `APP_SPECIFICATION.md` chromatic-mode addendum, `DESIGN.md` §8.1 corrections and §14 Q1/Q2 resolutions, `DECISIONS.md` 13 new entries.
  2. **`feat: tuner viewmodel and preferences for chromatic re-entry, auto-advance, and 432 Hz toggle`** — `TunerPreferences` interface + impl, `TunerUiState`, `TunerViewModel` (new actions, snapshot field, auto-advance gating, reference-pitch retune), `TuningStatusExt`, plus all unit and helper tests (`TunerViewModelTest` extensions, `TuningStatusExtTest`, fake updates).
  3. **`feat: tuner screen UI`** — all 14 components under `tuner/presentation/ui/components/`, the two `tuner/presentation/util/` helpers, `ui/util/Motion.kt`, `TunerScreen.kt` replacing the Phase 4 placeholder, navigation wire-up, `androidTest`s, version-catalog and `build.gradle.kts` updates for Compose UI test artifacts.

## User Responsibilities (Verification in Android Studio and on Device)

### Build and unit tests

- [ ] **File → Sync Project with Gradle Files** completes without errors.
- [ ] **Build → Make Project** completes successfully.
- [ ] **Run → Run All Tests** (unit) reports green, including the extended `TunerViewModelTest` and the new `TuningStatusExtTest`.

### Instrumented / Compose UI tests

- [ ] Run `TunerScreenTest` on an Android 12+ emulator or device. All six tests green.

### On-device manual verification

The following scenarios must be walked end-to-end on a real device (or a capable emulator with microphone passthrough). Mark each scenario as passing only if it matches the design and the mockups.

- [ ] **First launch, permission not yet granted.** Open the tuner. Permission-denied card shows (not the system dialog). Body copy matches the design. Tapping "Grant access" launches the system permission dialog. Granting permission causes the card to disappear and the readout to start listening (mint `MIC LIVE` indicator visible, "LISTENING —" status, needle centred and faded).
- [ ] **Permission permanently denied.** Deny twice (or once with "Don't ask again"). Re-open the tuner. The card still shows. Tapping "Grant access" opens the system app-settings screen rather than launching the dialog again. After granting permission in settings and returning, the screen recovers automatically and starts listening.
- [ ] **Preset picker.** Tap the preset chip's left half ("6-STRING · DROP"). The bottom sheet opens. The segmented control reflects the current preset's string count. Switching string counts shows the correct grouped list. Tapping any preset dismisses the sheet and updates the screen (kicker shows the new tuning's display name, string selector shows the new strings).
- [ ] **Mode toggle.** Tap the preset chip's chevron. A popover with "Preset" and "Chromatic" appears, with a check on "Preset". Tapping "Chromatic" dismisses the menu and switches the screen into chromatic mode (the readout hero shows whatever note is detected; the string selector no longer highlights a current string).
- [ ] **Chromatic re-entry restores position.** From preset mode at string 3, enter chromatic via the mode menu. Then re-open the mode menu and tap "Preset". The screen returns to preset mode at string 3 with no check marks (per Option 2). `tunedStringIndices` is empty; the user re-tunes from there.
- [ ] **Auto-advance enabled (default).** Play each string of a 6-string standard tuning in turn. Each string in turn highlights, the needle moves toward 0°, the status colour cycles cyan/amber/mint as the player tunes, and on sustained-in-tune the haptic fires, the check mark appears, and after ~200 ms the next string activates. After the sixth string, the success ring fades in around the well, holds 1.2 s, then fades out, and the screen flips to chromatic mode.
- [ ] **Auto-advance disabled.** Open settings sheet, switch off "Auto-advance strings", close the sheet. Sustained-in-tune still triggers the haptic and the check mark, but the next string is **not** automatically activated. The user can tap the next string to advance.
- [ ] **432 Hz toggle.** Open settings sheet, switch reference pitch to 432 Hz. Close the sheet. The header kicker now reads `TUNER · A4 = 432 HZ`. The TARGET Hz field in the readout reflects the 432-Hz-derived value (e.g. E2 ≈ 80.92 Hz, not 82.41). Reopen the app — the 432 Hz setting persists.
- [ ] **Persisted auto-advance.** Switch auto-advance off, kill and reopen the app. The switch is still off in the settings sheet.
- [ ] **Persisted last preset.** Select Drop D, kill and reopen the app. Drop D is still the active preset.
- [ ] **Rotation.** Rotate the device while in preset mode mid-tuning. The active string and check marks are preserved. No duplicate haptic fires during the rotation. No noticeable mic drop-out (the 5-second `WhileSubscribed` grace covers the rotation gap).
- [ ] **Background / foreground.** Tab away to another bottom-nav destination, wait 6 s, tab back. The mic is released during the gap (verify via OS mic indicator if available); on return, capture resumes promptly.
- [ ] **Dark and light palettes.** Toggle the system theme (Settings → Display → Dark theme). Open the tuner in each mode. All elements render correctly with the appropriate `Tq.*` token values; no hardcoded colors leak through.
- [ ] **Reduced motion.** Enable Developer Options → "Animator duration scale" = "Animation off". Open the tuner. The needle settles in ~80 ms with linear easing (snappier than usual). The success ring appears instantly rather than fading in.
- [ ] **Font scaling.** Set system font size to maximum. Open the tuner. The detected-note hero letter does not scale (it uses `NonScalingText` per §13.1). Other text scales up. No clipping or overflow in the readout well, status line, or string-selector pills.
- [ ] **No Logcat crashes or Hilt binding errors** during any of the above.

### Decision Log

- [ ] All 13 decisions listed in the plan's "DECISIONS.md" section are present in `DECISIONS.md` before the phase is marked complete.

## Phase Completion

This phase is complete when:
1. Every agent-side checkbox above is met.
2. Every user-side scenario above passes verification on a real device.
3. The user confirms green on the build and all test suites.
4. `DECISIONS.md` reflects every decision made in this phase.
