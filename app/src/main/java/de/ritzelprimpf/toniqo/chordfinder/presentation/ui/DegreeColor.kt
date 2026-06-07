package de.ritzelprimpf.toniqo.chordfinder.presentation.ui

import androidx.compose.ui.graphics.Color
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.ui.theme.Tq

/** Maps a [ChordQuality] to its semantic signal colour per DESIGN.md §2.4. */
object DegreeColor {
    fun of(quality: ChordQuality): Color = when (quality) {
        ChordQuality.MAJOR      -> Tq.Color.SignalMint
        ChordQuality.MINOR      -> Tq.Color.SignalCyan
        ChordQuality.DIMINISHED -> Tq.Color.SignalAmber
        ChordQuality.AUGMENTED  -> Tq.Color.SignalViolet
    }
}
