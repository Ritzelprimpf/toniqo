package de.ritzelprimpf.toniqo.common.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleTypeTest {

    // ─────────────────────────── Inventory shape ───────────────────────────

    @Test
    fun `exactly 14 scale types are defined`() {
        assertEquals(14, ScaleType.entries.size)
    }

    @Test
    fun `every type has exactly 7 interval values`() {
        ScaleType.entries.forEach { type ->
            assertEquals("${type.name} should have 7 degrees", 7, type.intervalsFromRoot.size)
        }
    }

    @Test
    fun `every type starts on the unison offset 0`() {
        ScaleType.entries.forEach { type ->
            assertEquals("${type.name} first offset should be 0", 0, type.intervalsFromRoot[0])
        }
    }

    @Test
    fun `every type has strictly ascending intervals`() {
        ScaleType.entries.forEach { type ->
            val iv = type.intervalsFromRoot
            for (i in 1 until iv.size) {
                assertTrue(
                    "${type.name}: interval[$i]=${iv[i]} must be > interval[${i-1}]=${iv[i-1]}",
                    iv[i] > iv[i - 1],
                )
            }
        }
    }

    @Test
    fun `all interval values are within 0 to 11`() {
        ScaleType.entries.forEach { type ->
            type.intervalsFromRoot.forEach { interval ->
                assertTrue("${type.name}: interval $interval out of range 0-11", interval in 0..11)
            }
        }
    }

    // ─────────────────────── rankOrder uniqueness & range ──────────────────

    @Test
    fun `rankOrder values are unique across all 14 types`() {
        val orders = ScaleType.entries.map { it.rankOrder }
        assertEquals("rankOrder values must all be distinct", orders.size, orders.distinct().size)
    }

    @Test
    fun `rankOrder covers exactly 0 through 13`() {
        val orders = ScaleType.entries.map { it.rankOrder }.sorted()
        assertEquals((0..13).toList(), orders)
    }

    @Test
    fun `rankOrder matches the common-first ordering`() {
        val expectedOrder = listOf(
            ScaleType.IONIAN,
            ScaleType.AEOLIAN,
            ScaleType.DORIAN,
            ScaleType.PHRYGIAN,
            ScaleType.LYDIAN,
            ScaleType.MIXOLYDIAN,
            ScaleType.LOCRIAN,
            ScaleType.HARMONIC_MINOR,
            ScaleType.PHRYGIAN_DOMINANT,
            ScaleType.LOCRIAN_NATURAL_6,
            ScaleType.MELODIC_MINOR,
            ScaleType.LYDIAN_DOMINANT,
            ScaleType.ALTERED,
            ScaleType.DORIAN_FLAT_2,
        )
        expectedOrder.forEachIndexed { expectedRankOrder, type ->
            assertEquals(
                "${type.name} should have rankOrder $expectedRankOrder",
                expectedRankOrder,
                type.rankOrder,
            )
        }
    }

    // ─────────────────── Diatonic types match Mode patterns ────────────────

    @Test
    fun `IONIAN intervals equal Mode IONIAN — single source of truth`() {
        assertArrayEquals(Mode.IONIAN.intervalsFromRoot, ScaleType.IONIAN.intervalsFromRoot)
    }

    @Test
    fun `DORIAN intervals equal Mode DORIAN`() {
        assertArrayEquals(Mode.DORIAN.intervalsFromRoot, ScaleType.DORIAN.intervalsFromRoot)
    }

    @Test
    fun `PHRYGIAN intervals equal Mode PHRYGIAN`() {
        assertArrayEquals(Mode.PHRYGIAN.intervalsFromRoot, ScaleType.PHRYGIAN.intervalsFromRoot)
    }

    @Test
    fun `LYDIAN intervals equal Mode LYDIAN`() {
        assertArrayEquals(Mode.LYDIAN.intervalsFromRoot, ScaleType.LYDIAN.intervalsFromRoot)
    }

    @Test
    fun `MIXOLYDIAN intervals equal Mode MIXOLYDIAN`() {
        assertArrayEquals(Mode.MIXOLYDIAN.intervalsFromRoot, ScaleType.MIXOLYDIAN.intervalsFromRoot)
    }

    @Test
    fun `AEOLIAN intervals equal Mode AEOLIAN`() {
        assertArrayEquals(Mode.AEOLIAN.intervalsFromRoot, ScaleType.AEOLIAN.intervalsFromRoot)
    }

    @Test
    fun `LOCRIAN intervals equal Mode LOCRIAN`() {
        assertArrayEquals(Mode.LOCRIAN.intervalsFromRoot, ScaleType.LOCRIAN.intervalsFromRoot)
    }

    // ─────────────────── Exact interval patterns for all 14 ────────────────

    @Test
    fun `Harmonic Minor pattern is correct`() {
        assertArrayEquals(intArrayOf(0, 2, 3, 5, 7, 8, 11), ScaleType.HARMONIC_MINOR.intervalsFromRoot)
    }

    @Test
    fun `Phrygian Dominant pattern is correct`() {
        assertArrayEquals(intArrayOf(0, 1, 4, 5, 7, 8, 10), ScaleType.PHRYGIAN_DOMINANT.intervalsFromRoot)
    }

    @Test
    fun `Locrian Natural 6 pattern is correct`() {
        assertArrayEquals(intArrayOf(0, 1, 3, 5, 6, 9, 10), ScaleType.LOCRIAN_NATURAL_6.intervalsFromRoot)
    }

    @Test
    fun `Melodic Minor pattern is correct`() {
        assertArrayEquals(intArrayOf(0, 2, 3, 5, 7, 9, 11), ScaleType.MELODIC_MINOR.intervalsFromRoot)
    }

    @Test
    fun `Lydian Dominant pattern is correct`() {
        assertArrayEquals(intArrayOf(0, 2, 4, 6, 7, 9, 10), ScaleType.LYDIAN_DOMINANT.intervalsFromRoot)
    }

    @Test
    fun `Altered pattern is correct`() {
        assertArrayEquals(intArrayOf(0, 1, 3, 4, 6, 8, 10), ScaleType.ALTERED.intervalsFromRoot)
    }

    @Test
    fun `Dorian Flat 2 pattern is correct`() {
        assertArrayEquals(intArrayOf(0, 1, 3, 5, 7, 9, 10), ScaleType.DORIAN_FLAT_2.intervalsFromRoot)
    }

    // ──────────────────────── Family assignments ────────────────────────────

    @Test
    fun `all seven diatonic types belong to the DIATONIC family`() {
        val diatonicTypes = setOf(
            ScaleType.IONIAN, ScaleType.DORIAN, ScaleType.PHRYGIAN,
            ScaleType.LYDIAN, ScaleType.MIXOLYDIAN, ScaleType.AEOLIAN, ScaleType.LOCRIAN,
        )
        diatonicTypes.forEach { type ->
            assertEquals("${type.name} should be DIATONIC", ScaleFamily.DIATONIC, type.family)
        }
    }

    @Test
    fun `harmonic-minor family types are assigned correctly`() {
        assertEquals(ScaleFamily.HARMONIC_MINOR, ScaleType.HARMONIC_MINOR.family)
        assertEquals(ScaleFamily.HARMONIC_MINOR, ScaleType.PHRYGIAN_DOMINANT.family)
        assertEquals(ScaleFamily.HARMONIC_MINOR, ScaleType.LOCRIAN_NATURAL_6.family)
    }

    @Test
    fun `melodic-minor family types are assigned correctly`() {
        assertEquals(ScaleFamily.MELODIC_MINOR, ScaleType.MELODIC_MINOR.family)
        assertEquals(ScaleFamily.MELODIC_MINOR, ScaleType.LYDIAN_DOMINANT.family)
        assertEquals(ScaleFamily.MELODIC_MINOR, ScaleType.ALTERED.family)
        assertEquals(ScaleFamily.MELODIC_MINOR, ScaleType.DORIAN_FLAT_2.family)
    }

    // ─────────────────── Companion DIATONIC list ────────────────────────────

    @Test
    fun `DIATONIC companion list contains exactly 7 entries`() {
        assertEquals(7, ScaleType.DIATONIC.size)
    }

    @Test
    fun `DIATONIC companion list contains all and only diatonic types`() {
        val expected = setOf(
            ScaleType.IONIAN, ScaleType.AEOLIAN, ScaleType.DORIAN,
            ScaleType.PHRYGIAN, ScaleType.LYDIAN, ScaleType.MIXOLYDIAN, ScaleType.LOCRIAN,
        )
        assertEquals(expected, ScaleType.DIATONIC.toSet())
    }

    // ───────────────────── Resource key non-blank guard ─────────────────────

    @Test
    fun `every type exposes non-blank primary label and subtitle resource keys`() {
        ScaleType.entries.forEach { type ->
            assertTrue("${type.name} primaryLabelKey should not be blank", type.primaryLabelKey.isNotBlank())
            assertTrue("${type.name} subtitleKey should not be blank", type.subtitleKey.isNotBlank())
        }
    }

    @Test
    fun `all resource keys are unique — no accidental duplicates`() {
        val labelKeys = ScaleType.entries.map { it.primaryLabelKey }
        val subtitleKeys = ScaleType.entries.map { it.subtitleKey }
        assertEquals("primaryLabelKey values must all be distinct", labelKeys.size, labelKeys.distinct().size)
        assertEquals("subtitleKey values must all be distinct", subtitleKeys.size, subtitleKeys.distinct().size)
    }
}
