package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.MetronomeUiState
import de.ritzelprimpf.toniqo.ui.components.InfoDialog
import de.ritzelprimpf.toniqo.ui.components.ScreenHeader
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

/**
 * Stateless metronome content layout.
 *
 * Takes [MetronomeUiState] and event lambdas — no ViewModel dependency — so it can be
 * previewed and tested in isolation. [MetronomeScreen] is the ViewModel-wired entry point.
 *
 * Layout (top to bottom):
 *  1. [ScreenHeader]: status kicker (METRONOME · RUNNING / STOPPED + optional pulsing dot) above
 *     the "Metronome" title, with an info button (top-end) opening an [InfoDialog] — shared
 *     header structure with Key Finder/Chord Finder/Tuner.
 *  2. Tempo card (TEMPO kicker + BPM display + tempo descriptor + slider + ± buttons)
 *  3. Beat indicator header (BEAT · X / N + beat unit label)
 *  4. Beat indicator segments
 *  5. SIGNATURE / SUBDIVIDE dropdowns, side by side
 *  6. Bottom row: TAP circle + Start/Stop pill
 */
@Composable
internal fun MetronomeContent(
    state: MetronomeUiState,
    onPlayToggled: () -> Unit,
    onBpmChanged: (Int) -> Unit,
    onBpmIncrement: () -> Unit,
    onBpmDecrement: () -> Unit,
    onTimeSignatureChanged: (Int, Int) -> Unit,
    onSubdivisionChanged: (Subdivision) -> Unit,
    onTapTempo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showBpmDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showBpmDialog) {
        BpmInputDialog(
            initialBpm = state.config.bpm,
            onConfirm = { bpm ->
                onBpmChanged(bpm)
                showBpmDialog = false
            },
            onDismiss = { showBpmDialog = false },
        )
    }

    if (showInfoDialog) {
        InfoDialog(
            title = stringResource(R.string.metronome_info_dialog_title),
            body = stringResource(R.string.metronome_info_dialog_body),
            onDismiss = { showInfoDialog = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Tq.Color.BgBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Tq.Sp.s5)
            .padding(top = Tq.Sp.s5, bottom = Tq.Sp.s6),
    ) {
        ScreenHeader(
            title = stringResource(R.string.metronome_title),
            kicker = { MetronomeStatusKicker(isPlaying = state.isPlaying) },
            trailingAction = {
                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.metronome_cd_info),
                        tint = Tq.Color.FgTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        )
        Spacer(Modifier.height(Tq.Sp.s5))

        TempoCard(
            bpm = state.config.bpm,
            tempoDescriptor = state.tempoDescriptor,
            onBpmDisplayClick = { showBpmDialog = true },
            onBpmChanged = onBpmChanged,
            onIncrement = onBpmIncrement,
            onDecrement = onBpmDecrement,
        )
        Spacer(Modifier.height(Tq.Sp.s5))

        BeatIndicatorHeader(
            currentBeat = state.currentBeat,
            numerator = state.config.timeSignatureNumerator,
            denominator = state.config.timeSignatureDenominator,
        )
        Spacer(Modifier.height(Tq.Sp.s2))

        BeatIndicator(
            numerator = state.config.timeSignatureNumerator,
            currentBeat = state.currentBeat,
            isPlaying = state.isPlaying,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Tq.Sp.s5))

        Row(modifier = Modifier.fillMaxWidth()) {
            TimeSignatureDropdown(
                numerator = state.config.timeSignatureNumerator,
                denominator = state.config.timeSignatureDenominator,
                onSelectionChanged = onTimeSignatureChanged,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Tq.Sp.s3))
            SubdivideDropdown(
                subdivision = state.config.subdivision,
                onSelectionChanged = onSubdivisionChanged,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(Tq.Sp.s6))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TapTempoButton(onClick = onTapTempo)
            Spacer(Modifier.width(Tq.Sp.s3))
            PlayStopButton(
                isPlaying = state.isPlaying,
                onClick = onPlayToggled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "MetronomeContent — Stopped 4/4", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun MetronomeContentPreviewStopped() {
    ToniqoTheme(useDarkTheme = true) {
        MetronomeContent(
            state = MetronomeUiState(isPlaying = false),
            onPlayToggled = {}, onBpmChanged = {}, onBpmIncrement = {}, onBpmDecrement = {},
            onTimeSignatureChanged = { _, _ -> }, onSubdivisionChanged = {}, onTapTempo = {},
        )
    }
}

@Preview(name = "MetronomeContent — Playing 4/4", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun MetronomeContentPreviewPlaying() {
    ToniqoTheme(useDarkTheme = true) {
        MetronomeContent(
            state = MetronomeUiState(
                isPlaying = true,
                currentBeat = 1,
                config = MetronomeConfig(bpm = 120, timeSignatureNumerator = 4, timeSignatureDenominator = 4, subdivision = Subdivision.NONE),
            ),
            onPlayToggled = {}, onBpmChanged = {}, onBpmIncrement = {}, onBpmDecrement = {},
            onTimeSignatureChanged = { _, _ -> }, onSubdivisionChanged = {}, onTapTempo = {},
        )
    }
}

@Preview(name = "MetronomeContent — Stopped 7/8", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun MetronomeContentPreviewStopped78() {
    ToniqoTheme(useDarkTheme = true) {
        MetronomeContent(
            state = MetronomeUiState(
                isPlaying = false,
                config = MetronomeConfig(bpm = 92, timeSignatureNumerator = 7, timeSignatureDenominator = 8, subdivision = Subdivision.NONE),
            ),
            onPlayToggled = {}, onBpmChanged = {}, onBpmIncrement = {}, onBpmDecrement = {},
            onTimeSignatureChanged = { _, _ -> }, onSubdivisionChanged = {}, onTapTempo = {},
        )
    }
}
