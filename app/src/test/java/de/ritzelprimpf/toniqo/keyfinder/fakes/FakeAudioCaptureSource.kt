package de.ritzelprimpf.toniqo.keyfinder.fakes

import de.ritzelprimpf.toniqo.audio.AudioCaptureSource
import de.ritzelprimpf.toniqo.audio.CaptureEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

/**
 * Test double for [AudioCaptureSource] used in Key Finder tests.
 *
 * Returns a finite flow of the supplied events. The flow completes after all events are emitted,
 * allowing [de.ritzelprimpf.toniqo.keyfinder.data.StableNoteDetectorImpl.captureJob] to
 * complete naturally so tests can [kotlinx.coroutines.Job.join] it without artificial delays.
 */
class FakeAudioCaptureSource(
    private val events: List<CaptureEvent>,
) : AudioCaptureSource {

    override fun samples(): Flow<CaptureEvent> = events.asFlow()
}
