package de.ritzelprimpf.toniqo.keyfinder.presentation

import de.ritzelprimpf.toniqo.common.model.ScaleType
import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleCandidate
import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleMatch
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure unit tests for [scaleLabelData] and [scaleDegreeLabel].
 *
 * These functions are the only place where Key Finder display strings are assembled. Tests assert
 * that representative scale types map to the correct string-resource keys and spelled roots, and
 * that degree labels are computed correctly for a cross-section of interval patterns.
 *
 * No Android [android.content.Context] is required — the test runs on the JVM.
 */
class KeyFinderLabelMappingTest {

    // ── scaleLabelData ───────────────────────────────────────────────────────

    @Test
    fun `C Major maps to major label key and Ionian subtitle with root C`() {
        val data = scaleLabelData(match(rootPitchClass = 0, type = ScaleType.IONIAN))
        assertEquals("scale_type_label_major", data.primaryLabelKey)
        assertEquals("scale_type_subtitle_ionian", data.subtitleKey)
        assertEquals("C", data.spelledRoot)
    }

    @Test
    fun `A Natural Minor maps to natural minor label key and Aeolian subtitle with root A`() {
        val data = scaleLabelData(match(rootPitchClass = 9, type = ScaleType.AEOLIAN))
        assertEquals("scale_type_label_natural_minor", data.primaryLabelKey)
        assertEquals("scale_type_subtitle_aeolian", data.subtitleKey)
        assertEquals("A", data.spelledRoot)
    }

    @Test
    fun `E Phrygian Dominant maps to phrygian dominant label key with root E`() {
        val data = scaleLabelData(match(rootPitchClass = 4, type = ScaleType.PHRYGIAN_DOMINANT))
        assertEquals("scale_type_label_phrygian_dominant", data.primaryLabelKey)
        assertEquals("scale_type_subtitle_phrygian_dominant", data.subtitleKey)
        assertEquals("E", data.spelledRoot)
    }

    @Test
    fun `G Altered maps to altered label key with root G`() {
        val data = scaleLabelData(match(rootPitchClass = 7, type = ScaleType.ALTERED))
        assertEquals("scale_type_label_altered", data.primaryLabelKey)
        assertEquals("scale_type_subtitle_altered", data.subtitleKey)
        assertEquals("G", data.spelledRoot)
    }

    @Test
    fun `Bb Melodic Minor uses canonical root spelling Bb not A#`() {
        val data = scaleLabelData(match(rootPitchClass = 10, type = ScaleType.MELODIC_MINOR))
        assertEquals("B♭", data.spelledRoot)
    }

    @Test
    fun `F# Lydian Dominant uses canonical root spelling Fs`() {
        val data = scaleLabelData(match(rootPitchClass = 6, type = ScaleType.LYDIAN_DOMINANT))
        assertEquals("F♯", data.spelledRoot)
    }

    // ── scaleDegreeLabel ─────────────────────────────────────────────────────

    @Test
    fun `major scale degree labels are all natural`() {
        val majorIntervals = intArrayOf(0, 2, 4, 5, 7, 9, 11)
        val expected = listOf("1", "2", "3", "4", "5", "6", "7")
        val actual = majorIntervals.mapIndexed { i, interval -> scaleDegreeLabel(i, interval) }
        assertEquals(expected, actual)
    }

    @Test
    fun `natural minor degree labels include three flats`() {
        val naturalMinorIntervals = intArrayOf(0, 2, 3, 5, 7, 8, 10)
        val expected = listOf("1", "2", "♭3", "4", "5", "♭6", "♭7")
        val actual = naturalMinorIntervals.mapIndexed { i, interval -> scaleDegreeLabel(i, interval) }
        assertEquals(expected, actual)
    }

    @Test
    fun `Lydian degree labels have a raised 4`() {
        val lydianIntervals = intArrayOf(0, 2, 4, 6, 7, 9, 11)
        val expected = listOf("1", "2", "3", "♯4", "5", "6", "7")
        val actual = lydianIntervals.mapIndexed { i, interval -> scaleDegreeLabel(i, interval) }
        assertEquals(expected, actual)
    }

    @Test
    fun `Locrian degree labels include lowered 2 and 5`() {
        val locrianIntervals = intArrayOf(0, 1, 3, 5, 6, 8, 10)
        val expected = listOf("1", "♭2", "♭3", "4", "♭5", "♭6", "♭7")
        val actual = locrianIntervals.mapIndexed { i, interval -> scaleDegreeLabel(i, interval) }
        assertEquals(expected, actual)
    }

    @Test
    fun `Harmonic minor has natural 7 (raised compared to natural minor)`() {
        val hmIntervals = intArrayOf(0, 2, 3, 5, 7, 8, 11)
        val labels = hmIntervals.mapIndexed { i, interval -> scaleDegreeLabel(i, interval) }
        assertEquals("7", labels[6])  // natural 7th
        assertEquals("♭6", labels[5])
    }

    @Test
    fun `Phrygian Dominant has lowered 2 and natural 3`() {
        val intervals = intArrayOf(0, 1, 4, 5, 7, 8, 10)
        val labels = intervals.mapIndexed { i, interval -> scaleDegreeLabel(i, interval) }
        assertEquals("♭2", labels[1])
        assertEquals("3", labels[2])  // natural 3rd (distinguishes from Phrygian)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun match(rootPitchClass: Int, type: ScaleType) = ScaleMatch(
        candidate = ScaleCandidate(rootPitchClass = rootPitchClass, type = type),
        percent = 100,
        isFull = true,
        isRootMatch = false,
        rank = 1,
    )
}
