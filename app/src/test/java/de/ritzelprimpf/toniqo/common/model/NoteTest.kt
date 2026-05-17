package de.ritzelprimpf.toniqo.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NoteTest {

    @Test
    fun `data class equality holds for identical name and octave`() {
        val a = Note(NoteName.A, octave = 4)
        val b = Note(NoteName.A, octave = 4)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `notes differ when octave differs`() {
        val a4 = Note(NoteName.A, octave = 4)
        val a5 = Note(NoteName.A, octave = 5)

        assertNotEquals(a4, a5)
    }

    @Test
    fun `notes differ when pitch class differs`() {
        val a4 = Note(NoteName.A, octave = 4)
        val b4 = Note(NoteName.B, octave = 4)

        assertNotEquals(a4, b4)
    }

    @Test
    fun `frequencyHz throws because Phase 2 leaves it unimplemented`() {
        val note = Note(NoteName.A, octave = 4)

        assertThrows(NotImplementedError::class.java) { note.frequencyHz() }
    }
}
