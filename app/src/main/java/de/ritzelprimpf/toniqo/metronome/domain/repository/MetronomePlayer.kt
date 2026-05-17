package de.ritzelprimpf.toniqo.metronome.domain.repository

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import kotlinx.coroutines.flow.Flow

/**
 * Drives audible click playback for the metronome.
 *
 * Implementations encapsulate the low-latency audio path (`AudioTrack` in streaming mode per
 * `IMPLEMENTATION_NOTES.md`). The interface stays free of platform types so the rest of the stack
 * remains unit-testable and the implementation choice can change without rippling outward.
 */
interface MetronomePlayer {

    /**
     * A stream of one-based beat numbers emitted as each beat is *audibly* triggered.
     *
     * The first beat of every measure is `1`; the cycle repeats with the measure. Subdivision
     * clicks are not surfaced here — only main beats.
     */
    val currentBeat: Flow<Int>

    /**
     * Starts playback at the tempo and meter described by [config]. No-op if playback is already
     * running.
     *
     * @param config The settings to play with.
     */
    fun start(config: MetronomeConfig)

    /** Stops playback. No-op if playback is not running. */
    fun stop()

    /**
     * Replaces the current settings with [config]. BPM changes take effect on the next beat;
     * meter or subdivision changes restart the beat cycle from beat 1 on the next downbeat
     * (see `APP_SPECIFICATION.md` Metronome > Controls).
     *
     * @param config The new settings.
     */
    fun updateConfig(config: MetronomeConfig)
}
