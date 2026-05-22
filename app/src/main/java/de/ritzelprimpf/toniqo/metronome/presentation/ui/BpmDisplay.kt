package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.metronome.domain.model.TempoDescriptor
import de.ritzelprimpf.toniqo.ui.components.NonScalingText
import de.ritzelprimpf.toniqo.ui.theme.Tq
import java.util.Locale

/**
 * Displays the current BPM at 96dp fixed size (non-scaling) and the tempo descriptor below.
 *
 * Tapping the composable opens the BPM input dialog (handled by the caller via [onClick]).
 * `NonScalingText` ensures the 96sp token renders as 96dp regardless of the user's
 * font-size accessibility setting (DESIGN.md §13.1).
 */
@Composable
internal fun BpmDisplay(
    bpm: Int,
    tempoDescriptor: TempoDescriptor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NonScalingText(
            text = bpm.toString(),
            style = Tq.Type.DisplayXl.copy(color = Tq.Color.FgPrimary),
        )
        Spacer(Modifier.height(Tq.Sp.s1))
        Text(
            text = stringResource(tempoDescriptor.labelResId).uppercase(Locale.ROOT),
            style = Tq.Type.MonoMicro,
            color = Tq.Color.FgTertiary,
        )
    }
}

/**
 * Maps a [TempoDescriptor] to its string resource ID.
 *
 * Defined as an extension here rather than on the domain enum itself to keep the
 * domain layer free of Android dependencies (R class is an Android artifact).
 */
internal val TempoDescriptor.labelResId: Int
    get() = when (this) {
        TempoDescriptor.ADAGIO   -> R.string.tempo_adagio
        TempoDescriptor.ANDANTE  -> R.string.tempo_andante
        TempoDescriptor.MODERATO -> R.string.tempo_moderato
        TempoDescriptor.ALLEGRO  -> R.string.tempo_allegro
        TempoDescriptor.PRESTO   -> R.string.tempo_presto
    }
