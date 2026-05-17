package de.ritzelprimpf.toniqo.metronome

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import de.ritzelprimpf.toniqo.metronome.domain.usecase.StartMetronomeUseCase
import de.ritzelprimpf.toniqo.metronome.domain.usecase.StopMetronomeUseCase
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.MetronomeUiState
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.MetronomeViewModel
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class MetronomeViewModelTest {

    private val startMetronome: StartMetronomeUseCase = mockk(relaxed = true)
    private val stopMetronome: StopMetronomeUseCase = mockk(relaxed = true)

    @Test
    fun `initial uiState matches the documented defaults`() {
        val viewModel = MetronomeViewModel(startMetronome, stopMetronome)

        val state = viewModel.uiState.value

        assertEquals(false, state.isPlaying)
        assertEquals(MetronomeConfig.DEFAULT_BPM, state.config.bpm)
        assertEquals(MetronomeConfig.DEFAULT_TIME_SIGNATURE_NUMERATOR, state.config.timeSignatureNumerator)
        assertEquals(MetronomeConfig.DEFAULT_TIME_SIGNATURE_DENOMINATOR, state.config.timeSignatureDenominator)
        assertEquals(Subdivision.NONE, state.config.subdivision)
        assertEquals(MetronomeUiState.INITIAL_BEAT, state.currentBeat)
    }
}
