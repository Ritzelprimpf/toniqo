package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalMathTest {

    @Test
    fun `NANOS_PER_MINUTE equals 60 billion`() {
        assertEquals(60_000_000_000L, NANOS_PER_MINUTE)
    }

    @Test
    fun `NANOS_PER_MS equals one million`() {
        assertEquals(1_000_000L, NANOS_PER_MS)
    }

    @Test
    fun `intervalNanos at 120 bpm with no subdivision returns 500ms`() {
        assertEquals(500_000_000L, intervalNanos(120, Subdivision.NONE))
    }

    @Test
    fun `intervalNanos at 120 bpm with eighths returns 250ms`() {
        assertEquals(250_000_000L, intervalNanos(120, Subdivision.EIGHTHS))
    }

    @Test
    fun `intervalNanos at 120 bpm with sixteenths returns 125ms`() {
        assertEquals(125_000_000L, intervalNanos(120, Subdivision.SIXTEENTHS))
    }

    @Test
    fun `intervalNanos at 120 bpm with triplets returns one sixth of a second`() {
        // 60_000_000_000 / 120 / 3 = 166_666_666 (integer division)
        assertEquals(166_666_666L, intervalNanos(120, Subdivision.TRIPLETS))
    }

    @Test
    fun `intervalNanos at 60 bpm with no subdivision returns 1 second`() {
        assertEquals(1_000_000_000L, intervalNanos(60, Subdivision.NONE))
    }

    @Test
    fun `intervalNanos at 60 bpm with triplets returns one third of a second`() {
        // 60_000_000_000 / 60 / 3 = 333_333_333 (integer division)
        assertEquals(333_333_333L, intervalNanos(60, Subdivision.TRIPLETS))
    }

    @Test
    fun `intervalNanos at 300 bpm with no subdivision returns 200ms`() {
        assertEquals(200_000_000L, intervalNanos(300, Subdivision.NONE))
    }

    @Test
    fun `intervalNanos at 1 bpm with no subdivision returns 60 seconds`() {
        assertEquals(60_000_000_000L, intervalNanos(1, Subdivision.NONE))
    }

    @Test
    fun `intervalNanos decreases as bpm increases`() {
        val slow = intervalNanos(60, Subdivision.NONE)
        val fast = intervalNanos(120, Subdivision.NONE)

        assert(slow > fast) { "Interval at 60 bpm ($slow) should be longer than at 120 bpm ($fast)" }
    }

    @Test
    fun `intervalNanos decreases as subdivision multiplier increases`() {
        val noSub = intervalNanos(120, Subdivision.NONE)
        val eighths = intervalNanos(120, Subdivision.EIGHTHS)
        val sixteenths = intervalNanos(120, Subdivision.SIXTEENTHS)

        assert(noSub > eighths) { "NONE interval should be longer than EIGHTHS" }
        assert(eighths > sixteenths) { "EIGHTHS interval should be longer than SIXTEENTHS" }
    }
}
