package de.ritzelprimpf.toniqo.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeTest {

    @Test
    fun `all seven diatonic modes are defined`() {
        val expectedNames = setOf("IONIAN", "DORIAN", "PHRYGIAN", "LYDIAN", "MIXOLYDIAN", "AEOLIAN", "LOCRIAN")
        val actualNames = Mode.entries.map { it.name }.toSet()

        assertEquals(expectedNames, actualNames)
    }

    @Test
    fun `every mode's interval pattern starts at unison and ends at octave`() {
        for (mode in Mode.entries) {
            assertEquals(
                "${mode.name} should start on UNISON",
                Interval.UNISON,
                mode.intervalsFromRoot.first(),
            )
            assertEquals(
                "${mode.name} should end on OCTAVE",
                Interval.OCTAVE,
                mode.intervalsFromRoot.last(),
            )
        }
    }

    @Test
    fun `every mode has a non-blank display name`() {
        for (mode in Mode.entries) {
            assertTrue("${mode.name} should have a display name", mode.displayName.isNotBlank())
        }
    }
}
