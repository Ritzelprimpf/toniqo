package de.ritzelprimpf.toniqo.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ScaleTest {

    private val cMajor = Scale(
        root = Note(NoteName.C, octave = 4),
        intervals = Mode.IONIAN.intervalsFromRoot,
    )

    @Test
    fun `equality depends on root and intervals only`() {
        val twin = Scale(
            root = Note(NoteName.C, octave = 4),
            intervals = Mode.IONIAN.intervalsFromRoot,
        )

        assertEquals(cMajor, twin)
        assertEquals(cMajor.hashCode(), twin.hashCode())
    }

    @Test
    fun `scales differ when intervals differ`() {
        val cMinor = Scale(
            root = Note(NoteName.C, octave = 4),
            intervals = Mode.AEOLIAN.intervalsFromRoot,
        )

        assertNotEquals(cMajor, cMinor)
    }

    @Test
    fun `notes is derived and unimplemented in Phase 2`() {
        assertThrows(NotImplementedError::class.java) { cMajor.notes }
    }
}
