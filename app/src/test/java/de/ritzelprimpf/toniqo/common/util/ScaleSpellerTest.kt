package de.ritzelprimpf.toniqo.common.util

import de.ritzelprimpf.toniqo.common.model.ScaleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleSpellerTest {

    // ─────────────────────── rootName canonical table ──────────────────────

    @Test
    fun `rootName returns C for pitch class 0`() {
        assertEquals("C", ScaleSpeller.rootName(0, ScaleType.IONIAN))
    }

    @Test
    fun `rootName returns Db for pitch class 1`() {
        assertEquals("D${ScaleSpeller.FLAT}", ScaleSpeller.rootName(1, ScaleType.IONIAN))
    }

    @Test
    fun `rootName returns Fs for pitch class 6`() {
        assertEquals("F${ScaleSpeller.SHARP}", ScaleSpeller.rootName(6, ScaleType.IONIAN))
    }

    @Test
    fun `rootName returns Bb for pitch class 10`() {
        assertEquals("B${ScaleSpeller.FLAT}", ScaleSpeller.rootName(10, ScaleType.IONIAN))
    }

    @Test
    fun `rootName covers all 12 pitch classes with non-blank results`() {
        for (pc in 0 until 12) {
            val name = ScaleSpeller.rootName(pc, ScaleType.IONIAN)
            assertTrue("rootName($pc) should not be blank", name.isNotBlank())
        }
    }

    // ───────────────────────── scaleNoteNames canon ─────────────────────────

    @Test
    fun `C Major spells as C D E F G A B`() {
        val notes = ScaleSpeller.scaleNoteNames(0, ScaleType.IONIAN)
        assertEquals(listOf("C", "D", "E", "F", "G", "A", "B"), notes)
    }

    @Test
    fun `G Major spells as G A B C D E Fs`() {
        val notes = ScaleSpeller.scaleNoteNames(7, ScaleType.IONIAN)
        assertEquals(listOf("G", "A", "B", "C", "D", "E", "F${ScaleSpeller.SHARP}"), notes)
    }

    @Test
    fun `F Major spells as F G A Bb C D E`() {
        val notes = ScaleSpeller.scaleNoteNames(5, ScaleType.IONIAN)
        assertEquals(listOf("F", "G", "A", "B${ScaleSpeller.FLAT}", "C", "D", "E"), notes)
    }

    @Test
    fun `Bb Major spells as Bb C D Eb F G A`() {
        val notes = ScaleSpeller.scaleNoteNames(10, ScaleType.IONIAN)
        val flat = ScaleSpeller.FLAT
        assertEquals(listOf("B$flat", "C", "D", "E$flat", "F", "G", "A"), notes)
    }

    @Test
    fun `Fs Major spells as Fs Gs As B Cs Ds Es`() {
        val notes = ScaleSpeller.scaleNoteNames(6, ScaleType.IONIAN)
        val sharp = ScaleSpeller.SHARP
        assertEquals(
            listOf("F$sharp", "G$sharp", "A$sharp", "B", "C$sharp", "D$sharp", "E$sharp"),
            notes,
        )
    }

    @Test
    fun `A Natural Minor spells as A B C D E F G`() {
        val notes = ScaleSpeller.scaleNoteNames(9, ScaleType.AEOLIAN)
        assertEquals(listOf("A", "B", "C", "D", "E", "F", "G"), notes)
    }

    @Test
    fun `A Harmonic Minor spells as A B C D E F Gs`() {
        val notes = ScaleSpeller.scaleNoteNames(9, ScaleType.HARMONIC_MINOR)
        assertEquals(listOf("A", "B", "C", "D", "E", "F", "G${ScaleSpeller.SHARP}"), notes)
    }

    @Test
    fun `A Melodic Minor spells as A B C D E Fs Gs`() {
        val notes = ScaleSpeller.scaleNoteNames(9, ScaleType.MELODIC_MINOR)
        val sharp = ScaleSpeller.SHARP
        assertEquals(listOf("A", "B", "C", "D", "E", "F$sharp", "G$sharp"), notes)
    }

    @Test
    fun `E Phrygian Dominant spells as E F Gs A B C D`() {
        val notes = ScaleSpeller.scaleNoteNames(4, ScaleType.PHRYGIAN_DOMINANT)
        assertEquals(listOf("E", "F", "G${ScaleSpeller.SHARP}", "A", "B", "C", "D"), notes)
    }

    @Test
    fun `C Lydian Dominant spells as C D E Fs G A Bb`() {
        val notes = ScaleSpeller.scaleNoteNames(0, ScaleType.LYDIAN_DOMINANT)
        val sharp = ScaleSpeller.SHARP
        val flat = ScaleSpeller.FLAT
        assertEquals(listOf("C", "D", "E", "F$sharp", "G", "A", "B$flat"), notes)
    }

    @Test
    fun `G Altered spells as G Ab Bb Cb Db Eb F`() {
        val notes = ScaleSpeller.scaleNoteNames(7, ScaleType.ALTERED)
        val flat = ScaleSpeller.FLAT
        assertEquals(listOf("G", "A$flat", "B$flat", "C$flat", "D$flat", "E$flat", "F"), notes)
    }

    // ─────────────── Letter-per-degree rule: all 12 × 14 = 168 scales ──────

    @Test
    fun `every scale uses 7 distinct letter names — letter-per-degree rule holds for all 168`() {
        val letters = setOf("A", "B", "C", "D", "E", "F", "G")
        for (rootPc in 0 until 12) {
            for (type in ScaleType.entries) {
                val notes = ScaleSpeller.scaleNoteNames(rootPc, type)
                val baseLetters = notes.map { it[0].toString() }.toSet()
                assertEquals(
                    "root=$rootPc type=${type.name}: expected 7 distinct letters, got $notes",
                    letters,
                    baseLetters,
                )
            }
        }
    }

    @Test
    fun `every scale has exactly 7 note names`() {
        for (rootPc in 0 until 12) {
            for (type in ScaleType.entries) {
                val notes = ScaleSpeller.scaleNoteNames(rootPc, type)
                assertEquals(
                    "root=$rootPc type=${type.name} should produce 7 names",
                    7,
                    notes.size,
                )
            }
        }
    }

    // ─────────────── Accidental constants are the expected glyphs ───────────

    @Test
    fun `SHARP constant is the music sharp glyph`() {
        assertEquals("♯", ScaleSpeller.SHARP)
    }

    @Test
    fun `FLAT constant is the music flat glyph`() {
        assertEquals("♭", ScaleSpeller.FLAT)
    }
}
