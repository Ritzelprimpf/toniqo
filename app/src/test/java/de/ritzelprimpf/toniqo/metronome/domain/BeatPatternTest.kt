package de.ritzelprimpf.toniqo.metronome.domain

import de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import de.ritzelprimpf.toniqo.metronome.domain.model.clickKindFor
import de.ritzelprimpf.toniqo.metronome.domain.model.clicksPerBar
import org.junit.Assert.assertEquals
import org.junit.Test

class BeatPatternTest {

    // =========================================================================
    // clicksPerBar — every (numerator, subdivision) combination from the spec
    // =========================================================================

    // Numerator 2
    @Test fun `clicksPerBar 2 NONE is 2`() = assertEquals(2, clicksPerBar(2, Subdivision.NONE))
    @Test fun `clicksPerBar 2 EIGHTHS is 4`() = assertEquals(4, clicksPerBar(2, Subdivision.EIGHTHS))
    @Test fun `clicksPerBar 2 SIXTEENTHS is 8`() = assertEquals(8, clicksPerBar(2, Subdivision.SIXTEENTHS))
    @Test fun `clicksPerBar 2 TRIPLETS is 6`() = assertEquals(6, clicksPerBar(2, Subdivision.TRIPLETS))

    // Numerator 3
    @Test fun `clicksPerBar 3 NONE is 3`() = assertEquals(3, clicksPerBar(3, Subdivision.NONE))
    @Test fun `clicksPerBar 3 EIGHTHS is 6`() = assertEquals(6, clicksPerBar(3, Subdivision.EIGHTHS))
    @Test fun `clicksPerBar 3 SIXTEENTHS is 12`() = assertEquals(12, clicksPerBar(3, Subdivision.SIXTEENTHS))
    @Test fun `clicksPerBar 3 TRIPLETS is 9`() = assertEquals(9, clicksPerBar(3, Subdivision.TRIPLETS))

    // Numerator 4 (4/4 — the most commonly tested time signature)
    @Test fun `clicksPerBar 4 NONE is 4`() = assertEquals(4, clicksPerBar(4, Subdivision.NONE))
    @Test fun `clicksPerBar 4 EIGHTHS is 8`() = assertEquals(8, clicksPerBar(4, Subdivision.EIGHTHS))
    @Test fun `clicksPerBar 4 SIXTEENTHS is 16`() = assertEquals(16, clicksPerBar(4, Subdivision.SIXTEENTHS))
    @Test fun `clicksPerBar 4 TRIPLETS is 12`() = assertEquals(12, clicksPerBar(4, Subdivision.TRIPLETS))

    // Numerator 5
    @Test fun `clicksPerBar 5 NONE is 5`() = assertEquals(5, clicksPerBar(5, Subdivision.NONE))
    @Test fun `clicksPerBar 5 EIGHTHS is 10`() = assertEquals(10, clicksPerBar(5, Subdivision.EIGHTHS))
    @Test fun `clicksPerBar 5 SIXTEENTHS is 20`() = assertEquals(20, clicksPerBar(5, Subdivision.SIXTEENTHS))
    @Test fun `clicksPerBar 5 TRIPLETS is 15`() = assertEquals(15, clicksPerBar(5, Subdivision.TRIPLETS))

    // Numerator 6 (6/8)
    @Test fun `clicksPerBar 6 NONE is 6`() = assertEquals(6, clicksPerBar(6, Subdivision.NONE))
    @Test fun `clicksPerBar 6 EIGHTHS is 12`() = assertEquals(12, clicksPerBar(6, Subdivision.EIGHTHS))
    @Test fun `clicksPerBar 6 SIXTEENTHS is 24`() = assertEquals(24, clicksPerBar(6, Subdivision.SIXTEENTHS))
    @Test fun `clicksPerBar 6 TRIPLETS is 18`() = assertEquals(18, clicksPerBar(6, Subdivision.TRIPLETS))

    // Numerator 7 (7/8)
    @Test fun `clicksPerBar 7 NONE is 7`() = assertEquals(7, clicksPerBar(7, Subdivision.NONE))
    @Test fun `clicksPerBar 7 EIGHTHS is 14`() = assertEquals(14, clicksPerBar(7, Subdivision.EIGHTHS))
    @Test fun `clicksPerBar 7 SIXTEENTHS is 28`() = assertEquals(28, clicksPerBar(7, Subdivision.SIXTEENTHS))
    @Test fun `clicksPerBar 7 TRIPLETS is 21`() = assertEquals(21, clicksPerBar(7, Subdivision.TRIPLETS))

    // Numerator 9 (9/8)
    @Test fun `clicksPerBar 9 NONE is 9`() = assertEquals(9, clicksPerBar(9, Subdivision.NONE))
    @Test fun `clicksPerBar 9 EIGHTHS is 18`() = assertEquals(18, clicksPerBar(9, Subdivision.EIGHTHS))
    @Test fun `clicksPerBar 9 SIXTEENTHS is 36`() = assertEquals(36, clicksPerBar(9, Subdivision.SIXTEENTHS))
    @Test fun `clicksPerBar 9 TRIPLETS is 27`() = assertEquals(27, clicksPerBar(9, Subdivision.TRIPLETS))

    // Numerator 12 (12/8)
    @Test fun `clicksPerBar 12 NONE is 12`() = assertEquals(12, clicksPerBar(12, Subdivision.NONE))
    @Test fun `clicksPerBar 12 EIGHTHS is 24`() = assertEquals(24, clicksPerBar(12, Subdivision.EIGHTHS))
    @Test fun `clicksPerBar 12 SIXTEENTHS is 48`() = assertEquals(48, clicksPerBar(12, Subdivision.SIXTEENTHS))
    @Test fun `clicksPerBar 12 TRIPLETS is 36`() = assertEquals(36, clicksPerBar(12, Subdivision.TRIPLETS))

    // =========================================================================
    // clickKindFor — full-bar walkthrough in 4/4 for each subdivision
    // =========================================================================

    @Test
    fun `clickKindFor 4-4 with NONE all non-zero indices are STANDARD`() {
        // NONE multiplier = 1; every non-zero index is a multiple of 1 → STANDARD
        assertEquals(ClickKind.ACCENTED, clickKindFor(0, Subdivision.NONE))
        assertEquals(ClickKind.STANDARD, clickKindFor(1, Subdivision.NONE))
        assertEquals(ClickKind.STANDARD, clickKindFor(2, Subdivision.NONE))
        assertEquals(ClickKind.STANDARD, clickKindFor(3, Subdivision.NONE))
    }

    @Test
    fun `clickKindFor 4-4 with EIGHTHS alternates STANDARD and SUBDIVISION`() {
        // Beat positions (multiples of 2): 0→ACCENTED, 2→STANDARD, 4→STANDARD, 6→STANDARD
        // Off-beats (not multiples of 2): 1, 3, 5, 7 → SUBDIVISION
        assertEquals(ClickKind.ACCENTED, clickKindFor(0, Subdivision.EIGHTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(1, Subdivision.EIGHTHS))
        assertEquals(ClickKind.STANDARD, clickKindFor(2, Subdivision.EIGHTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(3, Subdivision.EIGHTHS))
        assertEquals(ClickKind.STANDARD, clickKindFor(4, Subdivision.EIGHTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(5, Subdivision.EIGHTHS))
        assertEquals(ClickKind.STANDARD, clickKindFor(6, Subdivision.EIGHTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(7, Subdivision.EIGHTHS))
    }

    @Test
    fun `clickKindFor 4-4 with SIXTEENTHS beat positions are STANDARD others are SUBDIVISION`() {
        // Beat positions (multiples of 4): 0→ACCENTED, 4→STANDARD, 8→STANDARD, 12→STANDARD
        // All others (1,2,3,5,6,7,9,10,11,13,14,15) → SUBDIVISION
        assertEquals(ClickKind.ACCENTED, clickKindFor(0, Subdivision.SIXTEENTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(1, Subdivision.SIXTEENTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(2, Subdivision.SIXTEENTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(3, Subdivision.SIXTEENTHS))
        assertEquals(ClickKind.STANDARD, clickKindFor(4, Subdivision.SIXTEENTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(5, Subdivision.SIXTEENTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(6, Subdivision.SIXTEENTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(7, Subdivision.SIXTEENTHS))
        assertEquals(ClickKind.STANDARD, clickKindFor(8, Subdivision.SIXTEENTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(9, Subdivision.SIXTEENTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(10, Subdivision.SIXTEENTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(11, Subdivision.SIXTEENTHS))
        assertEquals(ClickKind.STANDARD, clickKindFor(12, Subdivision.SIXTEENTHS))
    }

    @Test
    fun `clickKindFor 4-4 with TRIPLETS beat positions are STANDARD others are SUBDIVISION`() {
        // Beat positions (multiples of 3): 0→ACCENTED, 3→STANDARD, 6→STANDARD, 9→STANDARD
        // All others (1,2,4,5,7,8,10,11) → SUBDIVISION
        assertEquals(ClickKind.ACCENTED, clickKindFor(0, Subdivision.TRIPLETS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(1, Subdivision.TRIPLETS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(2, Subdivision.TRIPLETS))
        assertEquals(ClickKind.STANDARD, clickKindFor(3, Subdivision.TRIPLETS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(4, Subdivision.TRIPLETS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(5, Subdivision.TRIPLETS))
        assertEquals(ClickKind.STANDARD, clickKindFor(6, Subdivision.TRIPLETS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(7, Subdivision.TRIPLETS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(8, Subdivision.TRIPLETS))
        assertEquals(ClickKind.STANDARD, clickKindFor(9, Subdivision.TRIPLETS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(10, Subdivision.TRIPLETS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(11, Subdivision.TRIPLETS))
    }

    // =========================================================================
    // EIGHTHS-in-/8 no-op identity (6/8 with EIGHTHS)
    // =========================================================================

    @Test
    fun `clickKindFor 6-8 with EIGHTHS produces expected pattern including no-op identity`() {
        // Spec (Phase6-Metronome-Decisions.md Item 8): selecting EIGHTHS when the time-signature
        // denominator is /8 is mathematically a no-op — the beat unit is already an eighth, so
        // "eighth subdivision" adds no new information. The function still produces a well-defined
        // output: index 0 is ACCENTED; even non-zero indices are STANDARD (multiples of 2);
        // odd indices are SUBDIVISION. This is the function's actual behavior, not a special case.
        //
        // clicksPerBar(6, EIGHTHS) = 12 — so indices 0..11 cover the bar.
        assertEquals(12, clicksPerBar(6, Subdivision.EIGHTHS))

        // Index 0: downbeat → ACCENTED
        assertEquals(ClickKind.ACCENTED, clickKindFor(0, Subdivision.EIGHTHS))
        // Even non-zero indices → STANDARD (multiples of multiplier 2)
        assertEquals(ClickKind.STANDARD, clickKindFor(2, Subdivision.EIGHTHS))
        assertEquals(ClickKind.STANDARD, clickKindFor(4, Subdivision.EIGHTHS))
        assertEquals(ClickKind.STANDARD, clickKindFor(6, Subdivision.EIGHTHS))
        assertEquals(ClickKind.STANDARD, clickKindFor(8, Subdivision.EIGHTHS))
        assertEquals(ClickKind.STANDARD, clickKindFor(10, Subdivision.EIGHTHS))
        // Odd indices → SUBDIVISION
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(1, Subdivision.EIGHTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(3, Subdivision.EIGHTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(5, Subdivision.EIGHTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(7, Subdivision.EIGHTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(9, Subdivision.EIGHTHS))
        assertEquals(ClickKind.SUBDIVISION, clickKindFor(11, Subdivision.EIGHTHS))
    }
}
