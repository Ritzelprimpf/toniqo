package de.ritzelprimpf.toniqo.tuner.domain.repository

/**
 * Plays a short reference tone at an arbitrary frequency.
 *
 * Backs the string selector's "tap a string pill to hear its target pitch" behaviour
 * ([de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerViewModel.onStringSelected]).
 */
interface TonePlayer {

    /**
     * Plays a sine tone at [frequencyHz] for [durationMs], suspending until playback completes.
     *
     * Cancelling the calling coroutine stops playback immediately and releases the underlying
     * audio resources — callers that want to interrupt an in-flight tone (e.g. a new string was
     * tapped before the previous tone finished) do so by cancelling the job [play] was launched
     * in, not by calling [play] again concurrently.
     */
    suspend fun play(frequencyHz: Double, durationMs: Long)
}
