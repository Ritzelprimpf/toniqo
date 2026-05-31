package de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel

import de.ritzelprimpf.toniqo.common.model.Note
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract between [de.ritzelprimpf.toniqo.keyfinder.presentation.ui.KeyFinderScreen] and its
 * ViewModel. Extracted as an interface so Compose UI tests can inject a fake without Hilt.
 *
 * All state is exposed through [uiState]; all user interactions are forwarded via the intent
 * functions below. No business logic lives in the implementing composable.
 */
interface KeyFinderScreenViewModel {

    /** The current screen state. Collect with `collectAsStateWithLifecycle` in the screen. */
    val uiState: StateFlow<KeyFinderUiState>

    /**
     * Adds the note identified by [note]'s pitch class to the chip list.
     * No-op if the pitch class is already present or the max-note cap is reached.
     */
    fun addNoteFromPicker(note: Note)

    /**
     * Removes the note with the given [pitchClass].
     * If it was the marked root, the root is cleared (no auto-reassignment).
     */
    fun removeNote(pitchClass: Int)

    /**
     * Marks [pitchClass] as the sole root, or unmarks it if it is already the root.
     * No-op if [pitchClass] is not in the note list.
     */
    fun toggleRoot(pitchClass: Int)

    /** Clears all notes and the root marker. */
    fun clearAll()

    /**
     * Starts the microphone and begins routing stable pitch-class detections into the note list.
     * No-op if already listening.
     */
    fun startListening()

    /**
     * Stops the microphone. Notes already in the list are preserved.
     * No-op if not currently listening.
     */
    fun stopListening()
}
