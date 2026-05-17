package de.ritzelprimpf.toniqo.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ChordTest {

    private val root = Note(NoteName.C, octave = 4)
    private val cMajorTriad = Chord(
        root = root,
        quality = ChordQuality.MAJOR,
        notes = listOf(
            Note(NoteName.C, octave = 4),
            Note(NoteName.E, octave = 4),
            Note(NoteName.G, octave = 4),
        ),
    )

    @Test
    fun `data class equality holds for matching root, quality, and notes`() {
        val twin = Chord(
            root = root,
            quality = ChordQuality.MAJOR,
            notes = listOf(
                Note(NoteName.C, octave = 4),
                Note(NoteName.E, octave = 4),
                Note(NoteName.G, octave = 4),
            ),
        )

        assertEquals(cMajorTriad, twin)
        assertEquals(cMajorTriad.hashCode(), twin.hashCode())
    }

    @Test
    fun `chords differ when quality differs`() {
        val cMinor = cMajorTriad.copy(
            quality = ChordQuality.MINOR,
            notes = listOf(
                Note(NoteName.C, octave = 4),
                Note(NoteName.DSharp, octave = 4),
                Note(NoteName.G, octave = 4),
            ),
        )

        assertNotEquals(cMajorTriad, cMinor)
    }

    @Test
    fun `displayName throws because Phase 2 leaves it unimplemented`() {
        assertThrows(NotImplementedError::class.java) { cMajorTriad.displayName() }
    }
}
