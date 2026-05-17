package de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ritzelprimpf.toniqo.keyfinder.domain.usecase.FindKeysUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Key Finder screen.
 *
 * Phase 2 exposes only the default UI state; note-entry and tonic-selection handlers, plus the
 * query-and-update logic, land in a later phase.
 *
 * @property findKeys Use case that returns the ranked match list. Injected by Hilt.
 */
@HiltViewModel
class KeyFinderViewModel @Inject constructor(
    private val findKeys: FindKeysUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeyFinderUiState())

    /** The screen's current UI state. */
    val uiState: StateFlow<KeyFinderUiState> = _uiState.asStateFlow()
}
