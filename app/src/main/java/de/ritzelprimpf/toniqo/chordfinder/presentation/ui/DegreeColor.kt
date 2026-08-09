package de.ritzelprimpf.toniqo.chordfinder.presentation.ui

import androidx.compose.ui.graphics.Color
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.ui.theme.TqPalette

/**
 * Maps a [ChordQuality] to its semantic signal colour per DESIGN.md §2.4.
 *
 * Deliberately a plain (non-`@Composable`) function, so it stays covered by fast JVM unit tests
 * instead of requiring a Compose UI test host. [palette] is the theme-reactive value the caller
 * reads via `Tq.Palette` in composable scope and passes in explicitly.
 */
object DegreeColor {
    fun of(quality: ChordQuality, palette: TqPalette): Color = when (quality) {
        ChordQuality.MAJOR      -> palette.signalMint
        ChordQuality.MINOR      -> palette.signalCyan
        ChordQuality.DIMINISHED -> palette.signalAmber
        ChordQuality.AUGMENTED  -> palette.signalViolet
        // Unreachable: this function is only ever called with DegreeChord.triadQuality, which
        // ChordQualityResolver.triad() never resolves to POWER. No DESIGN.md signal colour is
        // assigned to power chords (they aren't part of the diatonic degree-row UI this maps
        // for); reusing MAJOR's colour here is an arbitrary but harmless placeholder.
        ChordQuality.POWER      -> palette.signalMint
    }
}
