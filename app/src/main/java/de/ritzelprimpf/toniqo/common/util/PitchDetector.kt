package de.ritzelprimpf.toniqo.common.util

/**
 * Estimates the fundamental frequency of a captured audio buffer.
 *
 * Defined as a domain interface so callers depend on an abstraction rather than any specific
 * algorithm. The concrete implementation is [YinPitchDetector]; the choice is locked in
 * `DECISIONS.md` during Phase 5.2.
 *
 * Moved from `tuner/domain/repository/` to `common/util/` in Phase 5.2, because the
 * implementation lives in `common/util/` (pure Kotlin, no Android dependencies) and the
 * interface belongs alongside it. Decision recorded in `DECISIONS.md`.
 */
interface PitchDetector {

    /**
     * Estimates the fundamental frequency of the audio in [audioBuffer].
     *
     * @param audioBuffer The captured audio samples, as 32-bit floats normalised to the
     *   approximate range `[-1.0, +1.0]`. Mono only.
     * @param sampleRateHz The sample rate of [audioBuffer] in Hertz (e.g. `44100`).
     * @return The estimated fundamental frequency in Hertz, or `null` if no reliable pitch could
     *   be extracted (silence, noise, or an inharmonic signal).
     */
    fun detectPitch(audioBuffer: FloatArray, sampleRateHz: Int): Double?
}
