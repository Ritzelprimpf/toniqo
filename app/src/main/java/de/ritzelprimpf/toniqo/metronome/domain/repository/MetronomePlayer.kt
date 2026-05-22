package de.ritzelprimpf.toniqo.metronome.domain.repository

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.PlayerEvent
import kotlinx.coroutines.flow.Flow

/**
 * Audio playback engine for the metronome.
 *
 * The metronome plays for as long as a collector is collecting the [run] flow. Cancelling the
 * collector stops playback and releases all resources (audio focus, `AudioTrack`). There is no
 * separate start / stop API — the collector's coroutine scope is the lifetime.
 *
 * This flow-based shape supersedes the Phase 2 imperative API (`start` / `stop` / `updateConfig` /
 * `currentBeat`). The change is recorded in `DECISIONS.md` (2026-05-22).
 *
 * Mirrors the `MicrophoneAudioSource.samples()` lifetime contract established in Phase 5.2.
 */
interface MetronomePlayer {

    /**
     * Starts playback with [initialConfig]. Subsequent config changes flow in via [configFlow].
     *
     * The returned [Flow] emits:
     * - [PlayerEvent.Started] — once, when audio playback begins.
     * - [PlayerEvent.BeatTick] — once per **main beat** (subdivision-only clicks are not surfaced).
     * - [PlayerEvent.Failed] — if playback cannot start or is interrupted; the flow then terminates.
     *
     * Cancelling the collector stops playback and releases all audio resources. The player handles
     * audio focus request / abandon internally.
     *
     * @param initialConfig The configuration to start playback with.
     * @param configFlow Live config updates. BPM-only changes re-anchor the scheduler at the next
     *   beat; time signature or subdivision changes restart the beat cycle from beat 1.
     */
    fun run(initialConfig: MetronomeConfig, configFlow: Flow<MetronomeConfig>): Flow<PlayerEvent>
}
