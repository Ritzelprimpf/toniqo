package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * The dark inset card that wraps the readout area (detected-note hero, status line, needle gauge,
 * Hz readout pair).
 *
 * Specification (DESIGN.md §6.6 + §8.1):
 * - Background: `bg.inset`
 * - Border: `line.faint` 1dp hairline
 * - Radius: `r.xl` (18dp)
 * - Padding: `sp.5` (20dp) horizontal, `sp.3` (12dp) vertical
 *
 * @param content Composable slot for the well's contents, laid out in a centred [Column].
 */
@Composable
fun ReadoutWell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Tq.Color.LineFaint,
                shape = RoundedCornerShape(Tq.Radius.Xl),
            ),
        color = Tq.Color.BgInset,
        shape = RoundedCornerShape(Tq.Radius.Xl),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Tq.Sp.s5, vertical = Tq.Sp.s3),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}
