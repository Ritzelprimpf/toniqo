package de.ritzelprimpf.toniqo.common.util

import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicTheoryFrequencyTest {

    // ── noteToFrequency ───────────────────────────────────────────────────────────

    @Test
    fun `noteToFrequency A4 equals 440 Hz`() {
        assertEquals(440.0, MusicTheory.noteToFrequency(Note(NoteName.A, 4)), 0.01)
    }

    @Test
    fun `noteToFrequency A3 equals 220 Hz`() {
        assertEquals(220.0, MusicTheory.noteToFrequency(Note(NoteName.A, 3)), 0.01)
    }

    @Test
    fun `noteToFrequency C4 equals 261_63 Hz`() {
        assertEquals(261.6256, MusicTheory.noteToFrequency(Note(NoteName.C, 4)), 0.01)
    }

    @Test
    fun `noteToFrequency E2 equals 82_41 Hz`() {
        assertEquals(82.4069, MusicTheory.noteToFrequency(Note(NoteName.E, 2)), 0.01)
    }

    @Test
    fun `noteToFrequency E4 equals 329_63 Hz`() {
        assertEquals(329.6276, MusicTheory.noteToFrequency(Note(NoteName.E, 4)), 0.01)
    }

    @Test
    fun `noteToFrequency at 432 Hz reference produces A4 equals 432 Hz`() {
        assertEquals(432.0, MusicTheory.noteToFrequency(Note(NoteName.A, 4), referencePitchHz = 432.0), 0.01)
    }

    // ── frequencyToNote ───────────────────────────────────────────────────────────

    @Test
    fun `frequencyToNote 440 Hz returns A4`() {
        assertEquals(Note(NoteName.A, 4), MusicTheory.frequencyToNote(440.0))
    }

    @Test
    fun `frequencyToNote 220 Hz returns A3`() {
        assertEquals(Note(NoteName.A, 3), MusicTheory.frequencyToNote(220.0))
    }

    @Test
    fun `frequencyToNote 261_63 Hz returns C4`() {
        assertEquals(Note(NoteName.C, 4), MusicTheory.frequencyToNote(261.63))
    }

    @Test
    fun `frequencyToNote uses sharp spelling for accidentals`() {
        // C# / Db is at 277.18 Hz
        val note = MusicTheory.frequencyToNote(277.18)!!
        assertEquals(NoteName.CSharp, note.name)
    }

    @Test
    fun `frequencyToNote at 432 Hz reference returns A4`() {
        assertEquals(Note(NoteName.A, 4), MusicTheory.frequencyToNote(432.0, referencePitchHz = 432.0))
    }

    @Test
    fun `noteToFrequency and frequencyToNote round-trip for all guitar strings`() {
        // Spot-check a representative set of guitar-range notes.
        val notes = listOf(
            Note(NoteName.E, 2), Note(NoteName.A, 2), Note(NoteName.D, 3),
            Note(NoteName.G, 3), Note(NoteName.B, 3), Note(NoteName.E, 4),
            Note(NoteName.B, 1), Note(NoteName.A, 1), Note(NoteName.FSharp, 1),
        )
        notes.forEach { original ->
            val freq = MusicTheory.noteToFrequency(original)
            val recovered = MusicTheory.frequencyToNote(freq)
            assertEquals("round-trip for ${original.displayName()}", original, recovered)
        }
    }

    // ── Invalid inputs ────────────────────────────────────────────────────────────

    @Test
    fun `frequencyToNote returns null for zero`() {
        assertNull(MusicTheory.frequencyToNote(0.0))
    }

    @Test
    fun `frequencyToNote returns null for negative frequency`() {
        assertNull(MusicTheory.frequencyToNote(-100.0))
    }

    @Test
    fun `frequencyToNote returns null for NaN`() {
        assertNull(MusicTheory.frequencyToNote(Double.NaN))
    }

    @Test
    fun `frequencyToNote returns null for positive infinity`() {
        assertNull(MusicTheory.frequencyToNote(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `frequencyToNote returns null for negative infinity`() {
        assertNull(MusicTheory.frequencyToNote(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `frequencyToNote returns null for sub-audible frequency outside C0-B9 range`() {
        // C0 ≈ 16.35 Hz. A frequency far below that should return null.
        assertNull(MusicTheory.frequencyToNote(1.0))
    }
}
