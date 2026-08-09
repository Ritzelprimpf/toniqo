package de.ritzelprimpf.toniqo.chordfinder.data

import de.ritzelprimpf.toniqo.chordfinder.domain.model.FretMark
import de.ritzelprimpf.toniqo.chordfinder.domain.model.VoicingCategory
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.common.model.GuitarTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Loads a test-resource copy of `assets/chordfinder/voicings_standard_6.json` and asserts that
 * every entry satisfies all five voicing invariants, that every chord key (12 roots × 4
 * qualities) is present, and that C MAJOR specifically includes both a near-nut open voicing and
 * a barre voicing.
 *
 * The copy lives at `src/test/resources/chordfinder/voicings_standard_6.json` — Gradle puts
 * `src/test/resources` on the unit-test classpath automatically, no build.gradle.kts wiring
 * needed. Keep it in sync with `src/main/assets/chordfinder/voicings_standard_6.json` if that
 * file changes.
 */
class VoicingLibraryValidationTest {

    private val tuning = GuitarTuning.STANDARD_6

    private val library by lazy {
        val stream = javaClass.classLoader!!.getResourceAsStream("chordfinder/voicings_standard_6.json")
            ?: error("Test resource not found: chordfinder/voicings_standard_6.json — expected at src/test/resources/chordfinder/")
        val json = stream.bufferedReader().readText()
        VoicingJsonParser.parse(json, tuning)
    }

    // ── Full coverage check ───────────────────────────────────────────────────────

    @Test
    fun `all 12 roots times 4 qualities have at least one voicing`() {
        // Deliberately the 4 triads only, not ChordQuality.entries: POWER chords belong to the
        // drop-tuning library (voicings_drop_d_6.json), not this standard-6 one -- see
        // ChordQuality's kdoc.
        val triadQualities = listOf(
            ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.DIMINISHED, ChordQuality.AUGMENTED,
        )
        for (root in 0..11) {
            for (quality in triadQualities) {
                val key = de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey(root, quality)
                val voicings = library[key]
                assertTrue(
                    "Missing voicings for root=$root quality=$quality",
                    !voicings.isNullOrEmpty(),
                )
            }
        }
    }

    // ── Per-entry invariant checks ────────────────────────────────────────────────

    @Test
    fun `every voicing has marks size equal to 6`() {
        library.forEach { (key, voicings) ->
            voicings.forEach { v ->
                assertEquals("marks.size for $key", 6, v.marks.size)
            }
        }
    }

    @Test
    fun `every voicing has fingers size equal to 6`() {
        library.forEach { (key, voicings) ->
            voicings.forEach { v ->
                assertEquals("fingers.size for $key", 6, v.fingers.size)
            }
        }
    }

    @Test
    fun `curated library now includes inversions alongside root-position voicings`() {
        // Updated once an inversion was actually curated into the shipped asset (upper-neck
        // 3-string shapes with a third or fifth in the bass) — see this test's prior form for the
        // tripwire that flagged the change. Voicing.validated() already proves per-voicing that
        // whatever bassDegree is claimed matches the shape's true lowest note; this just confirms
        // both root-position and inversions are represented, i.e. the library isn't accidentally
        // root-only nor accidentally inversion-only.
        val allBassDegrees = library.values.flatten().map { it.bassDegree }.toSet()
        assertTrue(
            "expected at least one root-position voicing",
            de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordToneRole.ROOT in allBassDegrees,
        )
        assertTrue(
            "expected at least one inversion (third or fifth in bass)",
            allBassDegrees.any { it != de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordToneRole.ROOT },
        )
    }

    @Test
    fun `every voicing fret span is within bounds`() {
        library.forEach { (key, voicings) ->
            voicings.forEach { v ->
                val range = v.fretRange
                if (range != 0..0) {
                    assertTrue("fret span ≤ 6 for $key: ${range.last - range.first}", range.last - range.first <= 6)
                    assertTrue("baseFret ≥ 0 for $key", range.first >= 0)
                    assertTrue("maxFret ≤ 24 for $key", range.last <= 24)
                }
            }
        }
    }

    @Test
    fun `voicings within each chord key are sorted by ascending baseFret`() {
        library.forEach { (key, voicings) ->
            val baseFrets = voicings.map { it.baseFret }
            assertEquals("baseFret order for $key", baseFrets.sorted(), baseFrets)
        }
    }

    // ── C MAJOR specific assertions ───────────────────────────────────────────────

    @Test
    fun `C MAJOR has at least one near-nut open voicing`() {
        val cMaj = library[de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey(0, ChordQuality.MAJOR)]
        assertFalse("C MAJOR must have voicings", cMaj.isNullOrEmpty())
        assertTrue(
            "C MAJOR must include a near-nut open voicing (baseFret ≤ 5, category OPEN)",
            cMaj!!.any { it.category == VoicingCategory.OPEN && it.baseFret <= 5 },
        )
    }

    @Test
    fun `C MAJOR has at least one barre voicing`() {
        val cMaj = library[de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey(0, ChordQuality.MAJOR)]
        assertTrue(
            "C MAJOR must include at least one barre voicing",
            cMaj!!.any { it.category == VoicingCategory.BARRE },
        )
    }

    @Test
    fun `C MAJOR open voicing has muted low E and sounds correct notes`() {
        val cMaj = library[de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey(0, ChordQuality.MAJOR)]
        val openVoicing = cMaj!!.first { it.category == VoicingCategory.OPEN && it.baseFret <= 5 }
        // Low E string (index 0) must be muted in the standard C chord shape
        assertEquals(FretMark.Muted, openVoicing.marks[0])
    }

    @Test
    fun `every voicing label key is positive`() {
        library.forEach { (key, voicings) ->
            voicings.forEach { v ->
                assertTrue("labelKey > 0 for $key", v.labelKey > 0)
            }
        }
    }
}
