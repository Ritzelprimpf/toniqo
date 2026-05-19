package de.ritzelprimpf.toniqo.tuner

import de.ritzelprimpf.toniqo.tuner.data.TunerPresetRepositoryImpl
import de.ritzelprimpf.toniqo.tuner.data.YinPitchDetector
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerCategory
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset
import de.ritzelprimpf.toniqo.tuner.domain.usecase.GetTunerPresetsUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TunerStubsTest {

    // ── TunerPresetRepositoryImpl ─────────────────────────────────────────────────

    @Test
    fun `TunerPresetRepositoryImpl getPresets returns a non-empty list`() = runBlocking {
        val repo = TunerPresetRepositoryImpl()
        assertTrue(repo.getPresets().isNotEmpty())
    }

    @Test
    fun `TunerPresetRepositoryImpl getPresetById returns null for unknown id`() = runBlocking {
        val repo = TunerPresetRepositoryImpl()
        assertEquals(null, repo.getPresetById("does_not_exist"))
    }

    // ── GetTunerPresetsUseCase ────────────────────────────────────────────────────

    @Test
    fun `GetTunerPresetsUseCase returns presets from repository`() = runBlocking {
        val useCase = GetTunerPresetsUseCase(repository = TunerPresetRepositoryImpl())
        assertTrue(useCase().isNotEmpty())
    }

    // ── YinPitchDetector (Phase 5.2 stub) ────────────────────────────────────────

    @Test
    fun `YinPitchDetector can be constructed and throws on detectPitch`() {
        val detector = YinPitchDetector()
        assertThrows(NotImplementedError::class.java) {
            detector.detectPitch(audioBuffer = FloatArray(size = 0), sampleRateHz = 44100)
        }
    }

    // ── TunerPreset data class ────────────────────────────────────────────────────

    @Test
    fun `TunerPreset data class equality holds for matching fields`() {
        val a = SAMPLE_PRESET
        val b = SAMPLE_PRESET.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `TunerPreset data class differs when id differs`() {
        assertNotEquals(SAMPLE_PRESET, SAMPLE_PRESET.copy(id = "different"))
    }

    companion object {
        private val SAMPLE_PRESET = TunerPreset(
            id = "sample",
            displayName = "Sample",
            category = TunerCategory.STANDARD,
            stringCount = 0,
            notes = emptyList(),
        )
    }
}
