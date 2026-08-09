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

    // ── DROP_D_6 correctness ──────────────────────────────────────────────────────

    private val dropD = GuitarTuning.DROP_D_6

    @Test
    fun `DROP_D_6 has string count 6`() {
        assertEquals(6, dropD.stringCount)
    }

    @Test
    fun `DROP_D_6 open notes are D2 A2 D3 G3 B3 E4`() {
        val expected = listOf(
            Note(NoteName.D, 2),
            Note(NoteName.A, 2),
            Note(NoteName.D, 3),
            Note(NoteName.G, 3),
            Note(NoteName.B, 3),
            Note(NoteName.E, 4),
        )
        assertEquals(expected, dropD.openNotes)
    }

    @Test
    fun `DROP_D_6 id is drop_d_6`() {
        assertEquals("drop_d_6", dropD.id)
    }

    @Test
    fun `DROP_D_6 is not a uniform offset of STANDARD_6`() {
        assertNull(dropD.uniformOffsetFrom(standard))
    }

    @Test
    fun `STANDARD_6 is not a uniform offset of DROP_D_6`() {
        assertNull(standard.uniformOffsetFrom(dropD))
    }

    // ── uniformOffsetFrom(DROP_D_6): the other 6-string drop tunings ────────────────

    @Test
    fun `Drop D to Drop D is offset 0`() {
        assertEquals(0, dropD.uniformOffsetFrom(dropD))
    }

    @Test
    fun `Drop C sharp is offset minus 1 from Drop D`() {
        val dropCSharp = GuitarTuning(
            id = "drop_cs",
            openNotes = listOf(
                Note(NoteName.CSharp, 2), Note(NoteName.GSharp, 2), Note(NoteName.CSharp, 3),
                Note(NoteName.FSharp, 3), Note(NoteName.ASharp, 3), Note(NoteName.DSharp, 4),
            ),
        )
        assertEquals(-1, dropCSharp.uniformOffsetFrom(dropD))
    }

    @Test
    fun `Drop C is offset minus 2 from Drop D`() {
        val dropC = GuitarTuning(
            id = "drop_c",
            openNotes = listOf(
                Note(NoteName.C, 2), Note(NoteName.G, 2), Note(NoteName.C, 3),
                Note(NoteName.F, 3), Note(NoteName.A, 3), Note(NoteName.D, 4),
            ),
        )
        assertEquals(-2, dropC.uniformOffsetFrom(dropD))
    }

    @Test
    fun `Drop B is offset minus 3 from Drop D`() {
        val dropB = GuitarTuning(
            id = "drop_b",
            openNotes = listOf(
                Note(NoteName.B, 1), Note(NoteName.FSharp, 2), Note(NoteName.B, 2),
                Note(NoteName.E, 3), Note(NoteName.GSharp, 3), Note(NoteName.CSharp, 4),
            ),
        )
        assertEquals(-3, dropB.uniformOffsetFrom(dropD))
    }

    @Test
    fun `Drop Bb is offset minus 4 from Drop D`() {
        val dropBb = GuitarTuning(
            id = "drop_bb",
            openNotes = listOf(
                Note(NoteName.ASharp, 1), Note(NoteName.F, 2), Note(NoteName.ASharp, 2),
                Note(NoteName.DSharp, 3), Note(NoteName.G, 3), Note(NoteName.C, 4),
            ),
        )
        assertEquals(-4, dropBb.uniformOffsetFrom(dropD))
    }

    @Test
    fun `Drop A is offset minus 5 from Drop D`() {
        val dropA = GuitarTuning(
            id = "drop_a",
            openNotes = listOf(
                Note(NoteName.A, 1), Note(NoteName.E, 2), Note(NoteName.A, 2),
                Note(NoteName.D, 3), Note(NoteName.FSharp, 3), Note(NoteName.B, 3),
            ),
        )
        assertEquals(-5, dropA.uniformOffsetFrom(dropD))
    }
}
