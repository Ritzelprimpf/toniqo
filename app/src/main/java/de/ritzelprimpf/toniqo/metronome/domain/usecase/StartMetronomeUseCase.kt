package de.ritzelprimpf.toniqo.metronome.domain.usecase

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.repository.MetronomePlayer
import javax.inject.Inject

/**
 * Starts metronome playback with the supplied configuration.
 *
 * Thin wrapper around [MetronomePlayer.start]; exists so the presentation layer depends on a use
 * case rather than the player directly.
 *
 * @property player The audio player. Injected by Hilt.
 */
class StartMetronomeUseCase @Inject constructor(
    private val player: MetronomePlayer,
) {

    /**
     * Starts playback. No-op if the metronome is already playing.
     *
     * @param config The settings to play with.
     */
    operator fun invoke(config: MetronomeConfig) {
        player.start(config)
    }
}
