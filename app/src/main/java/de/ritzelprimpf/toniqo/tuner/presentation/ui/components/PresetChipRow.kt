package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerMode
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * Row containing the [PresetChip] on the left and the `MIC LIVE` indicator on the right
 * (per DESIGN.md §8.1).
 *
 * The `MIC LIVE` indicator is a 6dp mint dot followed by the kicker text. It signals that the
 * microphone is active and capturing audio even when no note is detected.
 */
@Composable
fun PresetChipRow(
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PresetChip(
            preset = preset,
            mode = mode,
            expanded = expanded,
            onLabelClick = onLabelClick,
            onChevronClick = onChevronClick,
            onDismissMenu = onDismissMenu,
            onExitChromaticMode = onExitChromaticMode,
            onSelectChromaticMode = onSelectChromaticMode,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s1),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)  // 6dp per DESIGN.md §8.1 MIC LIVE spec
                    .background(color = Tq.Color.SignalMint, shape = CircleShape),
            )
            Text(
                text = stringResource(R.string.tuner_mic_live),
                style = Tq.Type.Kicker,
                color = Tq.Color.FgSecondary,
            )
        }
    }
}
