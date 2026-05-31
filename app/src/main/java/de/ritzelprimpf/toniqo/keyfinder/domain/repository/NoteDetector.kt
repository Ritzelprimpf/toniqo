package de.ritzelprimpf.toniqo.keyfinder.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Converts a live microphone feed into a stream of stable, confirmed pitch classes.
 *
 * The implementation ([de.ritzelprimpf.toniqo.keyfinder.data.StableNoteDetectorImpl]) composes
 * [de.ritzelprimpf.toniqo.audio.AudioCaptureSource] and
 * [de.ritzelprimpf.toniqo.audio.PitchDetector] with a confirmation/debounce state machine to
 * ensure each emission represents a note the user deliberately played and held, not a transient.
 *
 * ## Typical lifecycle
 *
 * ```kotlin
 * viewModelScope.launch { noteDetector.start() }
 * viewModelScope.launch { noteDetector.detectedNotes().collect { addToNoteList(it) } }
 * // ...on stop:
 * viewModelScope.launch { noteDetector.stop() }
 * ```
 *
 * `start()` launches audio capture in the background and returns immediately. `stop()` cancels
 * the capture coroutine and waits for it to finish. Both sides are safe to call multiple times.
 */
interface NoteDetector {

    /**
     * A hot [Flow] that emits a pitch class (0–11) each time the user plays and holds a
     * clear, stable note.
     *
     * 0 = C, 1 = C♯/D♭, 2 = D, …, 11 = B (same ordinal as
     * [de.ritzelprimpf.toniqo.common.model.NoteName.semitonesFromC]).
     *
     * Emissions are debounced: a continuously held note emits exactly once; re-emission
     * requires a silence gap or a pitch-class change. See `DECISIONS.md` for the chosen
     * confirmation window and debounce rule.
     */
    fun detectedNotes(): Flow<Int>

    /**
     * Starts audio capture and pitch detection in the background.
     *
     * Safe to call while already running; the previous capture is cancelled first. Returns
     * immediately after launching the background coroutine.
     */
    suspend fun start()

    /**
     * Stops audio capture and waits for the background coroutine to finish.
     *
     * Safe to call when not running. After this returns no further emissions will arrive on
     * [detectedNotes] until [start] is called again.
     */
    suspend fun stop()
}
