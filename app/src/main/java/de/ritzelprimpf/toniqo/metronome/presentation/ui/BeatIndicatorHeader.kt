package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.ui.theme.Tq
import java.util.Locale

/**
 * The two-part header row rendered above the beat indicator segments.
 *
 * Left side: `BEAT · X / N` where X is the 1-indexed current main beat and N is the
 * time-signature numerator. Right side: the beat unit name derived from the denominator (e.g.
 * `QUARTER NOTES` for /4, `EIGHTH NOTES` for /8, `16TH NOTES` for /16 — see [beatUnitLabelResId]
 * for the full set now that custom signatures allow any of [MetronomeConfig.SUPPORTED_DENOMINATORS]).
 *
 * @param currentBeat 0-indexed beat index from [MetronomeUiState.currentBeat].
 * @param numerator Time-signature numerator (beats per measure).
 * @param denominator Time-signature denominator (4 = quarter, 8 = eighth, etc.).
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

/**
 * Maps a time-signature denominator to its beat-unit label.
 *
 * Covers every value in [MetronomeConfig.SUPPORTED_DENOMINATORS] (1, 2, 4, 8, 16, 32) — now that
 * custom signatures allow any of them, not just 4 and 8. The `else` branch is unreachable for any
 * config that has passed [MetronomeConfig.isSupportedTimeSignature] (every config that reaches
 * this UI), but returns a safe default instead of crashing rather than relying on that invariant
 * holding across every future caller.
 */
private fun beatUnitLabelResId(denominator: Int): Int = when (denominator) {
    1 -> R.string.metronome_beat_unit_whole_notes
    2 -> R.string.metronome_beat_unit_half_notes
    4 -> R.string.metronome_beat_unit_quarter_notes
    8 -> R.string.metronome_beat_unit_eighth_notes
    16 -> R.string.metronome_beat_unit_sixteenth_notes
    32 -> R.string.metronome_beat_unit_thirty_second_notes
    else -> R.string.metronome_beat_unit_quarter_notes
}
