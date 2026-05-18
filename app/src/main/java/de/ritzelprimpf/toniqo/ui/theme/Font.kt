package de.ritzelprimpf.toniqo.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import de.ritzelprimpf.toniqo.R

/**
 * Space Grotesk font family — UI chrome, headers, labels, buttons.
 *
 * Weights: Regular (400), Medium (500), SemiBold (600), Bold (700).
 *
 * Font files the developer must place in `app/src/main/res/font/` before building:
 *   - `space_grotesk_regular.ttf`
 *   - `space_grotesk_medium.ttf`
 *   - `space_grotesk_semibold.ttf`
 *   - `space_grotesk_bold.ttf`
 *
 * See the Phase 3 handoff summary for download links.
 */
val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

/**
 * JetBrains Mono font family — numerals, frequencies, scale degrees, kickers,
 * and any monospace content. All mono [Tq.Type] tokens apply font features
 * `"tnum"` (tabular numbers) and `"ss01"` (alternate letterforms).
 *
 * Weights: Light (300), Regular (400), Medium (500).
 *
 * Font files the developer must place in `app/src/main/res/font/` before building:
 *   - `jetbrains_mono_light.ttf`
 *   - `jetbrains_mono_regular.ttf`
 *   - `jetbrains_mono_medium.ttf`
 *
 * See the Phase 3 handoff summary for download links.
 */
val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_light, FontWeight.Light),
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)
