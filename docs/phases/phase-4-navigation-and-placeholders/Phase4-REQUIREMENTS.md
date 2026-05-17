# Phase 4 — Requirements & Acceptance Criteria

All items below must be satisfied for Phase 4 to be considered complete.

## Agent Responsibilities

### Navigation

- [ ] Navigation routes are defined as constants under `ui/navigation/Routes.kt` — no raw route strings in composables.
- [ ] `AppNavHost.kt` exists and wires all five top-level destinations plus the nested Info graph.
- [ ] `NavController` is created at the top level and passed (or hoisted via `CompositionLocal`) appropriately.
- [ ] Back navigation works correctly within the Info nested graph.
- [ ] Bottom-nav tab switching preserves state via `saveState` / `restoreState` on each `NavigationBarItem`.

### Main Layout

- [ ] `MainScreen.kt` hosts both the `AppNavigationBar` and the content area via a Material 3 `Scaffold`.
- [ ] `AppNavigationBar` contains five entries in this order: Tuner, Metronome, Key Finder, Chord Finder, More.
- [ ] The active tab is highlighted with the mint indicator pill and 6dp glow per `DESIGN.md` §6.4.
- [ ] The bottom bar remains visible when navigating within the Info nested graph.

### Components

- [ ] `ui/components/ToniqoCard.kt` matches `DESIGN.md` §6.6: `bg.elev1`, `line.faint` 1dp border, `r.lg` radius, `sp.4` padding.
- [ ] `ui/components/AppNavigationBar.kt` matches `DESIGN.md` §6.4 (`icon-label` style only): 48dp content + 18dp gesture pill, 20dp icons, `mono.micro` uppercase labels, 18×2dp mint indicator with 6dp glow.

### Module Placeholder Screens

Each lives under `<module>/presentation/ui/` and meets the requirements in `Phase4-PLAN.md`'s "Placeholder Screen Requirements" section:

- [ ] `TunerScreen` — module name in `Tq.Type.H1`, description, representative icon.
- [ ] `MetronomeScreen` — same.
- [ ] `KeyFinderScreen` — same.
- [ ] `ChordFinderScreen` — same.

### Info Section

All under `ui/info/`, all using `ToniqoCard`:

- [ ] `InfoHomeScreen` lists links to all four sub-screens with leading icon, `Tq.Type.BodyStrong` title, trailing `chevron-right`.
- [ ] `HelpScreen` shows static placeholder text per module.
- [ ] `PrivacyPolicyScreen` shows static placeholder text.
- [ ] `LicensesScreen` shows static placeholder text.
- [ ] `RateAndShareScreen` has two buttons stubbed with `Snackbar` feedback.

### MainActivity wiring

- [ ] `MainActivity.setContent { ... }` is `ToniqoTheme { MainScreen() }`.
- [ ] The Phase 3 `// TODO: replaced in Phase 4 by AppNavHost` comment is removed.
- [ ] `TokensPreviewScreen` is still present in the codebase for reference but is not registered in `AppNavHost` and is not reachable from the running app.

### Code Quality

- [ ] Every screen-level composable has a `@Preview` annotation showing it under `ToniqoTheme` (both dark and light where feasible).
- [ ] No hardcoded colour, font, spacing, or radius values anywhere. Everything goes through `Tq.*` or `MaterialTheme.colorScheme.*` (which is itself sourced from `Tq` via `ToniqoTheme`).
- [ ] No user-visible string literals in composables — all in `res/values/strings.xml`.
- [ ] No module-specific logic is implemented this phase.

### Tests

- [ ] `./gradlew test`-equivalent (Android Studio test run) continues to pass — all Phase 1–3 tests still green (user-verified).
- [ ] At least one Compose UI test in `androidTest/` verifies that all five top-level destinations are reachable via the bottom nav.

### Handoff

- [ ] Summary message to the user lists files added, files modified, and what to verify in Android Studio.

## User Responsibilities (Verification in Android Studio)

- [ ] After Gradle sync, **Build → Make Project** completes with no errors.
- [ ] App launches on an Android 12+ emulator/device.
- [ ] All five bottom-nav destinations are reachable.
- [ ] Switching between tabs preserves each tab's back stack (verify by navigating into Info → Help, switching to Tuner, switching back to More: Help should still be on screen).
- [ ] No exceptions in Logcat during normal navigation.
- [ ] Visual spot-check: every screen looks like it belongs to the Toniqo design system — no stray Material defaults, no white surfaces in dark mode.

## Decision Log

- [ ] Any non-trivial decision taken during Phase 4 is recorded in `DECISIONS.md`.
