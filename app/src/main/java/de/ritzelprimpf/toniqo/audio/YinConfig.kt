package de.ritzelprimpf.toniqo.audio

/**
 * Tunable parameters for the YIN pitch-detection algorithm.
 *
 * The defaults are the production values chosen in Phase 5.2 and recorded in `DECISIONS.md`.
 *
 * Promoted from `common/util/YinConfig` to `audio/` in Phase 7.2.
 *
 * @property threshold YIN's cumulative-mean-normalized-difference threshold.
 *   Lower values are more strict (the algorithm reports a pitch only when its CMND dips clearly
 *   below this level); higher values are more permissive but risk reporting spurious pitches on
 *   noise or inharmonic signals. The original YIN paper (de Cheveigné & Kawahara, 2002)
 *   recommends **0.15** as a general-purpose starting point, which is the production default here.
 * @property absoluteMinFrequencyHz Detected frequencies below this value are rejected even if the
 *   algorithm would otherwise report a candidate. Provides a hard guard against subharmonic
 *   confusion on the very low end.
 *   Default **30.0 Hz** — below the lowest string in any supported 8-string drop tuning
 *   (Drop C 8-string bottom string: C1 ≈ 32.7 Hz).
 * @property absoluteMaxFrequencyHz Detected frequencies above this value are rejected. Guitar
 *   fundamentals do not exceed ≈ 1400 Hz even on the highest frets; harmonics above this range
 *   are not fundamentals.
 *   Default **2000.0 Hz** — a generous ceiling that still filters out clearly non-fundamental
 *   detections.
 */
data class YinConfig(
    val threshold: Double = DEFAULT_THRESHOLD,
    val absoluteMinFrequencyHz: Double = DEFAULT_MIN_FREQUENCY_HZ,
    val absoluteMaxFrequencyHz: Double = DEFAULT_MAX_FREQUENCY_HZ,
) {
    companion object {
        const val DEFAULT_THRESHOLD = 0.15
        const val DEFAULT_MIN_FREQUENCY_HZ = 30.0
        const val DEFAULT_MAX_FREQUENCY_HZ = 2000.0
    }
}
