package de.ritzelprimpf.toniqo.chordfinder.domain

import de.ritzelprimpf.toniqo.chordfinder.domain.model.Barre
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordToneRole
import de.ritzelprimpf.toniqo.chordfinder.domain.model.FretMark
import de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VoicingTransposerTest {

    // open PCs for standard 6-string: [4, 9, 2, 7, 11, 4]
    private val openPcs = listOf(4, 9, 2, 7, 11, 4)

    // C major barre [8,10,10,9,8,8] — BARRE category (no open strings)
    private val cMajBarre = Voicing.validated(
        labelKey = 1,
        marks = listOf(
            FretMark.Fretted(8), FretMark.Fretted(10), FretMark.Fretted(10),
            FretMark.Fretted(9), FretMark.Fretted(8), FretMark.Fretted(8),
        ),
        fingers = listOf(1, 3, 4, 2, 1, 1),
        barre = Barre(8, 0, 5),
        rootStringIndices = setOf(0, 2, 5),
        bassDegree = ChordToneRole.ROOT,
        chordKey = ChordKey(0, ChordQuality.MAJOR),
        openNotes = openPcs,
    )

    // C major open [x,3,2,0,1,0] — OPEN category
    private val cMajOpen = Voicing.validated(
        labelKey = 2,
        marks = listOf(
            FretMark.Muted, FretMark.Fretted(3), FretMark.Fretted(2),
            FretMark.Open, FretMark.Fretted(1), FretMark.Open,
        ),
        fingers = listOf(0, 3, 2, 0, 1, 0),
        barre = null,
        rootStringIndices = setOf(1, 4),
        bassDegree = ChordToneRole.ROOT,
        chordKey = ChordKey(0, ChordQuality.MAJOR),
        openNotes = openPcs,
    )

    // ── shift by +2 ───────────────────────────────────────────────────────────────

    @Test
    fun `shift barre voicing by 2 adds 2 to all fretted marks`() {
        val shifted = VoicingTransposer.shift(cMajBarre, deltaFrets = 2, maxFret = 15)
        assertNotNull(shifted)
        val frets = shifted!!.marks.filterIsInstance<FretMark.Fretted>().map { it.fret }
        assertEquals(listOf(10, 12, 12, 11, 10, 10), frets)
    }

    @Test
    fun `shift barre voicing by 2 shifts barre fret`() {
        val shifted = VoicingTransposer.shift(cMajBarre, deltaFrets = 2, maxFret = 15)
        assertNotNull(shifted)
        assertEquals(10, shifted!!.barre!!.fret)
    }

    @Test
    fun `shift does not change fingers`() {
        val shifted = VoicingTransposer.shift(cMajBarre, deltaFrets = 2, maxFret = 15)
        assertNotNull(shifted)
        assertEquals(cMajBarre.fingers, shifted!!.fingers)
    }

    @Test
    fun `shift does not change rootStringIndices`() {
        val shifted = VoicingTransposer.shift(cMajBarre, deltaFrets = 2, maxFret = 15)
        assertNotNull(shifted)
        assertEquals(cMajBarre.rootStringIndices, shifted!!.rootStringIndices)
    }

    // ── open voicing returns null ─────────────────────────────────────────────────

    @Test
    fun `open-category voicing returns null`() {
        val result = VoicingTransposer.shift(cMajOpen, deltaFrets = 1, maxFret = 15)
        assertNull(result)
    }

    // ── off-window shift returns null ─────────────────────────────────────────────

    @Test
    fun `shift that pushes fret above maxFret returns null`() {
        val result = VoicingTransposer.shift(cMajBarre, deltaFrets = 8, maxFret = 15)
        assertNull("fret 10+8=18 > 15, expected null", result)
    }

    @Test
    fun `shift by 0 returns unchanged voicing`() {
        val shifted = VoicingTransposer.shift(cMajBarre, deltaFrets = 0, maxFret = 15)
        assertNotNull(shifted)
        assertEquals(cMajBarre.marks, shifted!!.marks)
        assertEquals(cMajBarre.barre, shifted.barre)
    }
}
