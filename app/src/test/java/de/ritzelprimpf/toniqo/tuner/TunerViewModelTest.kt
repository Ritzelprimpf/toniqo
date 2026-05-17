package de.ritzelprimpf.toniqo.tuner

import de.ritzelprimpf.toniqo.tuner.domain.model.TuningStatus
import de.ritzelprimpf.toniqo.tuner.domain.usecase.DetectTunedStringUseCase
import de.ritzelprimpf.toniqo.tuner.domain.usecase.GetTunerPresetsUseCase
import de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerUiState
import de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerViewModel
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class TunerViewModelTest {

    private val getTunerPresets: GetTunerPresetsUseCase = mockk(relaxed = true)
    private val detectTunedString: DetectTunedStringUseCase = mockk(relaxed = true)

    @Test
    fun `initial uiState matches the documented defaults`() {
        val viewModel = TunerViewModel(getTunerPresets, detectTunedString)

        val state = viewModel.uiState.value

        assertEquals(emptyList<Any>(), state.availablePresets)
        assertEquals(null, state.selectedPreset)
        assertEquals(TunerUiState.DEFAULT_STRING_INDEX, state.currentStringIndex)
        assertEquals(null, state.detectedFrequencyHz)
        assertEquals(null, state.centsOffTarget)
        assertEquals(TuningStatus.IDLE, state.status)
    }
}
