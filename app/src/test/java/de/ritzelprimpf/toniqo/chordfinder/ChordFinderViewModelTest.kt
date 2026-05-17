package de.ritzelprimpf.toniqo.chordfinder

import de.ritzelprimpf.toniqo.chordfinder.domain.usecase.FindChordsUseCase
import de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel.ChordFinderViewModel
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ChordFinderViewModelTest {

    private val findChords: FindChordsUseCase = mockk(relaxed = true)

    @Test
    fun `initial uiState matches the documented defaults`() {
        val viewModel = ChordFinderViewModel(findChords)

        val state = viewModel.uiState.value

        assertEquals(null, state.selectedRoot)
        assertEquals(null, state.selectedMode)
        assertEquals(false, state.includeSeventhChords)
        assertEquals(null, state.result)
    }
}
