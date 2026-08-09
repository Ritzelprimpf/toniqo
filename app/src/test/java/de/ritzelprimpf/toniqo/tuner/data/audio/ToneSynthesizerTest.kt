package de.ritzelprimpf.toniqo.tuner.data.audio

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToneSynthesizerTest {

    private val synthesizer = ToneSynthesizer()

    // E2 ≈ 82.41 Hz — a real tuner target, and low enough to only complete a handful of cycles
    // within the fade window, which is exactly the case worth exercising.
    private val frequencyHz = 82.41
    private val durationMs = 1_000L

    private val warmupSamples = (ToneParameters.WARMUP_MS * ToneAudioFormat.SAMPLE_RATE_HZ / 1000).toInt()
    private val expectedToneLength = (durationMs * ToneAudioFormat.SAMPLE_RATE_HZ / 1000).toInt()
    private val expectedLength = warmupSamples + expectedToneLength

    // -------------------------------------------------------------------------
    // Buffer length — WARMUP_MS of leading silence + the requested tone duration
    // -------------------------------------------------------------------------

    @Test
    fun `buffer length is warmup silence plus tone duration at the configured sample rate`() {
        assertEquals(expectedLength, synthesizer.generate(frequencyHz, durationMs).size)
    }

    @Test
    fun `buffer length scales with a different tone duration, warmup stays constant`() {
        val halfSecond = synthesizer.generate(frequencyHz, 500L)
        assertEquals(warmupSamples + expectedToneLength / 2, halfSecond.size)
    }

    // -------------------------------------------------------------------------
    // No clipping
    // -------------------------------------------------------------------------

    @Test
    fun `no sample exceeds plus or minus PCM16 full scale`() {
        val buf = synthesizer.generate(frequencyHz, durationMs)
        assertTrue(buf.all { it.toInt() in -ToneParameters.PCM16_FULL_SCALE..ToneParameters.PCM16_FULL_SCALE })
    }

    // -------------------------------------------------------------------------
    // Leading silence (cold-start "crack" absorption) + fade-in / fade-out envelope
    // -------------------------------------------------------------------------

    @Test
    fun `entire warmup region is exact silence`() {
        val buf = synthesizer.generate(frequencyHz, durationMs)
        assertTrue(buf.copyOfRange(0, warmupSamples).all { it == 0.toShort() })
    }

    @Test
    fun `first sample of the tone itself, right after warmup, is exactly zero (start of fade-in)`() {
        assertEquals(0.toShort(), synthesizer.generate(frequencyHz, durationMs)[warmupSamples])
    }

    @Test
    fun `last sample is at or near zero (end of fade-out)`() {
        val buf = synthesizer.generate(frequencyHz, durationMs)
        val peak = (ToneParameters.AMPLITUDE * ToneParameters.PCM16_FULL_SCALE).toInt()
        val threshold = peak * 0.02
        assertTrue(
            "last sample magnitude ${abs(buf.last().toInt())} should be < 2% of peak ($threshold)",
            abs(buf.last().toInt()) < threshold,
        )
    }

    @Test
    fun `mid-buffer sample can reach full envelope (outside the fade window)`() {
        val buf = synthesizer.generate(frequencyHz, durationMs)
        val peak = (ToneParameters.AMPLITUDE * ToneParameters.PCM16_FULL_SCALE).toInt()
        val observedPeak = buf.maxOf { abs(it.toInt()) }
        assertWithinTenPercent(peak, observedPeak)
    }

    // -------------------------------------------------------------------------
    // Soft-clip drive raises RMS (perceived loudness) above a pure sine's
    // -------------------------------------------------------------------------

    @Test
    fun `soft-clip drive raises RMS above a pure sine's inherent 70_7 percent of peak`() {
        val buf = synthesizer.generate(frequencyHz, durationMs)
        val peak = ToneParameters.AMPLITUDE * ToneParameters.PCM16_FULL_SCALE
        // Only the flat (post-warmup, post-fade, pre-fade-out) middle section reflects
        // steady-state RMS — the leading silence and the envelope ramps drag the average down
        // regardless of waveshaping.
        val fadeSamples = (ToneParameters.FADE_MS * ToneAudioFormat.SAMPLE_RATE_HZ / 1000).toInt()
        val steadyState = buf.slice(warmupSamples + fadeSamples until buf.size - fadeSamples)
        val rms = kotlin.math.sqrt(steadyState.map { (it.toDouble() / peak).let { s -> s * s } }.average())

        val pureSineRms = 1.0 / kotlin.math.sqrt(2.0) // ~0.707, the theoretical ceiling for DRIVE = 1 (no shaping)
        assertTrue(
            "shaped RMS $rms should exceed a pure sine's ~$pureSineRms (DRIVE = ${ToneParameters.DRIVE} should soften/boost it)",
            rms > pureSineRms,
        )
    }

    // -------------------------------------------------------------------------
    // Near-zero DC offset
    // -------------------------------------------------------------------------

    @Test
    fun `buffer has near-zero DC offset`() {
        val buf = synthesizer.generate(frequencyHz, durationMs)
        val mean = buf.map { it.toLong() }.sum().toDouble() / buf.size
        val limit = ToneParameters.PCM16_FULL_SCALE / 1000.0
        assertTrue("DC offset |$mean| should be < $limit", abs(mean) < limit)
    }

    // -------------------------------------------------------------------------
    // Deterministic output
    // -------------------------------------------------------------------------

    @Test
    fun `calling generate twice with the same arguments produces identical buffers`() {
        val first = synthesizer.generate(frequencyHz, durationMs)
        val second = synthesizer.generate(frequencyHz, durationMs)
        assertTrue(first.contentEquals(second))
    }

    @Test
    fun `different frequencies produce different buffers`() {
        val low = synthesizer.generate(frequencyHz, durationMs)
        val high = synthesizer.generate(frequencyHz * 2, durationMs)
        assertTrue(!low.contentEquals(high))
    }

    // -------------------------------------------------------------------------
    // Frequency correctness — zero-crossing count approximates the target frequency
    // -------------------------------------------------------------------------

    @Test
    fun `zero-crossing rate approximates the target frequency`() {
        val buf = synthesizer.generate(frequencyHz, durationMs)
        var crossings = 0
        for (i in 1 until buf.size) {
            if (buf[i - 1] < 0 && buf[i] >= 0) crossings++
        }
        // One upward zero-crossing per cycle; buffer spans durationMs/1000 seconds.
        val observedHz = crossings / (durationMs / 1000.0)
        assertTrue(
            "observed ~$observedHz Hz should be within 5% of target $frequencyHz Hz",
            abs(observedHz - frequencyHz) < frequencyHz * 0.05,
        )
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun assertWithinTenPercent(expected: Int, observed: Int) {
        val lower = expected * 0.90
        val upper = expected * 1.10
        assertTrue(
            "observed peak $observed should be within ±10% of expected $expected ([$lower, $upper])",
            observed >= lower && observed <= upper,
        )
    }
}
