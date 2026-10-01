package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.tuner.data.TunerPreferences.Companion.REFERENCE_PITCH_HZ_DEFAULT
import de.ritzelprimpf.toniqo.tuner.data.TunerPreferences.Companion.REFERENCE_PITCH_HZ_MAX
import de.ritzelprimpf.toniqo.tuner.data.TunerPreferences.Companion.REFERENCE_PITCH_HZ_MIN
import de.ritzelprimpf.toniqo.ui.theme.Tq
import kotlin.math.roundToInt

/**
 * Settings sheet for the tuner — reference pitch and auto-advance preferences.
 *
 * Specification (DESIGN.md §8.1 "Settings sheet"):
 * - `ModalBottomSheet`, approximately 280dp tall.
 * - Reference pitch row: label left, current value + "Reset" text button right (`A4 = 440 Hz`),
 *   a slider with ±1 Hz icon buttons on either side below, ranging over
 *   [REFERENCE_PITCH_HZ_MIN]..[REFERENCE_PITCH_HZ_MAX] (430–450) in whole-Hz steps — same slider
 *   + ± button pattern as the metronome's BPM control (`TempoCard.kt`). "Reset" restores
 *   [REFERENCE_PITCH_HZ_DEFAULT] (440).
 * - Auto-advance row: label + `Switch`, description below.
 * - All controls write through to the ViewModel immediately; clamping to the valid range happens
 *   in `TunerViewModel.onReferencePitchChanged`, not here — see that function's doc.
 *
 * @param referencePitchHz Current reference pitch, in [REFERENCE_PITCH_HZ_MIN]..[REFERENCE_PITCH_HZ_MAX].
 * @param autoAdvanceEnabled Current auto-advance state.
 * @param onReferencePitchChanged Called with the requested new reference pitch (not pre-clamped).
 * @param onAutoAdvanceChanged Called with the new auto-advance value.
 * @param onDismiss Called when the sheet should close.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerSettingsSheet(
    referencePitchHz: Double,
    autoAdvanceEnabled: Boolean,
    onReferencePitchChanged: (Double) -> Unit,
    onAutoAdvanceChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Tq.Color.BgElev1,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tq.Sp.s5),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.tuner_settings_title),
                    style = Tq.Type.Kicker,
                    color = Tq.Color.FgTertiary,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.offset(x = Tq.Sp.s2),  // align icon edge to padding edge
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.tuner_cd_close_sheet),
                        tint = Tq.Color.FgSecondary,
                    )
                }
            }

            Spacer(Modifier.height(Tq.Sp.s4))

            // Reference pitch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.tuner_settings_ref_pitch_label),
                    style = Tq.Type.Body,
                    color = Tq.Color.FgPrimary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.tuner_settings_ref_pitch_current, referencePitchHz.roundToInt()),
                        style = Tq.Type.Body,
                        color = Tq.Color.FgSecondary,
                    )
                    TextButton(
                        onClick = { onReferencePitchChanged(REFERENCE_PITCH_HZ_DEFAULT) },
                        contentPadding = PaddingValues(horizontal = Tq.Sp.s2),
                    ) {
                        Text(
                            text = stringResource(R.string.tuner_settings_ref_pitch_reset),
                            style = Tq.Type.Body,
                            color = Tq.Color.FgTertiary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Tq.Sp.s2))
            ReferencePitchSliderRow(
                referencePitchHz = referencePitchHz,
                onReferencePitchChanged = onReferencePitchChanged,
            )

            Spacer(Modifier.height(Tq.Sp.s4))

            // Auto-advance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.tuner_settings_auto_advance_label),
                    style = Tq.Type.Body,
                    color = Tq.Color.FgPrimary,
                )
                Switch(
                    checked = autoAdvanceEnabled,
                    onCheckedChange = onAutoAdvanceChanged,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Tq.Color.SignalMint,
                        checkedThumbColor = Tq.Color.BgBase,
                    ),
                )
            }
            Text(
                text = stringResource(R.string.tuner_settings_auto_advance_desc),
                style = Tq.Type.Body,
                color = Tq.Color.FgTertiary,
            )

            Spacer(Modifier.height(Tq.Sp.s5))
        }
    }
}

/**
 * ±1 Hz icon buttons flanking a slider over [REFERENCE_PITCH_HZ_MIN]..[REFERENCE_PITCH_HZ_MAX] —
 * same structure as the metronome's `TempoCard.BpmSliderRow`. The slider's `onValueChange` floors
 * to whole Hz (matching that function's `.toInt()` truncation) since the control only ever deals
 * in whole-Hz steps; clamping for the ± buttons happens in the ViewModel, not here.
 */
@Composable
private fun ReferencePitchSliderRow(
    referencePitchHz: Double,
    onReferencePitchChanged: (Double) -> Unit,
) {
    val decrementCd = stringResource(R.string.tuner_cd_ref_pitch_decrement)
    val incrementCd = stringResource(R.string.tuner_cd_ref_pitch_increment)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
    ) {
        IconButton(
            onClick = { onReferencePitchChanged(referencePitchHz - 1.0) },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = decrementCd,
                tint = Tq.Color.FgSecondary,
            )
        }
        Slider(
            value = referencePitchHz.toFloat(),
            onValueChange = { onReferencePitchChanged(it.toInt().toDouble()) },
            valueRange = REFERENCE_PITCH_HZ_MIN.toFloat()..REFERENCE_PITCH_HZ_MAX.toFloat(),
            steps = (REFERENCE_PITCH_HZ_MAX - REFERENCE_PITCH_HZ_MIN).toInt() - 1,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Tq.Color.FgPrimary,
                activeTrackColor = Tq.Color.SignalMint,
                inactiveTrackColor = Tq.Color.LineFaint,
            ),
        )
        IconButton(
            onClick = { onReferencePitchChanged(referencePitchHz + 1.0) },
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
