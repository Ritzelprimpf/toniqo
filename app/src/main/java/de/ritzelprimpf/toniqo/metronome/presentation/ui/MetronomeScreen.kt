package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.MetronomeEvent
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.MetronomeViewModel
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * Metronome screen — top-level composable.
 *
 * Wires [MetronomeViewModel] via [hiltViewModel], sets up a [SnackbarHost] for
 * [MetronomeEvent.AudioUnavailable] errors, and delegates layout to the stateless
 * [MetronomeContent].
 *
 * Screen-on management ([KeepScreenOnWhilePlaying]) is registered here as a side effect
 * rather than inside [MetronomeContent] so it stays active for the full lifetime of the
 * screen composition and is not affected by [MetronomeContent] recompositions.
 */
@Composable
internal fun MetronomeScreen(
    viewModel: MetronomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val audioUnavailableMessage = stringResource(R.string.metronome_error_audio_unavailable)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MetronomeEvent.AudioUnavailable ->
                    snackbarHostState.showSnackbar(audioUnavailableMessage)
            }
        }
    }

    KeepScreenOnWhilePlaying(isPlaying = state.isPlaying)

    Scaffold(
        containerColor = Tq.Color.BgBase,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        MetronomeContent(
            state = state,
            onPlayToggled = viewModel::onPlayToggled,
            onBpmChanged = viewModel::onBpmChanged,
            onBpmIncrement = viewModel::onBpmIncrement,
            onBpmDecrement = viewModel::onBpmDecrement,
            onTimeSignatureChanged = viewModel::onTimeSignatureChanged,
            onSubdivisionChanged = viewModel::onSubdivisionChanged,
            onTapTempo = viewModel::onTapTempo,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
