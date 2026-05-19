package de.ritzelprimpf.toniqo.keyfinder

import de.ritzelprimpf.toniqo.common.model.Mode
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import de.ritzelprimpf.toniqo.common.model.Scale
import de.ritzelprimpf.toniqo.keyfinder.data.KeyFinderServiceImpl
import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderInput
import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderResult
import de.ritzelprimpf.toniqo.keyfinder.domain.usecase.FindKeysUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class KeyFinderStubsTest {

    private val cNote = Note(NoteName.C, octave = 4)
    private val sampleInput = KeyFinderInput(
        notes = setOf(cNote, Note(NoteName.E, octave = 4)),
        tonic = cNote,
    )

    @Test
    fun `KeyFinderServiceImpl can be constructed and throws on findKeys`() {
        val service = KeyFinderServiceImpl()

        assertThrows(NotImplementedError::class.java) { service.findKeys(sampleInput) }
    }

    @Test
    fun `FindKeysUseCase propagates the service stub's NotImplementedError`() {
        val useCase = FindKeysUseCase(service = KeyFinderServiceImpl())

        assertThrows(NotImplementedError::class.java) { useCase(sampleInput) }
    }

    @Test
    fun `KeyFinderInput data class equality holds for matching fields`() {
        val a = sampleInput
        val b = sampleInput.copy()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `KeyFinderInput data class differs when tonic differs`() {
        val a = sampleInput
        val b = sampleInput.copy(tonic = null)

        assertNotEquals(a, b)
    }

    @Test
    fun `KeyFinderResult data class equality holds for matching fields`() {
        val scale = Scale(root = cNote, mode = Mode.IONIAN)
        val a = KeyFinderResult(
            scale = scale,
            modeName = "C Major (Ionian)",
            matchScore = 1.0f,
            isFullMatch = true,
            matchesTonic = true,
        )
        val b = a.copy()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
