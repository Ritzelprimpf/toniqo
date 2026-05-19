package de.ritzelprimpf.toniqo.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ScaleTest {

    private val c4 = Note(NoteName.C, 4)
    private val a3 = Note(NoteName.A, 3)

    // ── Data-class contract ───────────────────────────────────────────────────────

    @Test
    fun `equality depends on root and mode only`() {
        val a = Scale(c4, Mode.IONIAN)
        val b = Scale(c4, Mode.IONIAN)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `scales differ when mode differs`() {
        assertNotEquals(Scale(c4, Mode.IONIAN), Scale(c4, Mode.AEOLIAN))
    }

    // ── Derived notes ─────────────────────────────────────────────────────────────

    @Test
    fun `C Ionian produces the C major scale`() {
        val notes = Scale(c4, Mode.IONIAN).notes.map { it.displayName() }
        assertEquals(listOf("C4", "D4", "E4", "F4", "G4", "A4", "B4"), notes)
    }

    @Test
    fun `A Aeolian from A3 wraps octave correctly at C`() {
        val notes = Scale(a3, Mode.AEOLIAN).notes.map { it.displayName() }
        // A B (same octave) then C rises an octave
        assertEquals(listOf("A3", "B3", "C4", "D4", "E4", "F4", "G4"), notes)
    }

    @Test
    fun `D Dorian produces the correct notes`() {
        val notes = Scale(Note(NoteName.D, 4), Mode.DORIAN).notes.map { it.displayName() }
        assertEquals(listOf("D4", "E4", "F4", "G4", "A4", "B4", "C5"), notes)
    }

    @Test
    fun `E Phrygian produces the correct notes`() {
        val notes = Scale(Note(NoteName.E, 4), Mode.PHRYGIAN).notes.map { it.displayName() }
        assertEquals(listOf("E4", "F4", "G4", "A4", "B4", "C5", "D5"), notes)
    }

    @Test
    fun `G Mixolydian produces the correct notes`() {
        val notes = Scale(Note(NoteName.G, 4), Mode.MIXOLYDIAN).notes.map { it.displayName() }
        assertEquals(listOf("G4", "A4", "B4", "C5", "D5", "E5", "F5"), notes)
    }

    @Test
    fun `B Locrian produces the correct notes`() {
        val notes = Scale(Note(NoteName.B, 3), Mode.LOCRIAN).notes.map { it.displayName() }
        assertEquals(listOf("B3", "C4", "D4", "E4", "F4", "G4", "A4"), notes)
    }

    @Test
    fun `every scale produces exactly 7 notes`() {
        Mode.entries.forEach { mode ->
            assertEquals("${mode.name} scale should have 7 notes", 7, Scale(c4, mode).notes.size)
        }
    }
}
