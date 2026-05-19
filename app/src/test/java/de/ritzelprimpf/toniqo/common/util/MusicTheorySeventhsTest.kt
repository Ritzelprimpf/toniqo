package de.ritzelprimpf.toniqo.common.util

import de.ritzelprimpf.toniqo.common.model.Mode
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import de.ritzelprimpf.toniqo.common.model.Scale
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicTheorySeventhsTest {

    private fun sevenths(root: Note, mode: Mode): List<String> =
        MusicTheory.buildSeventhChords(Scale(root, mode)).map { it.displayName() }

    // ── Canonical test ────────────────────────────────────────────────────────────

    @Test
    fun `C Ionian seventh chords equal the canonical sequence`() {
        assertEquals(
            listOf("Cmaj7", "Dm7", "Em7", "Fmaj7", "G7", "Am7", "Bm7♭5"),
            sevenths(Note(NoteName.C, 4), Mode.IONIAN),
        )
    }

    // ── One test per remaining mode ───────────────────────────────────────────────

    @Test
    fun `D Dorian seventh chords are Dm7 Em7 Fmaj7 G7 Am7 Bm7b5 Cmaj7`() {
        assertEquals(
            listOf("Dm7", "Em7", "Fmaj7", "G7", "Am7", "Bm7♭5", "Cmaj7"),
            sevenths(Note(NoteName.D, 4), Mode.DORIAN),
        )
    }

    @Test
    fun `E Phrygian seventh chords are Em7 Fmaj7 G7 Am7 Bm7b5 Cmaj7 Dm7`() {
        assertEquals(
            listOf("Em7", "Fmaj7", "G7", "Am7", "Bm7♭5", "Cmaj7", "Dm7"),
            sevenths(Note(NoteName.E, 4), Mode.PHRYGIAN),
        )
    }

    @Test
    fun `F Lydian seventh chords are Fmaj7 G7 Am7 Bm7b5 Cmaj7 Dm7 Em7`() {
        assertEquals(
            listOf("Fmaj7", "G7", "Am7", "Bm7♭5", "Cmaj7", "Dm7", "Em7"),
            sevenths(Note(NoteName.F, 4), Mode.LYDIAN),
        )
    }

    @Test
    fun `G Mixolydian seventh chords are G7 Am7 Bm7b5 Cmaj7 Dm7 Em7 Fmaj7`() {
        assertEquals(
            listOf("G7", "Am7", "Bm7♭5", "Cmaj7", "Dm7", "Em7", "Fmaj7"),
            sevenths(Note(NoteName.G, 4), Mode.MIXOLYDIAN),
        )
    }

    @Test
    fun `A Aeolian seventh chords are Am7 Bm7b5 Cmaj7 Dm7 Em7 Fmaj7 G7`() {
        assertEquals(
            listOf("Am7", "Bm7♭5", "Cmaj7", "Dm7", "Em7", "Fmaj7", "G7"),
            sevenths(Note(NoteName.A, 3), Mode.AEOLIAN),
        )
    }

    @Test
    fun `B Locrian seventh chords are Bm7b5 Cmaj7 Dm7 Em7 Fmaj7 G7 Am7`() {
        assertEquals(
            listOf("Bm7♭5", "Cmaj7", "Dm7", "Em7", "Fmaj7", "G7", "Am7"),
            sevenths(Note(NoteName.B, 3), Mode.LOCRIAN),
        )
    }

    @Test
    fun `buildSeventhChords returns exactly 7 chords`() {
        assertEquals(7, MusicTheory.buildSeventhChords(Scale(Note(NoteName.C, 4), Mode.IONIAN)).size)
    }
}
