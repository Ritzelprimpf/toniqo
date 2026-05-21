package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.ui.components.NonScalingText
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * Large detected-note hero used in the readout well.
 *
 * Renders the note letter (e.g. "E", "C#") at 64dp JetBrains Mono Light via [NonScalingText]
 * (§13.1 — does not scale with system font size) and the octave digit (e.g. "2") at a smaller
 * size below. When [note] is `null`, an em-dash `"—"` is displayed without a subscript.
 *
 * The [semanticColor] parameter is plumbed through but currently unused on the letter itself —
 * the mockups show a flat white letter. It is kept for forward compatibility in case the design
 * later calls for a coloured hero letter.
 *
 * @param note The detected note to display, or `null` for the idle state.
 * @param semanticColor The current semantic colour (currently unused on the letter; reserved).
 */
@Composable
fun DetectedNoteHero(
    note: Note?,
    semanticColor: Color,
    modifier: Modifier = Modifier,
) {
    if (note == null) {
        NonScalingText(
            text = "—",
            style = Tq.Type.DisplayL,
            modifier = modifier,
        )
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.Bottom,
        ) {
            NonScalingText(
                text = note.name.sharpName,
                style = Tq.Type.DisplayL.copy(color = Tq.Color.FgPrimary),
            )
            Text(
                text = note.octave.toString(),
                style = Tq.Type.NumericM.copy(color = Tq.Color.FgTertiary),
                modifier = Modifier.padding(bottom = Tq.Sp.s2),
            )
        }
    }
}
