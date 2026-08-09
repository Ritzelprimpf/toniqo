package de.ritzelprimpf.toniqo.tuner.fakes

import de.ritzelprimpf.toniqo.audio.AudioCaptureSource
import de.ritzelprimpf.toniqo.audio.CaptureEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

/**
 * Test double for [AudioCaptureSource].
 *
 * Supports two construction modes:
 *
 * 1. **Single-session mode** (`FakeAudioCaptureSource(events)`): every call to [samples]
 *    returns the same list of events. Suitable for use-case tests where [samples] is called once.
 *
 * 2. **Multi-session mode** (`FakeAudioCaptureSource(sessions = listOf(..., ...))`) : each call
 *    to [samples] dequeues the next session's events. When all sessions are consumed, subsequent
 *    calls return an empty flow. Suitable for ViewModel tests where the pipeline is restarted
 *    across string advances (each restart calls [samples] again).
 */
class FakeAudioCaptureSource private constructor(
    private val sessions: List<List<CaptureEvent>>,
    private val multiSession: Boolean,
) : AudioCaptureSource {

    private var sessionIndex = 0

    /** Number of times [samples] has been called — one call per pipeline (re)subscription. */
    var samplesCallCount = 0
        private set

    /** Single-session constructor: every [samples] call replays the same [events]. */
    constructor(events: List<CaptureEvent>) : this(listOf(events), multiSession = false)

    companion object {
        /**
         * Multi-session constructor: each [samples] call returns the next session's events.
         * Once sessions are exhausted subsequent calls return an empty flow.
         */
        fun multiSession(sessions: List<List<CaptureEvent>>): FakeAudioCaptureSource =
            FakeAudioCaptureSource(sessions, multiSession = true)
    }

    override fun samples(): Flow<CaptureEvent> {
        samplesCallCount++
        return if (multiSession) {
            val events = sessions.getOrElse(sessionIndex) { emptyList() }
            sessionIndex++
            events.asFlow()
        } else {
            sessions[0].asFlow()
        }
    }
}
