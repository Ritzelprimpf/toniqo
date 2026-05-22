package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.components.PulsingDot
import de.ritzelprimpf.toniqo.ui.theme.Tq
import java.util.Locale

/**
 * Page status kicker shown above the screen title on the Metronome screen.
 *
 * Renders `METRONOME · RUNNING` with a leading mint pulsing dot when [isPlaying] is true,
 * or `METRONOME · STOPPED` without a dot otherwise. The string is uppercased at the call
 * site because [Tq.Type.MonoMicro] has no textAllCaps equivalent in Compose.
 */
@Composable
internal fun MetronomeStatusKicker(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (isPlaying) {
            PulsingDot(color = Tq.Color.SignalMint)
            Spacer(Modifier.width(Tq.Sp.s2))
        }
        Text(
            text = stringResource(
                if (isPlaying) R.string.metronome_status_running
                else R.string.metronome_status_stopped,
            ).uppercase(Locale.ROOT),
            style = Tq.Type.MonoMicro,
            color = Tq.Color.FgTertiary,
        )
    }
}
