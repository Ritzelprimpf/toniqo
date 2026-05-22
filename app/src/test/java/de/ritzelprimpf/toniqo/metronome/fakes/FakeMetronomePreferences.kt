package de.ritzelprimpf.toniqo.metronome.fakes

import de.ritzelprimpf.toniqo.metronome.data.MetronomePreferences
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory test double for [MetronomePreferences].
 *
 * Backed by a [MutableStateFlow] so tests can seed the initial config and observe writes via the
 * [storedConfig] inspection property. Simulates first-launch state when constructed with
 * [MetronomeConfig.DEFAULT].
 *
 * @param initialConfig Starting value for [config]; defaults to [MetronomeConfig.DEFAULT].
 */
class FakeMetronomePreferences(
    initialConfig: MetronomeConfig = MetronomeConfig.DEFAULT,
) : MetronomePreferences {

    private val _config = MutableStateFlow(initialConfig)

    override val config: Flow<MetronomeConfig> = _config.asStateFlow()

    override suspend fun setConfig(config: MetronomeConfig) {
        _config.value = config
    }

    /** Inspection property: the last value written via [setConfig], or the initial config. */
    val storedConfig: MetronomeConfig get() = _config.value
}
