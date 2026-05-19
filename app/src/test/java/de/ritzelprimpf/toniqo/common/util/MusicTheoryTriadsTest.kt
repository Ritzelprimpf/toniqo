package de.ritzelprimpf.toniqo.common.util

import de.ritzelprimpf.toniqo.common.model.Mode
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import de.ritzelprimpf.toniqo.common.model.Scale
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicTheoryTriadsTest {

    private fun triads(root: Note, mode: Mode): List<String> =
        MusicTheory.buildTriads(Scale(root, mode)).map { it.displayName() }

    @Test
    fun `C Ionian triads are C Dm Em F G Am Bdim`() {
        assertEquals(
            listOf("C", "Dm", "Em", "F", "G", "Am", "Bdim"),
            triads(Note(NoteName.C, 4), Mode.IONIAN),
        )
    }

    @Test
    fun `D Dorian triads are Dm Em F G Am Bdim C`() {
        assertEquals(
            listOf("Dm", "Em", "F", "G", "Am", "Bdim", "C"),
            triads(Note(NoteName.D, 4), Mode.DORIAN),
        )
    }

    @Test
    fun `E Phrygian triads are Em F G Am Bdim C Dm`() {
        assertEquals(
            listOf("Em", "F", "G", "Am", "Bdim", "C", "Dm"),
            triads(Note(NoteName.E, 4), Mode.PHRYGIAN),
        )
    }

    @Test
    fun `F Lydian triads are F G Am Bdim C Dm Em`() {
        assertEquals(
            listOf("F", "G", "Am", "Bdim", "C", "Dm", "Em"),
            triads(Note(NoteName.F, 4), Mode.LYDIAN),
        )
    }

    @Test
    fun `G Mixolydian triads are G Am Bdim C Dm Em F`() {
        assertEquals(
            listOf("G", "Am", "Bdim", "C", "Dm", "Em", "F"),
            triads(Note(NoteName.G, 4), Mode.MIXOLYDIAN),
        )
    }

    @Test
    fun `A Aeolian triads are Am Bdim C Dm Em F G`() {
        assertEquals(
            listOf("Am", "Bdim", "C", "Dm", "Em", "F", "G"),
            triads(Note(NoteName.A, 3), Mode.AEOLIAN),
        )
    }

    @Test
    fun `B Locrian triads are Bdim C Dm Em F G Am`() {
        assertEquals(
            listOf("Bdim", "C", "Dm", "Em", "F", "G", "Am"),
            triads(Note(NoteName.B, 3), Mode.LOCRIAN),
        )
    }

    @Test
    fun `buildTriads returns exactly 7 chords`() {
        assertEquals(7, MusicTheory.buildTriads(Scale(Note(NoteName.C, 4), Mode.IONIAN)).size)
    }
}
