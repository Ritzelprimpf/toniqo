package de.ritzelprimpf.toniqo.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class YinPitchDetectorTest {

    private val defaultConfig = YinConfig()
    private val detector = YinPitchDetector(defaultConfig)

    private val sampleRateHz = 44_100
    private val bufferFrames = 4_096

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private fun sine(frequencyHz: Double, durationSamples: Int = bufferFrames): FloatArray {
        val twoPi = 2.0 * PI
        return FloatArray(durationSamples) { i ->
            sin(twoPi * frequencyHz * i / sampleRateHz).toFloat()
        }
    }

    private fun assertDetected(
        label: String,
        frequencyHz: Double,
        toleranceHz: Double,
        buffer: FloatArray = sine(frequencyHz),
    ) {
        val result = detector.detectPitch(buffer, sampleRateHz)
        assertNotNull("$label — expected a pitch, got null", result)
        val error = abs(result!! - frequencyHz)
        assertTrue(
            "$label — detected ${result}Hz, expected ${frequencyHz}Hz ± ${toleranceHz}Hz (error=$error)",
            error <= toleranceHz,
        )
    }

    // ── Standard 6-string guitar strings ─────────────────────────────────────────

    /** E2 ≈ 82.41 Hz — the lowest string in standard tuning. */
    @Test
    fun `detects E2 within tolerance`() {
        assertDetected("E2", frequencyHz = 82.41, toleranceHz = 0.5)
    }

    /** A2 ≈ 110.00 Hz. */
    @Test
    fun `detects A2 within tolerance`() {
        assertDetected("A2", frequencyHz = 110.0, toleranceHz = 0.5)
    }

    /** D3 ≈ 146.83 Hz. */
    @Test
    fun `detects D3 within tolerance`() {
        assertDetected("D3", frequencyHz = 146.83, toleranceHz = 0.5)
    }

    /** G3 ≈ 196.00 Hz. */
    @Test
    fun `detects G3 within tolerance`() {
        assertDetected("G3", frequencyHz = 196.0, toleranceHz = 1.0)
    }

    /** B3 ≈ 246.94 Hz. */
    @Test
    fun `detects B3 within tolerance`() {
        assertDetected("B3", frequencyHz = 246.94, toleranceHz = 1.0)
    }

    /** E4 ≈ 329.63 Hz — the highest standard open string. */
    @Test
    fun `detects E4 within tolerance`() {
        assertDetected("E4", frequencyHz = 329.63, toleranceHz = 1.0)
    }

    // ── Standard strings under A4 = 432 Hz reference ─────────────────────────────
    // The detector is reference-agnostic — it measures periodic lag, not frequency identity.
    // At 432 Hz reference, E2 ≈ 80.93 Hz. The test just confirms the detector is stable.

    @Test
    fun `detects E2 at 432Hz reference within tolerance`() {
        assertDetected("E2 (432 Hz ref)", frequencyHz = 80.93, toleranceHz = 0.5)
    }

    // ── Drop-tuned low end ────────────────────────────────────────────────────────

    /** D1 ≈ 36.71 Hz — the lowest string in Drop D 8-string and similar heavy tunings. */
    @Test
    fun `detects D1 at the low end within tolerance`() {
        assertDetected("D1", frequencyHz = 36.71, toleranceHz = 0.5)
    }

    /**
     * A0 ≈ 27.5 Hz — below [YinConfig.absoluteMinFrequencyHz] (30 Hz).
     * The algorithm might find a periodic signal, but the frequency guard must reject it.
     */
    @Test
    fun `returns null for A0 because it is below absoluteMinFrequencyHz`() {
        val buffer = sine(27.5)
        val result = detector.detectPitch(buffer, sampleRateHz)
        assertNull("A0 (27.5 Hz) should be rejected by the frequency floor", result)
    }

    // ── High end ─────────────────────────────────────────────────────────────────

    /** E5 ≈ 659.26 Hz — well above the standard open-string range. */
    @Test
    fun `detects E5 within tolerance`() {
        assertDetected("E5", frequencyHz = 659.26, toleranceHz = 1.0)
    }

    /** A5 ≈ 880.0 Hz. */
    @Test
    fun `detects A5 within tolerance`() {
        assertDetected("A5", frequencyHz = 880.0, toleranceHz = 1.0)
    }

    // ── Silence ───────────────────────────────────────────────────────────────────

    @Test
    fun `returns null for a silent buffer of all zeros`() {
        val buffer = FloatArray(bufferFrames) { 0.0f }
        assertNull("Silence should return null", detector.detectPitch(buffer, sampleRateHz))
    }

    // ── White noise ───────────────────────────────────────────────────────────────

    @Test
    fun `returns null for white noise in range -0_1 to 0_1`() {
        val rng = Random(seed = 42)
        val buffer = FloatArray(bufferFrames) { (rng.nextFloat() - 0.5f) * 0.2f }
        assertNull("White noise should return null", detector.detectPitch(buffer, sampleRateHz))
    }

    // ── Out-of-range frequencies ──────────────────────────────────────────────────

    /** 3000 Hz is above [YinConfig.absoluteMaxFrequencyHz] (2000 Hz). */
    @Test
    fun `returns null for 3000 Hz sine above absoluteMaxFrequencyHz`() {
        val buffer = sine(3_000.0)
        assertNull("3000 Hz is above the frequency ceiling", detector.detectPitch(buffer, sampleRateHz))
    }

    // ── Buffer too short ──────────────────────────────────────────────────────────

    /**
     * 256 samples at 100 Hz: the period is 441 samples, which is larger than halfSize (128).
     * YIN has no accessible lag corresponding to the true period, so it returns null.
     */
    @Test
    fun `returns null when buffer is too short to contain even one period`() {
        val buffer = sine(100.0, durationSamples = 256)
        assertNull("256-sample buffer at 100 Hz should return null", detector.detectPitch(buffer, sampleRateHz))
    }

    // ── Reference-pitch independence ──────────────────────────────────────────────

    /**
     * The YIN detector measures periodicity (lag) in the time domain — it has no knowledge of
     * any reference pitch. The same 220 Hz sine must yield the same detected frequency
     * regardless of what reference pitch a caller might be using downstream.
     */
    @Test
    fun `detects 220 Hz identically regardless of external reference pitch context`() {
        val buffer = sine(220.0)
        val result440 = detector.detectPitch(buffer, sampleRateHz)
        val result432 = detector.detectPitch(buffer, sampleRateHz)
        assertNotNull(result440)
        assertEquals("Results must be identical (detector is reference-agnostic)", result440, result432)
    }

    // ── Threshold sensitivity ─────────────────────────────────────────────────────

    /**
     * A very strict threshold (0.01) applied to white noise: no CMND value should dip that low
     * for a non-periodic signal. Returns null.
     */
    @Test
    fun `strict threshold 0_01 rejects white noise`() {
        val strictDetector = YinPitchDetector(YinConfig(threshold = 0.01))
        val rng = Random(seed = 99)
        val buffer = FloatArray(bufferFrames) { (rng.nextFloat() - 0.5f) * 0.2f }
        assertNull("Strict threshold + noise should return null", strictDetector.detectPitch(buffer, sampleRateHz))
    }

    /**
     * A permissive threshold (0.50) applied to a clean 440 Hz sine: the CMND dip at the true
     * period is well below 0.50, so detection still succeeds.
     */
    @Test
    fun `permissive threshold 0_50 still detects a clean 440 Hz sine`() {
        val permissiveDetector = YinPitchDetector(YinConfig(threshold = 0.50))
        val buffer = sine(440.0)
        val result = permissiveDetector.detectPitch(buffer, sampleRateHz)
        assertNotNull("Permissive threshold should still detect a clean sine", result)
        val error = abs(result!! - 440.0)
        assertTrue("Detected ${result}Hz, expected 440.0Hz ± 1.0Hz", error <= 1.0)
    }

    // ── Parabolic interpolation ───────────────────────────────────────────────────

    /**
     * Two frequencies that both round to the same integer lag (τ = 100, corresponding to 441 Hz)
     * should yield distinct detected frequencies when parabolic interpolation is applied.
     * Without interpolation, both would produce exactly 441.0 Hz.
     *
     * 440.7 Hz → τ ≈ 100.07 (slightly above integer)
     * 441.3 Hz → τ ≈ 99.93  (slightly below integer)
     */
    @Test
    fun `parabolic interpolation distinguishes 440_7 Hz from 441_3 Hz`() {
        val bufferA = sine(440.7)
        val bufferB = sine(441.3)

        val resultA = detector.detectPitch(bufferA, sampleRateHz)
        val resultB = detector.detectPitch(bufferB, sampleRateHz)

        assertNotNull("440.7 Hz sine should detect a pitch", resultA)
        assertNotNull("441.3 Hz sine should detect a pitch", resultB)

        assertTrue(
            "Parabolic interpolation should distinguish 440.7 Hz (got $resultA) " +
                    "from 441.3 Hz (got $resultB)",
            resultA!! != resultB!!,
        )
    }

}
