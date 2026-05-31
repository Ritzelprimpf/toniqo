package de.ritzelprimpf.toniqo.metronome.domain.model

/**
 * Events emitted by [de.ritzelprimpf.toniqo.metronome.domain.repository.MetronomePlayer.run].
 *
 * Only main beats surface to the flow — subdivision-only clicks are heard but not observable.
 * This keeps the UI emission rate bounded by the time-signature numerator, independent of the
 * active subdivision.
 */
sealed interface PlayerEvent {

    /** Emitted exactly once when audio playback successfully begins. */
    data object Started : PlayerEvent

    /**
     * Emitted on every **main beat** click.
     *
     * Subdivision-only clicks do NOT emit this event. Beat 1 of each bar has
     * [beatIndexInBar] == 0.
     *
     * @param beatIndexInBar Zero-based index of this main beat within the current bar.
     *   Range: `[0, timeSignatureNumerator)`.
     */
    data class BeatTick(val beatIndexInBar: Int) : PlayerEvent

    /**
     * Emitted immediately before the flow terminates if playback could not start or was
     * interrupted by a non-recoverable error.
     *
     * After this event the flow closes; no further events are emitted.
     */
    data class Failed(val reason: PlayerFailureReason) : PlayerEvent
}

/** Reason codes for [PlayerEvent.Failed]. */
enum class PlayerFailureReason {
    /** `AudioTrack` failed to reach the `STATE_INITIALIZED` state during construction. */
    AUDIO_TRACK_INIT_FAILED,

    /** The audio focus request was not granted (e.g., another app holds exclusive focus). */
    AUDIO_FOCUS_DENIED,
}
