package de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ritzelprimpf.toniqo.chordfinder.domain.usecase.FindChordsUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Chord Finder screen.
 *
 * Phase 2 exposes only the default UI state; root/mode selection handling and seventh-toggle
 * wiring land in a later phase.
 *
 * @property findChords Use case that returns the diatonic-chord list. Injected by Hilt.
 */
@HiltViewModel
class ChordFinderViewModel @Inject constructor(
    private val findChords: FindChordsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChordFinderUiState())

    /** The screen's current UI state. */
    val uiState: StateFlow<ChordFinderUiState> = _uiState.asStateFlow()
}
