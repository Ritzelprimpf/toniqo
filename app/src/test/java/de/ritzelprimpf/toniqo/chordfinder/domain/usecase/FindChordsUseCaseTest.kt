package de.ritzelprimpf.toniqo.chordfinder.domain.usecase

import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordFinderInput
import de.ritzelprimpf.toniqo.chordfinder.domain.model.SeventhQuality
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.common.model.ScaleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FindChordsUseCaseTest {

    private val useCase = FindChordsUseCase()

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** Pitch class constants for readable test code. */
    private val C = 0; private val D = 2; private val E = 4; private val F = 5
    private val G = 7; private val A = 9; private val B = 11

    private fun triads(rootPc: Int, type: ScaleType) =
        useCase(ChordFinderInput(rootPc, type, includeSeventhChords = false))

    private fun sevenths(rootPc: Int, type: ScaleType) =
        useCase(ChordFinderInput(rootPc, type, includeSeventhChords = true))

    // ── C Ionian: triads ──────────────────────────────────────────────────────────

    @Test
    fun `C Ionian triads have correct symbols`() {
        val symbols = triads(C, ScaleType.IONIAN).chords.map { it.symbol }
        assertEquals(listOf("C", "Dm", "Em", "F", "G", "Am", "Bdim"), symbols)
    }

    @Test
    fun `C Ionian triads have correct roman numerals`() {
        val romans = triads(C, ScaleType.IONIAN).chords.map { it.romanNumeral }
        assertEquals(listOf("I", "ii", "iii", "IV", "V", "vi", "vii°"), romans)
    }

    @Test
    fun `C Ionian triads have correct note names`() {
        val notes = triads(C, ScaleType.IONIAN).chords.map { it.noteNames }
        assertEquals(listOf("C", "E", "G"), notes[0])    // C major
        assertEquals(listOf("D", "F", "A"), notes[1])    // Dm
        assertEquals(listOf("E", "G", "B"), notes[2])    // Em
        assertEquals(listOf("F", "A", "C"), notes[3])    // F
        assertEquals(listOf("G", "B", "D"), notes[4])    // G
        assertEquals(listOf("A", "C", "E"), notes[5])    // Am
        assertEquals(listOf("B", "D", "F"), notes[6])    // Bdim
    }

    @Test
    fun `C Ionian triads have correct qualities`() {
        val qualities = triads(C, ScaleType.IONIAN).chords.map { it.triadQuality }
        assertEquals(
            listOf(
                ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MINOR,
                ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MINOR,
                ChordQuality.DIMINISHED,
            ),
            qualities,
        )
    }

    @Test
    fun `C Ionian triads have null seventh quality`() {
        triads(C, ScaleType.IONIAN).chords.forEach { chord ->
            assertNull("Expected null seventhQuality for triads-only", chord.seventhQuality)
        }
    }

    @Test
    fun `C Ionian triads have degree 1 to 7`() {
        val degrees = triads(C, ScaleType.IONIAN).chords.map { it.degree }
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), degrees)
    }

    @Test
    fun `C Ionian triads have exactly 3 note names each`() {
        triads(C, ScaleType.IONIAN).chords.forEach { chord ->
            assertEquals(3, chord.noteNames.size)
        }
    }

    // ── C Ionian: sevenths ────────────────────────────────────────────────────────

    @Test
    fun `C Ionian sevenths have correct symbols`() {
        val symbols = sevenths(C, ScaleType.IONIAN).chords.map { it.symbol }
        assertEquals(listOf("Cmaj7", "Dm7", "Em7", "Fmaj7", "G7", "Am7", "Bm7♭5"), symbols)
    }

    @Test
    fun `C Ionian sevenths have correct seventh qualities`() {
        val seventhQualities = sevenths(C, ScaleType.IONIAN).chords.map { it.seventhQuality }
        assertEquals(
            listOf(
                SeventhQuality.MAJOR_SEVENTH,
                SeventhQuality.MINOR_SEVENTH,
                SeventhQuality.MINOR_SEVENTH,
                SeventhQuality.MAJOR_SEVENTH,
                SeventhQuality.DOMINANT_SEVENTH,
                SeventhQuality.MINOR_SEVENTH,
                SeventhQuality.HALF_DIMINISHED,
            ),
            seventhQualities,
        )
    }

    @Test
    fun `C Ionian sevenths have correct note names`() {
        val notes = sevenths(C, ScaleType.IONIAN).chords.map { it.noteNames }
        assertEquals(listOf("C", "E", "G", "B"), notes[0])     // Cmaj7
        assertEquals(listOf("D", "F", "A", "C"), notes[1])     // Dm7
        assertEquals(listOf("E", "G", "B", "D"), notes[2])     // Em7
        assertEquals(listOf("F", "A", "C", "E"), notes[3])     // Fmaj7
        assertEquals(listOf("G", "B", "D", "F"), notes[4])     // G7
        assertEquals(listOf("A", "C", "E", "G"), notes[5])     // Am7
        assertEquals(listOf("B", "D", "F", "A"), notes[6])     // Bm7♭5
    }

    @Test
    fun `C Ionian sevenths have exactly 4 note names each`() {
        sevenths(C, ScaleType.IONIAN).chords.forEach { chord ->
            assertEquals(4, chord.noteNames.size)
        }
    }

    @Test
    fun `C Ionian sevenths have non-null seventh quality`() {
        sevenths(C, ScaleType.IONIAN).chords.forEach { chord ->
            assertNotNull("Expected non-null seventhQuality for seventh chords", chord.seventhQuality)
        }
    }

    // ── A Aeolian: triads ─────────────────────────────────────────────────────────

    @Test
    fun `A Aeolian triads have correct symbols`() {
        val symbols = triads(A, ScaleType.AEOLIAN).chords.map { it.symbol }
        assertEquals(listOf("Am", "Bdim", "C", "Dm", "Em", "F", "G"), symbols)
    }

    @Test
    fun `A Aeolian triads have correct roman numerals`() {
        val romans = triads(A, ScaleType.AEOLIAN).chords.map { it.romanNumeral }
        assertEquals(listOf("i", "ii°", "III", "iv", "v", "VI", "VII"), romans)
    }

    // ── A Harmonic Minor: triads ──────────────────────────────────────────────────

    @Test
    fun `A Harmonic Minor triads have correct symbols including augmented`() {
        val symbols = triads(A, ScaleType.HARMONIC_MINOR).chords.map { it.symbol }
        assertEquals(listOf("Am", "Bdim", "Caug", "Dm", "E", "F", "G♯dim"), symbols)
    }

    @Test
    fun `A Harmonic Minor triads include all four triad qualities`() {
        val qualities = triads(A, ScaleType.HARMONIC_MINOR).chords.map { it.triadQuality }.toSet()
        assertEquals(
            setOf(
                ChordQuality.MINOR,
                ChordQuality.DIMINISHED,
                ChordQuality.AUGMENTED,
                ChordQuality.MAJOR,
            ),
            qualities,
        )
    }

    @Test
    fun `A Harmonic Minor triads have correct roman numerals`() {
        val romans = triads(A, ScaleType.HARMONIC_MINOR).chords.map { it.romanNumeral }
        assertEquals(listOf("i", "ii°", "III+", "iv", "V", "VI", "vii°"), romans)
    }

    @Test
    fun `A Harmonic Minor G-sharp is spelled correctly not A-flat`() {
        val gSharpChord = triads(A, ScaleType.HARMONIC_MINOR).chords[6]
        assertTrue(
            "Expected G♯ spelling, got '${gSharpChord.rootName}'",
            gSharpChord.rootName.startsWith("G"),
        )
        assertEquals("G♯dim", gSharpChord.symbol)
    }

    // ── A Harmonic Minor: sevenths ────────────────────────────────────────────────

    @Test
    fun `A Harmonic Minor sevenths have correct symbols including exotic types`() {
        val symbols = sevenths(A, ScaleType.HARMONIC_MINOR).chords.map { it.symbol }
        assertEquals(
            listOf("AmMaj7", "Bm7♭5", "Cmaj7♯5", "Dm7", "E7", "Fmaj7", "G♯dim7"),
            symbols,
        )
    }

    @Test
    fun `A Harmonic Minor seventh qualities include all three exotic types`() {
        val seventhQualities =
            sevenths(A, ScaleType.HARMONIC_MINOR).chords.map { it.seventhQuality }
        assertEquals(SeventhQuality.MINOR_MAJOR_SEVENTH, seventhQualities[0])     // AmMaj7
        assertEquals(SeventhQuality.HALF_DIMINISHED, seventhQualities[1])         // Bm7♭5
        assertEquals(SeventhQuality.AUGMENTED_MAJOR_SEVENTH, seventhQualities[2]) // Cmaj7♯5
        assertEquals(SeventhQuality.MINOR_SEVENTH, seventhQualities[3])           // Dm7
        assertEquals(SeventhQuality.DOMINANT_SEVENTH, seventhQualities[4])        // E7
        assertEquals(SeventhQuality.MAJOR_SEVENTH, seventhQualities[5])           // Fmaj7
        assertEquals(SeventhQuality.DIMINISHED_SEVENTH, seventhQualities[6])      // G♯dim7
    }

    @Test
    fun `A Harmonic Minor seventh G-sharp spelling is letter-correct`() {
        val gSharpChord = sevenths(A, ScaleType.HARMONIC_MINOR).chords[6]
        assertEquals("G♯dim7", gSharpChord.symbol)
        assertEquals("G♯", gSharpChord.rootName)
    }

    // ── A Melodic Minor: sevenths ─────────────────────────────────────────────────

    @Test
    fun `A Melodic Minor sevenths have correct symbols`() {
        val symbols = sevenths(A, ScaleType.MELODIC_MINOR).chords.map { it.symbol }
        assertEquals(
            listOf("AmMaj7", "Bm7", "Cmaj7♯5", "D7", "E7", "F♯m7♭5", "G♯m7♭5"),
            symbols,
        )
    }

    @Test
    fun `A Melodic Minor seventh F-sharp is spelled correctly`() {
        val fSharpChord = sevenths(A, ScaleType.MELODIC_MINOR).chords[5]
        assertEquals("F♯m7♭5", fSharpChord.symbol)
        assertTrue(fSharpChord.rootName.startsWith("F"))
    }

    // ── Roman numeral symbols ─────────────────────────────────────────────────────

    @Test
    fun `roman numerals cover all four quality forms`() {
        // A Harmonic Minor has i, ii°, III+, iv, V, VI, vii° — exercises all four
        val romans = triads(A, ScaleType.HARMONIC_MINOR).chords.map { it.romanNumeral }
        assertTrue("Missing lowercase minor", romans.any { it == "i" || it == "iv" || it == "v" })
        assertTrue("Missing diminished °", romans.any { it.endsWith("°") })
        assertTrue("Missing augmented +", romans.any { it.endsWith("+") })
        assertTrue("Missing uppercase major", romans.any { it == "V" || it == "VI" })
    }

    // ── 12 roots × 14 ScaleType sweep: triads ────────────────────────────────────

    @Test
    fun `all 12 roots times 14 scale types produce valid triads`() {
        val validQualities = ChordQuality.entries.toSet()

        for (root in 0..11) {
            for (scaleType in ScaleType.entries) {
                val result = triads(root, scaleType)
                val label = "root=$root, type=${scaleType.name}"

                assertEquals("Expected 7 chords for $label", 7, result.chords.size)

                val rootLetters = result.chords.map { it.rootName[0] }.toSet()
                assertEquals("Expected 7 distinct root letters for $label", 7, rootLetters.size)

                result.chords.forEach { chord ->
                    assertTrue(
                        "Unexpected quality ${chord.triadQuality} for $label",
                        chord.triadQuality in validQualities,
                    )
                    assertNull("Expected null seventhQuality in triads-only for $label", chord.seventhQuality)
                    assertEquals("Expected 3 noteNames for $label", 3, chord.noteNames.size)
                }
            }
        }
    }

    // ── 12 roots × 14 ScaleType sweep: sevenths ──────────────────────────────────

    @Test
    fun `all 12 roots times 14 scale types produce valid seventh chords`() {
        val validQualities = ChordQuality.entries.toSet()
        val validSeventhQualities = SeventhQuality.entries.toSet()

        for (root in 0..11) {
            for (scaleType in ScaleType.entries) {
                val result = sevenths(root, scaleType)
                val label = "root=$root, type=${scaleType.name}"

                assertEquals("Expected 7 chords for $label", 7, result.chords.size)

                val rootLetters = result.chords.map { it.rootName[0] }.toSet()
                assertEquals("Expected 7 distinct root letters for $label", 7, rootLetters.size)

                result.chords.forEach { chord ->
                    assertTrue(
                        "Unexpected triad quality ${chord.triadQuality} for $label",
                        chord.triadQuality in validQualities,
                    )
                    assertNotNull(
                        "Expected non-null seventhQuality for $label",
                        chord.seventhQuality,
                    )
                    assertTrue(
                        "Unexpected seventh quality ${chord.seventhQuality} for $label",
                        chord.seventhQuality in validSeventhQualities,
                    )
                    assertEquals("Expected 4 noteNames for $label", 4, chord.noteNames.size)
                }
            }
        }
    }

    // ── Result is always 7 chords in degree order ─────────────────────────────────

    @Test
    fun `result always contains exactly 7 chords`() {
        assertEquals(7, triads(C, ScaleType.IONIAN).chords.size)
        assertEquals(7, triads(G, ScaleType.MIXOLYDIAN).chords.size)
        assertEquals(7, sevenths(A, ScaleType.HARMONIC_MINOR).chords.size)
        assertEquals(7, sevenths(D, ScaleType.ALTERED).chords.size)
    }

    @Test
    fun `chords are returned in degree order 1 through 7`() {
        val degrees = triads(C, ScaleType.IONIAN).chords.map { it.degree }
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), degrees)
    }
}
