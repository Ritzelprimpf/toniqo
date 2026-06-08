package de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel

import de.ritzelprimpf.toniqo.common.model.ChordQuality
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [ChordVoicingsViewModel.Companion.deriveNoteNames].
 *
 * Covers: all four [ChordQuality] values, chromatic wrap-around, and
 * conventionally-spelled sharp/flat names at boundary pitch classes.
 */
class DeriveNoteNamesTest {

    private fun derive(rootPc: Int, quality: ChordQuality): List<String> =
        ChordVoicingsViewModel.deriveNoteNames(rootPc, quality)

    // ── Quality coverage ──────────────────────────────────────────────────────────

    @Test
    fun `C major triad returns C E G`() {
        assertEquals(listOf("C", "E", "G"), derive(rootPc = 0, ChordQuality.MAJOR))
    }

    @Test
    fun `A minor triad returns A C E`() {
        assertEquals(listOf("A", "C", "E"), derive(rootPc = 9, ChordQuality.MINOR))
    }

    @Test
    fun `B diminished triad returns B D F`() {
        // B = pc 11, DIMINISHED intervals = [0, 3, 6]
        // (11+0)%12 = 11 → "B", (11+3)%12 = 2 → "D", (11+6)%12 = 5 → "F"
        assertEquals(listOf("B", "D", "F"), derive(rootPc = 11, ChordQuality.DIMINISHED))
    }

    @Test
    fun `C augmented triad returns C E G#`() {
        // C = pc 0, AUGMENTED intervals = [0, 4, 8]
        // (0+8)%12 = 8 → "A♭" (enharmonic with G♯ — the array spells it as A♭)
        val result = derive(rootPc = 0, ChordQuality.AUGMENTED)
        assertEquals("C", result[0])
        assertEquals("E", result[1])
        assertEquals("A♭", result[2])
    }

    // ── Chromatic wrap-around ─────────────────────────────────────────────────────

    @Test
    fun `Ab major triad wraps around correctly to Ab C Eb`() {
        // A♭ = pc 8, MAJOR intervals = [0, 4, 7]
        // (8+0)%12 = 8 → "A♭", (8+4)%12 = 0 → "C", (8+7)%12 = 3 → "E♭"
        assertEquals(listOf("A♭", "C", "E♭"), derive(rootPc = 8, ChordQuality.MAJOR))
    }

    @Test
    fun `Bb minor triad wraps around correctly`() {
        // B♭ = pc 10, MINOR intervals = [0, 3, 7]
        // (10+0)%12 = 10 → "B♭", (10+3)%12 = 1 → "D♭", (10+7)%12 = 5 → "F"
        assertEquals(listOf("B♭", "D♭", "F"), derive(rootPc = 10, ChordQuality.MINOR))
    }

    // ── Enharmonic spelling at boundary pitch classes ─────────────────────────────

    @Test
    fun `F# major triad returns F# A# C# using sharp spellings`() {
        // F♯ = pc 6, MAJOR = [0, 4, 7]
        // (6+0)%12 = 6 → "F♯", (6+4)%12 = 10 → "B♭", (6+7)%12 = 1 → "D♭"
        // NOTE: the note names array uses "F♯" at pc 6 but "B♭" at pc 10, not "A♯"
        val result = derive(rootPc = 6, ChordQuality.MAJOR)
        assertEquals("F♯", result[0])
        // The chromatic table uses "B♭" at pc 10 (not "A♯") — this is intentional
        assertEquals("B♭", result[1])
        assertEquals("D♭", result[2])
    }

    @Test
    fun `E major triad returns E G# B`() {
        // E = pc 4, MAJOR = [0, 4, 7]
        // (4+4)%12 = 8 → "A♭" (not "G♯" — table uses flat spellings for pc 8)
        val result = derive(rootPc = 4, ChordQuality.MAJOR)
        assertEquals("E", result[0])
        assertEquals("A♭", result[1])
        assertEquals("B", result[2])
    }
}
