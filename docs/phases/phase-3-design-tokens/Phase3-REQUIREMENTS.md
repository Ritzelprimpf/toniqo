# Phase 3 — Requirements & Acceptance Criteria

Phase 3 covers design-token implementation only. No navigation, no screens. The phase is complete when both checklists below pass.

## Agent Responsibilities

### Fonts

- [ ] `app/src/main/res/font/` contains the Space Grotesk and JetBrains Mono `.ttf` / `.otf` files (or the agent has clearly flagged to the user that they must add them).
- [ ] `ui/theme/Font.kt` exposes two `FontFamily` constants (`SpaceGroteskFamily`, `JetBrainsMonoFamily`) with the weights specified in `DESIGN.md` §3.

### Token object

- [ ] `ui/theme/Tq.kt` exists.
- [ ] `Tq.Color` contains every dark-theme token listed in `DESIGN.md` §2.1 (surface, text, signal). Light-theme tokens from §2.2 are present in a parallel namespace.
- [ ] `Tq.Sp` contains all spacing tokens from §4 as `Dp` constants.
- [ ] `Tq.Radius` contains all radius tokens from §5 as `Dp` constants.
- [ ] `Tq.Type` contains every type token from §3 as `TextStyle` constants. Letter spacing uses `.em`, not `.sp`.
- [ ] Kicker and `MonoMicro` tokens apply uppercase transformation (either via `textAllCaps` or by callers being expected to uppercase the string — document the choice in the KDoc).
- [ ] All public types and members in `Tq.kt` have KDoc comments.

### Theme composable

- [ ] `ui/theme/ToniqoTheme.kt` defines `ToniqoTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)`.
- [ ] The composable builds a Material 3 `ColorScheme`, `Typography`, and `Shapes` from `Tq` tokens and passes them to `MaterialTheme`.
- [ ] The mapping from `Tq.Type` to Material's typography slots is documented in a KDoc block on `ToniqoTheme`.
- [ ] Switching `useDarkTheme` between `true` and `false` selects the corresponding palette from §2.1 / §2.2.

### Helpers

- [ ] `ui/components/NonScalingText.kt` defines `NonScalingText(text, style, modifier)` per `DESIGN.md` §13.1.
- [ ] KDoc on `NonScalingText` explains the two intended uses (BPM numeral, tuner detected-note letter).

### Preview screen

- [ ] `ui/theme/TokensPreviewScreen.kt` exists with the section structure described in `Phase3-PLAN.md` step 5.
- [ ] Two `@Preview` functions exist: one for dark, one for light.
- [ ] The preview screen is **not** registered in any future nav graph and is not reachable from the running app via UI.

### MainActivity wiring

- [ ] `MainActivity.setContent { ... }` temporarily shows `ToniqoTheme { TokensPreviewScreen() }`.
- [ ] A `// TODO: replaced in Phase 4 by AppNavHost` comment is present next to this wiring.

### Tests

- [ ] A sanity test under `app/src/test/java/de/ritzelprimpf/toniqo/ui/theme/` asserts a representative set of token values (at least one colour from each category, plus one type token's font size and letter spacing).
- [ ] `./gradlew test`-equivalent (Android Studio test run) passes (user-verified).

### Code Quality

- [ ] No hardcoded colour, font, spacing, or radius values anywhere — every value comes from `Tq`.
- [ ] No `MaterialTheme.colorScheme.primary` (etc.) calls outside `ToniqoTheme.kt` itself — callers within Phase 3's preview should use `Tq.Color.*` directly, since the Material mapping is documented but secondary.

### Handoff

- [ ] Summary message to the user lists files added, files modified, fonts required, and what to verify in Android Studio.

## User Responsibilities (Verification in Android Studio)

- [ ] User downloads the required font files and places them in `res/font/` (or confirms they were committed by the agent).
- [ ] After Gradle sync, **Build → Make Project** completes with no errors.
- [ ] `TokensPreviewScreen`'s `@Preview` renders correctly in the Android Studio Preview pane for both dark and light variants.
- [ ] Launching the app on an Android 12+ emulator/device displays the tokens preview screen without crashes.
- [ ] Visual spot-check against `DESIGN.md`: mint signal looks right, surfaces show clear elevation steps, type families render as Space Grotesk and JetBrains Mono.

## Decision Log

- [ ] Any deviation from `DESIGN.md` (e.g., a Material slot mapping that needed adjustment, a font weight substitution, a TextStyle that needed extra config) is recorded in `DECISIONS.md`.
