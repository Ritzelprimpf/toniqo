package de.ritzelprimpf.toniqo.chordfinder.domain.model

import de.ritzelprimpf.toniqo.common.model.ChordQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VoicingTest {

    // Standard 6-string open PCs: E=4, A=9, D=2, G=7, B=11, E=4
    private val openPcs = listOf(4, 9, 2, 7, 11, 4)

    // C major (root=0, tones={0,4,7}): voicing [x,3,2,0,1,0]
    private val cMajMarks = listOf(
        FretMark.Muted, FretMark.Fretted(3), FretMark.Fretted(2),
        FretMark.Open, FretMark.Fretted(1), FretMark.Open,
    )
    private val cMajFingers = listOf(0, 3, 2, 0, 1, 0)
    private val cMajKey = ChordKey(0, ChordQuality.MAJOR)

    // C major barre [8,10,10,9,8,8]
    private val cMajBarreMarks = listOf(
        FretMark.Fretted(8), FretMark.Fretted(10), FretMark.Fretted(10),
        FretMark.Fretted(9), FretMark.Fretted(8), FretMark.Fretted(8),
    )
    private val cMajBarreBarre = Barre(8, 0, 5)

    // Em [0,2,2,0,0,0]
    private val emMarks = listOf(
        FretMark.Open, FretMark.Fretted(2), FretMark.Fretted(2),
        FretMark.Open, FretMark.Open, FretMark.Open,
    )
    private val emKey = ChordKey(4, ChordQuality.MINOR)

    // Barre-only [5,7,7,6,5,5]
    private val aMajBarreMarks = listOf(
        FretMark.Fretted(5), FretMark.Fretted(7), FretMark.Fretted(7),
        FretMark.Fretted(6), FretMark.Fretted(5), FretMark.Fretted(5),
    )
    private val aMajBarreBarre = Barre(5, 0, 5)
    private val aMajKey = ChordKey(9, ChordQuality.MAJOR)

    // ── category derivation ───────────────────────────────────────────────────────

    @Test
    fun `OPEN category when marks include Open`() {
        val rootIndices = setOf(1, 4)
        val v = Voicing.validated(1, cMajMarks, cMajFingers, null, rootIndices, ChordToneRole.ROOT, cMajKey, openPcs)
        assertEquals(VoicingCategory.OPEN, v.category)
    }

    @Test
    fun `BARRE category when no Open marks but barre present`() {
        val rootIndices = setOf(0, 2, 5)
        val v = Voicing.validated(1, cMajBarreMarks, listOf(1,3,4,2,1,1), cMajBarreBarre, rootIndices, ChordToneRole.ROOT, cMajKey, openPcs)
        assertEquals(VoicingCategory.BARRE, v.category)
    }

    @Test
    fun `SHAPE category when no Open marks and no barre`() {
        // E-shape barre without the barre field → SHAPE
        val shapeFrets = listOf(
            FretMark.Fretted(5), FretMark.Fretted(7), FretMark.Fretted(7),
            FretMark.Fretted(6), FretMark.Fretted(5), FretMark.Fretted(5),
        )
        val rootIndices = setOf(0, 2, 5)
        val v = Voicing.validated(1, shapeFrets, listOf(1,3,4,2,1,1), null, rootIndices, ChordToneRole.ROOT, aMajKey, openPcs)
        assertEquals(VoicingCategory.SHAPE, v.category)
    }

    @Test
    fun `OPEN category takes precedence over barre`() {
        // Em has open strings AND a notional barre — OPEN wins
        val eMjBarre = Barre(2, 1, 2)
        val rootIndices = setOf(0, 2, 5)
        val v = Voicing.validated(1, emMarks, listOf(0,2,3,0,0,0), eMjBarre, rootIndices, ChordToneRole.ROOT, emKey, openPcs)
        assertEquals(VoicingCategory.OPEN, v.category)
    }

    // ── fretRange + baseFret derivation ───────────────────────────────────────────

    @Test
    fun `fretRange for open C major is 1 to 3`() {
        val rootIndices = setOf(1, 4)
        val v = Voicing.validated(1, cMajMarks, cMajFingers, null, rootIndices, ChordToneRole.ROOT, cMajKey, openPcs)
        assertEquals(1..3, v.fretRange)
    }

    @Test
    fun `baseFret for open C major is 1`() {
        val rootIndices = setOf(1, 4)
        val v = Voicing.validated(1, cMajMarks, cMajFingers, null, rootIndices, ChordToneRole.ROOT, cMajKey, openPcs)
        assertEquals(1, v.baseFret)
    }

    @Test
    fun `fretRange for C major barre is 8 to 10`() {
        val rootIndices = setOf(0, 2, 5)
        val v = Voicing.validated(1, cMajBarreMarks, listOf(1,3,4,2,1,1), cMajBarreBarre, rootIndices, ChordToneRole.ROOT, cMajKey, openPcs)
        assertEquals(8..10, v.fretRange)
    }

    // ── validated: invariant violations ──────────────────────────────────────────

    @Test
    fun `validated throws when marks size does not match string count`() {
        assertThrows(IllegalArgumentException::class.java) {
            Voicing.validated(1, listOf(FretMark.Open), listOf(0), null, emptySet(), ChordToneRole.ROOT, cMajKey, openPcs)
        }
    }

    @Test
    fun `validated throws when note not in chord`() {
        // Replace G-string (fret 0 = G = pc 7) with fret 1 = Ab = pc 8, not in C major
        val badMarks = listOf(
            FretMark.Muted, FretMark.Fretted(3), FretMark.Fretted(2),
            FretMark.Fretted(1), FretMark.Fretted(1), FretMark.Open,
        )
        assertThrows(IllegalArgumentException::class.java) {
            Voicing.validated(1, badMarks, listOf(0,3,2,1,1,0), null, setOf(1,4), ChordToneRole.ROOT, cMajKey, openPcs)
        }
    }

    @Test
    fun `validated throws when root not in bass`() {
        // Start from D-string at fret 2 = E (pc 4, the third) — all chord tones present
        // but lowest sounded string is E (third), not C (root)
        val badMarks = listOf(
            FretMark.Muted, FretMark.Muted, FretMark.Fretted(2),
            FretMark.Open, FretMark.Fretted(1), FretMark.Open,
        )
        assertThrows(IllegalArgumentException::class.java) {
            Voicing.validated(1, badMarks, listOf(0,0,2,0,1,0), null, setOf(4), ChordToneRole.ROOT, cMajKey, openPcs)
        }
    }

    @Test
    fun `validated throws when fret span exceeds maximum`() {
        val bigSpanMarks = listOf(
            FretMark.Fretted(1), FretMark.Fretted(6), FretMark.Fretted(4),
            FretMark.Fretted(5), FretMark.Fretted(3), FretMark.Fretted(1),
        )
        assertThrows(IllegalArgumentException::class.java) {
            Voicing.validated(1, bigSpanMarks, listOf(1,3,2,4,1,1), null, setOf(0,5), ChordToneRole.ROOT, cMajKey, openPcs)
        }
    }

    @Test
    fun `validated throws when rootStringIndices is wrong`() {
        val rootIndices = setOf(0) // wrong: string 0 is Muted
        assertThrows(IllegalArgumentException::class.java) {
            Voicing.validated(1, cMajMarks, cMajFingers, null, rootIndices, ChordToneRole.ROOT, cMajKey, openPcs)
        }
    }
}
