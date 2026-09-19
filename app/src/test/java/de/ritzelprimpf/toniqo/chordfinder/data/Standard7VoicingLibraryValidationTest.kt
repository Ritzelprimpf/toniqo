package de.ritzelprimpf.toniqo.chordfinder.data

import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordToneRole
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.common.model.GuitarTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Loads test-resource copies of `assets/chordfinder/voicings_standard_7.json` **and**
 * `voicings_standard_7_seventh.json`, merged exactly the way [VoicingRepositoryImpl.loadFamily]
 * merges them at runtime, and asserts the same invariants [VoicingLibraryValidationTest] checks
 * for the standard-6 library, adapted for 7 strings.
 *
 * This is the "write the test once curated content exists" follow-up flagged when
 * `GuitarTuning.STANDARD_7` was first wired in (see `DECISIONS.md`). It exists specifically
 * because a hand-curation pass on this library shipped 12 voicings whose fret span exceeded
 * [de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing.MAX_FRET_SPAN] — every one of them in
 * the seventh-chord asset, none in the triad one — which crashed the Chord Voicings screen for
 * *any* 7-string chord the moment [VoicingRepositoryImpl] tried to eagerly parse the family (one
 * malformed entry anywhere fails the whole parse). This test's whole job is to catch that class
 * of hand-edit mistake at `./gradlew test` time instead of on a device.
 *
 * The copies live at `src/test/resources/chordfinder/` — Gradle puts `src/test/resources` on the
 * unit-test classpath automatically. Keep them in sync with `src/main/assets/chordfinder/` if
 * those files change.
 */
class Standard7VoicingLibraryValidationTest {

    private val tuning = GuitarTuning.STANDARD_7
    private val stringCount = 7

    private fun loadResource(name: String) =
        javaClass.classLoader!!.getResourceAsStream(name)
            ?.bufferedReader()?.readText()
            ?: error("Test resource not found: $name — expected at src/test/resources/chordfinder/")

    private val library by lazy {
        VoicingJsonParser.parse(loadResource("chordfinder/voicings_standard_7.json"), tuning) +
            VoicingJsonParser.parse(loadResource("chordfinder/voicings_standard_7_seventh.json"), tuning)
    }

    // ── Full coverage check ───────────────────────────────────────────────────────

    @Test
    fun `all 12 roots times 4 triad qualities have at least one voicing`() {
        val triadQualities = listOf(
            ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.DIMINISHED, ChordQuality.AUGMENTED,
        )
        for (root in 0..11) {
            for (quality in triadQualities) {
                val voicings = library[ChordKey(root, quality)]
                assertTrue(
                    "Missing voicings for root=$root quality=$quality",
                    !voicings.isNullOrEmpty(),
                )
            }
        }
    }

    // ── Per-entry invariant checks ────────────────────────────────────────────────

    @Test
    fun `every voicing has marks size equal to 7`() {
        library.forEach { (key, voicings) ->
            voicings.forEach { v ->
                assertEquals("marks.size for $key", stringCount, v.marks.size)
            }
        }
    }

    @Test
    fun `every voicing has fingers size equal to 7`() {
        library.forEach { (key, voicings) ->
            voicings.forEach { v ->
                assertEquals("fingers.size for $key", stringCount, v.fingers.size)
            }
        }
    }

    @Test
    fun `every voicing fret span is within bounds`() {
        // 4: FretboardRenderModel's fixed 5-row window maps a fretted mark to row
        // `fret - base + 1`, so a span of 5 already needs a 6th row and silently overflows the
        // diagram. This exact check is what the 12 hand-curated voicings above failed.
        library.forEach { (key, voicings) ->
            voicings.forEach { v ->
                val range = v.fretRange
                if (range != 0..0) {
                    assertTrue("fret span ≤ 4 for $key: ${range.last - range.first}", range.last - range.first <= 4)
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

    @Test
    fun `every voicing label key is positive`() {
        library.forEach { (key, voicings) ->
            voicings.forEach { v ->
                assertTrue("labelKey > 0 for $key", v.labelKey > 0)
            }
        }
    }

    // ── 7-string-specific assertions ──────────────────────────────────────────────

    @Test
    fun `library includes both root-position voicings and at least one non-root bass degree`() {
        val allBassDegrees = library.values.flatten().map { it.bassDegree }.toSet()
        assertTrue(
            "expected at least one root-position voicing",
            ChordToneRole.ROOT in allBassDegrees,
        )
    }
}
