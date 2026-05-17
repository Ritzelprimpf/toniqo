package de.ritzelprimpf.toniqo.metronome.domain.usecase

import de.ritzelprimpf.toniqo.metronome.domain.repository.MetronomePlayer
import javax.inject.Inject

/**
 * Stops metronome playback.
 *
 * Thin wrapper around [MetronomePlayer.stop]; exists so the presentation layer depends on a use
 * case rather than the player directly.
 *
 * @property player The audio player. Injected by Hilt.
 */
class StopMetronomeUseCase @Inject constructor(
    private val player: MetronomePlayer,
) {

    /** Stops playback. No-op if the metronome is not playing. */
    operator fun invoke() {
        player.stop()
    }
}
