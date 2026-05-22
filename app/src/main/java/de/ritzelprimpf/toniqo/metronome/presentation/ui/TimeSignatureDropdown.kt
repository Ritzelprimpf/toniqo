package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.ui.theme.Tq
import java.util.Locale

/**
 * Pill-style 44dp dropdown for selecting the time signature from the 8 supported options.
 *
 * Shows the current value as "N/D" (e.g., "4/4"). The SIGNATURE kicker label sits above the
 * trigger. Options are sorted with /4 signatures first, then /8, both in ascending numerator order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeSignatureDropdown(
    numerator: Int,
    denominator: Int,
    onSelectionChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val sortedSignatures = remember {
        MetronomeConfig.SUPPORTED_SIGNATURES
            .sortedWith(compareBy({ it.second }, { it.first }))
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.metronome_signature_label).uppercase(Locale.ROOT),
            style = Tq.Type.MonoMicro,
            color = Tq.Color.FgTertiary,
            modifier = Modifier.padding(bottom = Tq.Sp.s1),
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            Box(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Tq.Color.BgElev2, RoundedCornerShape(Tq.Radius.Pill))
                    .border(1.dp, Tq.Color.LineFaint, RoundedCornerShape(Tq.Radius.Pill)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$numerator/$denominator",
                    style = Tq.Type.NumericM,
                    color = Tq.Color.FgPrimary,
                )
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                sortedSignatures.forEach { (n, d) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "$n/$d",
                                style = Tq.Type.NumericM,
                                color = Tq.Color.FgPrimary,
                            )
                        },
                        onClick = {
                            onSelectionChanged(n, d)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}
