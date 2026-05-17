# Phase 4 — Navigation Shell & Placeholders

## Goal

Build the complete UI shell of the app. Every screen and navigation destination is in place. Module screens contain placeholder content only — no real functionality. The Info section is fully wired with stub sub-screens.

By the end of this phase the app is fully navigable, dressed in the Toniqo design system from Phase 3, and ready to have real module functionality dropped in module by module starting at Phase 5.

## Scope

- App-level navigation with all five destinations (Tuner, Metronome, Key Finder, Chord Finder, Info).
- Bottom navigation bar matching `DESIGN.md` §6.4 (`icon-label` variant).
- Module placeholder screens, one per module, in each module's `presentation/ui/` package.
- Info section with five sub-screens (home, help, privacy, licenses, rate & share) under `ui/info/`.
- A reusable card primitive based on `DESIGN.md` §6.6 — the Info screens need it, and it's a clean place to introduce it.
- A reusable bottom nav bar component from `DESIGN.md` §6.4.

## Out of Scope

- Any real module functionality (that's Phase 5 onward).
- Live data from ViewModels — screens may observe ViewModel state but display placeholder text.
- Any audio or microphone access.
- Full component library (buttons, chips, note pills, etc.). Build only what this phase's screens actually need.

## Prerequisites

Phase 3 must be complete. Tokens (`Tq.*`), `ToniqoTheme`, and `NonScalingText` are assumed to exist and to be used everywhere. No hardcoded values for anything covered by the design system.

## Navigation Layout

The navigation chrome is a **Material 3 `NavigationBar`** at the bottom, matching `DESIGN.md` §6.4. Five destinations, in order: Tuner, Metronome, Key Finder, Chord Finder, More.

> **Naming note.** `DESIGN.md` uses "MORE" as the bottom-nav label for the Info section to keep the kicker-mono text short and consistent with the other four labels. Routes and code can still call it `info`; the label is just `More`.

The Info section is a nested nav graph. Tapping the More tab shows `InfoHomeScreen`; sub-screens push onto a nested back stack while the bottom bar remains visible.

## Navigation Structure

```
AppNavHost (top-level)
├── tuner_route        → TunerScreen        (placeholder)
├── metronome_route    → MetronomeScreen    (placeholder)
├── keyfinder_route    → KeyFinderScreen    (placeholder)
├── chordfinder_route  → ChordFinderScreen  (placeholder)
└── info (nested graph)
    ├── info_home_route    → InfoHomeScreen
    ├── help_route         → HelpScreen
    ├── privacy_route      → PrivacyPolicyScreen
    ├── licenses_route     → LicensesScreen
    └── rate_share_route   → RateAndShareScreen
```

Routes are defined as constants in a `sealed class` or `object` under `ui/navigation/Routes.kt`. No raw strings in composables.

## File Locations

| File | Location |
|---|---|
| `Routes.kt`, `BottomNavDestinations.kt`, `AppNavHost.kt` | `ui/navigation/` |
| `MainScreen.kt` (scaffold + nav bar) | `ui/` |
| `AppNavigationBar.kt` | `ui/components/` |
| `ToniqoCard.kt` (primitive from §6.6) | `ui/components/` |
| `TunerScreen` placeholder | `tuner/presentation/ui/` |
| `MetronomeScreen` placeholder | `metronome/presentation/ui/` |
| `KeyFinderScreen` placeholder | `keyfinder/presentation/ui/` |
| `ChordFinderScreen` placeholder | `chordfinder/presentation/ui/` |
| Info screens | `ui/info/` |

## Placeholder Screen Requirements

Each placeholder module screen must:
- Use `Tq.Color.BgBase` as the screen background.
- Display the module name in `Tq.Type.H1` centred near the top.
- Display a one-line description paraphrased from `APP_SPECIFICATION.md` in `Tq.Type.Body` with `Tq.Color.FgSecondary`.
- Show the module's representative icon (from the custom icon set if available, or a Material `Icons.Outlined.*` placeholder marked with a comment that it's pending the custom set).
- Be function-only — no real ViewModel state observation yet.

## Info Section Requirements

All Info screens live under `ui/info/` and use the `ToniqoCard` primitive.

- **`InfoHomeScreen`** — list of cards navigating to each sub-screen. Each card has a leading icon (20dp), title in `Tq.Type.BodyStrong`, and trailing `chevron-right` icon.
- **`HelpScreen`** — static placeholder text per module ("Help for the Guitar Tuner will be added here", etc.), wrapped in `ToniqoCard`s.
- **`PrivacyPolicyScreen`** — single `ToniqoCard` with placeholder text. WebView integration deferred until a privacy policy URL exists.
- **`LicensesScreen`** — single `ToniqoCard` with placeholder text. License collection deferred.
- **`RateAndShareScreen`** — two `btn.default` buttons (use a temporary inline `Button` styled with `Tq` tokens since `btn.default` isn't built yet). Tapping shows a `Snackbar` stub: "Will open Google Play" / "Will open share sheet".

> **Note on the "More" label vs. the Info screen header.** The bottom-nav label reads `MORE`. The Info home screen's header reads `Info` in `Tq.Type.H1`. This is intentional — short kicker label vs. proper screen title.

## Steps

1. Create `Routes.kt` and `BottomNavDestinations.kt` under `ui/navigation/`.
2. Build `ToniqoCard.kt` (the §6.6 card primitive) under `ui/components/`.
3. Build `AppNavigationBar.kt` per `DESIGN.md` §6.4 under `ui/components/`. The active indicator pill + 6dp mint glow (one of the two design-language glows from §10) lives here.
4. Build `AppNavHost.kt` wiring all destinations.
5. Build `MainScreen.kt` (Scaffold with `AppNavigationBar` + `NavHost` content slot).
6. Build the four module placeholder screens in their respective `<module>/presentation/ui/` packages.
7. Build the five Info screens under `ui/info/`.
8. Update `MainActivity.setContent { ... }` from the Phase 3 tokens preview to `ToniqoTheme { MainScreen() }`. Remove the `// TODO: replaced in Phase 4` comment from `MainActivity`.
9. Keep `TokensPreviewScreen` in the project for ongoing reference but do not register it in the nav graph.

## Completion Criteria

See `Phase4-REQUIREMENTS.md`.
