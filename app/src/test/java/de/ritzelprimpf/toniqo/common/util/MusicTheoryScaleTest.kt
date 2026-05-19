package de.ritzelprimpf.toniqo.common.util

import de.ritzelprimpf.toniqo.common.model.Mode
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicTheoryScaleTest {

    @Test
    fun `buildScale C Ionian produces the C major scale`() {
        val scale = MusicTheory.buildScale(Note(NoteName.C, 4), Mode.IONIAN)
        assertEquals(
            listOf("C4", "D4", "E4", "F4", "G4", "A4", "B4"),
            scale.notes.map { it.displayName() },
        )
    }

    @Test
    fun `buildScale D Dorian is correct`() {
        val scale = MusicTheory.buildScale(Note(NoteName.D, 4), Mode.DORIAN)
        assertEquals(
            listOf("D4", "E4", "F4", "G4", "A4", "B4", "C5"),
            scale.notes.map { it.displayName() },
        )
    }

    @Test
    fun `buildScale E Phrygian is correct`() {
        val scale = MusicTheory.buildScale(Note(NoteName.E, 4), Mode.PHRYGIAN)
        assertEquals(
            listOf("E4", "F4", "G4", "A4", "B4", "C5", "D5"),
            scale.notes.map { it.displayName() },
        )
    }

    @Test
    fun `buildScale G Mixolydian is correct`() {
        val scale = MusicTheory.buildScale(Note(NoteName.G, 4), Mode.MIXOLYDIAN)
        assertEquals(
            listOf("G4", "A4", "B4", "C5", "D5", "E5", "F5"),
            scale.notes.map { it.displayName() },
        )
    }

    @Test
    fun `buildScale A Aeolian from A3 wraps octave at C`() {
        val scale = MusicTheory.buildScale(Note(NoteName.A, 3), Mode.AEOLIAN)
        assertEquals(
            listOf("A3", "B3", "C4", "D4", "E4", "F4", "G4"),
            scale.notes.map { it.displayName() },
        )
    }

    @Test
    fun `buildScale B Locrian is correct`() {
        val scale = MusicTheory.buildScale(Note(NoteName.B, 3), Mode.LOCRIAN)
        assertEquals(
            listOf("B3", "C4", "D4", "E4", "F4", "G4", "A4"),
            scale.notes.map { it.displayName() },
        )
    }

    @Test
    fun `buildScale returns Scale with correct root and mode`() {
        val root = Note(NoteName.C, 4)
        val scale = MusicTheory.buildScale(root, Mode.LYDIAN)
        assertEquals(root, scale.root)
        assertEquals(Mode.LYDIAN, scale.mode)
    }
}
