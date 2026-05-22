package de.ritzelprimpf.toniqo.metronome.presentation.viewmodel

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.TempoDescriptor
import de.ritzelprimpf.toniqo.metronome.domain.model.tempoDescriptorFor

/**
 * Immutable snapshot of everything the Metronome screen needs to render.
 *
 * @property isPlaying Whether the metronome is currently producing audio.
 * @property config The active tempo / meter / subdivision settings.
 * @property currentBeat Beat index within the current bar, or [INITIAL_BEAT] if no beat has fired.
 * @property tempoDescriptor Human-readable tempo label derived from [config.bpm].
 * @property isInitialLoadComplete False until the first DataStore emission is received so the UI
 *   can suppress content until persisted values are known.
 */
data class MetronomeUiState(
    val isPlaying: Boolean = false,
    val config: MetronomeConfig = MetronomeConfig.DEFAULT,
    val currentBeat: Int = INITIAL_BEAT,
    val tempoDescriptor: TempoDescriptor = tempoDescriptorFor(MetronomeConfig.DEFAULT_BPM),
    val isInitialLoadComplete: Boolean = false,
) {
    companion object {
        /** Beat value shown before any beat has fired. */
        const val INITIAL_BEAT: Int = 0
    }
}
