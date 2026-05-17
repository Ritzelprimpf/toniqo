package de.ritzelprimpf.toniqo.tuner.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ritzelprimpf.toniqo.tuner.domain.usecase.DetectTunedStringUseCase
import de.ritzelprimpf.toniqo.tuner.domain.usecase.GetTunerPresetsUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Tuner screen.
 *
 * Holds the screen's UI state as a [StateFlow] and orchestrates the tuner use cases. Phase 2
 * exposes only the default state; the orchestration logic — preset loading, microphone capture,
 * mode handling — lands in Phase 5.
 *
 * @property getTunerPresets Use case that returns the available presets. Injected by Hilt.
 * @property detectTunedString Use case that runs a single detection-and-compare cycle. Injected
 *   by Hilt.
 */
@HiltViewModel
class TunerViewModel @Inject constructor(
    private val getTunerPresets: GetTunerPresetsUseCase,
    private val detectTunedString: DetectTunedStringUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TunerUiState())

    /** The screen's current UI state. */
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()
}
