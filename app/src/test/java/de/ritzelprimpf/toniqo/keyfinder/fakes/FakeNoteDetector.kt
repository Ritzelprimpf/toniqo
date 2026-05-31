package de.ritzelprimpf.toniqo.keyfinder.fakes

import de.ritzelprimpf.toniqo.keyfinder.domain.repository.NoteDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Test double for [NoteDetector].
 *
 * Exposes [emit] so tests can inject pitch-class values as if they had arrived from the
 * microphone — without any real audio I/O. [isRunning] tracks whether [start] has been called
 * so tests can assert that the ViewModel correctly started and stopped the detector.
 */
class FakeNoteDetector : NoteDetector {

    private val _detectedNotes = MutableSharedFlow<Int>(extraBufferCapacity = 16)

    /** `true` between [start] and [stop] calls. */
    var isRunning: Boolean = false
        private set

    override fun detectedNotes(): Flow<Int> = _detectedNotes

    override suspend fun start() {
        isRunning = true
    }

    override suspend fun stop() {
        isRunning = false
    }

    /**
     * Emits [pitchClass] on [detectedNotes] as if the user just played and held that note
     * into the microphone.
     *
     * @param pitchClass Pitch class to emit (0–11, where 0 = C and 11 = B).
     */
    suspend fun emit(pitchClass: Int) {
        _detectedNotes.emit(pitchClass)
    }
}
