package de.ritzelprimpf.toniqo.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

/**
 * Toniqo card primitive — the standard elevated surface for all card content.
 *
 * Specification (DESIGN.md §6.6):
 * - Background: `bg.elev1`
 * - Border: `line.faint` 1dp
 * - Radius: `r.lg` (16dp)
 * - Padding: `sp.4` (16dp)
 * - No drop shadow (flat design per §10)
 *
 * @param modifier Optional modifier applied to the card surface.
 * @param content Slot composable rendered inside the padded card area.
 */
@Composable
fun ToniqoCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = Tq.Color.LineFaint,
                shape = RoundedCornerShape(Tq.Radius.Lg),
            ),
        color = Tq.Color.BgElev1,
        shape = RoundedCornerShape(Tq.Radius.Lg),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(modifier = Modifier.padding(Tq.Sp.s4)) {
            content()
        }
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(name = "ToniqoCard — Dark", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun ToniqoCardPreviewDark() {
    ToniqoTheme(useDarkTheme = true) {
        ToniqoCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Card content", style = Tq.Type.Body, color = Tq.Color.FgPrimary)
        }
    }
}

@Preview(name = "ToniqoCard — Light", showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun ToniqoCardPreviewLight() {
    ToniqoTheme(useDarkTheme = false) {
        ToniqoCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Card content", style = Tq.Type.Body, color = Tq.LightColor.FgPrimary)
        }
    }
}
