package de.ritzelprimpf.toniqo.common.util

import de.ritzelprimpf.toniqo.common.model.Mode
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import de.ritzelprimpf.toniqo.common.model.Scale
import org.junit.Assert.assertThrows
import org.junit.Test

class MusicTheoryTest {

    private val a4 = Note(NoteName.A, octave = 4)
    private val cMajor = Scale(
        root = Note(NoteName.C, octave = 4),
        intervals = Mode.IONIAN.intervalsFromRoot,
    )

    @Test
    fun `noteToFrequency throws in Phase 2`() {
        assertThrows(NotImplementedError::class.java) { MusicTheory.noteToFrequency(a4) }
    }

    @Test
    fun `frequencyToNote throws in Phase 2`() {
        assertThrows(NotImplementedError::class.java) {
            MusicTheory.frequencyToNote(frequencyHz = 440.0)
        }
    }

    @Test
    fun `buildScale throws in Phase 2`() {
        assertThrows(NotImplementedError::class.java) {
            MusicTheory.buildScale(root = a4, mode = Mode.IONIAN)
        }
    }

    @Test
    fun `buildTriads throws in Phase 2`() {
        assertThrows(NotImplementedError::class.java) { MusicTheory.buildTriads(cMajor) }
    }

    @Test
    fun `buildSeventhChords throws in Phase 2`() {
        assertThrows(NotImplementedError::class.java) { MusicTheory.buildSeventhChords(cMajor) }
    }
}
