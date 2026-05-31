package de.ritzelprimpf.toniqo.audio

// Real-device paths are verified in Phase 5.4; see Phase5.2-REQUIREMENTS.md.
// The Listening, Samples, Failed, and UNPROCESSED→MIC fallback paths all require a real
// AudioRecord instance and therefore cannot be exercised in pure unit tests without
// Robolectric or instrumented tests. Those paths are intentionally left uncovered here and
// are deferred to the Phase 5.4 end-to-end verification on a real device.

import de.ritzelprimpf.toniqo.common.permission.AudioPermissionChecker
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCaptureSourceTest {

    private fun buildSource(hasPermission: Boolean): AudioRecordCaptureSource {
        val checker = object : AudioPermissionChecker {
            override fun hasRecordAudioPermission(): Boolean = hasPermission
        }
        return AudioRecordCaptureSource(checker)
    }

    @Test
    fun `emits exactly PermissionDenied and completes when permission is not granted`() = runBlocking {
        val source = buildSource(hasPermission = false)
        val events = source.samples().toList()

        assertEquals("Expected exactly one event", 1, events.size)
        assertTrue(
            "Expected CaptureEvent.PermissionDenied, got ${events.first()}",
            events.first() is CaptureEvent.PermissionDenied,
        )
    }
}
