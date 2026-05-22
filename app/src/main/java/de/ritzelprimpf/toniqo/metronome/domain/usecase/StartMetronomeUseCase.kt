package de.ritzelprimpf.toniqo.metronome.domain.usecase

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.PlayerEvent
import de.ritzelprimpf.toniqo.metronome.domain.repository.MetronomePlayer
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Starts metronome playback and returns the player's event flow.
 *
 * Thin delegation to [MetronomePlayer.run]. Collecting the returned flow starts audio playback;
 * cancelling the collector stops it. The ViewModel (Phase 6.3) owns the collector's lifetime.
 *
 * @property player The audio player. Injected by Hilt.
 */
class StartMetronomeUseCase @Inject constructor(
    private val player: MetronomePlayer,
) {

    /**
     * Returns the player event flow for [initialConfig] with live updates from [configFlow].
     *
     * The flow begins playback when collected and stops when the collector is cancelled.
     *
     * @param initialConfig The configuration to start playback with.
     * @param configFlow Live updates to the configuration during playback.
     */
    operator fun invoke(
        initialConfig: MetronomeConfig,
        configFlow: Flow<MetronomeConfig>,
    ): Flow<PlayerEvent> = player.run(initialConfig, configFlow)
}
