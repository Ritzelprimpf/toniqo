# Phase 3 — Design Tokens (Theme Implementation)

## Goal

Implement the design system defined in `DESIGN.md` as Compose code, with no application screens yet. The output of this phase is a fully working theme — colours, typography, spacing, radii, and the few shared helpers — that any future screen can adopt by wrapping its content in `ToniqoTheme { ... }`.

This phase exists between the backend outline (Phase 2) and the navigation shell (Phase 4) because every screen needs to consume tokens from day one. Building screens first against `MaterialTheme` defaults and re-skinning them later is wasted work.

## Scope

- Bundle the two fonts (Space Grotesk, JetBrains Mono) as local font assets — no Downloadable Fonts.
- Implement the colour palette per `DESIGN.md` §2 (both dark and light themes).
- Implement the type scale per `DESIGN.md` §3, using `.em` for letter spacing.
- Implement the spacing scale per `DESIGN.md` §4.
- Implement the radius tokens per `DESIGN.md` §5.
- Implement the `Tq` object stub per `DESIGN.md` §11, expanded to cover every token in §2–§5.
- Implement the `NonScalingText` helper per `DESIGN.md` §13.
- Build a `ToniqoTheme` composable that supplies a Material 3 `ColorScheme`, `Typography`, and `Shapes` derived from the `Tq` tokens. Material 3 callers that bypass the `Tq` object (any `MaterialTheme.colorScheme.primary`, etc.) must still receive on-brand values.
- Build a single internal demo composable (`TokensPreviewScreen`) that displays one swatch per token, used only to verify the theme renders correctly in Android Studio's Preview pane. Not navigable from the app.

## Out of Scope

- No navigation (Phase 4).
- No module placeholder screens (Phase 4).
- No `MainActivity` content change beyond temporarily setting it to `ToniqoTheme { TokensPreviewScreen() }` so the user can verify visually.
- No component primitives (buttons, chips, cards) beyond what `TokensPreviewScreen` needs to demonstrate. Those land in Phase 4 or later as they get used.

## Reading Order Before Starting

1. `DESIGN.md` end-to-end, especially the agent preamble and §2, §3, §11, §13.
2. `IMPLEMENTATION_NOTES.md` "Working with Android Studio" section.
3. `DECISIONS.md` for any prior design-related decisions.

## Steps

### 1. Bundle the fonts

- Download Space Grotesk (weights 400, 500, 600, 700) and JetBrains Mono (weights 300, 400, 500) from Google Fonts as `.ttf` or `.otf`.
- Place them under `app/src/main/res/font/`. Naming convention: `space_grotesk_regular.ttf`, `space_grotesk_medium.ttf`, etc.
- Add a Compose `FontFamily` for each in `ui/theme/Font.kt`. The agent should note that font files cannot be created by code — flag this clearly to the user and provide instructions for which files to download and place.

### 2. Implement the token object

Create `ui/theme/Tq.kt`. The full token object covers:

- `Tq.Color` — every token from `DESIGN.md` §2.1 (dark) as `Color(0xFF...)` constants. Light palette goes in a parallel `Tq.LightColor` namespace; pick one consistent structure and stick with it.
- `Tq.Sp` — every spacing token from §4 as `Dp` constants.
- `Tq.Radius` — every radius token from §5 as `Dp` constants.
- `Tq.Type` — every type token from §3 as `TextStyle` constants, using `.em` for letter spacing.

Use Kotlin `object` for these — they are pure stateless utilities (see `CLAUDE.md` §4 for the documented exception to the no-singletons rule).

### 3. Implement `ToniqoTheme`

Create `ui/theme/ToniqoTheme.kt`. The composable:

- Takes a `useDarkTheme: Boolean = isSystemInDarkTheme()` parameter.
- Builds a Material 3 `ColorScheme` by mapping `Tq.Color` tokens to Material's semantic slots. Suggested mapping (refine as needed):
  - `primary` → `Tq.Color.SignalMint`
  - `onPrimary` → `Tq.Color.BgBase`
  - `background` → `Tq.Color.BgBase`
  - `surface` → `Tq.Color.BgElev1`
  - `onSurface` → `Tq.Color.FgPrimary`
  - `onSurfaceVariant` → `Tq.Color.FgSecondary`
  - `outline` → `Tq.Color.Line`
  - `outlineVariant` → `Tq.Color.LineFaint`
  - `error` → `Tq.Color.SignalAmber` (placeholder — re-evaluate when an error pattern is actually designed)
- Builds a Material 3 `Typography` from `Tq.Type`, mapping the design tokens to Material's `displayLarge`, `headlineLarge`, `titleLarge`, `bodyLarge`, etc. Material's slots don't map 1:1 to our token names; document the mapping in a KDoc comment on `ToniqoTheme`.
- Builds a `Shapes` value from `Tq.Radius`.
- Wraps `MaterialTheme(...)` and yields its `content` slot.

### 4. Implement the `NonScalingText` helper

Create `ui/components/NonScalingText.kt` with the helper from `DESIGN.md` §13.1. One file, one composable, KDoc explaining when and why to use it (BPM numeral, tuner detected-note letter — anywhere a `display.xl` or `display.l` style is applied).

### 5. Build `TokensPreviewScreen`

Create `ui/theme/TokensPreviewScreen.kt`. It is an internal-only composable (not registered in any nav graph, not accessible from the running app). Its purpose is to give Android Studio's `@Preview` and the user a single place to eyeball every token.

Suggested structure: a vertical scroll containing:
- A header "Toniqo — Tokens Preview" in `Tq.Type.H1`.
- A "Colours — surfaces" section: a row of swatches for `bg.base`, `bg.elev1`, `bg.elev2`, `bg.elev3`, `bg.inset`, with each swatch labelled in `Tq.Type.Kicker`.
- A "Colours — text" section: a paragraph of placeholder text in each text token's colour over `bg.base`.
- A "Colours — signal" section: four swatches for mint, cyan, amber, violet, each labelled.
- A "Typography" section: one line of sample text per type token, with the token name in `Tq.Type.MonoMicro` underneath each sample.
- A "Spacing" section: a row of mint-coloured boxes whose widths are `Tq.Sp.s1` through `Tq.Sp.s12`, each labelled.
- A "Radii" section: a row of squares with each radius applied as a clip, labelled.

Provide two `@Preview` functions: one for dark, one for light, both wrapped in `ToniqoTheme(useDarkTheme = ...)`.

### 6. Temporarily wire `MainActivity` to the preview

In `MainActivity.kt`, replace the Phase 1 placeholder with:

```kotlin
setContent {
    ToniqoTheme {
        TokensPreviewScreen()
    }
}
```

This is overwritten in Phase 4 when real navigation comes in. Mark with a `// TODO: replaced in Phase 4 by AppNavHost` comment.

### 7. Write tests

Token files are constants, not logic, so they don't need behavioural tests. Add a single sanity test under `app/src/test/java/de/ritzelprimpf/toniqo/ui/theme/` that imports `Tq` and asserts a few representative values match expectations (e.g., `Tq.Color.SignalMint.toArgb() == 0xFF9CFF8B.toInt()`). This protects against accidental refactors that break the colour values.

### 8. Hand off for verification

Summary message to the user must include:
- Files added.
- Files modified.
- **A list of font files the user must download and place under `res/font/` themselves**, because the agent cannot produce binary font files. Specify exactly which weights from which Google Fonts URLs.
- Instructions to open `TokensPreviewScreen` in Android Studio's preview pane and confirm both dark and light variants render correctly.

## Completion Criteria

See `Phase3-REQUIREMENTS.md`.
