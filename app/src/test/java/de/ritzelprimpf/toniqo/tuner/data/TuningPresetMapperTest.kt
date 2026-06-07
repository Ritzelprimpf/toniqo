package de.ritzelprimpf.toniqo.tuner.data

import de.ritzelprimpf.toniqo.common.model.GuitarTuning
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerCategory
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class TuningPresetMapperTest {

    private fun preset(id: String, displayName: String, notes: List<Note>) = TunerPreset(
        id = id,
        displayName = displayName,
        category = TunerCategory.STANDARD,
        stringCount = notes.size,
        notes = notes,
    )

    private val standardNotes = listOf(
        Note(NoteName.E, 2),
        Note(NoteName.A, 2),
        Note(NoteName.D, 3),
        Note(NoteName.G, 3),
        Note(NoteName.B, 3),
        Note(NoteName.E, 4),
    )

    private val ebStandardNotes = listOf(
        Note(NoteName.DSharp, 2),
        Note(NoteName.GSharp, 2),
        Note(NoteName.CSharp, 3),
        Note(NoteName.FSharp, 3),
        Note(NoteName.ASharp, 3),
        Note(NoteName.DSharp, 4),
    )

    @Test
    fun `map preserves preset id as tuning id`() {
        val p = preset("six_string_standard_e", "E Standard", standardNotes)
        val tuning = TuningPresetMapper.map(p)
        assertEquals("six_string_standard_e", tuning.id)
    }

    @Test
    fun `map preserves open notes`() {
        val p = preset("six_string_standard_e", "E Standard", standardNotes)
        val tuning = TuningPresetMapper.map(p)
        assertEquals(standardNotes, tuning.openNotes)
    }

    @Test
    fun `E standard maps to a GuitarTuning equal to STANDARD_6`() {
        val p = preset("six_string_standard_e", "E Standard", standardNotes)
        val tuning = TuningPresetMapper.map(p)
        // uniformOffsetFrom(STANDARD_6) == 0 means the tuning is identical
        assertEquals(0, tuning.uniformOffsetFrom(GuitarTuning.STANDARD_6))
    }

    @Test
    fun `E-flat standard maps to a tuning that is minus 1 semitone from STANDARD_6`() {
        val p = preset("eb_standard", "E♭ Standard", ebStandardNotes)
        val tuning = TuningPresetMapper.map(p)
        val offset = tuning.uniformOffsetFrom(GuitarTuning.STANDARD_6)
        assertNotNull(offset)
        assertEquals(-1, offset)
    }

    @Test
    fun `result has correct string count`() {
        val p = preset("six_string_standard_e", "E Standard", standardNotes)
        val tuning = TuningPresetMapper.map(p)
        assertEquals(6, tuning.stringCount)
    }
}
