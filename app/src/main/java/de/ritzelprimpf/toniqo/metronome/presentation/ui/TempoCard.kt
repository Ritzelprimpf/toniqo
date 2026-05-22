package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig.Companion.BPM_MAX
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig.Companion.BPM_MIN
import de.ritzelprimpf.toniqo.metronome.domain.model.TempoDescriptor
import de.ritzelprimpf.toniqo.ui.theme.Tq
import java.util.Locale

/**
 * Visually-grouped card containing the BPM display, tempo descriptor, BPM slider, and ±1 buttons.
 *
 * Uses `bg.inset` background and `r.xl` (18dp) radius — the readout-well treatment from
 * DESIGN.md §6.6 — matching the approved mockup.
 */
@Composable
internal fun TempoCard(
    bpm: Int,
    tempoDescriptor: TempoDescriptor,
    onBpmDisplayClick: () -> Unit,
    onBpmChanged: (Int) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Tq.Color.BgInset, RoundedCornerShape(Tq.Radius.Xl))
            .border(1.dp, Tq.Color.LineFaint, RoundedCornerShape(Tq.Radius.Xl))
            .padding(horizontal = Tq.Sp.s5, vertical = Tq.Sp.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.metronome_tempo_label).uppercase(Locale.ROOT),
            style = Tq.Type.MonoMicro,
            color = Tq.Color.FgTertiary,
        )
        Spacer(Modifier.height(Tq.Sp.s1))
        BpmDisplay(
            bpm = bpm,
            tempoDescriptor = tempoDescriptor,
            onClick = onBpmDisplayClick,
        )
        Spacer(Modifier.height(Tq.Sp.s3))
        BpmSliderRow(
            bpm = bpm,
            onBpmChanged = onBpmChanged,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
        )
    }
}

@Composable
private fun BpmSliderRow(
    bpm: Int,
    onBpmChanged: (Int) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    val decrementCd = stringResource(R.string.metronome_cd_bpm_decrement)
    val incrementCd = stringResource(R.string.metronome_cd_bpm_increment)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
    ) {
        IconButton(
            onClick = onDecrement,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = decrementCd,
                tint = Tq.Color.FgSecondary,
            )
        }
        Slider(
            value = bpm.toFloat(),
            onValueChange = { onBpmChanged(it.toInt()) },
            valueRange = BPM_MIN.toFloat()..BPM_MAX.toFloat(),
            steps = BPM_MAX - BPM_MIN - 1,  // 298 integer steps across 1–300
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Tq.Color.FgPrimary,
                activeTrackColor = Tq.Color.SignalMint,
                inactiveTrackColor = Tq.Color.LineFaint,
            ),
        )
        IconButton(
            onClick = onIncrement,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = incrementCd,
                tint = Tq.Color.FgSecondary,
            )
        }
    }
}
