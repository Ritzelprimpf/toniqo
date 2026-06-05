package de.ritzelprimpf.toniqo.chordfinder.domain

import de.ritzelprimpf.toniqo.chordfinder.domain.model.SeventhQuality
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ChordQualityResolverTest {

    // ── Triad: valid pairs ────────────────────────────────────────────────────────

    @Test
    fun `triad (4,7) resolves to MAJOR`() {
        assertEquals(ChordQuality.MAJOR, ChordQualityResolver.triad(4, 7))
    }

    @Test
    fun `triad (3,7) resolves to MINOR`() {
        assertEquals(ChordQuality.MINOR, ChordQualityResolver.triad(3, 7))
    }

    @Test
    fun `triad (3,6) resolves to DIMINISHED`() {
        assertEquals(ChordQuality.DIMINISHED, ChordQualityResolver.triad(3, 6))
    }

    @Test
    fun `triad (4,8) resolves to AUGMENTED`() {
        assertEquals(ChordQuality.AUGMENTED, ChordQualityResolver.triad(4, 8))
    }

    // ── Triad: invalid pair throws ────────────────────────────────────────────────

    @Test
    fun `triad with invalid pair throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            ChordQualityResolver.triad(5, 7)
        }
    }

    @Test
    fun `triad with another invalid pair throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            ChordQualityResolver.triad(4, 6)
        }
    }

    // ── Seventh: valid combinations ───────────────────────────────────────────────

    @Test
    fun `seventh (MAJOR,11) resolves to MAJOR_SEVENTH`() {
        assertEquals(
            SeventhQuality.MAJOR_SEVENTH,
            ChordQualityResolver.seventh(ChordQuality.MAJOR, 11),
        )
    }

    @Test
    fun `seventh (MINOR,10) resolves to MINOR_SEVENTH`() {
        assertEquals(
            SeventhQuality.MINOR_SEVENTH,
            ChordQualityResolver.seventh(ChordQuality.MINOR, 10),
        )
    }

    @Test
    fun `seventh (MAJOR,10) resolves to DOMINANT_SEVENTH`() {
        assertEquals(
            SeventhQuality.DOMINANT_SEVENTH,
            ChordQualityResolver.seventh(ChordQuality.MAJOR, 10),
        )
    }

    @Test
    fun `seventh (DIMINISHED,10) resolves to HALF_DIMINISHED`() {
        assertEquals(
            SeventhQuality.HALF_DIMINISHED,
            ChordQualityResolver.seventh(ChordQuality.DIMINISHED, 10),
        )
    }

    @Test
    fun `seventh (DIMINISHED,9) resolves to DIMINISHED_SEVENTH`() {
        assertEquals(
            SeventhQuality.DIMINISHED_SEVENTH,
            ChordQualityResolver.seventh(ChordQuality.DIMINISHED, 9),
        )
    }

    @Test
    fun `seventh (MINOR,11) resolves to MINOR_MAJOR_SEVENTH`() {
        assertEquals(
            SeventhQuality.MINOR_MAJOR_SEVENTH,
            ChordQualityResolver.seventh(ChordQuality.MINOR, 11),
        )
    }

    @Test
    fun `seventh (AUGMENTED,11) resolves to AUGMENTED_MAJOR_SEVENTH`() {
        assertEquals(
            SeventhQuality.AUGMENTED_MAJOR_SEVENTH,
            ChordQualityResolver.seventh(ChordQuality.AUGMENTED, 11),
        )
    }

    // ── Seventh: invalid combinations throw ───────────────────────────────────────

    @Test
    fun `seventh with invalid combination throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            // AUGMENTED + minor seventh has no diatonic interpretation in any of the 14 types
            ChordQualityResolver.seventh(ChordQuality.AUGMENTED, 10)
        }
    }

    @Test
    fun `seventh with another invalid combination throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            // MAJOR + diminished seventh is not one of the seven valid types
            ChordQualityResolver.seventh(ChordQuality.MAJOR, 9)
        }
    }
}
