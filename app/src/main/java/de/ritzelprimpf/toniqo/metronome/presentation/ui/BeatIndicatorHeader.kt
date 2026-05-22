package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.theme.Tq
import java.util.Locale

/**
 * The two-part header row rendered above the beat indicator segments.
 *
 * Left side: `BEAT · X / N` where X is the 1-indexed current main beat and N is the
 * time-signature numerator. Right side: the beat unit name — `QUARTER NOTES` for /4
 * signatures, `EIGHTH NOTES` for /8 signatures.
 *
 * @param currentBeat 0-indexed beat index from [MetronomeUiState.currentBeat].
 * @param numerator Time-signature numerator (beats per measure).
 * @param denominator Time-signature denominator (4 = quarter, 8 = eighth).
 */
@Composable
internal fun BeatIndicatorHeader(
    currentBeat: Int,
    numerator: Int,
    denominator: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(
                R.string.metronome_beat_header_format,
                currentBeat + 1,  // display as 1-indexed
                numerator,
            ).uppercase(Locale.ROOT),
            style = Tq.Type.MonoMicro,
            color = Tq.Color.FgTertiary,
        )
        Text(
            text = stringResource(beatUnitLabelResId(denominator)).uppercase(Locale.ROOT),
            style = Tq.Type.MonoMicro,
            color = Tq.Color.FgTertiary,
        )
    }
}

private fun beatUnitLabelResId(denominator: Int): Int = when (denominator) {
    4 -> R.string.metronome_beat_unit_quarter_notes
    8 -> R.string.metronome_beat_unit_eighth_notes
    else -> error("Unsupported time-signature denominator: $denominator")
}
