package de.ritzelprimpf.toniqo.chordfinder.domain.model

import de.ritzelprimpf.toniqo.common.model.ChordQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class ChordKeyTest {

    // ── Triads: index-coincidental cases still classify correctly ──────────────────

    @Test
    fun `classifyToneRole returns ROOT for the root pitch class`() {
        val key = ChordKey(0, ChordQuality.MAJOR)
        assertEquals(ChordToneRole.ROOT, key.classifyToneRole(0))
    }

    @Test
    fun `classifyToneRole returns THIRD for a major triad's major third`() {
        val key = ChordKey(0, ChordQuality.MAJOR) // C major: third = E (pc 4)
        assertEquals(ChordToneRole.THIRD, key.classifyToneRole(4))
    }

    @Test
    fun `classifyToneRole returns THIRD for a minor triad's minor third`() {
        val key = ChordKey(0, ChordQuality.MINOR) // C minor: third = Eb (pc 3)
        assertEquals(ChordToneRole.THIRD, key.classifyToneRole(3))
    }

    @Test
    fun `classifyToneRole returns FIFTH for a diminished triad's diminished fifth`() {
        val key = ChordKey(0, ChordQuality.DIMINISHED) // C dim: fifth = Gb (pc 6)
        assertEquals(ChordToneRole.FIFTH, key.classifyToneRole(6))
    }

    @Test
    fun `classifyToneRole returns FIFTH for an augmented triad's augmented fifth`() {
        val key = ChordKey(0, ChordQuality.AUGMENTED) // C aug: fifth = G# (pc 8)
        assertEquals(ChordToneRole.FIFTH, key.classifyToneRole(8))
    }

    @Test
    fun `classifyToneRole returns OTHER for a pitch class outside the chord`() {
        val key = ChordKey(0, ChordQuality.MAJOR)
        assertEquals(ChordToneRole.OTHER, key.classifyToneRole(1)) // C# is not a C major tone
    }

    @Test
    fun `classifyToneRole wraps root pitch class arithmetic across the octave boundary`() {
        val key = ChordKey(11, ChordQuality.MAJOR) // B major: third = D# (pc 3), fifth = F# (pc 6)
        assertEquals(ChordToneRole.ROOT, key.classifyToneRole(11))
        assertEquals(ChordToneRole.THIRD, key.classifyToneRole(3))
        assertEquals(ChordToneRole.FIFTH, key.classifyToneRole(6))
    }

    // ── POWER: the case that broke the old index-based [1]/[2] lookup ──────────────
    // POWER's intervalsFromRoot = [0, 7] has only 2 elements, and its non-root interval (7) is a
    // fifth, sitting at index 1 -- exactly where a triad's *third* used to be assumed to live.

    @Test
    fun `classifyToneRole returns FIFTH for POWER's fifth, not THIRD`() {
        val key = ChordKey(7, ChordQuality.POWER) // G5: fifth = D (pc 2)
        assertEquals(ChordToneRole.FIFTH, key.classifyToneRole(2))
    }

    @Test
    fun `classifyToneRole returns ROOT for POWER's root`() {
        val key = ChordKey(7, ChordQuality.POWER)
        assertEquals(ChordToneRole.ROOT, key.classifyToneRole(7))
    }

    @Test
    fun `classifyToneRole returns OTHER for POWER's absent third, never throws`() {
        // C5's "third" pitch classes (Eb=3, E=4) are simply not part of a power chord.
        val key = ChordKey(0, ChordQuality.POWER)
        assertEquals(ChordToneRole.OTHER, key.classifyToneRole(3))
        assertEquals(ChordToneRole.OTHER, key.classifyToneRole(4))
    }

    // ── Seventh chords: the 4th tone and its role classification ───────────────────

    @Test
    fun `classifyToneRole returns SEVENTH for a dominant seventh's minor seventh`() {
        val key = ChordKey(7, ChordQuality.MAJOR, SeventhQuality.DOMINANT_SEVENTH) // G7: seventh = F (pc 5)
        assertEquals(ChordToneRole.SEVENTH, key.classifyToneRole(5))
    }

    @Test
    fun `classifyToneRole returns SEVENTH for a major seventh's major seventh`() {
        val key = ChordKey(0, ChordQuality.MAJOR, SeventhQuality.MAJOR_SEVENTH) // Cmaj7: seventh = B (pc 11)
        assertEquals(ChordToneRole.SEVENTH, key.classifyToneRole(11))
    }

    @Test
    fun `classifyToneRole still returns ROOT THIRD FIFTH for a seventh chord's triad tones`() {
        val key = ChordKey(0, ChordQuality.MAJOR, SeventhQuality.MAJOR_SEVENTH)
        assertEquals(ChordToneRole.ROOT, key.classifyToneRole(0))
        assertEquals(ChordToneRole.THIRD, key.classifyToneRole(4))
        assertEquals(ChordToneRole.FIFTH, key.classifyToneRole(7))
    }

    @Test
    fun `classifyToneRole returns OTHER for a pitch class outside a seventh chord`() {
        val key = ChordKey(0, ChordQuality.MAJOR, SeventhQuality.MAJOR_SEVENTH)
        assertEquals(ChordToneRole.OTHER, key.classifyToneRole(1)) // C# is not a Cmaj7 tone
    }

    @Test
    fun `classifyToneRole never returns SEVENTH when seventhQuality is null`() {
        val key = ChordKey(0, ChordQuality.MAJOR)
        // B (pc 11) would be Cmaj7's seventh, but this key is a plain triad -- must be OTHER.
        assertEquals(ChordToneRole.OTHER, key.classifyToneRole(11))
    }

    @Test
    fun `classifyToneRole wraps seventh pitch class arithmetic across the octave boundary`() {
        val key = ChordKey(11, ChordQuality.MAJOR, SeventhQuality.MAJOR_SEVENTH) // B major7: seventh = A# (pc 10)
        assertEquals(ChordToneRole.SEVENTH, key.classifyToneRole(10))
    }
}
