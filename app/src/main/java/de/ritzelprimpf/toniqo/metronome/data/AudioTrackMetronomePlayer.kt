package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.repository.MetronomePlayer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Stub implementation of [MetronomePlayer] that will be backed by `AudioTrack` in streaming mode.
 *
 * Phase 2 only wires the dependency graph; Phase 6+ (metronome implementation) builds the actual
 * audio path. Until then, [currentBeat] is an empty flow and the control methods throw
 * [NotImplementedError].
 */
@Singleton
class AudioTrackMetronomePlayer @Inject constructor() : MetronomePlayer {

    /**
     * Empty flow until the audio path is implemented in a later phase. Returning [emptyFlow]
     * (instead of `TODO()`) lets collectors register a subscription without throwing — a sensible
     * default that matches the "no beats" semantics of an unimplemented player.
     */
    override val currentBeat: Flow<Int> = emptyFlow()

    /**
     * Stub. Phase 6+ implements the audio start logic.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    override fun start(config: MetronomeConfig): Unit = TODO("Not yet implemented")

    /**
     * Stub. Phase 6+ implements the audio stop logic.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    override fun stop(): Unit = TODO("Not yet implemented")

    /**
     * Stub. Phase 6+ implements live config updates.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    override fun updateConfig(config: MetronomeConfig): Unit = TODO("Not yet implemented")
}
