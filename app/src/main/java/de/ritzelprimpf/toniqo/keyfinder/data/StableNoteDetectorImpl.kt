package de.ritzelprimpf.toniqo.keyfinder.data

import androidx.annotation.VisibleForTesting
import de.ritzelprimpf.toniqo.audio.AudioCaptureSource
import de.ritzelprimpf.toniqo.audio.CaptureEvent
import de.ritzelprimpf.toniqo.audio.PitchDetector
import de.ritzelprimpf.toniqo.common.util.MusicTheory
import de.ritzelprimpf.toniqo.keyfinder.domain.repository.NoteDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live microphone → confirmed pitch class converter for the Key Finder module.
 *
 * Composes [AudioCaptureSource] (raw buffers) + [PitchDetector] (YIN: buffer → Hz) +
 * [MusicTheory.frequencyToNote] (Hz → Note → pitch class) with a **confirmation/debounce
 * state machine** that ensures each [detectedNotes] emission represents a note the user
 * deliberately played and held.
 *
 * ## Confirmation rule
 *
 * A pitch class is emitted only after the **same** pitch class has been detected in
 * [CONFIRMATION_BUFFER_COUNT] consecutive capture buffers (~186 ms at the Phase 5.2 locked
 * parameters of 44 100 Hz / 4 096 frames). This filters out transients shorter than the window.
 *
 * ## Debounce rule
 *
 * Once a pitch class is emitted, it is **not re-emitted** until:
 * - A `null` (silence / no clear pitch) frame is received, OR
 * - A different pitch class is detected.
 *
 * A continuously held note therefore produces exactly one emission. A→silence→A produces two.
 * A→B→A produces three (pitch-class change is also a re-arm trigger).
 *
 * ## Lifecycle
 *
 * `start()` launches capture in an internal [CoroutineScope]. `stop()` cancels it. Both are safe
 * to call multiple times. The [NoteDetector.detectedNotes] flow is hot (SharedFlow); the Key
 * Finder ViewModel subscribes before calling `start()` to avoid missing early emissions.
 *
 * Decisions recorded: `DECISIONS.md` Phase 7.2 entries.
 *
 * @property audioCaptureSource Provides the raw audio stream.
 * @property pitchDetector Estimates fundamental frequency from each buffer.
 */
@Singleton
class StableNoteDetectorImpl @Inject constructor(
    private val audioCaptureSource: AudioCaptureSource,
    private val pitchDetector: PitchDetector,
) : NoteDetector {

    private val _detectedNotes = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val detectorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * The active capture [Job], or `null` when stopped.
     *
     * Exposed `internal` (with private setter) so unit tests can [Job.join] it after calling
     * [start], allowing the test to observe all emissions from a finite fake audio source before
     * asserting. Not intended for production use outside this class.
     */
    @VisibleForTesting
    internal var captureJob: Job? = null
        private set

    override fun detectedNotes(): Flow<Int> = _detectedNotes

    override suspend fun start() {
        captureJob?.cancelAndJoin()
        captureJob = detectorScope.launch { runCapture() }
    }

    override suspend fun stop() {
        captureJob?.cancelAndJoin()
        captureJob = null
    }

    private suspend fun runCapture() {
        var consecutiveCount = 0
        var currentPitchClass: Int? = null
        var lastEmittedPitchClass: Int? = null  // null means "re-armed, ready to emit"

        audioCaptureSource.samples().collect { event ->
            when (event) {
                is CaptureEvent.Listening -> {
                    // Capture session started; reset confirmation state.
                    consecutiveCount = 0
                    currentPitchClass = null
                    lastEmittedPitchClass = null
                }
                is CaptureEvent.Samples -> {
                    val hz = pitchDetector.detectPitch(event.buffer, SAMPLE_RATE_HZ)
                    if (hz == null) {
                        // Silence or unclear signal: reset confirmation and re-arm debounce.
                        consecutiveCount = 0
                        currentPitchClass = null
                        lastEmittedPitchClass = null
                    } else {
                        val note = MusicTheory.frequencyToNote(hz)
                        if (note == null) {
                            // Frequency out of musical range; treat as silence.
                            consecutiveCount = 0
                            currentPitchClass = null
                            lastEmittedPitchClass = null
                        } else {
                            val pitchClass = note.name.semitonesFromC
                            if (pitchClass != currentPitchClass) {
                                // Pitch class changed: reset confirmation, re-arm debounce.
                                consecutiveCount = 1
                                currentPitchClass = pitchClass
                                lastEmittedPitchClass = null
                            } else {
                                consecutiveCount++
                            }
                            // Emit once the confirmation window is satisfied AND the debounce
                            // is armed (lastEmittedPitchClass == null means ready to emit).
                            if (consecutiveCount >= CONFIRMATION_BUFFER_COUNT &&
                                lastEmittedPitchClass == null
                            ) {
                                _detectedNotes.emit(pitchClass)
                                lastEmittedPitchClass = pitchClass
                            }
                        }
                    }
                }
                is CaptureEvent.PermissionDenied, is CaptureEvent.Failed -> {
                    // Terminal event; the upstream flow completes and runCapture returns.
                }
            }
        }
    }

    companion object {
        /**
         * Number of consecutive buffers carrying the same pitch class that must be observed
         * before that pitch class is emitted.
         *
         * At the Phase 5.2 locked parameters (44 100 Hz, 4 096 frames/buffer), each buffer is
         * ~92.9 ms. Two buffers ≈ **186 ms** — within the 150–250 ms target range recorded in
         * `DECISIONS.md` Phase 7.2.
         */
        const val CONFIRMATION_BUFFER_COUNT = 2

        /** Sample rate passed to [PitchDetector]; must match Phase 5.2 locked value. */
        private const val SAMPLE_RATE_HZ = 44_100
    }
}
