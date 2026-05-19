package de.ritzelprimpf.toniqo.chordfinder

import de.ritzelprimpf.toniqo.chordfinder.data.ChordFinderServiceImpl
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordFinderInput
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordFinderResult
import de.ritzelprimpf.toniqo.chordfinder.domain.model.DegreeChord
import de.ritzelprimpf.toniqo.chordfinder.domain.usecase.FindChordsUseCase
import de.ritzelprimpf.toniqo.common.model.Chord
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.common.model.Mode
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ChordFinderStubsTest {

    private val cNote = Note(NoteName.C, octave = 4)
    private val sampleInput = ChordFinderInput(
        root = cNote,
        mode = Mode.IONIAN,
        includeSeventhChords = false,
    )

    @Test
    fun `ChordFinderServiceImpl can be constructed and throws on findChords`() {
        val service = ChordFinderServiceImpl()

        assertThrows(NotImplementedError::class.java) { service.findChords(sampleInput) }
    }

    @Test
    fun `FindChordsUseCase propagates the service stub's NotImplementedError`() {
        val useCase = FindChordsUseCase(service = ChordFinderServiceImpl())

        assertThrows(NotImplementedError::class.java) { useCase(sampleInput) }
    }

    @Test
    fun `ChordFinderInput data class equality holds for matching fields`() {
        val a = sampleInput
        val b = sampleInput.copy()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `ChordFinderInput data class differs when seventh toggle differs`() {
        val a = sampleInput
        val b = sampleInput.copy(includeSeventhChords = true)

        assertNotEquals(a, b)
    }

    @Test
    fun `DegreeChord data class equality holds for matching fields`() {
        val chord = Chord(root = cNote, quality = ChordQuality.MAJOR)
        val a = DegreeChord(degree = 1, romanNumeral = "I", chord = chord)
        val b = a.copy()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `ChordFinderResult data class equality holds for matching fields`() {
        val chord = Chord(root = cNote, quality = ChordQuality.MAJOR)
        val degree = DegreeChord(degree = 1, romanNumeral = "I", chord = chord)
        val a = ChordFinderResult(chords = listOf(degree))
        val b = a.copy()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
