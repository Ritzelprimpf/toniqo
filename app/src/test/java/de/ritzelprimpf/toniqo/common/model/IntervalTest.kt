package de.ritzelprimpf.toniqo.common.model

import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalTest {

    @Test
    fun `data class equality matches on semitone count`() {
        val a = Interval(semitones = 7)
        val b = Interval(semitones = 7)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `named intervals carry their canonical semitone counts`() {
        assertEquals(0, Interval.UNISON.semitones)
        assertEquals(1, Interval.MINOR_SECOND.semitones)
        assertEquals(2, Interval.MAJOR_SECOND.semitones)
        assertEquals(3, Interval.MINOR_THIRD.semitones)
        assertEquals(4, Interval.MAJOR_THIRD.semitones)
        assertEquals(5, Interval.PERFECT_FOURTH.semitones)
        assertEquals(6, Interval.TRITONE.semitones)
        assertEquals(7, Interval.PERFECT_FIFTH.semitones)
        assertEquals(8, Interval.MINOR_SIXTH.semitones)
        assertEquals(9, Interval.MAJOR_SIXTH.semitones)
        assertEquals(10, Interval.MINOR_SEVENTH.semitones)
        assertEquals(11, Interval.MAJOR_SEVENTH.semitones)
        assertEquals(12, Interval.OCTAVE.semitones)
    }
}
