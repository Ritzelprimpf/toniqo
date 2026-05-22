package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.fakes.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TapTempoCalculatorTest {

    private val clock = FakeClock(initialNanos = 0L)
    private lateinit var calculator: TapTempoCalculator

    @Before
    fun setUp() {
        calculator = TapTempoCalculator(clock)
    }

    /** Convenience: advance the clock by [ms] milliseconds then call onTap(). */
    private fun tapAfterMs(ms: Long): Int? {
        clock.advanceBy(ms * 1_000_000L)
        return calculator.onTap()
    }

    // ── Single tap ────────────────────────────────────────────────────────────

    @Test
    fun `first tap returns null because no interval exists yet`() {
        assertNull(calculator.onTap())
    }

    // ── Two-tap BPM calculation ───────────────────────────────────────────────

    @Test
    fun `two taps at 500ms interval gives 120 bpm`() {
        calculator.onTap()
        assertEquals(120, tapAfterMs(500))
    }

    @Test
    fun `two taps at 1000ms interval gives 60 bpm`() {
        calculator.onTap()
        assertEquals(60, tapAfterMs(1000))
    }

    @Test
    fun `two taps at 250ms interval gives 240 bpm`() {
        calculator.onTap()
        assertEquals(240, tapAfterMs(250))
    }

    // ── Rolling window average ────────────────────────────────────────────────

    @Test
    fun `multiple taps at regular 500ms interval gives stable 120 bpm`() {
        calculator.onTap()
        tapAfterMs(500)
        tapAfterMs(500)
        tapAfterMs(500)
        assertEquals(120, tapAfterMs(500))
    }

    @Test
    fun `rolling window drops oldest tap when six taps received`() {
        // Fill window: 5 taps at t = [0, 500, 1000, 1500, 2000] ms
        calculator.onTap()
        repeat(4) { tapAfterMs(500) }
        // 6th tap at t = 3000 ms (1000 ms after the 5th).
        // Oldest tap (t=0) is dropped; window = [500, 1000, 1500, 2000, 3000].
        // Intervals: [500, 500, 500, 1000], mean = 625 ms → round(60000/625) = 96 bpm.
        assertEquals(96, tapAfterMs(1000))
    }

    // ── Reset on timeout ──────────────────────────────────────────────────────

    @Test
    fun `tap after 2 second gap resets window and returns null`() {
        calculator.onTap()
        tapAfterMs(500)  // 120 bpm

        assertNull(tapAfterMs(2001)) // gap > 2 s → reset → single tap in window → null
    }

    @Test
    fun `new session begins cleanly after a timeout reset`() {
        calculator.onTap()
        tapAfterMs(500)
        tapAfterMs(2001) // reset, returns null

        // Next two taps form a fresh session: 250 ms apart → 240 bpm.
        assertEquals(240, tapAfterMs(250))
    }

    // ── BPM clamping ─────────────────────────────────────────────────────────

    @Test
    fun `very short tap interval is clamped to BPM_MAX`() {
        calculator.onTap()
        // 1 ms → 60 000 bpm, clamped to BPM_MAX (300).
        assertEquals(MetronomeConfig.BPM_MAX, tapAfterMs(1))
    }

    // ── reset() method ────────────────────────────────────────────────────────

    @Test
    fun `reset clears the window so the next tap returns null`() {
        calculator.onTap()
        tapAfterMs(500)

        calculator.reset()

        assertNull(calculator.onTap())
    }

    @Test
    fun `after reset two new taps establish a fresh session at the new tempo`() {
        repeat(5) { tapAfterMs(500) } // fill window at 120 bpm
        calculator.reset()

        calculator.onTap()
        // 300 ms apart → 200 bpm.
        assertEquals(200, tapAfterMs(300))
    }
}
