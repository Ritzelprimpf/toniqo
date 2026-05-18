package de.ritzelprimpf.toniqo.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.material3.Text

/**
 * A `Text` wrapper that prevents the given [style] from scaling with the user's
 * system font-size accessibility setting.
 *
 * ## When to use
 *
 * Use this composable — and **only** this composable — for:
 * - The **metronome BPM numeral** ([de.ritzelprimpf.toniqo.ui.theme.Tq.Type.DisplayXl])
 * - The **tuner detected-note letter** ([de.ritzelprimpf.toniqo.ui.theme.Tq.Type.DisplayL])
 *
 * Both tokens use `sp` units in source for consistency, but they are layout-critical
 * anchors. If they scaled to 200% at the user's maximum font-size setting, they would
 * overflow their cards and break the readout layout.
 *
 * All other text in the app uses plain `Text(...)` with `sp`-based sizes, which scale
 * normally. This composable is the sole exception. (DESIGN.md §13.1)
 *
 * ## How it works
 *
 * It overrides [LocalDensity] with a [Density] instance whose [Density.fontScale] is
 * pinned to `1f`, regardless of the system setting. Physical display density is
 * preserved, so the text still renders at the correct physical size on all screen
 * densities — it simply ignores the user's font-scale multiplier.
 *
 * @param text The text to display. Must already be in its final form (e.g., uppercase
 *   for kicker tokens, though DisplayXl/DisplayL are not kicker tokens).
 * @param style The [TextStyle] to apply. Should be [Tq.Type.DisplayXl] or
 *   [Tq.Type.DisplayL].
 * @param modifier Optional modifier.
 */
@Composable
fun NonScalingText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val baseDensity = LocalDensity.current
    val pinnedDensity = Density(density = baseDensity.density, fontScale = 1f)
    CompositionLocalProvider(LocalDensity provides pinnedDensity) {
        Text(text = text, style = style, modifier = modifier)
    }
}
