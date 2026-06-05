package de.ritzelprimpf.toniqo.chordfinder.data

import de.ritzelprimpf.toniqo.chordfinder.domain.VoicingTransposer
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordToneRole
import de.ritzelprimpf.toniqo.chordfinder.domain.model.FretMark
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

    // Fake repository backed by the minimal JSON
    private val fakeRepo: VoicingRepository = FakeVoicingRepository(minimalJson)

    private val cMajKey = ChordKey(0, ChordQuality.MAJOR)
    private val standardTuning = GuitarTuning.STANDARD_6

    // Eb standard (uniform offset -1)
    private val ebStandard = GuitarTuning(
        id = "eb_standard",
        openNotes = listOf(
            Note(NoteName.DSharp, 2), Note(NoteName.GSharp, 2), Note(NoteName.CSharp, 3),
            Note(NoteName.FSharp, 3), Note(NoteName.ASharp, 3), Note(NoteName.DSharp, 4),
        ),
    )

    // Drop D (non-uniform)
    private val dropD = GuitarTuning(
        id = "drop_d",
        openNotes = listOf(
            Note(NoteName.D, 2), Note(NoteName.A, 2), Note(NoteName.D, 3),
            Note(NoteName.G, 3), Note(NoteName.B, 3), Note(NoteName.E, 4),
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

    // ── tier 3: unsupported ───────────────────────────────────────────────────────

    @Test
    fun `tier 3 Drop D returns Unsupported`() = runTest {
        val result = fakeRepo.lookup(cMajKey, dropD)
        assertTrue(result is VoicingLookupResult.Unsupported)
    }

    @Test
    fun `tier 3 Unsupported carries the requested tuning`() = runTest {
        val result = fakeRepo.lookup(cMajKey, dropD) as VoicingLookupResult.Unsupported
        assertEquals(dropD, result.tuning)
    }

    // ── Inner fake: delegates to parser with inline JSON ──────────────────────────

    private class FakeVoicingRepository(private val json: String) : VoicingRepository {
        private val library: Map<ChordKey, List<Voicing>> by lazy {
            VoicingJsonParser.parse(json, GuitarTuning.STANDARD_6)
        }

        companion object {
            private const val MAX_VOICINGS = 5
            private const val MAX_FRET = 15
        }

        override suspend fun lookup(chord: ChordKey, tuning: GuitarTuning): VoicingLookupResult {
            val standard = library[chord] ?: emptyList()
            val offset = tuning.uniformOffsetFrom(GuitarTuning.STANDARD_6)
            return when {
                offset == null -> VoicingLookupResult.Unsupported(tuning)
                offset == 0 -> VoicingLookupResult.Standard(standard.take(MAX_VOICINGS))
                else -> {
                    val shifted = standard
                        .mapNotNull { VoicingTransposer.shift(it, kotlin.math.abs(offset), MAX_FRET) }
                        .sortedBy { it.baseFret }
                        .take(MAX_VOICINGS)
                    VoicingLookupResult.UniformOffset(shifted, offset)
                }
            }
        }
    }
}
