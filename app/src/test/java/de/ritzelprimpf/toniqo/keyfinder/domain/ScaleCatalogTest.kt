package de.ritzelprimpf.toniqo.keyfinder.domain

import de.ritzelprimpf.toniqo.common.model.ScaleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleCatalogTest {

    private val catalog = ScaleCatalog.DEFAULT

    @Test
    fun `default catalog contains exactly 168 candidates`() {
        assertEquals(168, catalog.candidates.size)
    }

    @Test
    fun `every root-type pair appears exactly once`() {
        val pairs = catalog.candidates.map { it.rootPitchClass to it.type }
        assertEquals(
            "Expected 168 unique (root, type) pairs",
            168,
            pairs.distinct().size,
        )
    }

    @Test
    fun `all 12 pitch classes are covered`() {
        val roots = catalog.candidates.map { it.rootPitchClass }.toSet()
        assertEquals((0..11).toSet(), roots)
    }

    @Test
    fun `all 14 scale types are covered`() {
        val types = catalog.candidates.map { it.type }.toSet()
        assertEquals(ScaleType.entries.toSet(), types)
    }

    @Test
    fun `each root has exactly 14 entries — one per scale type`() {
        val byRoot = catalog.candidates.groupBy { it.rootPitchClass }
        byRoot.forEach { (root, entries) ->
            assertEquals("root=$root should have 14 entries", 14, entries.size)
        }
    }

    @Test
    fun `every candidate's pitchClasses has exactly 7 distinct values`() {
        catalog.candidates.forEach { candidate ->
            assertEquals(
                "root=${candidate.rootPitchClass} type=${candidate.type.name}: pitchClasses should have 7 values",
                7,
                candidate.pitchClasses.size,
            )
        }
    }

    @Test
    fun `every candidate's pitchClasses contains its own root`() {
        catalog.candidates.forEach { candidate ->
            assertTrue(
                "root=${candidate.rootPitchClass} type=${candidate.type.name}: pitchClasses should contain root",
                candidate.rootPitchClass in candidate.pitchClasses,
            )
        }
    }

    @Test
    fun `all pitch class values in candidates are in 0 to 11`() {
        catalog.candidates.forEach { candidate ->
            candidate.pitchClasses.forEach { pc ->
                assertTrue(
                    "root=${candidate.rootPitchClass} type=${candidate.type.name}: pc=$pc out of range",
                    pc in 0..11,
                )
            }
        }
    }

    @Test
    fun `C Major candidate has correct pitch classes`() {
        val cMajor = catalog.candidates.first {
            it.rootPitchClass == 0 && it.type == ScaleType.IONIAN
        }
        assertEquals(setOf(0, 2, 4, 5, 7, 9, 11), cMajor.pitchClasses)
    }

    @Test
    fun `A Natural Minor candidate has correct pitch classes matching C Major`() {
        val aMinor = catalog.candidates.first {
            it.rootPitchClass == 9 && it.type == ScaleType.AEOLIAN
        }
        assertEquals(setOf(0, 2, 4, 5, 7, 9, 11), aMinor.pitchClasses)
    }

    @Test
    fun `A Harmonic Minor candidate has correct pitch classes`() {
        val aHarmonicMinor = catalog.candidates.first {
            it.rootPitchClass == 9 && it.type == ScaleType.HARMONIC_MINOR
        }
        // A=9, B=11, C=0, D=2, E=4, F=5, G#=8
        assertEquals(setOf(9, 11, 0, 2, 4, 5, 8), aHarmonicMinor.pitchClasses)
    }
}
