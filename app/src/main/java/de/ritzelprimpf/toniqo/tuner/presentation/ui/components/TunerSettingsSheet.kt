package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.components.SegmentedControl
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * Settings sheet for the tuner — reference pitch and auto-advance preferences.
 *
 * Specification (DESIGN.md §8.1 "Settings sheet"):
 * - `ModalBottomSheet`, approximately 280dp tall.
 * - Reference pitch row: label left, current value right (`A4 = 440/432 Hz`),
 *   segmented control [`440` | `432`] below.
 * - Auto-advance row: label + `Switch`, description below.
 * - Both controls write through to the ViewModel immediately.
 *
 * @param referencePitchHz Current reference pitch (440.0 or 432.0).
 * @param autoAdvanceEnabled Current auto-advance state.
 * @param onReferencePitchChanged Called with the new reference pitch (440.0 or 432.0).
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
                Text(
                    text = stringResource(R.string.tuner_settings_ref_pitch_current, referencePitchHz.toInt()),
                    style = Tq.Type.Body,
                    color = Tq.Color.FgSecondary,
                )
            }
            Spacer(Modifier.height(Tq.Sp.s2))
            SegmentedControl(
                options = listOf(
                    stringResource(R.string.tuner_settings_440),
                    stringResource(R.string.tuner_settings_432),
                ),
                selectedIndex = if (referencePitchHz == 440.0) 0 else 1,
                onSelect = { index ->
                    onReferencePitchChanged(if (index == 0) 440.0 else 432.0)
                },
                modifier = Modifier.fillMaxWidth(),
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
