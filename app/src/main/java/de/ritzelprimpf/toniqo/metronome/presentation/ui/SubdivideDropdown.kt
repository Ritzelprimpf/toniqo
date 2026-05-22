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
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import de.ritzelprimpf.toniqo.ui.theme.Tq
import java.util.Locale

/**
 * Pill-style 44dp dropdown for selecting beat subdivision.
 *
 * The kicker label reads "SUBDIVIDE" (verb form, per Phase6-Metronome-Decisions.md Item 23c).
 * Values are displayed in noun form: "None", "Eighth notes", "Sixteenth notes", "Eighth triplets".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubdivideDropdown(
    subdivision: Subdivision,
    onSelectionChanged: (Subdivision) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.metronome_subdivide_label).uppercase(Locale.ROOT),
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
                    text = stringResource(subdivision.labelResId),
                    style = Tq.Type.Body,
                    color = Tq.Color.FgPrimary,
                )
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                Subdivision.values().forEach { sub ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(sub.labelResId),
                                style = Tq.Type.Body,
                                color = Tq.Color.FgPrimary,
                            )
                        },
                        onClick = {
                            onSelectionChanged(sub)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

private val Subdivision.labelResId: Int
    get() = when (this) {
        Subdivision.NONE       -> R.string.subdivision_none
        Subdivision.EIGHTHS    -> R.string.subdivision_eighths
        Subdivision.SIXTEENTHS -> R.string.subdivision_sixteenths
        Subdivision.TRIPLETS   -> R.string.subdivision_triplets
    }
