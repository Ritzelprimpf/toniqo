package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerMode
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset

/**
 * Row of [StringPill]s — one per string in the selected preset.
 *
 * Each pill flex-expands equally to fill the row width. The gap between pills is 6dp — this
 * is a §8.1-specific override of the §6.3 inter-element gap (§6.3 says 8dp; §8.1 says 6dp).
 * The literal `6.dp` is used here; `Tq.Sp` has no 6dp step. See DECISIONS.md for the note.
 *
 * @param preset The selected preset; `null` produces no pills.
 * @param currentStringIndex The zero-based index of the currently targeted string.
 * @param tunedStringIndices Strings brought in tune in the current session.
 * @param activeSemanticColor Signal colour applied to the active pill's border.
 * @param mode Current tuner mode. In [TunerMode.CHROMATIC] no pill is highlighted as "active".
 * @param onStringTap Called with the string's zero-based index when the user taps a pill.
 */
@Composable
fun StringSelectorRow(
    preset: TunerPreset?,
    currentStringIndex: Int,
    tunedStringIndices: Set<Int>,
    activeSemanticColor: Color,
    mode: TunerMode,
    onStringTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val notes = preset?.notes ?: return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),  // §8.1: 6dp gap (no Tq.Sp step)
    ) {
        notes.forEachIndexed { index, note ->
            StringPill(
                note = note,
                stringIndex = index,
                isActive = mode == TunerMode.PRESET && index == currentStringIndex,
                isTuned = index in tunedStringIndices,
                semanticColor = activeSemanticColor,
                onClick = { onStringTap(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
