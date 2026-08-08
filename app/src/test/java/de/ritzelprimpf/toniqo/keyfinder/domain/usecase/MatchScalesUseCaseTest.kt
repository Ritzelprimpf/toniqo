package de.ritzelprimpf.toniqo.keyfinder.domain.usecase

import de.ritzelprimpf.toniqo.common.model.ScaleType
import de.ritzelprimpf.toniqo.keyfinder.domain.ScaleCatalog
import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderInput
import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exhaustive tests for [MatchScalesUseCase], covering every canonical worked example from
 * `Phase7-PLAN.md` and all gate, ranking, and flag rules.
 *
 * Pitch class reference: C=0, C#/Db=1, D=2, D#/Eb=3, E=4, F=5, F#/Gb=6, G=7, G#/Ab=8, A=9, Bb=10, B=11.
 */
class MatchScalesUseCaseTest {

    private val useCase = MatchScalesUseCase(ScaleCatalog.DEFAULT)

    // ══════════════════════════════ Gate tests ════════════════════════════════

    @Test
    fun `returns empty list for 0 input notes`() {
        val result = useCase(KeyFinderInput(emptySet(), null))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns empty list for 1 input note`() {
        val result = useCase(KeyFinderInput(setOf(0), null))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns empty list for 2 input notes`() {
        val result = useCase(KeyFinderInput(setOf(0, 4), null))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns non-empty list for 3 input notes`() {
        val result = useCase(KeyFinderInput(setOf(0, 4, 7), null)) // C E G
        assertTrue(result.isNotEmpty())
    }

    // ══════════════════════════ 43% canonical case ════════════════════════════

    // C=0, E=4, G=7 (n=3), no root → maxPoints=7, covered=3, percent=43
    private val cEgInput = KeyFinderInput(pitchClasses = setOf(0, 4, 7), rootPitchClass = null)

    @Test
    fun `C E G no root — C Major scores 43 percent`() {
        val results = useCase(cEgInput)
        val cMajor = results.first { it.candidate.rootPitchClass == 0 && it.candidate.type == ScaleType.IONIAN }
        assertEquals(43, cMajor.percent)
    }

    @Test
    fun `C E G no root — containing scale is FULL despite 43 percent`() {
        val results = useCase(cEgInput)
        val cMajor = results.first { it.candidate.rootPitchClass == 0 && it.candidate.type == ScaleType.IONIAN }
        assertTrue(cMajor.isFull)
    }

    @Test
    fun `C E G no root — A Natural Minor also scores 43 percent`() {
        // A Aeolian = {9,11,0,2,4,5,7} — contains C(0), E(4), G(7)
        val results = useCase(cEgInput)
        val aMinor = results.first { it.candidate.rootPitchClass == 9 && it.candidate.type == ScaleType.AEOLIAN }
        assertEquals(43, aMinor.percent)
    }

    // ═══════════════════════ 50% / 38% root-split case ═══════════════════════

    // C=0, E=4, G=7 (n=3), root=C(0) → maxPoints=8
    private val cEgRootC = KeyFinderInput(pitchClasses = setOf(0, 4, 7), rootPitchClass = 0)

    // Scoped to just C Major and A Natural Minor: against the full 168-candidate catalog, several
    // unrelated scales (e.g. F Ionian, G Lydian) coincidentally tie at the same 50%/38% scores as
    // these two, and MAX_RESULTS=7 can push one of the two intended candidates out of the result
    // list. That's correct ranking behaviour, not a bug — trim the catalog so this test isolates
    // the specific comparison it documents (mirrors the pattern at line ~305 below).
    private val cEgRootCUseCase = MatchScalesUseCase(
        ScaleCatalog(listOf(ScaleCandidate(0, ScaleType.IONIAN), ScaleCandidate(9, ScaleType.AEOLIAN))),
    )

    @Test
    fun `C E G root C — C Major scores 50 percent`() {
        val results = cEgRootCUseCase(cEgRootC)
        val cMajor = results.first { it.candidate.rootPitchClass == 0 && it.candidate.type == ScaleType.IONIAN }
        assertEquals(50, cMajor.percent)
    }

    @Test
    fun `C E G root C — A Natural Minor scores 38 percent`() {
        // A Aeolian contains C,E,G but root is A(9) ≠ C(0) → rootBonus=0, points=3, maxPoints=8
        val results = cEgRootCUseCase(cEgRootC)
        val aMinor = results.first { it.candidate.rootPitchClass == 9 && it.candidate.type == ScaleType.AEOLIAN }
        assertEquals(38, aMinor.percent)
    }

    @Test
    fun `C E G root C — C-rooted scale ranks above same-notes A-rooted scale`() {
        val results = cEgRootCUseCase(cEgRootC)
        val cMajorRank = results.first { it.candidate.rootPitchClass == 0 && it.candidate.type == ScaleType.IONIAN }.rank
        val aMinorRank = results.first { it.candidate.rootPitchClass == 9 && it.candidate.type == ScaleType.AEOLIAN }.rank
        assertTrue("C Major (50%) should rank above A Natural Minor (38%)", cMajorRank < aMinorRank)
    }

    @Test
    fun `C E G root C — C Major has isRootMatch true`() {
        val results = cEgRootCUseCase(cEgRootC)
        val cMajor = results.first { it.candidate.rootPitchClass == 0 && it.candidate.type == ScaleType.IONIAN }
        assertTrue(cMajor.isRootMatch)
    }

    @Test
    fun `C E G root C — A Natural Minor has isRootMatch false`() {
        val results = cEgRootCUseCase(cEgRootC)
        val aMinor = results.first { it.candidate.rootPitchClass == 9 && it.candidate.type == ScaleType.AEOLIAN }
        assertFalse(aMinor.isRootMatch)
    }

    // ══════════════════════ 100% tie — seven modes of C major ════════════════

    // C D E F G A B = {0,2,4,5,7,9,11} (n=7), no root → maxPoints=7
    private val sevenNaturals = KeyFinderInput(
        pitchClasses = setOf(0, 2, 4, 5, 7, 9, 11),
        rootPitchClass = null,
    )

    @Test
    fun `seven naturals no root — all seven modes of C major score 100 percent`() {
        val results = useCase(sevenNaturals)
        val cModes = listOf(
            0 to ScaleType.IONIAN,   // C Major
            9 to ScaleType.AEOLIAN,  // A Natural Minor
            2 to ScaleType.DORIAN,   // D Dorian
            4 to ScaleType.PHRYGIAN, // E Phrygian
            5 to ScaleType.LYDIAN,   // F Lydian
            7 to ScaleType.MIXOLYDIAN, // G Mixolydian
            11 to ScaleType.LOCRIAN, // B Locrian
        )
        cModes.forEach { (rootPc, type) ->
            val match = results.first { it.candidate.rootPitchClass == rootPc && it.candidate.type == type }
            assertEquals("$type at root $rootPc should score 100%", 100, match.percent)
        }
    }

    @Test
    fun `seven naturals no root — all seven modes carry the FULL flag`() {
        val results = useCase(sevenNaturals)
        val cModeCandidates = results.filter { it.percent == 100 }
        assertTrue("Should have at least 7 results at 100%", cModeCandidates.size >= 7)
        cModeCandidates.filter { it.percent == 100 }.forEach { match ->
            assertTrue("${match.candidate.type} at ${match.candidate.rootPitchClass} should be FULL", match.isFull)
        }
    }

    @Test
    fun `seven naturals no root — ordering is Major then Natural Minor then the rest in common-first order`() {
        val results = useCase(sevenNaturals)
        // The top 7 are the 7 C-major modes at 100%; they should come out in rankOrder order.
        val top7 = results.take(7)
        assertEquals(ScaleType.IONIAN, top7[0].candidate.type)   // rank 0
        assertEquals(ScaleType.AEOLIAN, top7[1].candidate.type)  // rank 1
        assertEquals(ScaleType.DORIAN, top7[2].candidate.type)   // rank 2
        assertEquals(ScaleType.PHRYGIAN, top7[3].candidate.type) // rank 3
        assertEquals(ScaleType.LYDIAN, top7[4].candidate.type)   // rank 4
        assertEquals(ScaleType.MIXOLYDIAN, top7[5].candidate.type) // rank 5
        assertEquals(ScaleType.LOCRIAN, top7[6].candidate.type)  // rank 6
    }

    // ══════════════════════ Root breaks the tie — 100% vs 88% ═══════════════

    // C D E F G A B = {0,2,4,5,7,9,11} (n=7), root=A(9) → maxPoints=8
    private val sevenNaturalsRootA = KeyFinderInput(
        pitchClasses = setOf(0, 2, 4, 5, 7, 9, 11),
        rootPitchClass = 9,
    )

    @Test
    fun `seven naturals root A — A Natural Minor scores 100 percent`() {
        val results = useCase(sevenNaturalsRootA)
        val aMinor = results.first { it.candidate.rootPitchClass == 9 && it.candidate.type == ScaleType.AEOLIAN }
        assertEquals(100, aMinor.percent)
    }

    @Test
    fun `seven naturals root A — A Natural Minor has TONIC and FULL flags`() {
        val results = useCase(sevenNaturalsRootA)
        val aMinor = results.first { it.candidate.rootPitchClass == 9 && it.candidate.type == ScaleType.AEOLIAN }
        assertTrue(aMinor.isRootMatch)
        assertTrue(aMinor.isFull)
    }

    @Test
    fun `seven naturals root A — A Natural Minor is ranked 1`() {
        val results = useCase(sevenNaturalsRootA)
        val aMinor = results.first { it.candidate.rootPitchClass == 9 && it.candidate.type == ScaleType.AEOLIAN }
        assertEquals(1, aMinor.rank)
    }

    @Test
    fun `seven naturals root A — C Major scores 88 percent`() {
        val results = useCase(sevenNaturalsRootA)
        val cMajor = results.first { it.candidate.rootPitchClass == 0 && it.candidate.type == ScaleType.IONIAN }
        assertEquals(88, cMajor.percent)
    }

    @Test
    fun `seven naturals root A — the six sibling modes score 88 percent`() {
        // Scoped to the 7 diatonic-family candidates at their natural roots: against the full
        // 168-candidate catalog, a few unrelated candidates (e.g. D Dorian and E Phrygian rooted
        // at A instead of their natural root) coincidentally also score 88% for this input, and
        // MAX_RESULTS=7 can push a genuine sibling mode out of the result list. That's correct
        // ranking behaviour, not a bug — trim the catalog so this test isolates the specific
        // 7-mode family it documents (mirrors the pattern at line ~305 below).
        val siblings = listOf(
            0 to ScaleType.IONIAN,     // C Major
            2 to ScaleType.DORIAN,     // D Dorian
            4 to ScaleType.PHRYGIAN,   // E Phrygian
            5 to ScaleType.LYDIAN,     // F Lydian
            7 to ScaleType.MIXOLYDIAN, // G Mixolydian
            11 to ScaleType.LOCRIAN,   // B Locrian
        )
        val familyUseCase = MatchScalesUseCase(
            ScaleCatalog(
                (siblings + (9 to ScaleType.AEOLIAN)).map { (rootPc, type) -> ScaleCandidate(rootPc, type) },
            ),
        )
        val results = familyUseCase(sevenNaturalsRootA)
        siblings.forEach { (rootPc, type) ->
            val match = results.first { it.candidate.rootPitchClass == rootPc && it.candidate.type == type }
            assertEquals("$type at root $rootPc should score 88%", 88, match.percent)
        }
    }

    // ════════════════════ Stray note — 88% and not FULL ══════════════════════

    // C D E F G A B + Bb = {0,2,4,5,7,9,10,11} (n=8), no root → maxPoints=8
    private val sevenNaturalsPlusBb = KeyFinderInput(
        pitchClasses = setOf(0, 2, 4, 5, 7, 9, 10, 11), // add Bb=10
        rootPitchClass = null,
    )

    @Test
    fun `seven naturals plus Bb stray note — C Major scores 88 percent`() {
        // C Major has 7 of the 8 input notes (covered=7, n=8, maxPoints=8, percent=88)
        val results = useCase(sevenNaturalsPlusBb)
        val cMajor = results.first { it.candidate.rootPitchClass == 0 && it.candidate.type == ScaleType.IONIAN }
        assertEquals(88, cMajor.percent)
    }

    @Test
    fun `seven naturals plus Bb stray note — C Major is not FULL`() {
        // Bb is in input but not in C Major → covered(7) != n(8)
        val results = useCase(sevenNaturalsPlusBb)
        val cMajor = results.first { it.candidate.rootPitchClass == 0 && it.candidate.type == ScaleType.IONIAN }
        assertFalse(cMajor.isFull)
    }

    // ═══════════════════════════ Exclusion rule ═══════════════════════════════

    @Test
    fun `scale sharing no notes with input never appears in results`() {
        // Input = {0} only — below gate — but with 3 notes that share nothing with B Locrian?
        // Use 3 notes outside B Locrian = {11,0,2,4,5,7,9}: pick C#=1, Eb=3, Ab=8
        val input = KeyFinderInput(pitchClasses = setOf(1, 3, 8), rootPitchClass = null)
        val results = useCase(input)
        // B Locrian {11,0,2,4,5,7,9} shares nothing with {1,3,8}
        val bLocrian = results.find { it.candidate.rootPitchClass == 11 && it.candidate.type == ScaleType.LOCRIAN }
        assertEquals(null, bLocrian)
    }

    // ════════════════════════ Cap at 7 results ════════════════════════════════

    @Test
    fun `result list is capped at 7 even when many scales match`() {
        // The 7 naturals input matches at least 7 scales at 100%
        val results = useCase(sevenNaturals)
        assertEquals(7, results.size)
    }

    @Test
    fun `result list has rank values 1 through size`() {
        val results = useCase(sevenNaturals)
        results.forEachIndexed { index, match ->
            assertEquals("rank should be ${index + 1}", index + 1, match.rank)
        }
    }

    // ════════════════════════ isFull at low percent ═══════════════════════════

    @Test
    fun `C E G no root — containing scales are FULL while scoring 43 percent`() {
        val results = useCase(cEgInput)
        val fullMatches = results.filter { it.isFull }
        assertTrue("Should have FULL matches", fullMatches.isNotEmpty())
        fullMatches.forEach { match ->
            assertEquals("FULL match should score 43% with n=3 and maxPoints=7", 43, match.percent)
        }
    }

    // ════════════════════ Tie-break: rankOrder then root ascending ════════════

    @Test
    fun `equal-percent results are ordered by rankOrder ascending`() {
        // With 7 naturals and no root, all 7 diatonic modes score 100% —
        // they should appear in rankOrder order: Ionian(0) < Aeolian(1) < Dorian(2) ...
        val results = useCase(sevenNaturals)
        val top7 = results.take(7)
        val rankOrders = top7.map { it.candidate.type.rankOrder }
        assertEquals(rankOrders.sorted(), rankOrders)
    }

    @Test
    fun `equal-percent and equal-rankOrder results are ordered by root pitch class ascending`() {
        // Construct a synthetic catalog with two IONIAN candidates at different roots,
        // both fully covering the input — they should be sorted root-ascending.
        val cIonian = ScaleCandidate(0, ScaleType.IONIAN)  // C Major
        val gIonian = ScaleCandidate(7, ScaleType.IONIAN)  // G Major
        val tinyUseCase = MatchScalesUseCase(ScaleCatalog(listOf(gIonian, cIonian)))
        // Input = notes in both C Major AND G Major: {0,2,4,7,9,11} — 6 notes in both
        // (C Major has {0,2,4,5,7,9,11}; G Major has {7,9,11,0,2,4,6}; intersection of input with
        //  each: need an input that's covered equally by both)
        // Simpler: use notes {0,2,4,7,9,11} which are in C Major (6 of 7) and G Major (6 of 7)
        val input = KeyFinderInput(pitchClasses = setOf(0, 2, 4, 7, 9, 11), rootPitchClass = null)
        val result = tinyUseCase(input)
        // Both score floor(6/7*100+0.5)=86%; same type (IONIAN, rankOrder=0); root asc: C(0) then G(7)
        assertEquals(2, result.size)
        assertEquals(0, result[0].candidate.rootPitchClass)  // C first
        assertEquals(7, result[1].candidate.rootPitchClass)  // G second
    }

    // ════════════════════════ isRootMatch semantics ═══════════════════════════

    @Test
    fun `isRootMatch is false for all results when no root is marked`() {
        val results = useCase(sevenNaturals)
        results.forEach { match ->
            assertFalse("No root marked — isRootMatch should be false for all", match.isRootMatch)
        }
    }

    @Test
    fun `isRootMatch is true only for scales whose root matches the marked root`() {
        val results = useCase(sevenNaturalsRootA)
        val rootMatches = results.filter { it.isRootMatch }
        assertTrue(rootMatches.isNotEmpty())
        rootMatches.forEach { match ->
            assertEquals("isRootMatch scale should have rootPitchClass=9 (A)", 9, match.candidate.rootPitchClass)
        }
    }

    // ══════════════════════ Scoring formula invariants ════════════════════════

    @Test
    fun `all returned results have percent in 1 to 100`() {
        val results = useCase(sevenNaturals)
        results.forEach { match ->
            assertTrue("percent should be >= 1", match.percent >= 1)
            assertTrue("percent should be <= 100", match.percent <= 100)
        }
    }

    @Test
    fun `no result with covered zero is included`() {
        // Any scale whose pitchClasses share no note with the input must be excluded.
        // Verified implicitly by the exclusion test above; also check all results have percent > 0.
        val results = useCase(sevenNaturals)
        results.forEach { match ->
            assertTrue("percent must be > 0 (covered == 0 should be excluded)", match.percent > 0)
        }
    }
}
