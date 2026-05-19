package de.ritzelprimpf.toniqo.tuner.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TunerPresetRepositoryImplTest {

    private val repo = TunerPresetRepositoryImpl()

    @Test
    fun `getPresets returns the full catalog`() = runBlocking {
        val presets = repo.getPresets()
        assertEquals(TunerPresets.all.size, presets.size)
    }

    @Test
    fun `getPresetById returns the matching preset for a known ID`() = runBlocking {
        val preset = repo.getPresetById("six_string_standard_e")
        assertNotNull(preset)
        assertEquals("E Standard", preset!!.displayName)
        assertEquals(6, preset.stringCount)
    }

    @Test
    fun `getPresetById returns null for an unknown ID`() = runBlocking {
        assertNull(repo.getPresetById("no_such_preset"))
    }

    @Test
    fun `getPresetsGrouped returns the same structure as TunerPresets grouped`() = runBlocking {
        val grouped = repo.getPresetsGrouped()
        assertEquals(TunerPresets.grouped.keys, grouped.keys)
        grouped.forEach { (count, byCategory) ->
            val expected = TunerPresets.grouped[count]!!
            assertEquals(expected.keys, byCategory.keys)
            byCategory.forEach { (category, presets) ->
                assertEquals(expected[category]!!.size, presets.size)
            }
        }
    }

    @Test
    fun `getPresetsGrouped outer keys are 6 7 and 8`() = runBlocking {
        assertEquals(setOf(6, 7, 8), repo.getPresetsGrouped().keys)
    }

    @Test
    fun `getPresetById round-trips for every preset ID`() = runBlocking {
        TunerPresets.all.forEach { expected ->
            val found = repo.getPresetById(expected.id)
            assertNotNull("Preset '${expected.id}' should be findable by ID", found)
            assertEquals(expected, found)
        }
    }

    @Test
    fun `getPresets result is non-empty`() = runBlocking {
        assertTrue(repo.getPresets().isNotEmpty())
    }
}
