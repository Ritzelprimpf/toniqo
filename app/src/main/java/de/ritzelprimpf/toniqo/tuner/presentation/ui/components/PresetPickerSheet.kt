package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerCategory
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset
import de.ritzelprimpf.toniqo.ui.components.SegmentedControl
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * Bottom sheet for selecting a tuning preset.
 *
 * Contents (DESIGN.md §8.1 + Phase5_4-PLAN.md "Preset picker sheet"):
 * - Sheet background `bg.elev1`, top radius `r.xl`.
 * - Header row: `TUNER SELECT` kicker title + close button.
 * - Segmented control switching between available string-count groups (6 / 7 / 8).
 * - Scrollable list grouped by [TunerCategory]: STANDARD → OPEN → DROPPED.
 * - Each row: preset display name left, note summary right, mint dot on the selected preset.
 *
 * @param grouped Presets keyed first by string count, then by category.
 * @param selectedPresetId The id of the currently selected preset (drives the mint dot).
 * @param onDismiss Called when the sheet should close without a selection.
 * @param onSelect Called with the tapped preset's id; caller is responsible for closing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetPickerSheet(
    grouped: Map<Int, Map<TunerCategory, List<TunerPreset>>>,
    selectedPresetId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortedCounts = grouped.keys.sorted()
    val initialCount = grouped.entries
        .firstOrNull { (_, categories) -> categories.values.flatten().any { it.id == selectedPresetId } }
        ?.key ?: sortedCounts.firstOrNull() ?: 6
    var selectedCount by rememberSaveable { mutableIntStateOf(initialCount) }

    val categoryOrder = listOf(TunerCategory.STANDARD, TunerCategory.OPEN, TunerCategory.DROPPED)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Tq.Color.BgElev1,
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Tq.Sp.s5, end = Tq.Sp.s2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tuner_picker_title),
                style = Tq.Type.Kicker,
                color = Tq.Color.FgTertiary,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.tuner_cd_close_sheet),
                    tint = Tq.Color.FgSecondary,
                )
            }
        }

        // Segmented control — only rendered when there are multiple string-count groups
        if (sortedCounts.size > 1) {
            SegmentedControl(
                options = sortedCounts.map { count ->
                    stringResource(R.string.tuner_string_count_label, count)
                },
                selectedIndex = sortedCounts.indexOf(selectedCount).coerceAtLeast(0),
                onSelect = { index -> sortedCounts.getOrNull(index)?.let { selectedCount = it } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Tq.Sp.s5, vertical = Tq.Sp.s2),
            )
        }

        // Preset list, grouped by category
        val groupsForCount = grouped[selectedCount] ?: emptyMap()
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = Tq.Sp.s5),
        ) {
            categoryOrder.forEach { category ->
                val presets = groupsForCount[category]
                if (!presets.isNullOrEmpty()) {
                    item(key = "${category.name}_header") {
                        Text(
                            text = categoryHeaderLabel(category),
                            style = Tq.Type.Kicker,
                            color = Tq.Color.FgTertiary,
                            modifier = Modifier.padding(
                                start = Tq.Sp.s5,
                                end = Tq.Sp.s5,
                                top = Tq.Sp.s3,
                                bottom = Tq.Sp.s1,
                            ),
                        )
                    }
                    items(presets, key = { it.id }) { preset ->
                        PresetRow(
                            preset = preset,
                            isSelected = preset.id == selectedPresetId,
                            onClick = { onSelect(preset.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun categoryHeaderLabel(category: TunerCategory): String = when (category) {
    TunerCategory.STANDARD -> stringResource(R.string.tuner_category_standard)
    TunerCategory.OPEN -> stringResource(R.string.tuner_category_open)
    TunerCategory.DROPPED -> stringResource(R.string.tuner_category_dropped)
}

@Composable
private fun PresetRow(
    preset: TunerPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Tq.Sp.s5, vertical = Tq.Sp.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = preset.displayName,
            style = Tq.Type.Body,
            color = Tq.Color.FgPrimary,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Tq.Sp.s2))
        Text(
            text = buildNoteSummary(preset.notes),
            style = Tq.Type.KickerS,
            color = Tq.Color.FgTertiary,
        )
        if (isSelected) {
            Spacer(Modifier.width(Tq.Sp.s2))
            Box(
                modifier = Modifier
                    .size(6.dp)  // selected-preset indicator dot matching MIC LIVE dot size
                    .background(color = Tq.Color.SignalMint, shape = CircleShape),
            )
        }
    }
}

private fun buildNoteSummary(notes: List<Note>): String =
    if (notes.size <= 6) notes.joinToString(" ") { it.displayName() }
    else "${notes.first().displayName()} … ${notes.last().displayName()}"
