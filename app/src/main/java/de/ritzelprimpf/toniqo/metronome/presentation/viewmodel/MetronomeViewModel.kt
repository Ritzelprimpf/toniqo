package de.ritzelprimpf.toniqo.metronome.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ritzelprimpf.toniqo.metronome.domain.usecase.StartMetronomeUseCase
import de.ritzelprimpf.toniqo.metronome.domain.usecase.StopMetronomeUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Metronome screen.
 *
 * Phase 2 exposes only the default UI state; tempo controls, beat-cycle handling, and audio
 * lifecycle land in the metronome implementation phase.
 *
 * @property startMetronome Use case that starts playback. Injected by Hilt.
 * @property stopMetronome Use case that stops playback. Injected by Hilt.
 */
@HiltViewModel
class MetronomeViewModel @Inject constructor(
    private val startMetronome: StartMetronomeUseCase,
    private val stopMetronome: StopMetronomeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetronomeUiState())

    /** The screen's current UI state. */
    val uiState: StateFlow<MetronomeUiState> = _uiState.asStateFlow()
}
