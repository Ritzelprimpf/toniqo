package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.tuner.domain.model.TuningStatus
import de.ritzelprimpf.toniqo.tuner.presentation.mapping.toSignalColor
import de.ritzelprimpf.toniqo.ui.theme.Tq
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Displays the status word and cents value side-by-side below the detected-note hero.
 *
 * Format examples (per DESIGN.md §8.1):
 * - `FLAT  −18¢`  (U+2212 minus)
 * - `IN TUNE  +2¢`
 * - `SHARP  +15¢`
 * - `LISTENING  —`  (em-dash, no cents value)
 *
 * The status word is coloured via [TuningStatus.toSignalColor]. The cents value is
 * always in [Tq.Color.FgTertiary]. [TuningStatus.PERMISSION_DENIED] and
 * [TuningStatus.CAPTURE_FAILED] are not displayed here — the screen renders separate
 * error cards for those states.
 *
 * @param status The current tuning status.
 * @param centsOffTarget Signed cents offset from the target. `null` when no detection has been made.
 */
@Composable
fun StatusLine(
    status: TuningStatus,
    centsOffTarget: Double?,
    modifier: Modifier = Modifier,
) {
    val statusWord = when (status) {
        TuningStatus.FLAT -> stringResource(R.string.tuner_status_flat)
        TuningStatus.IN_TUNE -> stringResource(R.string.tuner_status_in_tune)
        TuningStatus.SHARP -> stringResource(R.string.tuner_status_sharp)
        TuningStatus.ALL_STRINGS_TUNED -> stringResource(R.string.tuner_status_all_tuned)
        TuningStatus.LISTENING,
        TuningStatus.IDLE,
        TuningStatus.PERMISSION_DENIED,
        TuningStatus.CAPTURE_FAILED -> stringResource(R.string.tuner_status_listening)
    }

    val centsText = if (centsOffTarget != null) {
        val rounded = centsOffTarget.roundToInt()
        val sign = if (rounded >= 0) "+" else "−"  // U+2212 proper minus
        "$sign${rounded.absoluteValue}¢"           // ¢ cent sign
    } else {
        "—"  // em-dash
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = statusWord,
            style = Tq.Type.Kicker,
            color = status.toSignalColor(),
        )
        Text(
            text = centsText,
            style = Tq.Type.Kicker,
            color = if (centsOffTarget != null) Tq.Color.FgTertiary else Tq.Color.FgQuaternary,
        )
    }
}
