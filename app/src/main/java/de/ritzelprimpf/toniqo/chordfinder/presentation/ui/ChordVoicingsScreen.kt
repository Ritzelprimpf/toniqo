package de.ritzelprimpf.toniqo.chordfinder.presentation.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.chordfinder.domain.model.Barre
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordToneRole
import de.ritzelprimpf.toniqo.chordfinder.domain.model.FretMark
import de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing
import de.ritzelprimpf.toniqo.chordfinder.domain.model.VoicingCategory
import de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel.ChordVoicingsUiState
import de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel.ChordVoicingsViewModel
import de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel.VoicingTier
import de.ritzelprimpf.toniqo.ui.components.FretboardDiagram
import de.ritzelprimpf.toniqo.ui.components.ScreenHeader
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme
import java.util.Locale

/**
 * Hilt-wired entry point for the Chord Voicings screen.
 *
 * @param onBack Called when the user presses the back arrow.
 */
@Composable
fun ChordVoicingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChordVoicingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChordVoicingsContent(
        state    = state,
        onBack   = onBack,
        modifier = modifier,
    )
}

/**
 * Stateless Chord Voicings screen content.
 *
 * Shows a [ScreenHeader] (kicker, back arrow inline with the chord-name H1 title), then a
 * note-pill header, optional tier annotations, and a 2-column grid of [VoicingCard]s.
 *
 * When [ChordVoicingsUiState.tier] is [VoicingTier.UNIFORM_OFFSET], a one-line shift notice
 * is shown above the grid. When it is [VoicingTier.UNSUPPORTED], a prominent notice is shown
 * explaining that voicings are displayed for standard tuning.
 *
 * @param state Current UI state from [ChordVoicingsViewModel].
 * @param onBack Invoked when the back icon is pressed.
 */
@Composable
fun ChordVoicingsContent(
    state: ChordVoicingsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Tq.Color.BgBase),
    ) {
        ScreenHeader(
            title = state.chordName,
            kicker = {
                Text(
                    text  = stringResource(R.string.cf_voicings_kicker),
                    style = Tq.Type.Kicker,
                    color = Tq.Color.FgTertiary,
                )
            },
            onBack = onBack,
            modifier = Modifier.padding(
                start  = Tq.Sp.s3,
                end    = Tq.Sp.s5,
                top    = Tq.Sp.s2,
                bottom = Tq.Sp.s1,
            ),
        )

        // ── Content: loading spinner or voicings grid ────────────────────────────
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color    = Tq.Color.SignalMint,
                    trackColor = Tq.Color.BgElev2,
                )
            }
        } else {
            LazyVerticalGrid(
                columns             = GridCells.Fixed(2),
                contentPadding      = PaddingValues(
                    start  = Tq.Sp.s5,
                    end    = Tq.Sp.s5,
                    top    = Tq.Sp.s2,
                    bottom = Tq.Sp.s10,
                ),
                horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
                verticalArrangement   = Arrangement.spacedBy(Tq.Sp.s3),
            ) {
                // Full-width header section
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    VoicingsHeaderSection(state = state)
                }

                items(state.voicings) { voicing ->
                    VoicingCard(voicing = voicing)
                }

                if (state.voicings.isEmpty()) {
                    item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Tq.Sp.s8),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text  = "NO VOICINGS FOUND",
                                style = Tq.Type.KickerS,
                                color = Tq.Color.FgQuaternary,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Header section ────────────────────────────────────────────────────────────

@Composable
private fun VoicingsHeaderSection(
    state: ChordVoicingsUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Note pills
        Row(horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s1)) {
            state.noteNames.forEachIndexed { i, name ->
                NoteChip(name = name, isRoot = i == 0)
            }
        }

        Spacer(Modifier.height(Tq.Sp.s3))

        // Shapes count + ROOT legend + tuning indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text  = stringResource(R.string.cf_shapes_count, state.voicings.size),
                style = Tq.Type.KickerS,
                color = Tq.Color.FgTertiary,
            )
            Spacer(Modifier.width(Tq.Sp.s3))
            // Mint dot as ROOT legend indicator
            Box(
                modifier = Modifier
                    .size(Tq.Sp.s2)
                    .clip(CircleShape)
                    .background(Tq.Color.SignalMint),
            )
            Spacer(Modifier.width(Tq.Sp.s1))
            Text(
                text  = stringResource(R.string.cf_root_legend),
                style = Tq.Type.KickerS,
                color = Tq.Color.FgTertiary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text  = stringResource(
                    R.string.cf_tuning_indicator,
                    state.tuningLabel.uppercase(Locale.ROOT),
                ),
                style = Tq.Type.KickerS,
                color = Tq.Color.FgTertiary,
            )
        }

        // UNIFORM_OFFSET tier notice
        val offset = state.offsetSemitones
        if (state.tier == VoicingTier.UNIFORM_OFFSET && offset != null) {
            Spacer(Modifier.height(Tq.Sp.s2))
            val sign = if (offset >= 0) "+" else ""
            Text(
                text  = stringResource(R.string.cf_offset_note, "$sign$offset st"),
                style = Tq.Type.Caption,
                color = Tq.Color.FgTertiary,
            )
        }

        // UNSUPPORTED tier notice
        if (state.tier == VoicingTier.UNSUPPORTED) {
            Spacer(Modifier.height(Tq.Sp.s3))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Tq.Radius.Md))
                    .background(Tq.Color.BgElev1)
                    .padding(Tq.Sp.s3),
            ) {
                Text(
                    text  = stringResource(R.string.cf_unsupported_tuning_notice),
                    style = Tq.Type.Caption,
                    color = Tq.Color.FgSecondary,
                )
            }
        }

        Spacer(Modifier.height(Tq.Sp.s4))
    }
}

// ── Voicing card ──────────────────────────────────────────────────────────────

/**
 * Single voicing card: fretboard diagram + fret-range label + category chip.
 *
 * The [Voicing] is mapped to a [FretboardRenderModel] internally; the Canvas composable
 * never receives the domain [Voicing].
 */
@Composable
private fun VoicingCard(
    voicing: Voicing,
    modifier: Modifier = Modifier,
) {
    val renderModel = voicing.toRenderModel()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Tq.Radius.Lg))
            .background(Tq.Color.BgElev1)
            .padding(Tq.Sp.s3),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FretboardDiagram(model = renderModel)

        Spacer(Modifier.height(Tq.Sp.s2))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FretRangeLabel(voicing = voicing)
            CategoryChip(category = voicing.category)
        }
    }
}

// ── Support composables ───────────────────────────────────────────────────────

/**
 * Small pill showing a chord tone name. Root note uses mint fill.
 */
@Composable
private fun NoteChip(
    name: String,
    isRoot: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Tq.Radius.Xs))
            .background(if (isRoot) Tq.Color.SignalMint else Tq.Color.BgElev2)
            .padding(horizontal = Tq.Sp.s2, vertical = Tq.Sp.s1),
    ) {
        Text(
            text  = name,
            style = Tq.Type.Caption,
            color = if (isRoot) Tq.Color.BgBase else Tq.Color.FgSecondary,
        )
    }
}

/**
 * Fret range label shown below the fretboard diagram, e.g. "FR 1–3" or "FR 5".
 */
@Composable
private fun FretRangeLabel(
    voicing: Voicing,
    modifier: Modifier = Modifier,
) {
    val range = voicing.fretRange
    val text = when {
        range.first == 0           -> ""
        range.first == range.last  -> stringResource(R.string.cf_fret_single, range.first)
        else                       -> stringResource(R.string.cf_fret_range, range.first, range.last)
    }
    if (text.isNotEmpty()) {
        Text(
            text     = text,
            style    = Tq.Type.MonoMicro,
            color    = Tq.Color.FgTertiary,
            modifier = modifier,
        )
    } else {
        Spacer(modifier)
    }
}

/** Coloured badge showing voicing technique: OPEN (mint), BARRE (amber), SHAPE (tertiary). */
@Composable
private fun CategoryChip(
    category: VoicingCategory,
    modifier: Modifier = Modifier,
) {
    val (labelRes, chipColor) = when (category) {
        VoicingCategory.OPEN  -> R.string.cf_category_open  to Tq.Color.SignalMint
        VoicingCategory.BARRE -> R.string.cf_category_barre to Tq.Color.SignalAmber
        VoicingCategory.SHAPE -> R.string.cf_category_shape to Tq.Color.FgTertiary
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Tq.Radius.Xs))
            .background(chipColor.copy(alpha = 0.15f))
            .padding(horizontal = Tq.Sp.s2, vertical = Tq.Sp.s1),
    ) {
        Text(
            text  = stringResource(labelRes),
            style = Tq.Type.KickerS,
            color = chipColor,
        )
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

private val sampleVoicings = listOf(
    Voicing(
        labelKey          = 1,
        marks             = listOf(FretMark.Muted, FretMark.Fretted(3), FretMark.Fretted(2), FretMark.Open, FretMark.Fretted(1), FretMark.Open),
        fingers           = listOf(0, 3, 2, 0, 1, 0),
        barre             = null,
        rootStringIndices = setOf(1, 4),
        bassDegree        = ChordToneRole.ROOT,
    ),
    Voicing(
        labelKey          = 2,
        marks             = listOf(FretMark.Fretted(8), FretMark.Fretted(10), FretMark.Fretted(10), FretMark.Fretted(9), FretMark.Fretted(8), FretMark.Fretted(8)),
        fingers           = listOf(1, 3, 4, 2, 1, 1),
        barre             = Barre(fret = 8, fromString = 0, toString = 5),
        rootStringIndices = setOf(0, 5),
        bassDegree        = ChordToneRole.ROOT,
    ),
)

/** Preview: standard tuning, 2 voicings. */
@Preview(name = "ChordVoicings – Standard", showBackground = true, backgroundColor = 0xFF1A1F22, widthDp = 360)
@Composable
private fun PreviewStandard() {
    ToniqoTheme {
        ChordVoicingsContent(
            state = ChordVoicingsUiState(
                chordName    = "C",
                noteNames    = listOf("C", "E", "G"),
                rootNoteName = "C",
                tuningLabel  = "E Standard",
                tier         = VoicingTier.STANDARD,
                voicings     = sampleVoicings,
                isLoading    = false,
            ),
            onBack = {},
        )
    }
}

/** Preview: uniform-offset tuning (e.g. E♭ Standard). */
@Preview(name = "ChordVoicings – Uniform Offset", showBackground = true, backgroundColor = 0xFF1A1F22, widthDp = 360)
@Composable
private fun PreviewUniformOffset() {
    ToniqoTheme {
        ChordVoicingsContent(
            state = ChordVoicingsUiState(
                chordName       = "Am",
                noteNames       = listOf("A", "C", "E"),
                rootNoteName    = "A",
                tuningLabel     = "E♭ Standard",
                tier            = VoicingTier.UNIFORM_OFFSET,
                voicings        = sampleVoicings,
                offsetSemitones = -1,
                isLoading       = false,
            ),
            onBack = {},
        )
    }
}

/** Preview: unsupported tuning — fallback to standard diagrams. */
@Preview(name = "ChordVoicings – Unsupported Tuning", showBackground = true, backgroundColor = 0xFF1A1F22, widthDp = 360)
@Composable
private fun PreviewUnsupportedTuning() {
    ToniqoTheme {
        ChordVoicingsContent(
            state = ChordVoicingsUiState(
                chordName    = "Gdim",
                noteNames    = listOf("G", "B♭", "D♭"),
                rootNoteName = "G",
                tuningLabel  = "D–A–D–G–A–D",
                tier         = VoicingTier.UNSUPPORTED,
                voicings     = sampleVoicings,
                isLoading    = false,
            ),
            onBack = {},
        )
    }
}

/** Preview: loading state. */
@Preview(name = "ChordVoicings – Loading", showBackground = true, backgroundColor = 0xFF1A1F22, widthDp = 360)
@Composable
private fun PreviewLoading() {
    ToniqoTheme {
        ChordVoicingsContent(
            state  = ChordVoicingsUiState(isLoading = true),
            onBack = {},
        )
    }
}
