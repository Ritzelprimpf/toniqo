package de.ritzelprimpf.toniqo.keyfinder.fakes

import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel.KeyFinderScreenViewModel
import de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel.KeyFinderUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake [KeyFinderScreenViewModel] for Compose UI tests. Tests drive state by writing to
 * [mutableState] directly; interaction counts are exposed for assertion.
 */
class FakeKeyFinderScreenViewModel(
    initialState: KeyFinderUiState = KeyFinderUiState(),
) : KeyFinderScreenViewModel {

    val mutableState = MutableStateFlow(initialState)

    override val uiState: StateFlow<KeyFinderUiState> = mutableState.asStateFlow()

    val addedNotes = mutableListOf<Note>()
    val removedPitchClasses = mutableListOf<Int>()
    val toggledRoots = mutableListOf<Int>()
    var clearAllCount = 0
    var startListeningCount = 0
    var stopListeningCount = 0

    override fun addNoteFromPicker(note: Note) {
        addedNotes += note
    }

    override fun removeNote(pitchClass: Int) {
        removedPitchClasses += pitchClass
    }

    override fun toggleRoot(pitchClass: Int) {
        toggledRoots += pitchClass
    }

    override fun clearAll() {
        clearAllCount++
    }

    override fun startListening() {
        startListeningCount++
        mutableState.value = mutableState.value.copy(isListening = true)
    }

    override fun stopListening() {
        stopListeningCount++
        mutableState.value = mutableState.value.copy(isListening = false)
    }
}
