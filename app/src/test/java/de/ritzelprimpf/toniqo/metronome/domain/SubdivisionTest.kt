package de.ritzelprimpf.toniqo.metronome.domain

import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import org.junit.Assert.assertEquals
import org.junit.Test

class SubdivisionTest {

    @Test
    fun `NONE multiplier is 1`() {
        assertEquals(1, Subdivision.NONE.multiplier)
    }

    @Test
    fun `EIGHTHS multiplier is 2`() {
        assertEquals(2, Subdivision.EIGHTHS.multiplier)
    }

    @Test
    fun `SIXTEENTHS multiplier is 4`() {
        assertEquals(4, Subdivision.SIXTEENTHS.multiplier)
    }

    @Test
    fun `TRIPLETS multiplier is 3`() {
        assertEquals(3, Subdivision.TRIPLETS.multiplier)
    }
}
