package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalMathTest {

    @Test
    fun `samplesPerClick at 120 bpm 4 denominator with no subdivision returns half the sample rate`() {
        assertEquals(24_000.0, samplesPerClick(120, Subdivision.NONE, denominator = 4, sampleRateHz = 48_000), 0.0)
    }

    @Test
    fun `samplesPerClick at 120 bpm 4 denominator with eighths returns a quarter of the sample rate`() {
        assertEquals(12_000.0, samplesPerClick(120, Subdivision.EIGHTHS, denominator = 4, sampleRateHz = 48_000), 0.0)
    }

    @Test
    fun `samplesPerClick at 120 bpm 4 denominator with sixteenths returns an eighth of the sample rate`() {
        assertEquals(6_000.0, samplesPerClick(120, Subdivision.SIXTEENTHS, denominator = 4, sampleRateHz = 48_000), 0.0)
    }

    @Test
    fun `samplesPerClick at 120 bpm 4 denominator with triplets divides the sample rate by six`() {
        assertEquals(8_000.0, samplesPerClick(120, Subdivision.TRIPLETS, denominator = 4, sampleRateHz = 48_000), 0.0)
    }

    @Test
    fun `samplesPerClick at 60 bpm 4 denominator with no subdivision equals the full sample rate`() {
        assertEquals(48_000.0, samplesPerClick(60, Subdivision.NONE, denominator = 4, sampleRateHz = 48_000), 0.0)
    }

    @Test
    fun `samplesPerClick at 1 bpm 4 denominator with no subdivision equals sixty times the sample rate`() {
        assertEquals(2_880_000.0, samplesPerClick(1, Subdivision.NONE, denominator = 4, sampleRateHz = 48_000), 0.0)
    }

    @Test
    fun `samplesPerClick at 300 bpm 4 denominator with no subdivision returns a fifth of the sample rate`() {
        assertEquals(9_600.0, samplesPerClick(300, Subdivision.NONE, denominator = 4, sampleRateHz = 48_000), 0.0)
    }

    @Test
    fun `samplesPerClick decreases as bpm increases`() {
        val slow = samplesPerClick(60, Subdivision.NONE, denominator = 4, sampleRateHz = 48_000)
        val fast = samplesPerClick(120, Subdivision.NONE, denominator = 4, sampleRateHz = 48_000)

        assert(fast < slow)
    }

    @Test
    fun `samplesPerClick decreases as subdivision multiplier increases`() {
        val noSub = samplesPerClick(120, Subdivision.NONE, denominator = 4, sampleRateHz = 48_000)
        val eighths = samplesPerClick(120, Subdivision.EIGHTHS, denominator = 4, sampleRateHz = 48_000)
        val sixteenths = samplesPerClick(120, Subdivision.SIXTEENTHS, denominator = 4, sampleRateHz = 48_000)

        assert(eighths < noSub)
        assert(sixteenths < eighths)
    }

    @Test
    fun `samplesPerClick scales linearly with sample rate`() {
        val at48k = samplesPerClick(120, Subdivision.NONE, denominator = 4, sampleRateHz = 48_000)
        val at96k = samplesPerClick(120, Subdivision.NONE, denominator = 4, sampleRateHz = 96_000)

        assertEquals(at48k * 2, at96k, 0.0)
    }

    @Test
    fun `samplesPerClick at an odd bpm is fractional, not truncated`() {
        // 48000 * 60 / 117 = 24615.384... — must NOT be silently floored by the helper itself;
        // BeatScheduler relies on this fractional precision to avoid cumulative drift.
        val result = samplesPerClick(117, Subdivision.NONE, denominator = 4, sampleRateHz = 48_000)

        assertEquals(24_615.384615, result, 0.001)
    }

    // ── Denominator scaling ─────────────────────────────────────────────────────

    @Test
    fun `denominator 4 is a no-op reference — same result as the old denominator-less formula`() {
        assertEquals(24_000.0, samplesPerClick(120, Subdivision.NONE, denominator = 4, sampleRateHz = 48_000), 0.0)
    }

    @Test
    fun `denominator 8 clicks exactly twice as fast as denominator 4 at the same bpm`() {
        val quarterTime = samplesPerClick(120, Subdivision.NONE, denominator = 4, sampleRateHz = 48_000)
        val eighthTime = samplesPerClick(120, Subdivision.NONE, denominator = 8, sampleRateHz = 48_000)

        assertEquals(quarterTime / 2, eighthTime, 0.0)
    }

    @Test
    fun `denominator 2 clicks exactly half as fast as denominator 4 at the same bpm`() {
        val quarterTime = samplesPerClick(120, Subdivision.NONE, denominator = 4, sampleRateHz = 48_000)
        val halfTime = samplesPerClick(120, Subdivision.NONE, denominator = 2, sampleRateHz = 48_000)

        assertEquals(quarterTime * 2, halfTime, 0.0)
    }

    @Test
    fun `denominator scaling and subdivision multiplier compose`() {
        // 12/8 with EIGHTHS subdivision: denominator doubles the beat rate, subdivision doubles
        // it again — four times as fast as a plain 4/4 at the same BPM.
        val plain4over4 = samplesPerClick(120, Subdivision.NONE, denominator = 4, sampleRateHz = 48_000)
        val eighthsIn12over8 = samplesPerClick(120, Subdivision.EIGHTHS, denominator = 8, sampleRateHz = 48_000)

        assertEquals(plain4over4 / 4, eighthsIn12over8, 0.0)
    }
}
