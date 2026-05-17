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
import org.junit.Test

class TunerStubsTest {

    @Test
    fun `TunerPresetRepositoryImpl can be constructed and throws on getPresets`() {
        val repo = TunerPresetRepositoryImpl()

        assertThrows(NotImplementedError::class.java) { runBlocking { repo.getPresets() } }
    }

    @Test
    fun `TunerPresetRepositoryImpl throws on getPresetById`() {
        val repo = TunerPresetRepositoryImpl()

        assertThrows(NotImplementedError::class.java) {
            runBlocking { repo.getPresetById(id = "anything") }
        }
    }

    @Test
    fun `YinPitchDetector can be constructed and throws on detectPitch`() {
        val detector = YinPitchDetector()

        assertThrows(NotImplementedError::class.java) {
            detector.detectPitch(audioBuffer = FloatArray(size = 0), sampleRateHz = 44100)
        }
    }

    @Test
    fun `GetTunerPresetsUseCase propagates the repository stub's NotImplementedError`() {
        val useCase = GetTunerPresetsUseCase(repository = TunerPresetRepositoryImpl())

        assertThrows(NotImplementedError::class.java) { runBlocking { useCase() } }
    }

    @Test
    fun `TunerPreset data class equality holds for matching fields`() {
        val a = SAMPLE_PRESET
        val b = SAMPLE_PRESET.copy()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `TunerPreset data class differs when id differs`() {
        val a = SAMPLE_PRESET
        val b = SAMPLE_PRESET.copy(id = "different")

        assertNotEquals(a, b)
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
