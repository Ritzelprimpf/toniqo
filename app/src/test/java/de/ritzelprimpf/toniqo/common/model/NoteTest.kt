package de.ritzelprimpf.toniqo.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteTest {

    // ── Data-class contract ───────────────────────────────────────────────────────

    @Test
    fun `data class equality holds for identical name and octave`() {
        assertEquals(Note(NoteName.A, 4), Note(NoteName.A, 4))
    }

    @Test
    fun `notes differ when octave differs`() {
        assertNotEquals(Note(NoteName.A, 4), Note(NoteName.A, 5))
    }

    @Test
    fun `notes differ when pitch class differs`() {
        assertNotEquals(Note(NoteName.A, 4), Note(NoteName.B, 4))
    }

    // ── frequencyHz ───────────────────────────────────────────────────────────────

    @Test
    fun `A4 at 440 Hz reference equals 440 Hz`() {
        assertEquals(440.0, Note(NoteName.A, 4).frequencyHz(), 0.01)
    }

    @Test
    fun `A3 is one octave below A4`() {
        assertEquals(220.0, Note(NoteName.A, 3).frequencyHz(), 0.01)
    }

    @Test
    fun `C4 equals 261_63 Hz`() {
        assertEquals(261.6256, Note(NoteName.C, 4).frequencyHz(), 0.01)
    }

    @Test
    fun `E2 equals 82_41 Hz`() {
        assertEquals(82.4069, Note(NoteName.E, 2).frequencyHz(), 0.01)
    }

    @Test
    fun `E4 equals 329_63 Hz`() {
        assertEquals(329.6276, Note(NoteName.E, 4).frequencyHz(), 0.01)
    }

    @Test
    fun `A4 at 432 Hz reference equals 432 Hz`() {
        assertEquals(432.0, Note(NoteName.A, 4).frequencyHz(referencePitchHz = 432.0), 0.01)
    }

    @Test
    fun `B3 is one semitone below C4`() {
        val b3 = Note(NoteName.B, 3).frequencyHz()
        val c4 = Note(NoteName.C, 4).frequencyHz()
        // One semitone = ratio 2^(1/12); B3 * 2^(1/12) should equal C4.
        assertEquals(c4, b3 * Math.pow(2.0, 1.0 / 12.0), 0.01)
    }

    // ── displayName ───────────────────────────────────────────────────────────────

    @Test
    fun `displayName returns sharp-spelled name by default`() {
        assertEquals("A4", Note(NoteName.A, 4).displayName())
        assertEquals("C#4", Note(NoteName.CSharp, 4).displayName())
        assertEquals("E2", Note(NoteName.E, 2).displayName())
    }

    @Test
    fun `displayName with useFlats returns flat-spelled name`() {
        assertEquals("Db4", Note(NoteName.CSharp, 4).displayName(useFlats = true))
        assertEquals("Bb3", Note(NoteName.ASharp, 3).displayName(useFlats = true))
        assertEquals("A4", Note(NoteName.A, 4).displayName(useFlats = true))
    }

    // ── parse ─────────────────────────────────────────────────────────────────────

    @Test
    fun `parse round-trips with displayName for natural notes`() {
        listOf("C4", "D3", "E2", "G5", "A4", "B1").forEach { str ->
            val note = Note.parse(str)!!
            assertEquals("round-trip for $str", str, note.displayName())
        }
    }

    @Test
    fun `parse round-trips with displayName for sharp notes`() {
        listOf("C#4", "F#2", "G#3", "A#1").forEach { str ->
            val note = Note.parse(str)!!
            assertEquals("round-trip for $str", str, note.displayName())
        }
    }

    @Test
    fun `parse accepts flat spellings`() {
        assertEquals(Note(NoteName.CSharp, 4), Note.parse("Db4"))
        assertEquals(Note(NoteName.DSharp, 2), Note.parse("Eb2"))
        assertEquals(Note(NoteName.ASharp, 1), Note.parse("Bb1"))
        assertEquals(Note(NoteName.FSharp, 3), Note.parse("Gb3"))
        assertEquals(Note(NoteName.GSharp, 2), Note.parse("Ab2"))
    }

    @Test
    fun `parse returns null for invalid input`() {
        assertNull(Note.parse(""))
        assertNull(Note.parse("X4"))
        assertNull(Note.parse("A"))       // no octave
        assertNull(Note.parse("4A"))      // wrong order
        assertNull(Note.parse("C##4"))
    }
}
