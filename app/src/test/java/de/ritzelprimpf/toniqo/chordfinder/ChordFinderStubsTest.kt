package de.ritzelprimpf.toniqo.chordfinder

import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordFinderInput
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordToneRole
import de.ritzelprimpf.toniqo.chordfinder.domain.model.DegreeChord
import de.ritzelprimpf.toniqo.chordfinder.domain.model.FretMark
import de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing
import de.ritzelprimpf.toniqo.chordfinder.domain.usecase.FindChordsUseCase
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.common.model.ScaleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Compilation-safety and data-class contract tests for the Phase 8 Chord Finder model.
 *
 * These tests replaced the obsolete Phase 2 stubs, which referenced the old
 * `ChordFinderInput(root, mode)` and `DegreeChord(chord=…)` APIs.
 */
class ChordFinderStubsTest {

    private val useCase = FindChordsUseCase()

    // ── FindChordsUseCase smoke tests ─────────────────────────────────────────────

    @Test
    fun `FindChordsUseCase C major returns 7 chords`() {
        val input = ChordFinderInput(rootPitchClass = 0, scaleType = ScaleType.IONIAN, includeSeventhChords = false)
        val result = useCase(input)

        assertEquals(7, result.chords.size)
    }

    @Test
    fun `FindChordsUseCase A natural minor first chord is Am`() {
        val input = ChordFinderInput(rootPitchClass = 9, scaleType = ScaleType.AEOLIAN, includeSeventhChords = false)
        val result = useCase(input)

        val firstChord = result.chords.first()
        assertEquals(1, firstChord.degree)
        assertEquals(ChordQuality.MINOR, firstChord.triadQuality)
        assertEquals("Am", firstChord.symbol)
    }

    @Test
    fun `FindChordsUseCase seventh chords produce 4 note names per chord`() {
        val input = ChordFinderInput(rootPitchClass = 0, scaleType = ScaleType.IONIAN, includeSeventhChords = true)
        val result = useCase(input)

        result.chords.forEach { chord ->
            assertEquals("Expected 4 note names for ${chord.symbol}", 4, chord.noteNames.size)
        }
    }

    // ── ChordFinderInput data class ───────────────────────────────────────────────

    @Test
    fun `ChordFinderInput data class equality holds for matching fields`() {
        val a = ChordFinderInput(rootPitchClass = 0, scaleType = ScaleType.IONIAN, includeSeventhChords = false)
        val b = a.copy()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `ChordFinderInput data class differs when seventh toggle differs`() {
        val a = ChordFinderInput(rootPitchClass = 0, scaleType = ScaleType.IONIAN, includeSeventhChords = false)
        val b = a.copy(includeSeventhChords = true)

        assertNotEquals(a, b)
    }

    // ── DegreeChord data class ────────────────────────────────────────────────────

    @Test
    fun `DegreeChord data class equality holds for matching fields`() {
        val a = DegreeChord(
            degree = 1,
            romanNumeral = "I",
            triadQuality = ChordQuality.MAJOR,
            seventhQuality = null,
            rootName = "C",
            noteNames = listOf("C", "E", "G"),
            symbol = "C",
        )
        val b = a.copy()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ── ChordKey data class ───────────────────────────────────────────────────────

    @Test
    fun `ChordKey equality holds for same root and quality`() {
        val a = ChordKey(rootPitchClass = 0, quality = ChordQuality.MAJOR)
        val b = ChordKey(rootPitchClass = 0, quality = ChordQuality.MAJOR)

        assertEquals(a, b)
    }

    @Test
    fun `ChordKey differs when root differs`() {
        val a = ChordKey(rootPitchClass = 0, quality = ChordQuality.MAJOR)
        val b = ChordKey(rootPitchClass = 2, quality = ChordQuality.MAJOR)

        assertNotEquals(a, b)
    }

    // ── Voicing data class ────────────────────────────────────────────────────────

    @Test
    fun `Voicing data class can be constructed and baseFret is derived from marks`() {
        val voicing = Voicing(
            labelKey = 1,
            marks = List(6) { FretMark.Fretted(3) },
            fingers = List(6) { 1 },
            barre = null,
            rootStringIndices = setOf(0),
            bassDegree = ChordToneRole.ROOT,
        )

        assertNotNull(voicing)
        assertEquals(3, voicing.baseFret)
    }
}
