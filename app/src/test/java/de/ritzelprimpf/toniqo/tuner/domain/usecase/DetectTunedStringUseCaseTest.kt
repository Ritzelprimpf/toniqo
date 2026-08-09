package de.ritzelprimpf.toniqo.tuner.domain.usecase

import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import de.ritzelprimpf.toniqo.common.util.MusicTheory
import de.ritzelprimpf.toniqo.audio.AudioSourceKind
import de.ritzelprimpf.toniqo.audio.CaptureEvent
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerInput
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerMode
import de.ritzelprimpf.toniqo.tuner.fakes.FakeAudioCaptureSource
import de.ritzelprimpf.toniqo.tuner.fakes.FakePitchDetector
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DetectTunedStringUseCaseTest {

    private val refHz = 440.0
    private val targetNote = Note(NoteName.E, 2)             // E2 ≈ 82.41 Hz
    private val targetHz get() = targetNote.frequencyHz(refHz)

    // Frequency that is exactly in tune with E2 at 440 Hz.
    private val inTuneHz get() = targetHz

    // Frequency that is ~20 cents sharp of E2 — out of tolerance.
    private val sharpHz get() = targetHz * Math.pow(2.0, 20.0 / 1200.0)

    private val sampleBuffer = FloatArray(4096)

    private val presetInput = TunerInput(
        mode = TunerMode.PRESET,
        targetNote = targetNote,
        referencePitchHz = refHz,
    )

    private val chromaticInput = TunerInput(
        mode = TunerMode.CHROMATIC,
        targetNote = null,
        referencePitchHz = refHz,
    )

    private fun makeSamplesEvent() = CaptureEvent.Samples(sampleBuffer)

    private fun listeningEvent() = CaptureEvent.Listening(
        sampleRateHz = 44100,
        bufferFrames = 4096,
        source = AudioSourceKind.MIC,
    )

    // ── Permission-denied path ────────────────────────────────────────────────────

    @Test
    fun `permission denied propagates and flow completes`() = runTest {
        val source = FakeAudioCaptureSource(listOf(CaptureEvent.PermissionDenied))
        val detector = FakePitchDetector(emptyList())
        val useCase = DetectTunedStringUseCase(source, detector)

        val events = useCase.execute(presetInput).toList()

        assertEquals(1, events.size)
        assertTrue(events[0] is DetectionEvent.PermissionDenied)
    }

    // ── Listening propagation ─────────────────────────────────────────────────────

    @Test
    fun `listening propagates`() = runTest {
        val source = FakeAudioCaptureSource(listOf(listeningEvent()))
        val detector = FakePitchDetector(emptyList())
        val useCase = DetectTunedStringUseCase(source, detector)

        val events = useCase.execute(presetInput).toList()

        assertEquals(1, events.size)
        assertTrue(events[0] is DetectionEvent.Listening)
    }

    // ── Capture failure ───────────────────────────────────────────────────────────

    @Test
    fun `capture failure propagates`() = runTest {
        val source = FakeAudioCaptureSource(listOf(CaptureEvent.Failed("boom")))
        val detector = FakePitchDetector(emptyList())
        val useCase = DetectTunedStringUseCase(source, detector)

        val events = useCase.execute(presetInput).toList()

        assertEquals(1, events.size)
        val failed = events[0] as DetectionEvent.Failed
        assertEquals("boom", failed.reason)
    }

    // ── Sustained-tone window — PRESET mode ───────────────────────────────────────

    @Test
    fun `six in-tolerance detections — sixth is sustained`() = runTest {
        val samples = List(6) { makeSamplesEvent() }
        val source = FakeAudioCaptureSource(samples)
        val detector = FakePitchDetector(List(6) { inTuneHz })
        val useCase = DetectTunedStringUseCase(source, detector)

        val detections = useCase.execute(presetInput).toList()
            .filterIsInstance<DetectionEvent.Detection>()

        assertEquals(6, detections.size)
        // First five: window not yet full or threshold not yet met
        detections.take(5).forEach { assertFalse(it.isSustainedInTune) }
        // Sixth: window full with 6/6 in tolerance
        assertTrue(detections[5].isSustainedInTune)
    }

    @Test
    fun `glitch budget — 5 of 6 in-tolerance still sustains`() = runTest {
        // 5 in-tune, then 1 out-of-tune glitch, then 1 more in-tune.
        // After the glitch the window is [T,T,T,T,T,F] = 5/6 → STILL sustained on the glitch frame.
        // After the 7th the window is [T,T,T,T,F,T] = 5/6 → still sustained.
        val frequencies = listOf(
            inTuneHz, inTuneHz, inTuneHz, inTuneHz, inTuneHz,
            sharpHz,   // glitch — window becomes [T,T,T,T,T,F] = 5/6
            inTuneHz,  // window becomes [T,T,T,T,F,T] = 5/6
        )
        val source = FakeAudioCaptureSource(List(7) { makeSamplesEvent() })
        val detector = FakePitchDetector(frequencies)
        val useCase = DetectTunedStringUseCase(source, detector)

        val detections = useCase.execute(presetInput).toList()
            .filterIsInstance<DetectionEvent.Detection>()

        assertEquals(7, detections.size)
        // Frames 1–4: window is still filling (size < 6) → not sustained.
        detections.take(4).forEachIndexed { i, d ->
            assertFalse("Frame ${i + 1} should not be sustained (window not full)", d.isSustainedInTune)
        }
        // Frame 5: window = [T,T,T,T,T], size = 5 ≠ 6 → still not sustained.
        assertFalse("5th should not be sustained (window not full)", detections[4].isSustainedInTune)
        // Frame 6 (glitch): window = [T,T,T,T,T,F], 5/6 → SUSTAINED (glitch budget).
        assertTrue("6th (glitch) should be sustained: window is [T,T,T,T,T,F] = 5/6", detections[5].isSustainedInTune)
        // Frame 7: window = [T,T,T,T,F,T], 5/6 → still sustained.
        assertTrue("7th should be sustained: window is [T,T,T,T,F,T] = 5/6", detections[6].isSustainedInTune)
    }

    @Test
    fun `two glitches reset — no sustained trigger after two out-of-tolerance`() = runTest {
        // 4 in-tune, 2 glitches — window [T,T,T,T,F,F] = 4/6 which is < 5 → not sustained
        val frequencies = listOf(
            inTuneHz, inTuneHz, inTuneHz, inTuneHz,
            sharpHz, sharpHz,
        )
        val source = FakeAudioCaptureSource(List(6) { makeSamplesEvent() })
        val detector = FakePitchDetector(frequencies)
        val useCase = DetectTunedStringUseCase(source, detector)

        val detections = useCase.execute(presetInput).toList()
            .filterIsInstance<DetectionEvent.Detection>()

        assertEquals(6, detections.size)
        detections.forEach { assertFalse("No detection should be sustained", it.isSustainedInTune) }
    }

    // ── Null detection handling ───────────────────────────────────────────────────

    @Test
    fun `null detection pushes false into window — no Detection event emitted for null`() = runTest {
        // 5 in-tune, then 1 null: window = [T,T,T,T,T,F] = 5/6 — but no event for the null frame.
        val source = FakeAudioCaptureSource(List(6) { makeSamplesEvent() })
        val detector = FakePitchDetector(listOf(inTuneHz, inTuneHz, inTuneHz, inTuneHz, inTuneHz, null))
        val useCase = DetectTunedStringUseCase(source, detector)

        val events = useCase.execute(presetInput).toList()

        // Only 5 Detection events emitted (null frame emits nothing)
        val detections = events.filterIsInstance<DetectionEvent.Detection>()
        assertEquals(5, detections.size)
        // None are sustained (window size was 5 when the 5th in-tune hit; not full of size 6 yet)
        detections.forEach { assertFalse(it.isSustainedInTune) }
    }

    @Test
    fun `null after 5 in-tune — next in-tune frame sustains (5 of 6 window)`() = runTest {
        // 5 in-tune, 1 null, 1 in-tune → window = [T,T,T,T,F,T] = 5/6 → sustained on 7th call
        val frequencies = listOf(
            inTuneHz, inTuneHz, inTuneHz, inTuneHz, inTuneHz,
            null,
            inTuneHz,
        )
        val source = FakeAudioCaptureSource(List(7) { makeSamplesEvent() })
        val detector = FakePitchDetector(frequencies)
        val useCase = DetectTunedStringUseCase(source, detector)

        val detections = useCase.execute(presetInput).toList()
            .filterIsInstance<DetectionEvent.Detection>()

        // 6 detections (null frame skipped)
        assertEquals(6, detections.size)
        // 7th buffer = index 6 in detections (0-based) = detections[5]
        assertTrue("Detection after null should sustain (5 of 6)", detections[5].isSustainedInTune)
    }

    @Test
    fun `two consecutive nulls prevent sustained — window has at most 4 of 6`() = runTest {
        // 4 in-tune, 2 nulls → window = [T,T,T,T,F,F] = 4/6 → not sustained on next in-tune
        val frequencies = listOf(
            inTuneHz, inTuneHz, inTuneHz, inTuneHz,
            null, null,
            inTuneHz,
        )
        val source = FakeAudioCaptureSource(List(7) { makeSamplesEvent() })
        val detector = FakePitchDetector(frequencies)
        val useCase = DetectTunedStringUseCase(source, detector)

        val detections = useCase.execute(presetInput).toList()
            .filterIsInstance<DetectionEvent.Detection>()

        // 5 detections emitted (2 null frames skipped)
        assertEquals(5, detections.size)
        detections.forEach { assertFalse("No detection should be sustained", it.isSustainedInTune) }
    }

    // ── Chromatic mode ────────────────────────────────────────────────────────────

    @Test
    fun `chromatic mode resolves target via frequencyToNote`() = runTest {
        // G3 ≈ 196.0 Hz
        val g3 = Note(NoteName.G, 3)
        val g3Hz = g3.frequencyHz(refHz)

        val source = FakeAudioCaptureSource(listOf(makeSamplesEvent()))
        val detector = FakePitchDetector(listOf(g3Hz))
        val useCase = DetectTunedStringUseCase(source, detector)

        val events = useCase.execute(chromaticInput).toList()

        val detection = events.filterIsInstance<DetectionEvent.Detection>().single()
        assertEquals(g3, detection.targetNote)
        assertEquals(g3Hz, detection.targetFrequencyHz, 0.01)
        // Cents from G3 to G3 should be ~0
        assertTrue(abs(detection.centsOff) < 1.0)
    }

    @Test
    fun `chromatic with frequencyToNote returning null skips emission`() = runTest {
        // 0.5 Hz is outside MusicTheory range → frequencyToNote returns null
        val source = FakeAudioCaptureSource(listOf(makeSamplesEvent()))
        val detector = FakePitchDetector(listOf(0.5))
        val useCase = DetectTunedStringUseCase(source, detector)

        val events = useCase.execute(chromaticInput).toList()

        // No Detection event emitted for a frame where the chromatic target resolves to null.
        assertTrue(
            "Expected no DetectionEvent.Detection for out-of-range chromatic detection",
            events.filterIsInstance<DetectionEvent.Detection>().isEmpty(),
        )
    }

    // ── Raw cents not clamped ─────────────────────────────────────────────────────

    @Test
    fun `raw cents are not clamped — 100 cents flat emits -100`() = runTest {
        // 100 cents flat = exactly one semitone below targetNote
        val oneSemiBelowHz = targetHz * Math.pow(2.0, -100.0 / 1200.0)

        val source = FakeAudioCaptureSource(listOf(makeSamplesEvent()))
        val detector = FakePitchDetector(listOf(oneSemiBelowHz))
        val useCase = DetectTunedStringUseCase(source, detector)

        val detections = useCase.execute(presetInput).toList()
            .filterIsInstance<DetectionEvent.Detection>()

        assertEquals(1, detections.size)
        assertEquals(-100.0, detections[0].centsOff, 0.1)
    }

    // ── Displayed-value smoothing ─────────────────────────────────────────────────

    @Test
    fun `detectedFrequencyHz is a moving average of the last 2 valid detections`() = runTest {
        val f1 = targetHz - 3.0
        val f2 = targetHz
        val f3 = targetHz + 3.0

        val source = FakeAudioCaptureSource(List(3) { makeSamplesEvent() })
        val detector = FakePitchDetector(listOf(f1, f2, f3))
        val useCase = DetectTunedStringUseCase(source, detector)

        val detections = useCase.execute(presetInput).toList()
            .filterIsInstance<DetectionEvent.Detection>()

        assertEquals(3, detections.size)
        // Window fills incrementally: [f1], [f1,f2], then f1 is evicted (FIFO, size 2) → [f2,f3].
        assertEquals(f1, detections[0].detectedFrequencyHz, 0.001)
        assertEquals((f1 + f2) / 2.0, detections[1].detectedFrequencyHz, 0.001)
        assertEquals((f2 + f3) / 2.0, detections[2].detectedFrequencyHz, 0.001)
        // centsOff is derived from the same smoothed frequency, not the raw per-frame reading.
        val expectedCents = MusicTheory.centsBetween(targetHz, (f2 + f3) / 2.0)
        assertEquals(expectedCents, detections[2].centsOff, 0.001)
    }

    @Test
    fun `null detections are skipped from the frequency smoothing window, not just the sustained window`() = runTest {
        val f1 = targetHz - 3.0
        val f2 = targetHz + 3.0

        // f1, null, f2 — the null frame must not dilute/reset the frequency average.
        val source = FakeAudioCaptureSource(List(3) { makeSamplesEvent() })
        val detector = FakePitchDetector(listOf(f1, null, f2))
        val useCase = DetectTunedStringUseCase(source, detector)

        val detections = useCase.execute(presetInput).toList()
            .filterIsInstance<DetectionEvent.Detection>()

        // Only 2 Detection events (null frame emits nothing).
        assertEquals(2, detections.size)
        assertEquals(f1, detections[0].detectedFrequencyHz, 0.001)
        // If the null had reset the window, this would equal f2 alone instead.
        assertEquals((f1 + f2) / 2.0, detections[1].detectedFrequencyHz, 0.001)
    }

    // ── Target and detected note in PRESET mode ───────────────────────────────────

    @Test
    fun `in PRESET mode targetNote is always the preset note regardless of detected note`() = runTest {
        // Playing something sharp of the target — detected note may differ from target.
        val verySharpHz = targetHz * Math.pow(2.0, 200.0 / 1200.0) // 2 semitones sharp

        val source = FakeAudioCaptureSource(listOf(makeSamplesEvent()))
        val detector = FakePitchDetector(listOf(verySharpHz))
        val useCase = DetectTunedStringUseCase(source, detector)

        val detection = useCase.execute(presetInput).toList()
            .filterIsInstance<DetectionEvent.Detection>().single()

        assertEquals(targetNote, detection.targetNote)
        // detectedNote will be a different note (2 semitones up from E2 = F#2)
        assertTrue(detection.detectedNote != null)
        assertTrue(detection.detectedNote != targetNote)
    }
}
