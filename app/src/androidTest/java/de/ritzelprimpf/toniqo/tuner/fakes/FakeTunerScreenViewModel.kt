package de.ritzelprimpf.toniqo.tuner.fakes

import de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerEvent
import de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerScreenViewModel
import de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class FakeTunerScreenViewModel(
    initialState: TunerUiState = TunerUiState(),
) : TunerScreenViewModel {
    val mutableState = MutableStateFlow(initialState)
    val mutableEvents = MutableSharedFlow<TunerEvent>()

    override val uiState: StateFlow<TunerUiState> = mutableState
    override val events: SharedFlow<TunerEvent> = mutableEvents

    override fun onPresetSelected(presetId: String) {}
    override fun onStringSelected(stringIndex: Int) {}
    override fun onEnterChromaticMode() {}
    override fun onExitChromaticMode() {}
    override fun onAutoAdvanceChanged(enabled: Boolean) {}
    override fun onReferencePitchChanged(hz: Double) {}
    override fun onPermissionRequested() {}
    override fun onResumed() {}
}
