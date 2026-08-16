package de.ritzelprimpf.toniqo.chordfinder.presentation.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.model.DegreeChord
import de.ritzelprimpf.toniqo.chordfinder.domain.model.SeventhQuality
import de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel.ChordFinderUiState
import de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel.ChordFinderViewModel
import de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel.ChordNavEvent
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.common.model.ScaleType
import de.ritzelprimpf.toniqo.ui.components.InfoDialog
import de.ritzelprimpf.toniqo.ui.components.ScreenHeader
import de.ritzelprimpf.toniqo.ui.components.SegmentedControl
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

// §8.4: Mode dropdown height uses 42dp — distinct from the metronome's 44dp.
// Tq.Sp has no 42dp step; this is a component-specific layout constant.
private val DROPDOWN_HEIGHT: Dp = 42.dp

// Width of the TRIADS/7THS segmented control. Tq.Sp has no step between s8 (32dp) and s10 (40dp)
// for the total; 152dp gives each segment ≈76dp which comfortably fits "TRIADS".
private val TOGGLE_CONTROL_WIDTH: Dp = 152.dp

private val DropdownShape = RoundedCornerShape(Tq.Radius.Md)

// Converts a ScaleType to its root-free mode label for the dropdown, e.g. "Major · Ionian".
// Key pattern: cf_mode_label_{scaletype_name_lowercase} — matches all 14 entries in strings.xml.
private fun ScaleType.modeLabel(context: Context): String {
    val key = "cf_mode_label_${name.lowercase()}"
    val resId = context.resources.getIdentifier(key, "string", context.packageName)
    return if (resId != 0) context.getString(resId) else name
}

// ─── Stateful entry point ─────────────────────────────────────────────────────

/**
 * Chord Finder list screen wired to a Hilt [ChordFinderViewModel].
 *
 * Navigation events are consumed here and forwarded to [onChordSelected]. The nav graph wiring
 * (Phase 8.5) will replace the no-op lambda at the [AppNavHost] call site.
 */
@Composable
fun ChordFinderScreen(
    onChordSelected: (ChordKey, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChordFinderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel.navEvents, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.navEvents.collect { event ->
                when (event) {
                    is ChordNavEvent.NavigateToVoicings ->
                        onChordSelected(event.chordKey, event.chordName)
                }
            }
        }
    }

    ChordFinderContent(
        state = uiState,
        onSetRoot = viewModel::setRoot,
        onSetScaleType = viewModel::setScaleType,
        onToggleSevenths = viewModel::toggleSevenths,
        onSelectChord = viewModel::selectChord,
        modifier = modifier,
    )
}

// ─── Stateless content ────────────────────────────────────────────────────────

/**
 * Stateless Chord Finder list screen.
 *
 * Accepts the full UI state and lambdas for every user intent so it is Hilt-free and
 * directly testable with Compose UI tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChordFinderContent(
    state: ChordFinderUiState,
    onSetRoot: (Int) -> Unit,
    onSetScaleType: (ScaleType) -> Unit,
    onToggleSevenths: () -> Unit,
    onSelectChord: (DegreeChord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showInfoDialog by remember { mutableStateOf(false) }

    val screenTitle = remember(state.spelledRoot, state.scaleType) {
        val resId = context.resources.getIdentifier(
            state.scaleType.primaryLabelKey, "string", context.packageName,
        )
        if (resId != 0) context.getString(resId, state.spelledRoot)
        else "${state.spelledRoot} ${state.scaleType.name}"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Tq.Color.BgBase)
            .padding(top = Tq.Sp.s5),
        contentPadding = PaddingValues(
            start = Tq.Sp.s5,
            top = Tq.Sp.s0,
            end = Tq.Sp.s5,
            bottom = Tq.Sp.s12,
        ),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            ScreenHeader(
                title = screenTitle,
                kicker = {
                    Text(
                        text = stringResource(R.string.cf_kicker),
                        style = Tq.Type.Kicker,
                        color = Tq.Color.FgTertiary,
                    )
                },
                trailingAction = {
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.cf_cd_info),
                            tint = Tq.Color.FgTertiary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )
        }

        // ── Root + Mode selectors ─────────────────────────────────────────────
        item {
            Spacer(Modifier.height(Tq.Sp.s4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
            ) {
                // Root dropdown — flex 1 (§8.4)
                ChordFinderDropdown(
                    kickerLabel = stringResource(R.string.cf_label_root),
                    currentLabel = state.spelledRoot,
                    modifier = Modifier.weight(1f),
                ) { dismiss ->
                    for (pc in 0..11) {
                        val name = scaleRootName(context, pc)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = name,
                                    style = Tq.Type.Body,
                                    color = Tq.Color.FgPrimary,
                                )
                            },
                            onClick = {
                                onSetRoot(pc)
                                dismiss()
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
                // Mode dropdown — flex 1.4 (§8.4)
                ChordFinderDropdown(
                    kickerLabel = stringResource(R.string.cf_label_mode),
                    currentLabel = state.scaleType.modeLabel(context),
                    modifier = Modifier.weight(1.4f),
                ) { dismiss ->
                    ScaleType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = type.modeLabel(context),
                                    style = Tq.Type.Body,
                                    color = Tq.Color.FgPrimary,
                                )
                            },
                            onClick = {
                                onSetScaleType(type)
                                dismiss()
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
        }

        // ── Count + TRIADS / 7THS toggle ──────────────────────────────────────
        item {
            Spacer(Modifier.height(Tq.Sp.s3))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Mint dot + chord count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
                ) {
                    Box(
                        modifier = Modifier
                            .size(Tq.Sp.s2)
                            .background(Tq.Color.SignalMint, CircleShape),
                    )
                    Text(
                        text = stringResource(R.string.cf_chords_count, state.chords.size),
                        style = Tq.Type.Kicker,
                        color = Tq.Color.FgSecondary,
                    )
                }
                Spacer(Modifier.weight(1f))
                // TRIADS / 7THS segmented control
                SegmentedControl(
                    options = listOf(
                        stringResource(R.string.cf_toggle_triads),
                        stringResource(R.string.cf_toggle_sevenths),
                    ),
                    selectedIndex = if (state.includeSeventhChords) 1 else 0,
                    onSelect = { index ->
                        val wantsSevenths = index == 1
                        if (wantsSevenths != state.includeSeventhChords) onToggleSevenths()
                    },
                    modifier = Modifier.width(TOGGLE_CONTROL_WIDTH),
                )
            }
            Spacer(Modifier.height(Tq.Sp.s3))
        }

        // ── Chord rows ────────────────────────────────────────────────────────
        items(state.chords, key = { it.degree }) { chord ->
            ChordDegreeRow(
                degreeChord = chord,
                onClick = { onSelectChord(chord) },
            )
            Spacer(Modifier.height(Tq.Sp.s2))
        }
    }

    if (showInfoDialog) {
        InfoDialog(
            title = stringResource(R.string.cf_info_dialog_title),
            body = stringResource(R.string.cf_info_dialog_body),
            onDismiss = { showInfoDialog = false },
        )
    }
}

// ─── Private components ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChordFinderDropdown(
    kickerLabel: String,
    currentLabel: String,
    modifier: Modifier = Modifier,
    menuContent: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = kickerLabel,
            style = Tq.Type.KickerS,
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
                    .height(DROPDOWN_HEIGHT)
                    .background(Tq.Color.BgElev2, DropdownShape)
                    .border(1.dp, Tq.Color.LineFaint, DropdownShape),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Tq.Sp.s3),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = currentLabel,
                        style = Tq.Type.Body,
                        color = Tq.Color.FgPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Tq.Color.FgTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                menuContent { expanded = false }
            }
        }
    }
}

// Returns the canonical spelling for a pitch class (0–11) from the ROOT_DISPLAY_NAMES table.
// Kept as a top-level helper so the dropdown lambda is free of direct ScaleSpeller imports.
private fun scaleRootName(context: Context, pitchClass: Int): String {
    val names = arrayOf("C", "D♭", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B")
    return names[pitchClass]
}

// ─── Previews ─────────────────────────────────────────────────────────────────

private val previewTriadChords = listOf(
    DegreeChord(1, "I",    ChordQuality.MAJOR,      null,                          "C", listOf("C", "E", "G"),       "C"),
    DegreeChord(2, "ii",   ChordQuality.MINOR,      null,                          "D", listOf("D", "F", "A"),       "Dm"),
    DegreeChord(3, "iii",  ChordQuality.MINOR,      null,                          "E", listOf("E", "G", "B"),       "Em"),
    DegreeChord(4, "IV",   ChordQuality.MAJOR,      null,                          "F", listOf("F", "A", "C"),       "F"),
    DegreeChord(5, "V",    ChordQuality.MAJOR,      null,                          "G", listOf("G", "B", "D"),       "G"),
    DegreeChord(6, "vi",   ChordQuality.MINOR,      null,                          "A", listOf("A", "C", "E"),       "Am"),
    DegreeChord(7, "vii°", ChordQuality.DIMINISHED, null,                          "B", listOf("B", "D", "F"),       "Bdim"),
)

private val previewSeventhChords = listOf(
    DegreeChord(1, "I",    ChordQuality.MAJOR,      SeventhQuality.MAJOR_SEVENTH,   "C", listOf("C", "E", "G", "B"),  "Cmaj7"),
    DegreeChord(2, "ii",   ChordQuality.MINOR,      SeventhQuality.MINOR_SEVENTH,   "D", listOf("D", "F", "A", "C"),  "Dm7"),
    DegreeChord(3, "iii",  ChordQuality.MINOR,      SeventhQuality.MINOR_SEVENTH,   "E", listOf("E", "G", "B", "D"),  "Em7"),
    DegreeChord(4, "IV",   ChordQuality.MAJOR,      SeventhQuality.MAJOR_SEVENTH,   "F", listOf("F", "A", "C", "E"),  "Fmaj7"),
    DegreeChord(5, "V",    ChordQuality.MAJOR,      SeventhQuality.DOMINANT_SEVENTH,"G", listOf("G", "B", "D", "F"),  "G7"),
    DegreeChord(6, "vi",   ChordQuality.MINOR,      SeventhQuality.MINOR_SEVENTH,   "A", listOf("A", "C", "E", "G"),  "Am7"),
    DegreeChord(7, "vii°", ChordQuality.DIMINISHED, SeventhQuality.HALF_DIMINISHED, "B", listOf("B", "D", "F", "A"),  "Bm7♭5"),
)

private val previewStateTriads = ChordFinderUiState(
    rootPitchClass = 0,
    scaleType = ScaleType.IONIAN,
    includeSeventhChords = false,
    spelledRoot = "C",
    chords = previewTriadChords,
    isInitialLoadComplete = true,
)

private val previewStateSevenths = previewStateTriads.copy(
    includeSeventhChords = true,
    chords = previewSeventhChords,
)

@Preview(name = "Chord Finder Triads — Dark", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun PreviewTriadsDark() {
    ToniqoTheme(useDarkTheme = true) {
        ChordFinderContent(
            state = previewStateTriads,
            onSetRoot = {},
            onSetScaleType = {},
            onToggleSevenths = {},
            onSelectChord = {},
        )
    }
}

@Preview(name = "Chord Finder Triads — Light", showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun PreviewTriadsLight() {
    ToniqoTheme(useDarkTheme = false) {
        ChordFinderContent(
            state = previewStateTriads,
            onSetRoot = {},
            onSetScaleType = {},
            onToggleSevenths = {},
            onSelectChord = {},
        )
    }
}

@Preview(name = "Chord Finder 7ths — Dark", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun PreviewSeventhsDark() {
    ToniqoTheme(useDarkTheme = true) {
        ChordFinderContent(
            state = previewStateSevenths,
            onSetRoot = {},
            onSetScaleType = {},
            onToggleSevenths = {},
            onSelectChord = {},
        )
    }
}

@Preview(name = "Chord Finder 7ths — Light", showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun PreviewSeventhsLight() {
    ToniqoTheme(useDarkTheme = false) {
        ChordFinderContent(
            state = previewStateSevenths,
            onSetRoot = {},
            onSetScaleType = {},
            onToggleSevenths = {},
            onSelectChord = {},
        )
    }
}
