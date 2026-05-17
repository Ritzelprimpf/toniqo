package de.ritzelprimpf.toniqo.tuner.domain.repository

/**
 * Estimates the fundamental frequency of a captured audio buffer.
 *
 * Defined as a domain interface so the rest of the tuner stack compiles independently of any
 * specific algorithm. The concrete pitch-detection algorithm is locked in `DECISIONS.md` during
 * Phase 5.1 and implemented in `data/`.
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
