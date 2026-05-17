package de.ritzelprimpf.toniqo.metronome.presentation.viewmodel

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig

/**
 * Immutable snapshot of everything the Metronome screen needs to render.
 *
 * Defaults represent the screen's initial state: not playing, default [MetronomeConfig], no
 * beats received yet (beat index [INITIAL_BEAT]).
 *
 * @property isPlaying Whether the metronome is currently producing audio.
 * @property config The active tempo / meter / subdivision settings.
 * @property currentBeat The one-based beat number within the current measure, or [INITIAL_BEAT]
 *   if no beat has fired yet.
 */
data class MetronomeUiState(
    val isPlaying: Boolean = false,
    val config: MetronomeConfig = MetronomeConfig(),
    val currentBeat: Int = INITIAL_BEAT,
) {
    companion object {
        /** Beat value shown before any beat has fired. */
        const val INITIAL_BEAT: Int = 0
    }
}
