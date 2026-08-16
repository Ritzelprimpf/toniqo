package de.ritzelprimpf.toniqo.keyfinder.domain.model

import de.ritzelprimpf.toniqo.common.model.Mode
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import de.ritzelprimpf.toniqo.common.model.Scale
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyFinderResultTest {

    @Test
    fun `data class equality holds for matching fields`() {
        val cNote = Note(NoteName.C, octave = 4)
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
