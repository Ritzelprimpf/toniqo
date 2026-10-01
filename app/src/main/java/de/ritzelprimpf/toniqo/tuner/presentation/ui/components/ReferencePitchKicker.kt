package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.theme.Tq
import kotlin.math.roundToInt

/**
 * The Tuner screen's kicker-line content, for use as [de.ritzelprimpf.toniqo.ui.components.ScreenHeader]'s
 * `kicker` slot.
 *
 * Layout (per DESIGN.md §8.1):
 * ```
 * ●  TUNER · A4 = 440 HZ
 * ```
 * - A 6dp mint filled circle (acting as the MIC-active indicator shared with the chip row).
 * - The kicker text "TUNER · A4 = <value> HZ" in [Tq.Type.Kicker], for any whole-Hz
 *   [referencePitchHz] (previously hardcoded to literal "440 HZ"/"432 HZ" strings — generalized
 *   once the reference pitch became a 430–450Hz slider, see `docs/DECISIONS.md`, 2026-10-02
 *   "tuner reference pitch becomes a 430-450Hz slider" entry).
 *
 * The settings button and the preset-name title live outside this composable, as
 * `ScreenHeader`'s `trailingAction` and `title` respectively — this renders only the kicker line
 * itself.
 *
 * @param referencePitchHz The current reference pitch in Hz. Used to build the kicker label.
 */
@Composable
fun ReferencePitchKicker(
    referencePitchHz: Double,
    modifier: Modifier = Modifier,
) {
    val pitchLabel = stringResource(R.string.tuner_kicker_format, referencePitchHz.roundToInt())

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Mint dot — MIC-active indicator
        Box(
            modifier = Modifier
                .size(6.dp)  // 6dp per plan spec; no Tq.Sp step for this exact size
                .background(color = Tq.Color.SignalMint, shape = CircleShape),
        )
        Spacer(modifier = Modifier.width(Tq.Sp.s2))
        Text(
            text = pitchLabel,
            style = Tq.Type.Kicker,
            color = Tq.Color.FgSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}
