package de.ritzelprimpf.toniqo.common.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeTest {

    @Test
    fun `all seven diatonic modes are defined`() {
        val expected = setOf("IONIAN", "DORIAN", "PHRYGIAN", "LYDIAN", "MIXOLYDIAN", "AEOLIAN", "LOCRIAN")
        assertEquals(expected, Mode.entries.map { it.name }.toSet())
    }

    @Test
    fun `every mode has exactly 7 interval values`() {
        Mode.entries.forEach { mode ->
            assertEquals("${mode.name} should have 7 scale degrees", 7, mode.intervalsFromRoot.size)
        }
    }

    @Test
    fun `every mode starts on the unison (offset 0)`() {
        Mode.entries.forEach { mode ->
            assertEquals("${mode.name} first offset should be 0", 0, mode.intervalsFromRoot[0])
        }
    }

    @Test
    fun `every mode's intervals are strictly ascending`() {
        Mode.entries.forEach { mode ->
            val intervals = mode.intervalsFromRoot
            for (i in 1 until intervals.size) {
                assertTrue(
                    "${mode.name}: interval[$i]=${intervals[i]} should be > interval[${i-1}]=${intervals[i-1]}",
                    intervals[i] > intervals[i - 1],
                )
            }
        }
    }

    @Test
    fun `Ionian pattern matches the major scale`() {
        assertArrayEquals(intArrayOf(0, 2, 4, 5, 7, 9, 11), Mode.IONIAN.intervalsFromRoot)
    }

    @Test
    fun `Dorian pattern is correct`() {
        assertArrayEquals(intArrayOf(0, 2, 3, 5, 7, 9, 10), Mode.DORIAN.intervalsFromRoot)
    }

    @Test
    fun `Phrygian pattern is correct`() {
        assertArrayEquals(intArrayOf(0, 1, 3, 5, 7, 8, 10), Mode.PHRYGIAN.intervalsFromRoot)
    }

    @Test
    fun `Lydian pattern is correct`() {
        assertArrayEquals(intArrayOf(0, 2, 4, 6, 7, 9, 11), Mode.LYDIAN.intervalsFromRoot)
    }

    @Test
    fun `Mixolydian pattern is correct`() {
        assertArrayEquals(intArrayOf(0, 2, 4, 5, 7, 9, 10), Mode.MIXOLYDIAN.intervalsFromRoot)
    }

    @Test
    fun `Aeolian pattern matches the natural minor scale`() {
        assertArrayEquals(intArrayOf(0, 2, 3, 5, 7, 8, 10), Mode.AEOLIAN.intervalsFromRoot)
    }

    @Test
    fun `Locrian pattern is correct`() {
        assertArrayEquals(intArrayOf(0, 1, 3, 5, 6, 8, 10), Mode.LOCRIAN.intervalsFromRoot)
    }

    @Test
    fun `Ionian displayName contains Major`() {
        assertTrue(Mode.IONIAN.displayName.contains("Major"))
    }

    @Test
    fun `Aeolian displayName contains Natural Minor`() {
        assertTrue(Mode.AEOLIAN.displayName.contains("Natural Minor"))
    }

    @Test
    fun `every mode has a non-blank display name`() {
        Mode.entries.forEach { mode ->
            assertTrue("${mode.name} should have a display name", mode.displayName.isNotBlank())
        }
    }
}
