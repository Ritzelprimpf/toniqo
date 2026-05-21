package de.ritzelprimpf.toniqo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * Two- or three-segment segmented control per DESIGN.md §6.8.
 *
 * Layout: a pill-shaped track at `bg.elev2` with 3dp inset padding. Each segment fills an
 * equal share of the width. The active segment gets `bg.elev3` fill and a `line` 1dp border;
 * inactive segments are transparent with `fg.tertiary` text.
 *
 * The 3dp track inset is a §6.8-specified value; `Tq.Sp` has no 3dp step.
 *
 * @param options Display labels for each segment. Callers must pass uppercase strings —
 *   [Tq.Type.KickerS] has no automatic text-transform.
 * @param selectedIndex Zero-based index of the currently active segment.
 * @param onSelect Called with the tapped segment's index.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackShape = RoundedCornerShape(Tq.Radius.Pill)
    val segmentShape = RoundedCornerShape(Tq.Radius.Pill)

    Row(
        modifier = modifier
            .clip(trackShape)
            .background(color = Tq.Color.BgElev2, shape = trackShape)
            .padding(3.dp),  // §6.8 track padding (no Tq.Sp step at 3dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(segmentShape)
                    .then(
                        if (isSelected) Modifier.background(Tq.Color.BgElev3, segmentShape)
                        else Modifier
                    )
                    .then(
                        if (isSelected) Modifier.border(1.dp, Tq.Color.Line, segmentShape)
                        else Modifier
                    )
                    .clickable(onClick = { onSelect(index) })
                    .padding(vertical = Tq.Sp.s2),
            ) {
                Text(
                    text = label,
                    style = Tq.Type.KickerS,
                    color = if (isSelected) Tq.Color.FgPrimary else Tq.Color.FgTertiary,
                )
            }
        }
    }
}
