package de.ritzelprimpf.toniqo.chordfinder.presentation.ui

import de.ritzelprimpf.toniqo.chordfinder.domain.model.Barre
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordToneRole
import de.ritzelprimpf.toniqo.chordfinder.domain.model.FretMark
import de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Voicing.toRenderModel].
 *
 * Tests are grouped by the property they verify:
 * - Nut vs position label selection
 * - Dot / open / muted mark conversion
 * - Root flag propagation
 * - Barre mapping
 * - High-position fret windowing
 * - 6 / 7 / 8-string string count
 */
class VoicingRenderModelMapperTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun sixStringVoicing(
        marks: List<FretMark>,
        fingers: List<Int> = List(marks.size) { 0 },
        barre: Barre? = null,
        roots: Set<Int> = emptySet(),
    ) = Voicing(
        labelKey          = 1,
        marks             = marks,
        fingers           = fingers,
        barre             = barre,
        rootStringIndices = roots,
        bassDegree        = ChordToneRole.ROOT,
    )

    private fun nStringVoicing(
        n: Int,
        fret: Int = 1,
        roots: Set<Int> = setOf(0),
    ) = Voicing(
        labelKey          = 1,
        marks             = List(n) { FretMark.Fretted(fret) },
        fingers           = List(n) { 1 },
        barre             = null,
        rootStringIndices = roots,
        bassDegree        = ChordToneRole.ROOT,
    )

    // ── Nut vs position label ─────────────────────────────────────────────────

    @Test
    fun `showNut is true and positionLabel is null when baseFret is 1`() {
        val model = sixStringVoicing(
            marks = listOf(
                FretMark.Fretted(1), FretMark.Fretted(2), FretMark.Open,
                FretMark.Open, FretMark.Fretted(1), FretMark.Open,
            ),
        ).toRenderModel()

        assertTrue(model.showNut)
        assertNull(model.positionLabel)
    }

    @Test
    fun `showNut is false and positionLabel shows baseFret when baseFret is greater than 1`() {
        val model = sixStringVoicing(
            marks = listOf(
                FretMark.Fretted(5), FretMark.Fretted(7), FretMark.Fretted(7),
                FretMark.Fretted(7), FretMark.Fretted(5), FretMark.Fretted(5),
            ),
            barre = Barre(fret = 5, fromString = 0, toString = 5),
        ).toRenderModel()

        assertFalse(model.showNut)
        assertEquals("5fr", model.positionLabel)
    }

    @Test
    fun `positionLabel uses baseFret as position number`() {
        val voicing = sixStringVoicing(
            marks = listOf(
                FretMark.Fretted(9), FretMark.Fretted(11), FretMark.Fretted(11),
                FretMark.Fretted(10), FretMark.Fretted(9), FretMark.Fretted(9),
            ),
            barre = Barre(fret = 9, fromString = 0, toString = 5),
        )
        assertEquals("9fr", voicing.toRenderModel().positionLabel)
    }

    // ── Nut-anchored windowing for non-barre chords ───────────────────────────

    @Test
    fun `non-barre voicing windows from the nut when the shape fits within reach of it`() {
        // Open G-style shape: lowest fretted note is fret 2, but no barre — should anchor to the
        // nut (base = 1) rather than left-aligning to fret 2.
        val model = sixStringVoicing(
            marks = listOf(
                FretMark.Fretted(3), FretMark.Fretted(2), FretMark.Open,
                FretMark.Open, FretMark.Open, FretMark.Fretted(3),
            ),
        ).toRenderModel()

        assertTrue(model.showNut)
        assertNull(model.positionLabel)
        val dotAtString1 = model.dots.first { it.stringIndex == 1 }
        assertEquals(2, dotAtString1.fretWithinWindow)
        val dotAtString0 = model.dots.first { it.stringIndex == 0 }
        assertEquals(3, dotAtString0.fretWithinWindow)
    }

    @Test
    fun `non-barre voicing with a single high fretted note still windows from the nut`() {
        // Em-style shape: both fretted notes sit on fret 2 — should still anchor to the nut
        // instead of showing a one-row window starting at fret 2.
        val model = sixStringVoicing(
            marks = listOf(
                FretMark.Open, FretMark.Fretted(2), FretMark.Fretted(2),
                FretMark.Open, FretMark.Open, FretMark.Open,
            ),
        ).toRenderModel()

        assertTrue(model.showNut)
        assertNull(model.positionLabel)
        val dotAtString1 = model.dots.first { it.stringIndex == 1 }
        assertEquals(2, dotAtString1.fretWithinWindow)
    }

    @Test
    fun `non-barre voicing that cannot reach the nut still windows from its lowest fret`() {
        // Open-position shape whose highest fret (7) exceeds the window size — cannot be
        // nut-anchored, so it must fall back to windowing from the lowest fretted note.
        val model = sixStringVoicing(
            marks = listOf(
                FretMark.Open, FretMark.Fretted(7), FretMark.Fretted(5),
                FretMark.Fretted(4), FretMark.Fretted(5), FretMark.Open,
            ),
        ).toRenderModel()

        assertFalse(model.showNut)
        assertEquals("4fr", model.positionLabel)
    }

    @Test
    fun `barre voicing windows from the barre fret even when the shape would fit from the nut`() {
        // Shape spans frets 2-5, which would fit in a nut-anchored window for a non-barre voicing
        // — but this is a barre chord, so it must still show its actual position, not the nut.
        val model = sixStringVoicing(
            marks = listOf(
                FretMark.Fretted(2), FretMark.Fretted(4), FretMark.Fretted(5),
                FretMark.Fretted(4), FretMark.Fretted(2), FretMark.Fretted(2),
            ),
            barre = Barre(fret = 2, fromString = 0, toString = 5),
        ).toRenderModel()

        assertFalse(model.showNut)
        assertEquals("2fr", model.positionLabel)
        val dotAtString2 = model.dots.first { it.stringIndex == 2 }
        assertEquals(4, dotAtString2.fretWithinWindow)
    }

    // ── Mark type conversion ──────────────────────────────────────────────────

    @Test
    fun `Open marks are placed into openStrings set`() {
        val model = sixStringVoicing(
            marks = listOf(
                FretMark.Open, FretMark.Fretted(2), FretMark.Open,
                FretMark.Muted, FretMark.Fretted(1), FretMark.Open,
            ),
        ).toRenderModel()

        assertEquals(setOf(0, 2, 5), model.openStrings)
    }

    @Test
    fun `Muted marks are placed into mutedStrings set`() {
        val model = sixStringVoicing(
            marks = listOf(
                FretMark.Muted, FretMark.Fretted(3), FretMark.Fretted(2),
                FretMark.Open, FretMark.Fretted(1), FretMark.Muted,
            ),
        ).toRenderModel()

        assertEquals(setOf(0, 5), model.mutedStrings)
    }

    @Test
    fun `Fretted marks produce Dot entries — Open and Muted produce no dots`() {
        val model = sixStringVoicing(
            marks = listOf(
                FretMark.Muted, FretMark.Fretted(3), FretMark.Fretted(2),
                FretMark.Open, FretMark.Fretted(1), FretMark.Open,
            ),
        ).toRenderModel()

        assertEquals(3, model.dots.size)
        val stringIndices = model.dots.map { it.stringIndex }
        assertTrue(1 in stringIndices)
        assertTrue(2 in stringIndices)
        assertTrue(4 in stringIndices)
    }

    @Test
    fun `Dot finger is null when fingers entry is zero`() {
        val model = sixStringVoicing(
            marks   = listOf(FretMark.Open, FretMark.Fretted(2), FretMark.Fretted(3),
                             FretMark.Fretted(2), FretMark.Open, FretMark.Open),
            fingers = listOf(0, 2, 3, 2, 0, 0),
        ).toRenderModel()

        val dotAt1 = model.dots.first { it.stringIndex == 1 }
        assertEquals(2, dotAt1.finger)

        val dotAt2 = model.dots.first { it.stringIndex == 2 }
        assertEquals(3, dotAt2.finger)
    }

    @Test
    fun `Dot finger is null when fingers list contains zero for that string`() {
        val model = sixStringVoicing(
            marks   = listOf(FretMark.Fretted(1), FretMark.Open, FretMark.Open,
                             FretMark.Open, FretMark.Open, FretMark.Fretted(1)),
            fingers = listOf(0, 0, 0, 0, 0, 0),
        ).toRenderModel()

        assertTrue(model.dots.all { it.finger == null })
    }

    // ── Root flag ─────────────────────────────────────────────────────────────

    @Test
    fun `isRoot is true only for strings in rootStringIndices`() {
        val model = sixStringVoicing(
            marks  = listOf(
                FretMark.Fretted(3), FretMark.Fretted(5), FretMark.Fretted(5),
                FretMark.Fretted(5), FretMark.Fretted(3), FretMark.Fretted(3),
            ),
            fingers = listOf(1, 3, 4, 4, 1, 1),
            roots  = setOf(0, 5),
        ).toRenderModel()

        val rootDots = model.dots.filter { it.isRoot }.map { it.stringIndex }
        assertEquals(setOf(0, 5), rootDots.toSet())

        val nonRootDots = model.dots.filter { !it.isRoot }.map { it.stringIndex }
        assertEquals(setOf(1, 2, 3, 4), nonRootDots.toSet())
    }

    // ── Barre mapping ─────────────────────────────────────────────────────────

    @Test
    fun `barre is null in render model when voicing has no barre`() {
        val model = sixStringVoicing(
            marks = listOf(FretMark.Open, FretMark.Fretted(2), FretMark.Fretted(2),
                           FretMark.Fretted(1), FretMark.Open, FretMark.Open),
        ).toRenderModel()

        assertNull(model.barre)
    }

    @Test
    fun `barre fretWithinWindow is correctly computed relative to baseFret`() {
        val voicing = sixStringVoicing(
            marks = listOf(
                FretMark.Fretted(5), FretMark.Fretted(7), FretMark.Fretted(7),
                FretMark.Fretted(7), FretMark.Fretted(5), FretMark.Fretted(5),
            ),
            barre = Barre(fret = 5, fromString = 0, toString = 5),
        )
        val model = voicing.toRenderModel()

        assertNotNull(model.barre)
        assertEquals(1, model.barre!!.fretWithinWindow)
        assertEquals(0, model.barre!!.fromString)
        assertEquals(5, model.barre!!.toString)
    }

    @Test
    fun `barre at non-root position maps to correct window position`() {
        val voicing = sixStringVoicing(
            marks = listOf(
                FretMark.Fretted(5), FretMark.Fretted(5), FretMark.Fretted(7),
                FretMark.Fretted(7), FretMark.Fretted(7), FretMark.Fretted(5),
            ),
            barre = Barre(fret = 5, fromString = 0, toString = 5),
        )
        val model = voicing.toRenderModel()
        assertEquals(1, model.barre!!.fretWithinWindow)
    }

    // ── High-position windowing ───────────────────────────────────────────────

    @Test
    fun `fretWithinWindow is correctly offset from baseFret for high-position voicing`() {
        val voicing = sixStringVoicing(
            marks = listOf(
                FretMark.Fretted(9), FretMark.Fretted(11), FretMark.Fretted(11),
                FretMark.Fretted(10), FretMark.Fretted(9), FretMark.Fretted(9),
            ),
            barre   = Barre(fret = 9, fromString = 0, toString = 5),
            fingers = listOf(1, 3, 4, 2, 1, 1),
        )
        val model = voicing.toRenderModel()

        // baseFret = 9; fret 11 should be window position 11 - 9 + 1 = 3
        val dotAtString1 = model.dots.first { it.stringIndex == 1 }
        assertEquals(3, dotAtString1.fretWithinWindow)
    }

    @Test
    fun `fretWindow is always FRET_WINDOW_SIZE`() {
        val model = sixStringVoicing(
            marks = listOf(
                FretMark.Fretted(12), FretMark.Fretted(14), FretMark.Fretted(14),
                FretMark.Fretted(13), FretMark.Fretted(12), FretMark.Fretted(12),
            ),
            barre = Barre(fret = 12, fromString = 0, toString = 5),
        ).toRenderModel()

        assertEquals(FRET_WINDOW_SIZE, model.fretWindow)
    }

    // ── String count ──────────────────────────────────────────────────────────

    @Test
    fun `stringCount is 6 for a 6-string voicing`() {
        assertEquals(6, nStringVoicing(6).toRenderModel().stringCount)
    }

    @Test
    fun `stringCount is 7 for a 7-string voicing`() {
        assertEquals(7, nStringVoicing(7).toRenderModel().stringCount)
    }

    @Test
    fun `stringCount is 8 for an 8-string voicing`() {
        assertEquals(8, nStringVoicing(8).toRenderModel().stringCount)
    }

    // ── Edge: open-only strings in high-position voicing ─────────────────────

    @Test
    fun `open strings with baseFret greater than 1 still appear in openStrings`() {
        val model = sixStringVoicing(
            marks = listOf(
                FretMark.Fretted(5), FretMark.Fretted(5), FretMark.Open,
                FretMark.Fretted(5), FretMark.Fretted(5), FretMark.Fretted(5),
            ),
            barre = Barre(fret = 5, fromString = 0, toString = 1),
        ).toRenderModel()

        assertTrue(2 in model.openStrings)
        assertFalse(model.showNut) // baseFret = 5 > 1
    }

    // ── Regression: legal-schema edge cases from the regenerated voicing library ─────
    // These shapes are all legal per the documented JSON schema but were absent from the
    // original curated library: an all-open voicing, a 3-string voicing, and frets beyond the
    // old 15-fret ceiling. See Voicing.MAX_FRET and VoicingLibraryValidationTest for the
    // corresponding parsing-layer coverage.

    @Test
    fun `all-open voicing produces no dots and windows at the nut`() {
        // Em-style shape sounding only open strings: ["x","x","x",0,0,0]
        val model = sixStringVoicing(
            marks = listOf(
                FretMark.Muted, FretMark.Muted, FretMark.Muted,
                FretMark.Open, FretMark.Open, FretMark.Open,
            ),
        ).toRenderModel()

        assertTrue(model.dots.isEmpty())
        assertEquals(setOf(3, 4, 5), model.openStrings)
        assertEquals(setOf(0, 1, 2), model.mutedStrings)
        assertTrue(model.showNut)
        assertNull(model.positionLabel)
    }

    @Test
    fun `three-string voicing renders only its sounded strings`() {
        // ["x","x","x",4,3,2] — only half the strings sound.
        val model = sixStringVoicing(
            marks = listOf(
                FretMark.Muted, FretMark.Muted, FretMark.Muted,
                FretMark.Fretted(4), FretMark.Fretted(3), FretMark.Fretted(2),
            ),
        ).toRenderModel()

        assertEquals(setOf(0, 1, 2), model.mutedStrings)
        assertTrue(model.openStrings.isEmpty())
        assertEquals(3, model.dots.size)
        assertEquals(setOf(3, 4, 5), model.dots.map { it.stringIndex }.toSet())
    }

    @Test
    fun `frets beyond the old 15-fret ceiling map into the window without throwing`() {
        // [15,17,17,16,15,15] — exceeds the previous MAX_FRET of 15.
        val model = sixStringVoicing(
            marks = listOf(
                FretMark.Fretted(15), FretMark.Fretted(17), FretMark.Fretted(17),
                FretMark.Fretted(16), FretMark.Fretted(15), FretMark.Fretted(15),
            ),
            barre = Barre(fret = 15, fromString = 0, toString = 5),
        ).toRenderModel()

        assertEquals("15fr", model.positionLabel)
        val highestDot = model.dots.first { it.stringIndex == 1 }
        assertEquals(3, highestDot.fretWithinWindow) // 17 - 15 + 1
    }
}
