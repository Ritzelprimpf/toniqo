package de.ritzelprimpf.toniqo.chordfinder.data

import de.ritzelprimpf.toniqo.chordfinder.domain.VoicingTransposer
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordToneRole
import de.ritzelprimpf.toniqo.chordfinder.domain.model.FretMark
import de.ritzelprimpf.toniqo.chordfinder.domain.model.SeventhQuality
import de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing
import de.ritzelprimpf.toniqo.chordfinder.domain.model.VoicingCategory
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.VoicingLookupResult
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.VoicingRepository
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.common.model.GuitarTuning
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests [VoicingRepositoryImpl] using a fake implementation that delegates to
 * [VoicingJsonParser] with a minimal inline JSON, keeping these tests parser-agnostic and
 * not dependent on the real JSON asset.
 */
class VoicingRepositoryImplTest {

    // Minimal JSON containing just C MAJOR with two voicings: one open, one barre
    private val minimalJson = """
        {
          "tuningId": "standard_6",
          "version": 1,
          "chords": [
            {
              "rootPitchClass": 0,
              "quality": "MAJOR",
              "voicings": [
                { "frets": ["x",3,2,0,1,0], "fingers": [0,3,2,0,1,0], "barre": null },
                { "frets": [8,10,10,9,8,8], "fingers": [1,3,4,2,1,1],
                  "barre": { "fret": 8, "from": 0, "to": 5 } }
              ]
            }
          ]
        }
    """.trimIndent()

    // C MAJOR, third (E, D-string fret 2) in the bass — an inversion, not the classic open shape.
    private val inversionJson = """
        {
          "tuningId": "standard_6",
          "version": 1,
          "chords": [
            {
              "rootPitchClass": 0,
              "quality": "MAJOR",
              "voicings": [
                { "frets": ["x","x",2,0,1,0], "fingers": [0,0,2,0,1,0], "barre": null }
              ]
            }
          ]
        }
    """.trimIndent()

    // Minimal drop-D-family JSON: G MAJOR as an all-open triad on the D-G-B strings
    // (Drop D open pcs low→high: D=2 A=9 D=2 G=7 B=11 e=4 — D/G/B strings sound fifth/root/third).
    private val dropJson = """
        {
          "tuningId": "drop_d_6",
          "version": 1,
          "chords": [
            {
              "rootPitchClass": 7,
              "quality": "MAJOR",
              "voicings": [
                { "frets": ["x","x",0,0,0,"x"], "fingers": [0,0,0,0,0,0], "barre": null }
              ]
            }
          ]
        }
    """.trimIndent()

    // Fake repository backed by the minimal JSON (standard family only)
    private val fakeRepo: VoicingRepository = FakeVoicingRepository(minimalJson)

    // Fake repository with both families curated
    private val dropRepo: VoicingRepository = FakeVoicingRepository(minimalJson, dropJson)

    private val cMajKey = ChordKey(0, ChordQuality.MAJOR)
    private val gMajKey = ChordKey(7, ChordQuality.MAJOR)
    private val standardTuning = GuitarTuning.STANDARD_6
    private val dropDTuning = GuitarTuning.DROP_D_6

    // Eb standard (uniform offset -1)
    private val ebStandard = GuitarTuning(
        id = "eb_standard",
        openNotes = listOf(
            Note(NoteName.DSharp, 2), Note(NoteName.GSharp, 2), Note(NoteName.CSharp, 3),
            Note(NoteName.FSharp, 3), Note(NoteName.ASharp, 3), Note(NoteName.DSharp, 4),
        ),
    )

    // Open G (non-uniform relative to both STANDARD_6 and DROP_D_6 — genuinely unsupported)
    private val openG = GuitarTuning(
        id = "open_g",
        openNotes = listOf(
            Note(NoteName.D, 2), Note(NoteName.G, 2), Note(NoteName.D, 3),
            Note(NoteName.G, 3), Note(NoteName.B, 3), Note(NoteName.D, 4),
        ),
    )

    // ── tier 1: standard ─────────────────────────────────────────────────────────

    @Test
    fun `tier 1 standard returns Standard result`() = runTest {
        val result = fakeRepo.lookup(cMajKey, standardTuning)
        assertTrue(result is VoicingLookupResult.Standard)
    }

    @Test
    fun `tier 1 standard voicings are sorted by ascending baseFret`() = runTest {
        val result = fakeRepo.lookup(cMajKey, standardTuning) as VoicingLookupResult.Standard
        val baseFrets = result.voicings.map { it.baseFret }
        assertEquals(baseFrets.sorted(), baseFrets)
    }

    @Test
    fun `tier 1 standard returns both voicings (within cap)`() = runTest {
        val result = fakeRepo.lookup(cMajKey, standardTuning) as VoicingLookupResult.Standard
        assertEquals(2, result.voicings.size)
    }

    // ── tier 2: uniform offset ────────────────────────────────────────────────────

    @Test
    fun `tier 2 Eb standard returns UniformOffset result with offset minus 1`() = runTest {
        val result = fakeRepo.lookup(cMajKey, ebStandard)
        assertTrue(result is VoicingLookupResult.UniformOffset)
        assertEquals(-1, (result as VoicingLookupResult.UniformOffset).offsetSemitones)
    }

    @Test
    fun `tier 2 open voicings are dropped`() = runTest {
        val result = fakeRepo.lookup(cMajKey, ebStandard) as VoicingLookupResult.UniformOffset
        // The open voicing (category OPEN) must be absent; only the barre voicing remains (shifted)
        assertTrue("All voicings must be non-open", result.voicings.none { it.category == VoicingCategory.OPEN })
    }

    @Test
    fun `tier 2 movable voicings have frets shifted up by 1`() = runTest {
        val result = fakeRepo.lookup(cMajKey, ebStandard) as VoicingLookupResult.UniformOffset
        val shifted = result.voicings.first()
        val frettedFrets = shifted.marks.filterIsInstance<FretMark.Fretted>().map { it.fret }
        // Original barre at [8,10,10,9,8,8] shifted +1 → [9,11,11,10,9,9]
        assertEquals(listOf(9, 11, 11, 10, 9, 9), frettedFrets)
    }

    // ── bassDegree computation ────────────────────────────────────────────────────

    @Test
    fun `bassDegree is ROOT for a root-position voicing`() = runTest {
        val result = fakeRepo.lookup(cMajKey, standardTuning) as VoicingLookupResult.Standard
        assertTrue(result.voicings.all { it.bassDegree == ChordToneRole.ROOT })
    }

    @Test
    fun `bassDegree is computed as THIRD for an inverted voicing`() = runTest {
        val invertedRepo = FakeVoicingRepository(inversionJson)
        val result = invertedRepo.lookup(cMajKey, standardTuning) as VoicingLookupResult.Standard
        assertEquals(ChordToneRole.THIRD, result.voicings.single().bassDegree)
    }

    // ── tier 1/2: drop-D family ──────────────────────────────────────────────────

    @Test
    fun `tier 1 Drop D returns Standard result sourced from the drop library`() = runTest {
        val result = dropRepo.lookup(gMajKey, dropDTuning)
        assertTrue(result is VoicingLookupResult.Standard)
        assertEquals(1, (result as VoicingLookupResult.Standard).voicings.size)
    }

    @Test
    fun `tier 2 Drop C sharp returns UniformOffset with offset minus 1, drop family`() = runTest {
        val dropCSharp = GuitarTuning(
            id = "drop_cs",
            openNotes = listOf(
                Note(NoteName.CSharp, 2), Note(NoteName.GSharp, 2), Note(NoteName.CSharp, 3),
                Note(NoteName.FSharp, 3), Note(NoteName.ASharp, 3), Note(NoteName.DSharp, 4),
            ),
        )
        val result = dropRepo.lookup(gMajKey, dropCSharp)
        assertTrue(result is VoicingLookupResult.UniformOffset)
        assertEquals(-1, (result as VoicingLookupResult.UniformOffset).offsetSemitones)
    }

    @Test
    fun `Drop D with no curated drop asset returns Standard with empty voicings, not Unsupported`() = runTest {
        // fakeRepo only has a standard-family library. A missing/not-yet-curated family asset
        // must behave as empty, not crash and not fall through to Unsupported -- Drop D is a
        // real, matched family, it just has nothing curated for this chord yet.
        val result = fakeRepo.lookup(gMajKey, dropDTuning)
        assertTrue(result is VoicingLookupResult.Standard)
        assertTrue((result as VoicingLookupResult.Standard).voicings.isEmpty())
    }

    // ── seventh-asset merge ──────────────────────────────────────────────────────

    @Test
    fun `seventh chord voicings are merged in from the seventh asset without colliding with the triad`() = runTest {
        val standardSeventhJson = """
            {
              "tuningId": "standard_6",
              "version": 1,
              "chords": [
                {
                  "rootPitchClass": 0,
                  "quality": "MAJOR",
                  "seventhQuality": "MAJOR_SEVENTH",
                  "voicings": [
                    { "frets": ["x",3,2,0,0,0], "fingers": [0,3,2,0,0,0], "barre": null }
                  ]
                }
              ]
            }
        """.trimIndent()
        val repo = FakeVoicingRepository(minimalJson, standardSeventhJson = standardSeventhJson)
        val seventhKey = ChordKey(0, ChordQuality.MAJOR, SeventhQuality.MAJOR_SEVENTH)

        val triadResult = repo.lookup(cMajKey, standardTuning) as VoicingLookupResult.Standard
        val seventhResult = repo.lookup(seventhKey, standardTuning) as VoicingLookupResult.Standard

        assertEquals(2, triadResult.voicings.size) // unaffected by the seventh asset
        assertEquals(1, seventhResult.voicings.size)
    }

    @Test
    fun `a seventh chord with no curated seventh asset returns empty, not the triad's voicings`() = runTest {
        val seventhKey = ChordKey(0, ChordQuality.MAJOR, SeventhQuality.MAJOR_SEVENTH)
        val result = fakeRepo.lookup(seventhKey, standardTuning) as VoicingLookupResult.Standard
        assertTrue(result.voicings.isEmpty())
    }

    // ── tier 3: unsupported ───────────────────────────────────────────────────────

    @Test
    fun `tier 3 Open G returns Unsupported`() = runTest {
        val result = fakeRepo.lookup(cMajKey, openG)
        assertTrue(result is VoicingLookupResult.Unsupported)
    }

    @Test
    fun `tier 3 Unsupported carries the requested tuning`() = runTest {
        val result = fakeRepo.lookup(cMajKey, openG) as VoicingLookupResult.Unsupported
        assertEquals(openG, result.tuning)
    }

    // ── Inner fake: delegates to parser with inline JSON, mirrors VoicingRepositoryImpl's
    // multi-family resolution (standard family, then drop-D family) and its triad/seventh
    // asset merge ────────────────────────────────────────────────────────────────

    private class FakeVoicingRepository(
        standardJson: String,
        dropJson: String? = null,
        standardSeventhJson: String? = null,
        dropSeventhJson: String? = null,
    ) : VoicingRepository {

        private data class Family(val reference: GuitarTuning, val library: Map<ChordKey, List<Voicing>>)

        private fun parseOrEmpty(json: String?, tuning: GuitarTuning): Map<ChordKey, List<Voicing>> =
            json?.let { VoicingJsonParser.parse(it, tuning) } ?: emptyMap()

        private val families: List<Family> by lazy {
            listOf(
                Family(
                    GuitarTuning.STANDARD_6,
                    VoicingJsonParser.parse(standardJson, GuitarTuning.STANDARD_6) +
                        parseOrEmpty(standardSeventhJson, GuitarTuning.STANDARD_6),
                ),
                Family(
                    GuitarTuning.DROP_D_6,
                    parseOrEmpty(dropJson, GuitarTuning.DROP_D_6) +
                        parseOrEmpty(dropSeventhJson, GuitarTuning.DROP_D_6),
                ),
            )
        }

        companion object {
            private const val MAX_VOICINGS = 5
            private const val MAX_FRET = 15
        }

        override suspend fun lookup(chord: ChordKey, tuning: GuitarTuning): VoicingLookupResult {
            for (family in families) {
                val offset = tuning.uniformOffsetFrom(family.reference) ?: continue
                val reference = family.library[chord] ?: emptyList()
                return if (offset == 0) {
                    VoicingLookupResult.Standard(reference.take(MAX_VOICINGS))
                } else {
                    val shifted = reference
                        .mapNotNull { VoicingTransposer.shift(it, kotlin.math.abs(offset), MAX_FRET) }
                        .sortedBy { it.baseFret }
                        .take(MAX_VOICINGS)
                    VoicingLookupResult.UniformOffset(shifted, offset)
                }
            }
            return VoicingLookupResult.Unsupported(tuning)
        }
    }
}
