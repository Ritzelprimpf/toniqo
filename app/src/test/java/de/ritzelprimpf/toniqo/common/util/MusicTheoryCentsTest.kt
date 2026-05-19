package de.ritzelprimpf.toniqo.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicTheoryCentsTest {

    @Test
    fun `centsBetween returns 0 when frequencies are equal`() {
        assertEquals(0.0, MusicTheory.centsBetween(440.0, 440.0), 0.001)
    }

    @Test
    fun `centsBetween returns 100 for one semitone sharp`() {
        // A#4 / Bb4 = 466.16 Hz is one semitone above A4 = 440 Hz.
        assertEquals(100.0, MusicTheory.centsBetween(440.0, 466.16), 0.5)
    }

    @Test
    fun `centsBetween returns minus 100 for one semitone flat`() {
        assertEquals(-100.0, MusicTheory.centsBetween(466.16, 440.0), 0.5)
    }

    @Test
    fun `centsBetween returns 1200 for one octave sharp`() {
        assertEquals(1200.0, MusicTheory.centsBetween(220.0, 440.0), 0.001)
    }

    @Test
    fun `centsBetween returns minus 1200 for one octave flat`() {
        assertEquals(-1200.0, MusicTheory.centsBetween(440.0, 220.0), 0.001)
    }

    @Test
    fun `centsBetween is symmetric — swapping args negates the result`() {
        val ref = 440.0
        val detected = 450.0
        assertEquals(
            -MusicTheory.centsBetween(ref, detected),
            MusicTheory.centsBetween(detected, ref),
            0.001,
        )
    }

    @Test
    fun `centsBetween of 5 cents is between plus 4 and plus 6`() {
        // 5 cents sharp: detected = reference * 2^(5/1200)
        val detected = 440.0 * Math.pow(2.0, 5.0 / 1200.0)
        val cents = MusicTheory.centsBetween(440.0, detected)
        assertEquals(5.0, cents, 0.01)
    }

    @Test
    fun `centsBetween of minus 5 cents is near minus 5`() {
        val detected = 440.0 * Math.pow(2.0, -5.0 / 1200.0)
        assertEquals(-5.0, MusicTheory.centsBetween(440.0, detected), 0.01)
    }

    @Test
    fun `centsBetween of 50 cents is near 50`() {
        val detected = 440.0 * Math.pow(2.0, 50.0 / 1200.0)
        assertEquals(50.0, MusicTheory.centsBetween(440.0, detected), 0.1)
    }
}
