package de.ritzelprimpf.toniqo.keyfinder

import de.ritzelprimpf.toniqo.keyfinder.domain.usecase.FindKeysUseCase
import de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel.KeyFinderViewModel
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyFinderViewModelTest {

    private val findKeys: FindKeysUseCase = mockk(relaxed = true)

    @Test
    fun `initial uiState matches the documented defaults`() {
        val viewModel = KeyFinderViewModel(findKeys)

        val state = viewModel.uiState.value

        assertEquals(emptySet<Any>(), state.inputNotes)
        assertEquals(null, state.tonic)
        assertEquals(emptyList<Any>(), state.results)
    }
}
