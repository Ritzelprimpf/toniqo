package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import de.ritzelprimpf.toniqo.metronome.fakes.FakeMetronomePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies that [FakeMetronomePreferences] correctly fulfills the [de.ritzelprimpf.toniqo.metronome.data.MetronomePreferences]
 * contract, so tests that rely on the fake can trust its behavior.
 */
class FakeMetronomePreferencesTest {

    @Test
    fun `config flow emits the provided initial config`() = runTest {
        val initial = MetronomeConfig(bpm = 90, subdivision = Subdivision.EIGHTHS)
        val prefs = FakeMetronomePreferences(initial)

        assertEquals(initial, prefs.config.first())
    }

    @Test
    fun `defaults to MetronomeConfig DEFAULT when no initial config is given`() = runTest {
        val prefs = FakeMetronomePreferences()

        assertEquals(MetronomeConfig.DEFAULT, prefs.config.first())
    }

    @Test
    fun `setConfig updates the config flow`() = runTest {
        val prefs = FakeMetronomePreferences()
        val newConfig = MetronomeConfig(bpm = 140, subdivision = Subdivision.SIXTEENTHS)

        prefs.setConfig(newConfig)

        assertEquals(newConfig, prefs.config.first())
    }

    @Test
    fun `storedConfig reflects the most recently written value`() = runTest {
        val prefs = FakeMetronomePreferences()
        val config1 = MetronomeConfig(bpm = 80)
        val config2 = MetronomeConfig(bpm = 160)

        prefs.setConfig(config1)
        prefs.setConfig(config2)

        assertEquals(config2, prefs.storedConfig)
    }

    @Test
    fun `storedConfig returns the initial config before any setConfig call`() {
        val initial = MetronomeConfig(bpm = 75)
        val prefs = FakeMetronomePreferences(initial)

        assertEquals(initial, prefs.storedConfig)
    }

    @Test
    fun `sequential setConfig calls each update the emitted value`() = runTest {
        val prefs = FakeMetronomePreferences()
        val configs = listOf(
            MetronomeConfig(bpm = 60),
            MetronomeConfig(bpm = 120),
            MetronomeConfig(bpm = 200),
        )

        configs.forEach { config ->
            prefs.setConfig(config)
            assertEquals(config, prefs.config.first())
        }
    }
}
