package de.ritzelprimpf.toniqo.common.state

import de.ritzelprimpf.toniqo.common.model.GuitarTuning
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectedTuningStoreTest {

    private val store = SelectedTuningStore()

    @Test
    fun `default selection is E Standard with STANDARD_6 tuning`() {
        val selection = store.selection.value
        assertEquals(GuitarTuning.STANDARD_6, selection.tuning)
        assertEquals("E Standard", selection.label)
    }

    @Test
    fun `publish updates tuning and label`() {
        val ebStandard = GuitarTuning(
            id = "eb_standard",
            openNotes = listOf(
                Note(NoteName.DSharp, 2),
                Note(NoteName.GSharp, 2),
                Note(NoteName.CSharp, 3),
                Note(NoteName.FSharp, 3),
                Note(NoteName.ASharp, 3),
                Note(NoteName.DSharp, 4),
            ),
        )
        store.publish(ebStandard, "E♭ Standard")

        val selection = store.selection.value
        assertEquals(ebStandard, selection.tuning)
        assertEquals("E♭ Standard", selection.label)
    }

    @Test
    fun `successive publishes update selection to latest value`() {
        store.publish(GuitarTuning.STANDARD_6, "E Standard")
        val other = GuitarTuning(id = "other", openNotes = GuitarTuning.STANDARD_6.openNotes)
        store.publish(other, "Other")

        assertEquals("Other", store.selection.value.label)
        assertEquals(other, store.selection.value.tuning)
    }

    @Test
    fun `uniformOffsetFrom verifies E-flat standard is minus one semitone below standard`() {
        val ebStandard = GuitarTuning(
            id = "eb_standard",
            openNotes = listOf(
                Note(NoteName.DSharp, 2),
                Note(NoteName.GSharp, 2),
                Note(NoteName.CSharp, 3),
                Note(NoteName.FSharp, 3),
                Note(NoteName.ASharp, 3),
                Note(NoteName.DSharp, 4),
            ),
        )
        assertEquals(-1, ebStandard.uniformOffsetFrom(GuitarTuning.STANDARD_6))
    }
}
