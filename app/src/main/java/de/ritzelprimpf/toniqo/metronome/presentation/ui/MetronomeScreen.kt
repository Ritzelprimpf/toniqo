package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.MetronomeViewModel
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

// TODO(6.4): replace with real screen — Phase 6.3 placeholder wired to real ViewModel.
// Full metronome UI per DESIGN.md §8.2 lands in Phase 6.4.

/**
 * Metronome screen.
 *
 * Phase 6.3: minimal placeholder that observes [MetronomeViewModel] and exposes BPM +/−, time
 * signature, subdivision, and play/stop. Phase 6.4 replaces this composable entirely with the
 * full design-system UI per `DESIGN.md` §8.2.
 */
@Composable
fun MetronomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MetronomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Tq.Color.BgBase)
            .padding(horizontal = Tq.Sp.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // TODO: Replace with custom `metronome` icon from DESIGN.md §7
        Icon(
            imageVector = Icons.Outlined.Alarm,
            contentDescription = null,
            tint = Tq.Color.FgTertiary,
            modifier = Modifier.size(Tq.Sp.s8),
        )
        Spacer(modifier = Modifier.height(Tq.Sp.s4))
        Text(
            text = stringResource(R.string.metronome_title),
            style = Tq.Type.H1,
            color = Tq.Color.FgPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Tq.Sp.s2))
        Text(
            text = stringResource(R.string.metronome_description),
            style = Tq.Type.Body,
            color = Tq.Color.FgSecondary,
            textAlign = TextAlign.Center,
        )

        // ─── Phase 6.3 placeholder — TODO(6.4): replace with real screen ─────────────────────
        Spacer(modifier = Modifier.height(Tq.Sp.s6))

        // BPM row: − | value | +
        val decrementCd = stringResource(R.string.metronome_cd_bpm_decrement)
        val incrementCd = stringResource(R.string.metronome_cd_bpm_increment)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { viewModel.onBpmDecrement() },
                modifier = Modifier.semantics { contentDescription = decrementCd },
            ) {
                Text(text = "−", style = Tq.Type.H1, color = Tq.Color.FgPrimary)
            }
            Text(
                text = stringResource(R.string.metronome_bpm_display, uiState.config.bpm),
                style = Tq.Type.H1,
                color = Tq.Color.FgPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Tq.Sp.s3),
            )
            IconButton(
                onClick = { viewModel.onBpmIncrement() },
                modifier = Modifier.semantics { contentDescription = incrementCd },
            ) {
                Text(text = "+", style = Tq.Type.H1, color = Tq.Color.FgPrimary)
            }
        }

        Spacer(modifier = Modifier.height(Tq.Sp.s2))

        // Time signature
        Text(
            text = stringResource(
                R.string.metronome_time_sig_display,
                uiState.config.timeSignatureNumerator,
                uiState.config.timeSignatureDenominator,
            ),
            style = Tq.Type.Body,
            color = Tq.Color.FgSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Tq.Sp.s1))

        // Subdivision
        Text(
            text = uiState.config.subdivision.name,
            style = Tq.Type.Body,
            color = Tq.Color.FgSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Tq.Sp.s4))

        // Status
        Text(
            text = if (uiState.isPlaying) {
                stringResource(R.string.metronome_status_playing)
            } else {
                stringResource(R.string.metronome_status_stopped)
            },
            style = Tq.Type.Body,
            color = if (uiState.isPlaying) Tq.Color.SignalMint else Tq.Color.FgTertiary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Tq.Sp.s2))

        // Play / Stop
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Button(onClick = { viewModel.onPlayToggled() }) {
                Text(
                    text = if (uiState.isPlaying) {
                        stringResource(R.string.metronome_stop)
                    } else {
                        stringResource(R.string.metronome_play)
                    },
                )
            }
        }
        // ─── End Phase 6.3 placeholder ────────────────────────────────────────────────────────
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "MetronomeScreen — Dark", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun MetronomeScreenPreviewDark() {
    ToniqoTheme(useDarkTheme = true) { MetronomeScreen() }
}

@Preview(name = "MetronomeScreen — Light", showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun MetronomeScreenPreviewLight() {
    ToniqoTheme(useDarkTheme = false) { MetronomeScreen() }
}
