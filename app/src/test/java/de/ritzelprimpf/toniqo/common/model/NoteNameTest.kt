package de.ritzelprimpf.toniqo.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteNameTest {

    @Test
    fun `all 12 pitch classes are present in ordinal order`() {
        val expected = listOf("C", "CSharp", "D", "DSharp", "E", "F", "FSharp", "G", "GSharp", "A", "ASharp", "B")
        assertEquals(expected, NoteName.entries.map { it.name })
    }

    @Test
    fun `semitonesFromC returns 0 through 11 in ordinal order`() {
        NoteName.entries.forEachIndexed { index, noteName ->
            assertEquals("${noteName.name}.semitonesFromC", index, noteName.semitonesFromC)
        }
    }

    @Test
    fun `sharpName is correct for all 12 pitch classes`() {
        val expected = mapOf(
            NoteName.C to "C", NoteName.CSharp to "C#", NoteName.D to "D",
            NoteName.DSharp to "D#", NoteName.E to "E", NoteName.F to "F",
            NoteName.FSharp to "F#", NoteName.G to "G", NoteName.GSharp to "G#",
            NoteName.A to "A", NoteName.ASharp to "A#", NoteName.B to "B",
        )
        expected.forEach { (note, name) ->
            assertEquals("${note.name}.sharpName", name, note.sharpName)
        }
    }

    @Test
    fun `flatName is correct for all 12 pitch classes`() {
        val expected = mapOf(
            NoteName.C to "C", NoteName.CSharp to "Db", NoteName.D to "D",
            NoteName.DSharp to "Eb", NoteName.E to "E", NoteName.F to "F",
            NoteName.FSharp to "Gb", NoteName.G to "G", NoteName.GSharp to "Ab",
            NoteName.A to "A", NoteName.ASharp to "Bb", NoteName.B to "B",
        )
        expected.forEach { (note, name) ->
            assertEquals("${note.name}.flatName", name, note.flatName)
        }
    }

    @Test
    fun `natural notes have identical sharpName and flatName`() {
        val naturals = listOf(NoteName.C, NoteName.D, NoteName.E, NoteName.F,
            NoteName.G, NoteName.A, NoteName.B)
        naturals.forEach { note ->
            assertEquals("${note.name} natural should have same sharp and flat name",
                note.sharpName, note.flatName)
        }
    }

    @Test
    fun `accidentals have different sharpName and flatName`() {
        val accidentals = listOf(NoteName.CSharp, NoteName.DSharp, NoteName.FSharp,
            NoteName.GSharp, NoteName.ASharp)
        accidentals.forEach { note ->
            assert(note.sharpName != note.flatName) {
                "${note.name} accidental should have different sharp and flat names"
            }
        }
    }

    @Test
    fun `parse accepts sharp spellings case-insensitively`() {
        assertEquals(NoteName.C, NoteName.parse("C"))
        assertEquals(NoteName.CSharp, NoteName.parse("C#"))
        assertEquals(NoteName.FSharp, NoteName.parse("F#"))
        assertEquals(NoteName.GSharp, NoteName.parse("G#"))
        assertEquals(NoteName.ASharp, NoteName.parse("A#"))
        assertEquals(NoteName.B, NoteName.parse("b"))
        assertEquals(NoteName.CSharp, NoteName.parse("c#"))
    }

    @Test
    fun `parse accepts flat spellings case-insensitively`() {
        assertEquals(NoteName.CSharp, NoteName.parse("Db"))
        assertEquals(NoteName.DSharp, NoteName.parse("Eb"))
        assertEquals(NoteName.FSharp, NoteName.parse("Gb"))
        assertEquals(NoteName.GSharp, NoteName.parse("Ab"))
        assertEquals(NoteName.ASharp, NoteName.parse("Bb"))
        assertEquals(NoteName.DSharp, NoteName.parse("eb"))
        assertEquals(NoteName.ASharp, NoteName.parse("bb"))
    }

    @Test
    fun `parse trims whitespace`() {
        assertEquals(NoteName.A, NoteName.parse("  A  "))
        assertEquals(NoteName.CSharp, NoteName.parse(" C# "))
    }

    @Test
    fun `parse returns null for invalid input`() {
        assertNull(NoteName.parse(""))
        assertNull(NoteName.parse("X"))
        assertNull(NoteName.parse("H"))
        assertNull(NoteName.parse("C##"))
        assertNull(NoteName.parse("123"))
    }
}
