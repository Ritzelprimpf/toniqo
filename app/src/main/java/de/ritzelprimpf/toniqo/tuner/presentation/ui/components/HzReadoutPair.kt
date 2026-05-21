package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * Displays the "DETECTED / TARGET Hz" row at the bottom of the readout well.
 *
 * Each column shows a kicker label above the numeric value. When a value is null (no detection or
 * no target), an em-dash is rendered in place of the number (per DESIGN.md §8.1 hz-readout spec).
 *
 * @param detectedHz The most recent detected fundamental frequency, or `null` when absent.
 * @param targetHz The current target frequency, or `null` when no preset is selected.
 */
@Composable
fun HzReadoutPair(
    detectedHz: Double?,
    targetHz: Double?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = stringResource(R.string.tuner_label_detected),
                style = Tq.Type.Kicker,
                color = Tq.Color.FgTertiary,
            )
            Text(
                text = if (detectedHz != null) "%.2f Hz".format(detectedHz)
                       else stringResource(R.string.tuner_hz_null),
                style = Tq.Type.Body,
                color = if (detectedHz != null) Tq.Color.FgPrimary else Tq.Color.FgTertiary,
            )
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text(
                text = stringResource(R.string.tuner_label_target),
                style = Tq.Type.Kicker,
                color = Tq.Color.FgTertiary,
            )
            Text(
                text = if (targetHz != null) "%.2f Hz".format(targetHz)
                       else stringResource(R.string.tuner_hz_null),
                style = Tq.Type.Body,
                color = if (targetHz != null) Tq.Color.FgPrimary else Tq.Color.FgTertiary,
            )
        }
    }
}
