package de.ritzelprimpf.toniqo.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuitarTuningTest {

    private val standard = GuitarTuning.STANDARD_6

    // ── STANDARD_6 correctness ────────────────────────────────────────────────────

    @Test
    fun `STANDARD_6 has string count 6`() {
        assertEquals(6, standard.stringCount)
    }

    @Test
    fun `STANDARD_6 open notes are E2 A2 D3 G3 B3 E4`() {
        val expected = listOf(
            Note(NoteName.E, 2),
            Note(NoteName.A, 2),
            Note(NoteName.D, 3),
            Note(NoteName.G, 3),
            Note(NoteName.B, 3),
            Note(NoteName.E, 4),
        )
        assertEquals(expected, standard.openNotes)
    }

    @Test
    fun `STANDARD_6 id is standard_6`() {
        assertEquals("standard_6", standard.id)
    }

    // ── uniformOffsetFrom ─────────────────────────────────────────────────────────

    @Test
    fun `standard to standard is offset 0`() {
        assertEquals(0, standard.uniformOffsetFrom(standard))
    }

    @Test
    fun `Eb standard is offset minus 1 from standard`() {
        val ebStandard = GuitarTuning(
            id = "eb_standard",
            openNotes = listOf(
                Note(NoteName.DSharp, 2),
                Note(NoteName.GSharp, 2),
                Note(NoteName.CSharp, 3),
                Note(NoteName.FSharp, 3),
                Note(NoteName.ASharp, 3),
                Note(NoteName.DSharp, 4),
            ),
        )
        assertEquals(-1, ebStandard.uniformOffsetFrom(standard))
    }

    @Test
    fun `D standard is offset minus 2 from standard`() {
        val dStandard = GuitarTuning(
            id = "d_standard",
            openNotes = listOf(
                Note(NoteName.D, 2),
                Note(NoteName.G, 2),
                Note(NoteName.C, 3),
                Note(NoteName.F, 3),
                Note(NoteName.A, 3),
                Note(NoteName.D, 4),
            ),
        )
        assertEquals(-2, dStandard.uniformOffsetFrom(standard))
    }

    @Test
    fun `Drop D is not a uniform offset`() {
        val dropD = GuitarTuning(
            id = "drop_d",
            openNotes = listOf(
                Note(NoteName.D, 2),  // down 2
                Note(NoteName.A, 2),  // unchanged
                Note(NoteName.D, 3),
                Note(NoteName.G, 3),
                Note(NoteName.B, 3),
                Note(NoteName.E, 4),
            ),
        )
        assertNull(dropD.uniformOffsetFrom(standard))
    }

    @Test
    fun `7-string tuning returns null against 6-string standard`() {
        val sevenString = GuitarTuning(
            id = "seven_b_standard",
            openNotes = listOf(
                Note(NoteName.B, 1),
                Note(NoteName.E, 2),
                Note(NoteName.A, 2),
                Note(NoteName.D, 3),
                Note(NoteName.G, 3),
                Note(NoteName.B, 3),
                Note(NoteName.E, 4),
            ),
        )
        assertNull(sevenString.uniformOffsetFrom(standard))
    }
}
