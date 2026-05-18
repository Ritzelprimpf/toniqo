package de.ritzelprimpf.toniqo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Toniqo Material 3 theme. Wraps [MaterialTheme] with the brand's colour scheme,
 * typography, and shapes derived entirely from [Tq] tokens. Material callers that use
 * `MaterialTheme.colorScheme.*` or `MaterialTheme.typography.*` receive on-brand values.
 *
 * Pass [useDarkTheme] = `true` for the primary dark theme (design target);
 * `false` for the light fallback. Defaults to the system setting.
 *
 * Material You dynamic colour is **off** — the static brand palette is always used.
 * (See DECISIONS.md: "Design system locked in via DESIGN.md".)
 *
 * ---
 *
 * ## [Tq.Color] → Material 3 `ColorScheme` mapping (dark theme; light uses [Tq.LightColor])
 *
 * | Material slot           | Tq token                    | Rationale                              |
 * |-------------------------|-----------------------------|----------------------------------------|
 * | `primary`               | `SignalMint`                | Brand accent / primary action          |
 * | `onPrimary`             | `BgBase`                    | Dark text on mint surface              |
 * | `primaryContainer`      | `BgElev2`                   | Muted container behind primary actions |
 * | `onPrimaryContainer`    | `FgPrimary`                 | Readable text on elev2                 |
 * | `secondary`             | `FgSecondary`               | Neutral secondary accent               |
 * | `onSecondary`           | `BgBase`                    | Base surface behind secondary          |
 * | `secondaryContainer`    | `BgElev2`                   | Muted secondary surface                |
 * | `onSecondaryContainer`  | `FgPrimary`                 | Readable text on elev2                 |
 * | `tertiary`              | `FgTertiary`                | Low-emphasis accent                    |
 * | `onTertiary`            | `BgBase`                    | Base surface behind tertiary           |
 * | `tertiaryContainer`     | `BgElev2`                   | Muted tertiary surface                 |
 * | `onTertiaryContainer`   | `FgPrimary`                 | Readable text on elev2                 |
 * | `error`                 | `SignalAmber`               | Sharp / error semantic                 |
 * | `onError`               | `BgBase`                    | Base surface on error                  |
 * | `errorContainer`        | `BgElev2`                   | Muted error container                  |
 * | `onErrorContainer`      | `SignalAmber`               | Error text on muted surface            |
 * | `background`            | `BgBase`                    | Screen chassis                         |
 * | `onBackground`          | `FgPrimary`                 | Body text on screen root               |
 * | `surface`               | `BgElev1`                   | Cards, sheets                          |
 * | `onSurface`             | `FgPrimary`                 | Text on cards                          |
 * | `surfaceVariant`        | `BgElev2`                   | Chips, segment interiors               |
 * | `onSurfaceVariant`      | `FgSecondary`               | Sub-labels on elevated surfaces        |
 * | `outline`               | `Line`                      | Default border                         |
 * | `outlineVariant`        | `LineFaint`                 | Subtle / card border                   |
 * | `surfaceTint`           | `SignalMint`                | M3 elevation tint (used sparingly)     |
 * | `inverseSurface`        | `FgPrimary`                 | Snackbar / inverted surface            |
 * | `inverseOnSurface`      | `BgBase`                    | Text on inverted surface               |
 * | `inversePrimary`        | `SignalMint`                | Primary on inverted surface            |
 * | `scrim`                 | `BgBase`                    | Modal overlay tint                     |
 *
 * ---
 *
 * ## [Tq.Type] → Material 3 `Typography` mapping
 *
 * | Material slot    | Tq token        | Notes                                          |
 * |------------------|-----------------|------------------------------------------------|
 * | `displayLarge`   | `DisplayXl`     | BPM numeral — `NonScalingText` only            |
 * | `displayMedium`  | `DisplayL`      | Tuner note letter — `NonScalingText` only      |
 * | `displaySmall`   | `DisplayS`      | Artboard headlines                             |
 * | `headlineLarge`  | `H1`            | Screen titles                                  |
 * | `headlineMedium` | `H2`            | Card headers, chord names                      |
 * | `headlineSmall`  | `H2`            | No smaller headline in Tq scale; mirrors H2    |
 * | `titleLarge`     | `BodyStrong`    | Semi-bold body used as card title area         |
 * | `titleMedium`    | `BodyStrong`    | Tq scale has no distinct title tier            |
 * | `titleSmall`     | `Caption`       | Smallest title level                           |
 * | `bodyLarge`      | `Body`          | Default body text                              |
 * | `bodyMedium`     | `BodyStrong`    | Semi-bold body                                 |
 * | `bodySmall`      | `Caption`       | Small body / secondary copy                    |
 * | `labelLarge`     | `Kicker`        | Kicker labels (ALL-CAPS, callers must provide) |
 * | `labelMedium`    | `KickerS`       | Card kickers (ALL-CAPS, callers must provide)  |
 * | `labelSmall`     | `MonoMicro`     | Smallest labels (ALL-CAPS, callers must provide)|
 *
 * ---
 *
 * ## [Tq.Radius] → Material 3 `Shapes` mapping
 *
 * | Material slot  | Tq token       |
 * |----------------|----------------|
 * | `extraSmall`   | `Radius.Xs` 4dp  |
 * | `small`        | `Radius.Sm` 8dp  |
 * | `medium`       | `Radius.Md` 12dp |
 * | `large`        | `Radius.Lg` 16dp |
 * | `extraLarge`   | `Radius.Xl` 18dp |
 *
 * [Tq.Radius.Pill] (999dp) has no Material Shapes slot — apply it directly as
 * `RoundedCornerShape(Tq.Radius.Pill)` wherever pill buttons or segmented controls are built.
 */
@Composable
fun ToniqoTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) {
        darkColorScheme(
            primary              = Tq.Color.SignalMint,
            onPrimary            = Tq.Color.BgBase,
            primaryContainer     = Tq.Color.BgElev2,
            onPrimaryContainer   = Tq.Color.FgPrimary,
            secondary            = Tq.Color.FgSecondary,
            onSecondary          = Tq.Color.BgBase,
            secondaryContainer   = Tq.Color.BgElev2,
            onSecondaryContainer = Tq.Color.FgPrimary,
            tertiary             = Tq.Color.FgTertiary,
            onTertiary           = Tq.Color.BgBase,
            tertiaryContainer    = Tq.Color.BgElev2,
            onTertiaryContainer  = Tq.Color.FgPrimary,
            error                = Tq.Color.SignalAmber,
            onError              = Tq.Color.BgBase,
            errorContainer       = Tq.Color.BgElev2,
            onErrorContainer     = Tq.Color.SignalAmber,
            background           = Tq.Color.BgBase,
            onBackground         = Tq.Color.FgPrimary,
            surface              = Tq.Color.BgElev1,
            onSurface            = Tq.Color.FgPrimary,
            surfaceVariant       = Tq.Color.BgElev2,
            onSurfaceVariant     = Tq.Color.FgSecondary,
            outline              = Tq.Color.Line,
            outlineVariant       = Tq.Color.LineFaint,
            surfaceTint          = Tq.Color.SignalMint,
            inverseSurface       = Tq.Color.FgPrimary,
            inverseOnSurface     = Tq.Color.BgBase,
            inversePrimary       = Tq.Color.SignalMint,
            scrim                = Tq.Color.BgBase,
        )
    } else {
        lightColorScheme(
            primary              = Tq.LightColor.SignalMint,
            onPrimary            = Tq.LightColor.BgBase,
            primaryContainer     = Tq.LightColor.BgElev2,
            onPrimaryContainer   = Tq.LightColor.FgPrimary,
            secondary            = Tq.LightColor.FgSecondary,
            onSecondary          = Tq.LightColor.BgBase,
            secondaryContainer   = Tq.LightColor.BgElev2,
            onSecondaryContainer = Tq.LightColor.FgPrimary,
            tertiary             = Tq.LightColor.FgTertiary,
            onTertiary           = Tq.LightColor.BgBase,
            tertiaryContainer    = Tq.LightColor.BgElev2,
            onTertiaryContainer  = Tq.LightColor.FgPrimary,
            error                = Tq.LightColor.SignalAmber,
            onError              = Tq.LightColor.BgBase,
            errorContainer       = Tq.LightColor.BgElev2,
            onErrorContainer     = Tq.LightColor.SignalAmber,
            background           = Tq.LightColor.BgBase,
            onBackground         = Tq.LightColor.FgPrimary,
            surface              = Tq.LightColor.BgElev1,
            onSurface            = Tq.LightColor.FgPrimary,
            surfaceVariant       = Tq.LightColor.BgElev2,
            onSurfaceVariant     = Tq.LightColor.FgSecondary,
            outline              = Tq.LightColor.Line,
            outlineVariant       = Tq.LightColor.LineFaint,
            surfaceTint          = Tq.LightColor.SignalMint,
            inverseSurface       = Tq.LightColor.FgPrimary,
            inverseOnSurface     = Tq.LightColor.BgBase,
            inversePrimary       = Tq.LightColor.SignalMint,
            scrim                = Tq.LightColor.BgBase,
        )
    }

    val typography = Typography(
        displayLarge   = Tq.Type.DisplayXl,
        displayMedium  = Tq.Type.DisplayL,
        displaySmall   = Tq.Type.DisplayS,
        headlineLarge  = Tq.Type.H1,
        headlineMedium = Tq.Type.H2,
        headlineSmall  = Tq.Type.H2,
        titleLarge     = Tq.Type.BodyStrong,
        titleMedium    = Tq.Type.BodyStrong,
        titleSmall     = Tq.Type.Caption,
        bodyLarge      = Tq.Type.Body,
        bodyMedium     = Tq.Type.BodyStrong,
        bodySmall      = Tq.Type.Caption,
        labelLarge     = Tq.Type.Kicker,
        labelMedium    = Tq.Type.KickerS,
        labelSmall     = Tq.Type.MonoMicro,
    )

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(Tq.Radius.Xs),
        small      = RoundedCornerShape(Tq.Radius.Sm),
        medium     = RoundedCornerShape(Tq.Radius.Md),
        large      = RoundedCornerShape(Tq.Radius.Lg),
        extraLarge = RoundedCornerShape(Tq.Radius.Xl),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = typography,
        shapes      = shapes,
        content     = content,
    )
}
