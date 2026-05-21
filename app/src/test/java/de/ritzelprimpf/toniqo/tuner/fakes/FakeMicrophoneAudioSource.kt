package de.ritzelprimpf.toniqo.tuner.fakes

import de.ritzelprimpf.toniqo.tuner.data.CaptureEvent
import de.ritzelprimpf.toniqo.tuner.data.MicrophoneAudioSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

/**
 * Test double for [MicrophoneAudioSource].
 *
 * Supports two construction modes:
 *
 * 1. **Single-session mode** (`FakeMicrophoneAudioSource(events)`): every call to [samples]
 *    returns the same list of events. Suitable for use-case tests where [samples] is called once.
 *
 * 2. **Multi-session mode** (`FakeMicrophoneAudioSource(sessions = listOf(..., ...))`) : each call
 *    to [samples] dequeues the next session's events. When all sessions are consumed, subsequent
 *    calls return an empty flow. Suitable for ViewModel tests where the pipeline is restarted
 *    across string advances (each restart calls [samples] again).
 */
class FakeMicrophoneAudioSource private constructor(
    private val sessions: List<List<CaptureEvent>>,
    private val multiSession: Boolean,
) : MicrophoneAudioSource {

    private var sessionIndex = 0

    /** Single-session constructor: every [samples] call replays the same [events]. */
    constructor(events: List<CaptureEvent>) : this(listOf(events), multiSession = false)

    companion object {
        /**
         * Multi-session constructor: each [samples] call returns the next session's events.
         * Once sessions are exhausted subsequent calls return an empty flow.
         */
        fun multiSession(sessions: List<List<CaptureEvent>>): FakeMicrophoneAudioSource =
            FakeMicrophoneAudioSource(sessions, multiSession = true)
    }

    override fun samples(): Flow<CaptureEvent> {
        return if (multiSession) {
            val events = sessions.getOrElse(sessionIndex) { emptyList() }
            sessionIndex++
            events.asFlow()
        } else {
            sessions[0].asFlow()
        }
    }
}
