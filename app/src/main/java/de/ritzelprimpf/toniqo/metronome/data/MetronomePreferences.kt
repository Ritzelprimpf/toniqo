package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import kotlinx.coroutines.flow.Flow

/**
 * Persists the metronome's configuration (BPM, time signature, subdivision) across app launches.
 *
 * The active state ([MetronomeConfig.DEFAULT]) is exposed as a [Flow] so the ViewModel can react
 * to changes reactively. The player state (`isPlaying`, `currentBeat`) is **not** persisted —
 * the metronome always launches in the stopped state.
 *
 * DataStore-backed implementation: [MetronomePreferencesImpl] (production).
 * In-memory fake for tests: `FakeMetronomePreferences` (test source set).
 *
 * Per `Phase6-Metronome-Decisions.md` Item 4: separate DataStore file `metronome_preferences`,
 * independent of the tuner's `tuner_preferences`.
 */
interface MetronomePreferences {

    /**
     * The active metronome configuration as a reactive stream.
     *
     * Emits [MetronomeConfig.DEFAULT] on first launch (no persisted data). Validates and
     * self-heals corrupted data on read (see [MetronomePreferencesImpl]).
     */
    val config: Flow<MetronomeConfig>

    /**
     * Persists [config] to DataStore.
     *
     * Subsequent reads of [config] will emit the new value. Call on every user change to BPM,
     * time signature, or subdivision.
     */
    suspend fun setConfig(config: MetronomeConfig)
}
