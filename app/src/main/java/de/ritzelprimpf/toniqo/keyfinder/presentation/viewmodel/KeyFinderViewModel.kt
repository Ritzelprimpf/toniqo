package de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderInput
import de.ritzelprimpf.toniqo.keyfinder.domain.repository.NoteDetector
import de.ritzelprimpf.toniqo.keyfinder.domain.usecase.MatchScalesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Key Finder screen.
 *
 * Holds the note list (de-duplicated by pitch class, ordered by insertion), an optional root
 * marker, and mic on/off state. Re-ranks the 168-candidate scale catalog live on every
 * mutation — add, remove, root toggle, clear, or mic emission. No action button required.
 *
 * There is no logic in `init {}`. Mic listening is started only by explicit [startListening].
 *
 * **Threading.** [MatchScalesUseCase] is a pure synchronous function (microseconds); it is
 * called on the main dispatcher. The [NoteDetector] collection runs inside [viewModelScope].
 *
 * @property matchScales Synchronous scoring and ranking engine. Called on every mutation.
 * @property noteDetector Converts the live mic feed into confirmed pitch-class emissions.
 */
@HiltViewModel
class KeyFinderViewModel @Inject constructor(
    private val matchScales: MatchScalesUseCase,
    private val noteDetector: NoteDetector,
) : ViewModel() {

    companion object {
        /**
         * Maximum number of distinct notes (pitch classes) the user may add.
         *
         * 12 is the natural chromatic maximum — one per pitch class. Any attempt to add a
         * 13th pitch class would inevitably duplicate an existing one and be a no-op. The cap
         * is a named constant rather than an implicit floor.
         */
        const val MAX_NOTE_COUNT: Int = 12
    }

    /**
     * Insertion-ordered map from pitch class to display name.
     *
     * [LinkedHashMap] preserves the order in which notes were added, which determines the
     * left-to-right order of chips in the UI rail (Phase 7.4).
     */
    private val noteMap: LinkedHashMap<Int, String> = LinkedHashMap()

    private var rootPitchClass: Int? = null

    private val _uiState = MutableStateFlow(KeyFinderUiState())

    /** The screen's current UI state. Collect with `collectAsStateWithLifecycle` in Phase 7.4. */
    val uiState: StateFlow<KeyFinderUiState> = _uiState.asStateFlow()

    private var listeningJob: Job? = null

    // ─── Public intents ───────────────────────────────────────────────────────

    /**
     * Adds the note identified by [note]'s pitch class to the chip list.
     *
     * If the pitch class is already present or the [MAX_NOTE_COUNT] cap has been reached,
     * this is a no-op. Octave information is discarded; only the pitch class and its sharp
     * display name are retained.
     *
     * @param note The note selected in the picker.
     */
    fun addNoteFromPicker(note: Note) {
        val pitchClass = note.name.semitonesFromC
        if (noteMap.containsKey(pitchClass) || noteMap.size >= MAX_NOTE_COUNT) return
        noteMap[pitchClass] = note.name.sharpName
        recompute()
    }

    /**
     * Removes the note with the given [pitchClass] from the chip list and recomputes results.
     *
     * If the removed note was the marked root, the root is cleared (no auto-reassignment).
     * No-op if the pitch class is not in the list.
     *
     * @param pitchClass Pitch class to remove (0–11).
     */
    fun removeNote(pitchClass: Int) {
        if (!noteMap.containsKey(pitchClass)) return
        noteMap.remove(pitchClass)
        if (rootPitchClass == pitchClass) rootPitchClass = null
        recompute()
    }

    /**
     * Sets, moves, or unsets the sole root marker and recomputes results.
     *
     * - If [pitchClass] is not the current root, it becomes the new root (any previous root
     *   is cleared — exactly one root at a time).
     * - If [pitchClass] is already the root, the root is unset (toggle-off behaviour).
     * - No-op if [pitchClass] is not in the note list.
     *
     * @param pitchClass Pitch class to mark/unmark as root (0–11).
     */
    fun toggleRoot(pitchClass: Int) {
        if (!noteMap.containsKey(pitchClass)) return
        rootPitchClass = if (rootPitchClass == pitchClass) null else pitchClass
        recompute()
    }

    /**
     * Clears all notes and the root marker. Results become empty.
     */
    fun clearAll() {
        noteMap.clear()
        rootPitchClass = null
        recompute()
    }

    /**
     * Starts the microphone and begins routing confirmed note detections into the chip list.
     *
     * Sets [KeyFinderUiState.isListening] to `true`. Each pitch-class emission from
     * [NoteDetector.detectedNotes] is processed identically to a picker add (de-duped by pitch
     * class, cap-checked). No-op if already listening.
     */
    fun startListening() {
        if (_uiState.value.isListening) return
        _uiState.update { it.copy(isListening = true) }
        listeningJob = viewModelScope.launch {
            noteDetector.start()
            noteDetector.detectedNotes().collect { pitchClass ->
                addNoteByPitchClass(pitchClass)
            }
        }
    }

    /**
     * Stops the microphone and ceases routing mic notes into the chip list.
     *
     * Sets [KeyFinderUiState.isListening] to `false`. Notes already in the list are preserved.
     * Emissions arriving after this call are ignored. No-op if not currently listening.
     */
    fun stopListening() {
        if (!_uiState.value.isListening) return
        _uiState.update { it.copy(isListening = false) }
        listeningJob?.cancel()
        listeningJob = null
        viewModelScope.launch { noteDetector.stop() }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Adds a note by its raw pitch class using the sharp-spelled display name.
     *
     * Used by the mic path. Picker-path uses [addNoteFromPicker], which carries the [Note]'s
     * own spelling. Both paths share the same de-dup and cap check.
     */
    private fun addNoteByPitchClass(pitchClass: Int) {
        if (noteMap.containsKey(pitchClass) || noteMap.size >= MAX_NOTE_COUNT) return
        noteMap[pitchClass] = NoteName.entries[pitchClass].sharpName
        recompute()
    }

    /**
     * Single recompute path: builds [KeyFinderInput], calls [MatchScalesUseCase] (synchronous),
     * and pushes a new [KeyFinderUiState] snapshot.
     *
     * Called on every mutation. The ≥3-note gate lives inside the use case, so below 3 notes
     * [KeyFinderUiState.results] is naturally empty and the UI shows its idle state.
     */
    private fun recompute() {
        val root = rootPitchClass
        val pitchClasses = noteMap.keys.toSet()
        val results = matchScales(KeyFinderInput(pitchClasses = pitchClasses, rootPitchClass = root))
        _uiState.update {
            it.copy(
                notes = noteMap.entries.map { (pc, name) ->
                    NoteChip(pitchClass = pc, displayName = name, isRoot = pc == root)
                },
                rootPitchClass = root,
                results = results,
                matchCount = results.size,
            )
        }
    }
}
