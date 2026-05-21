package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerCategory
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerMode
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * Compound chip that provides two tap areas (per DESIGN.md §8.1):
 *
 * 1. **Label area** (left) — shows the string count and category (e.g. "6-STRING · DROP").
 *    Tapping opens the preset picker sheet via [onLabelClick].
 * 2. **Chevron area** (right) — opens the mode popover via [onChevronClick].
 *    The [DropdownMenu] offers "Preset" and "Chromatic" items with a check on the current mode.
 *
 * The visible chip is 26dp tall per §6.2; tap targets extend to ≥ 44×44dp via padding.
 *
 * @param preset The currently selected preset (used to derive the label text).
 * @param mode Current tuner mode (used to show the check in the popover).
 * @param expanded Whether the mode popover is currently visible.
 * @param onLabelClick Called when the label area is tapped.
 * @param onChevronClick Called when the chevron area is tapped.
 * @param onDismissMenu Called to dismiss the popover without action.
 * @param onExitChromaticMode Called when the user taps "Preset" while in chromatic mode.
 * @param onSelectChromaticMode Called when the user taps "Chromatic".
 */
@Composable
fun PresetChip(
    preset: TunerPreset?,
    mode: TunerMode,
    expanded: Boolean,
    onLabelClick: () -> Unit,
    onChevronClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onExitChromaticMode: () -> Unit,
    onSelectChromaticMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Tq.Radius.Pill)
    val labelText = buildChipLabel(preset)

    Surface(
        modifier = modifier
            .height(26.dp)
            .border(width = 1.dp, color = Tq.Color.LineFaint, shape = shape),
        color = Tq.Color.BgElev2,
        shape = shape,
        tonalElevation = 0.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Label area — tap opens the preset picker
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Tq.Sp.s3)
                    .clickable(
                        onClick = onLabelClick,
                        role = Role.Button,
                    )
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labelText,
                    style = Tq.Type.KickerS,
                    color = Tq.Color.FgSecondary,
                )
            }

            // Chevron area — tap opens the mode popover
            Box(
                modifier = Modifier
                    .size(Tq.Sp.s12)   // 44dp+ invisible tap target around the 14dp chevron
                    .clickable(onClick = onChevronClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.tuner_cd_mode_menu),
                    tint = Tq.Color.FgTertiary,
                    modifier = Modifier.size(14.dp),
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = onDismissMenu,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tuner_mode_preset), style = Tq.Type.Body) },
                        onClick = {
                            if (mode == TunerMode.CHROMATIC) onExitChromaticMode()
                            else onDismissMenu()
                        },
                        leadingIcon = {
                            if (mode == TunerMode.PRESET) {
                                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(Tq.Sp.s4))
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tuner_mode_chromatic), style = Tq.Type.Body) },
                        onClick = {
                            if (mode == TunerMode.PRESET) onSelectChromaticMode()
                            else onDismissMenu()
                        },
                        leadingIcon = {
                            if (mode == TunerMode.CHROMATIC) {
                                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(Tq.Sp.s4))
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun buildChipLabel(preset: TunerPreset?): String {
    if (preset == null) return "— · —"
    val stringLabel = "${preset.stringCount}-STRING"
    val categoryLabel = when (preset.category) {
        TunerCategory.DROPPED -> "DROP"
        TunerCategory.STANDARD -> "STANDARD"
        TunerCategory.OPEN -> "OPEN"
    }
    return "$stringLabel · $categoryLabel"
}
