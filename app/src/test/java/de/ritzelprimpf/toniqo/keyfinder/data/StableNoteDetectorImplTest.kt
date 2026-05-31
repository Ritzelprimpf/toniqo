package de.ritzelprimpf.toniqo.keyfinder.data

import de.ritzelprimpf.toniqo.audio.AudioSourceKind
import de.ritzelprimpf.toniqo.audio.CaptureEvent
import de.ritzelprimpf.toniqo.common.model.NoteName
import de.ritzelprimpf.toniqo.keyfinder.fakes.FakeAudioCaptureSource
import de.ritzelprimpf.toniqo.keyfinder.fakes.FakePitchDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [StableNoteDetectorImpl].
 *
 * Each test wires a [FakeAudioCaptureSource] (finite scripted events) with a [FakePitchDetector]
 * (scripted Hz / null sequences). After calling [StableNoteDetectorImpl.start], the test joins
 * the internal [StableNoteDetectorImpl.captureJob] to wait for the fake flow to be fully consumed
 * before asserting. No artificial delays are used.
 *
 * Pitch class reference: C=0, C♯=1, D=2, D♯=3, E=4, F=5, F♯=6, G=7, G♯=8, A=9, A♯=10, B=11.
 */
class StableNoteDetectorImplTest {

    // ── Constants ─────────────────────────────────────────────────────────────────

    private val sampleRateHz = 44_100
    private val bufferFrames = 4_096

    /** A4 = 440 Hz → NoteName.A → pitch class 9. */
    private val a4Hz = 440.0
    private val pitchClassA = NoteName.A.semitonesFromC  // 9

    /** B4 ≈ 493.88 Hz → NoteName.B → pitch class 11. */
    private val b4Hz = 493.88
    private val pitchClassB = NoteName.B.semitonesFromC  // 11

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private fun silentBuffer() = FloatArray(bufferFrames) { 0f }

    private fun listeningEvent() = CaptureEvent.Listening(sampleRateHz, bufferFrames, AudioSourceKind.UNPROCESSED)
    private fun samplesEvent() = CaptureEvent.Samples(silentBuffer())

    /**
     * Builds a detector, starts it, waits for the fake source to be fully consumed,
     * and returns all emitted pitch classes.
     *
     * The [FakeAudioCaptureSource] returns a finite flow; [StableNoteDetectorImpl.captureJob]
     * completes when the flow terminates, so [kotlinx.coroutines.Job.join] gives a deterministic
     * end point for assertions.
     */
    private fun collectEmissions(
        fakeSource: FakeAudioCaptureSource,
        fakePitchDetector: FakePitchDetector,
    ): List<Int> = runBlocking {
        val detector = StableNoteDetectorImpl(fakeSource, fakePitchDetector)

        val emitted = mutableListOf<Int>()
        val collectJob = launch(Dispatchers.Unconfined) {
            detector.detectedNotes().collect { emitted.add(it) }
        }

        detector.start()
        detector.captureJob?.join()  // wait for fake flow to be consumed
        collectJob.cancel()

        emitted
    }

    // ── Core behaviour ────────────────────────────────────────────────────────────

    @Test
    fun `held note that persists past confirmation window emits its pitch class exactly once`() {
        // 1 Listening + CONFIRMATION_BUFFER_COUNT + 2 extra same-pitch frames
        val eventCount = StableNoteDetectorImpl.CONFIRMATION_BUFFER_COUNT + 2
        val events = listOf(listeningEvent()) + List(eventCount) { samplesEvent() }
        val frequencies = List(eventCount) { a4Hz }

        val emitted = collectEmissions(
            FakeAudioCaptureSource(events),
            FakePitchDetector(frequencies),
        )

        assertEquals("A4 held should emit exactly once", listOf(pitchClassA), emitted)
    }

    @Test
    fun `held note does not re-emit until pitch class changes or silence intervenes`() {
        // 5 frames all A4 — confirmation fires at frame 2, frames 3-5 are suppressed
        val frames = 5
        val events = listOf(listeningEvent()) + List(frames) { samplesEvent() }
        val frequencies = List(frames) { a4Hz }

        val emitted = collectEmissions(
            FakeAudioCaptureSource(events),
            FakePitchDetector(frequencies),
        )

        assertEquals("Continuously held note should emit only once", listOf(pitchClassA), emitted)
    }

    @Test
    fun `transient shorter than confirmation window emits nothing`() {
        // Only 1 frame of A4 (below CONFIRMATION_BUFFER_COUNT = 2), then ends
        val events = listOf(listeningEvent(), samplesEvent())
        val frequencies = listOf(a4Hz)

        val emitted = collectEmissions(
            FakeAudioCaptureSource(events),
            FakePitchDetector(frequencies),
        )

        assertTrue("Transient shorter than window must not emit", emitted.isEmpty())
    }

    @Test
    fun `A then silence then A emits pitch class twice`() {
        // A4 × 2 → emit A; null (silence) → re-arm; A4 × 2 → emit A again
        val events = listOf(listeningEvent()) +
            List(2) { samplesEvent() } +   // A4 × 2
            listOf(samplesEvent()) +         // null (silence)
            List(2) { samplesEvent() }       // A4 × 2
        val frequencies = listOf(a4Hz, a4Hz, null, a4Hz, a4Hz)

        val emitted = collectEmissions(
            FakeAudioCaptureSource(events),
            FakePitchDetector(frequencies),
        )

        assertEquals(
            "A → silence → A should emit A twice",
            listOf(pitchClassA, pitchClassA),
            emitted,
        )
    }

    @Test
    fun `null mid-confirmation resets and prevents emission`() {
        // A4, then null (reset), then A4 only once more — final count=1, below threshold
        val events = listOf(listeningEvent()) +
            List(3) { samplesEvent() }   // A4, null, A4
        val frequencies = listOf(a4Hz, null, a4Hz)

        val emitted = collectEmissions(
            FakeAudioCaptureSource(events),
            FakePitchDetector(frequencies),
        )

        assertTrue(
            "null mid-confirmation should reset; only one A4 after reset is not enough to emit",
            emitted.isEmpty(),
        )
    }

    @Test
    fun `slightly detuned input maps to nearest pitch class`() {
        // 444 Hz is sharp of A4 but still closer to A4 (440 Hz) than A#4 (466 Hz)
        // MusicTheory.frequencyToNote(444.0) → A4 → pitch class A
        val detunedHz = 444.0
        val events = listOf(listeningEvent()) + List(2) { samplesEvent() }
        val frequencies = listOf(detunedHz, detunedHz)

        val emitted = collectEmissions(
            FakeAudioCaptureSource(events),
            FakePitchDetector(frequencies),
        )

        assertEquals(
            "444 Hz (sharp A4) should map to pitch class A via frequencyToNote",
            listOf(pitchClassA),
            emitted,
        )
    }

    @Test
    fun `pitch class change re-arms debounce and both notes emit`() {
        // A4 × 2 → emit A; B4 × 2 → emit B (pitch change re-arms debounce)
        val events = listOf(listeningEvent()) +
            List(2) { samplesEvent() } +   // A4 × 2
            List(2) { samplesEvent() }     // B4 × 2
        val frequencies = listOf(a4Hz, a4Hz, b4Hz, b4Hz)

        val emitted = collectEmissions(
            FakeAudioCaptureSource(events),
            FakePitchDetector(frequencies),
        )

        assertEquals(
            "A then B should each emit once",
            listOf(pitchClassA, pitchClassB),
            emitted,
        )
    }

    @Test
    fun `listening event resets confirmation state`() {
        // Frame before Listening doesn't exist; after Listening, need CONFIRMATION_BUFFER_COUNT
        // frames to confirm. Single A4 after Listening must not emit.
        val events = listOf(
            listeningEvent(),
            samplesEvent(),  // only 1 frame: not enough
        )
        val frequencies = listOf(a4Hz)

        val emitted = collectEmissions(
            FakeAudioCaptureSource(events),
            FakePitchDetector(frequencies),
        )

        assertTrue("A single frame after Listening must not emit (count=1 < 2)", emitted.isEmpty())
    }

    @Test
    fun `no emissions on permission denied`() {
        val events = listOf(CaptureEvent.PermissionDenied)

        val emitted = collectEmissions(
            FakeAudioCaptureSource(events),
            FakePitchDetector(emptyList()),
        )

        assertTrue("PermissionDenied must not produce any pitch class emissions", emitted.isEmpty())
    }

    @Test
    fun `no emissions on capture failed`() {
        val events = listOf(CaptureEvent.Failed("hardware error"))

        val emitted = collectEmissions(
            FakeAudioCaptureSource(events),
            FakePitchDetector(emptyList()),
        )

        assertTrue("Failed must not produce any pitch class emissions", emitted.isEmpty())
    }
}
