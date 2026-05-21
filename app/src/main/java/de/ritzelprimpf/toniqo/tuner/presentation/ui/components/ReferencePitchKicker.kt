package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * Top header area of the Tuner screen.
 *
 * Layout (per DESIGN.md §8.1):
 * ```
 * ●  TUNER · A4 = 440 HZ                                    ☼
 * Drop D
 * ```
 * - A 6dp mint filled circle (acting as the MIC-active indicator shared with the chip row).
 * - The kicker text "TUNER · A4 = 440 HZ" (or 432 HZ) in [Tq.Type.Kicker].
 * - A [Tq.Sp.s10] (36dp) icon-round settings button on the far right.
 * - The preset display name below the kicker in [Tq.Type.H1].
 *
 * @param referencePitchHz The current reference pitch (440.0 or 432.0). Used to build the kicker label.
 * @param presetDisplayName The display name of the selected preset (e.g. "Drop D"). Shown below the kicker.
 * @param onSettingsClick Called when the settings icon button is tapped.
 */
@Composable
fun ReferencePitchKicker(
    referencePitchHz: Double,
    presetDisplayName: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pitchLabel = if (referencePitchHz == 432.0) {
        stringResource(R.string.tuner_kicker_432)
    } else {
        stringResource(R.string.tuner_kicker_440)
    }

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
        // Settings icon button (uses 'settings' glyph per DECISIONS.md 2026-05-20)
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(Tq.Sp.s10),
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.tuner_cd_settings),
                tint = Tq.Color.FgSecondary,
                modifier = Modifier.size(Tq.Sp.s5),
            )
        }
    }

    Text(
        text = presetDisplayName,
        style = Tq.Type.H1,
        color = Tq.Color.FgPrimary,
    )
}
