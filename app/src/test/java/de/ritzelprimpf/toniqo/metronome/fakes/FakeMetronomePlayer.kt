package de.ritzelprimpf.toniqo.metronome.fakes

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.PlayerEvent
import de.ritzelprimpf.toniqo.metronome.domain.repository.MetronomePlayer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Test double for [MetronomePlayer].
 *
 * Emits [PlayerEvent.Started] immediately on collection, then replays whatever is fed via
 * [sendEvent]. Calling [complete] closes the underlying channel, ending the flow normally.
 *
 * Uses [channelFlow] (not [kotlinx.coroutines.flow.flow]) because the config-update launcher
 * inside [run] requires a [kotlinx.coroutines.CoroutineScope].
 */
internal class FakeMetronomePlayer : MetronomePlayer {

    private val _events = Channel<PlayerEvent>(Channel.UNLIMITED)

    /** Config values pushed to the player after playback started (first value is skipped via drop(1)). */
    val receivedConfigUpdates = mutableListOf<MetronomeConfig>()

    /** The [MetronomeConfig] passed to the most recent [run] call. */
    var receivedInitialConfig: MetronomeConfig? = null

    override fun run(
        initialConfig: MetronomeConfig,
        configFlow: Flow<MetronomeConfig>,
    ): Flow<PlayerEvent> = channelFlow {
        receivedInitialConfig = initialConfig
        send(PlayerEvent.Started)
        val configJob = launch {
            configFlow.drop(1).collect { receivedConfigUpdates.add(it) }
        }
        for (event in _events) {
            send(event)
            if (event is PlayerEvent.Failed) break
        }
        configJob.cancel()
    }

    /** Enqueues [event] to be emitted by the flow on the next collection cycle. */
    fun sendEvent(event: PlayerEvent) {
        _events.trySend(event)
    }

    /** Closes the event channel, causing the flow to complete normally. */
    fun complete() {
        _events.close()
    }
}
