package de.ritzelprimpf.toniqo.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChordTest {

    private val c4 = Note(NoteName.C, 4)

    // ── Data-class contract ───────────────────────────────────────────────────────

    @Test
    fun `equality depends on root and quality only`() {
        val a = Chord(c4, ChordQuality.MAJOR)
        val b = Chord(c4, ChordQuality.MAJOR)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `chords differ when quality differs`() {
        assertNotEquals(Chord(c4, ChordQuality.MAJOR), Chord(c4, ChordQuality.MINOR))
    }

    // ── Derived notes ─────────────────────────────────────────────────────────────

    @Test
    fun `C major triad notes are C4 E4 G4`() {
        val notes = Chord(c4, ChordQuality.MAJOR).notes
        assertEquals(listOf(Note(NoteName.C, 4), Note(NoteName.E, 4), Note(NoteName.G, 4)), notes)
    }

    @Test
    fun `C minor triad notes are C4 D#4 G4`() {
        val notes = Chord(c4, ChordQuality.MINOR).notes
        assertEquals(listOf(Note(NoteName.C, 4), Note(NoteName.DSharp, 4), Note(NoteName.G, 4)), notes)
    }

    @Test
    fun `C diminished triad notes are C4 D#4 F#4`() {
        val notes = Chord(c4, ChordQuality.DIMINISHED).notes
        assertEquals(listOf(Note(NoteName.C, 4), Note(NoteName.DSharp, 4), Note(NoteName.FSharp, 4)), notes)
    }

    @Test
    fun `C minor seventh notes are C4 D#4 G4 A#4`() {
        val notes = Chord(c4, ChordQuality.MINOR_SEVENTH).notes
        assertEquals(listOf(
            Note(NoteName.C, 4), Note(NoteName.DSharp, 4),
            Note(NoteName.G, 4), Note(NoteName.ASharp, 4),
        ), notes)
    }

    @Test
    fun `B half-diminished seventh spans correct octave`() {
        val b3 = Note(NoteName.B, 3)
        val notes = Chord(b3, ChordQuality.HALF_DIMINISHED).notes
        assertEquals(listOf(
            Note(NoteName.B, 3), Note(NoteName.D, 4),
            Note(NoteName.F, 4), Note(NoteName.A, 4),
        ), notes)
    }

    // ── displayName ───────────────────────────────────────────────────────────────

    @Test
    fun `displayName for major triad is just the pitch class name`() {
        // Octave is NOT included in chord display names.
        assertEquals("C", Chord(c4, ChordQuality.MAJOR).displayName())
    }

    @Test
    fun `displayName for minor chord appends m`() {
        assertEquals("Cm", Chord(c4, ChordQuality.MINOR).displayName())
    }

    @Test
    fun `displayName for minor seventh appends m7`() {
        assertEquals("Cm7", Chord(c4, ChordQuality.MINOR_SEVENTH).displayName())
    }

    @Test
    fun `displayName for dominant seventh appends 7`() {
        assertEquals("G7", Chord(Note(NoteName.G, 4), ChordQuality.DOMINANT_SEVENTH).displayName())
    }

    @Test
    fun `displayName for major seventh appends maj7`() {
        assertEquals("Cmaj7", Chord(c4, ChordQuality.MAJOR_SEVENTH).displayName())
    }

    @Test
    fun `displayName for half-diminished uses flat-5 symbol`() {
        assertEquals("Bm7♭5", Chord(Note(NoteName.B, 3), ChordQuality.HALF_DIMINISHED).displayName())
    }

    @Test
    fun `displayName uses sharp spelling for accidentals`() {
        assertEquals("C#m7", Chord(Note(NoteName.CSharp, 4), ChordQuality.MINOR_SEVENTH).displayName())
    }
}
