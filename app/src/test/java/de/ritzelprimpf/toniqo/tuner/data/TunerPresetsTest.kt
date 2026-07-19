package de.ritzelprimpf.toniqo.tuner.data

import de.ritzelprimpf.toniqo.tuner.domain.model.TunerCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TunerPresetsTest {

    @Test
    fun `all preset IDs are unique`() {
        val ids = TunerPresets.all.map { it.id }
        assertEquals("All preset IDs must be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun `every preset notes size matches its stringCount`() {
        TunerPresets.all.forEach { preset ->
            assertEquals(
                "Preset '${preset.id}': notes.size should equal stringCount",
                preset.stringCount,
                preset.notes.size,
            )
        }
    }

    @Test
    fun `catalog contains presets for exactly string counts 6 7 and 8`() {
        val counts = TunerPresets.all.map { it.stringCount }.toSet()
        assertEquals(setOf(6, 7, 8), counts)
    }

    @Test
    fun `grouped map outer keys are 6 7 and 8`() {
        assertEquals(setOf(6, 7, 8), TunerPresets.grouped.keys)
    }

    @Test
    fun `grouped map inner keys include STANDARD for every string count`() {
        TunerPresets.grouped.forEach { (count, byCategory) ->
            assertTrue(
                "String count $count should have STANDARD presets",
                byCategory.containsKey(TunerCategory.STANDARD),
            )
        }
    }

    @Test
    fun `grouped map contains all presets with no loss`() {
        val flattenedCount = TunerPresets.grouped.values
            .flatMap { it.values }
            .sumOf { it.size }
        assertEquals(TunerPresets.all.size, flattenedCount)
    }

    @Test
    fun `E Standard 6-string is in the catalog`() {
        val preset = TunerPresets.all.first { it.id == "six_string_standard_e" }
        assertEquals("E Standard", preset.displayName)
        assertEquals(6, preset.stringCount)
        assertEquals(TunerCategory.STANDARD, preset.category)
    }

    @Test
    fun `C Standard 6-string is in the catalog`() {
        val preset = TunerPresets.all.first { it.id == "six_string_standard_c" }
        assertEquals("C Standard", preset.displayName)
        assertEquals(6, preset.stringCount)
        assertEquals(TunerCategory.STANDARD, preset.category)
        // Lowest string should be C2, 4 half steps below E Standard's low E2.
        assertEquals("C2", preset.notes.first().displayName())
    }

    @Test
    fun `Drop C 8-string preset is present and parses correctly`() {
        val preset = TunerPresets.all.first { it.id == "eight_string_drop_c" }
        assertEquals(8, preset.notes.size)
        // Lowest string should be C1.
        assertEquals("C1", preset.notes.first().displayName())
    }

    @Test
    fun `all preset IDs follow the snake_case structural format`() {
        // IDs must contain at least two underscores and only lowercase + digits + underscores.
        TunerPresets.all.forEach { preset ->
            assertTrue(
                "Preset ID '${preset.id}' must match snake_case format",
                preset.id.matches(Regex("[a-z0-9_]+")) && preset.id.count { it == '_' } >= 2,
            )
        }
    }

    @Test
    fun `catalog has at least 30 presets`() {
        assertTrue("Expected ≥30 presets, got ${TunerPresets.all.size}", TunerPresets.all.size >= 30)
    }
}
