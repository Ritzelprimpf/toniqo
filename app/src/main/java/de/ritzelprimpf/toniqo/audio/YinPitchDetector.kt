package de.ritzelprimpf.toniqo.audio

import javax.inject.Inject

/**
 * Fundamental-frequency estimator based on the YIN algorithm.
 *
 * Reference: de Cheveigné, A. & Kawahara, H. (2002). YIN, a fundamental frequency estimator for
 * speech and music. *Journal of the Acoustical Society of America*, 111(4), 1917–1930.
 *
 * YIN is chosen over naive FFT-based approaches because guitar strings produce strong harmonics.
 * A naive spectral peak detector would frequently lock onto the second harmonic and report 2× the
 * true fundamental. YIN's cumulative-mean-normalized difference function suppresses this
 * behaviour by measuring periodicity in the time domain.
 *
 * ## Algorithm outline (four steps):
 *
 * 1. **Difference function** — for each lag τ, compute the sum of squared differences between the
 *    signal and a shifted copy of itself.
 * 2. **Cumulative mean normalized difference (CMND)** — normalize each difference value by the
 *    running mean of all previous differences, producing a measure that equals 1.0 for an
 *    aperiodic signal and dips toward 0.0 at the true period.
 * 3. **Absolute threshold** — find the smallest τ where the CMND crosses below
 *    [YinConfig.threshold] and is at or near a local minimum.
 * 4. **Parabolic interpolation** — refine the integer-lag estimate to sub-sample accuracy by
 *    fitting a parabola to the three CMND values around the chosen τ.
 *
 * This class has **no mutable state**: every piece of working memory is allocated per-call, so
 * multiple calls from different coroutine scopes are safe without synchronization.
 *
 * Promoted from `common/util/YinPitchDetector` to `audio/` in Phase 7.2.
 *
 * @property config Tunable parameters: YIN threshold, min/max frequency bounds.
 */
class YinPitchDetector @Inject constructor(
    private val config: YinConfig,
) : PitchDetector {

    /**
     * Estimates the fundamental frequency of the audio in [audioBuffer].
     *
     * Implements the four-step YIN algorithm (see class KDoc). Internal buffers are allocated
     * per-call; the detector instance holds no state between calls.
     *
     * @param audioBuffer Mono audio samples normalised to approximately `[-1.0, +1.0]`.
     * @param sampleRateHz Sample rate in Hertz.
     * @return The estimated fundamental frequency in Hertz, or `null` when:
     *   - The buffer is too short for the algorithm to operate (halfSize < 2).
     *   - No lag satisfies the CMND threshold (silence, noise, inharmonic signal).
     *   - The refined lag is ≤ 0 (degenerate parabola).
     *   - The resulting frequency falls outside [YinConfig.absoluteMinFrequencyHz] /
     *     [YinConfig.absoluteMaxFrequencyHz].
     */
    override fun detectPitch(audioBuffer: FloatArray, sampleRateHz: Int): Double? {
        val bufferSize = audioBuffer.size
        val halfSize = bufferSize / 2

        if (halfSize < 2) return null

        // Step 1: Difference function.
        // d(τ) = Σ_{j=0}^{halfSize-1} (x[j] − x[j+τ])²
        // Only the first halfSize samples are summed (j < halfSize), so the maximum index
        // accessed is j + τ ≤ (halfSize-1) + (halfSize-1) = bufferSize-2, always in bounds.
        val diff = DoubleArray(halfSize)
        // diff[0] remains 0 (no lag — zero difference by definition)
        for (tau in 1 until halfSize) {
            var sum = 0.0
            for (j in 0 until halfSize) {
                val delta = audioBuffer[j].toDouble() - audioBuffer[j + tau].toDouble()
                sum += delta * delta
            }
            diff[tau] = sum
        }

        // Step 2: Cumulative mean normalized difference (CMND).
        // cmnd[0] = 1 by definition.
        // cmnd[τ] = d(τ) × τ / Σ_{j=1}^{τ} d(j)   for τ > 0.
        // When runningSum is 0 (silent or constant signal), cmnd is set to 1.0 (no periodicity).
        val cmnd = DoubleArray(halfSize)
        cmnd[0] = 1.0
        var runningSum = 0.0
        for (tau in 1 until halfSize) {
            runningSum += diff[tau]
            cmnd[tau] = if (runningSum == 0.0) 1.0 else diff[tau] * tau / runningSum
        }

        // Step 3: Absolute threshold — find the smallest τ ≥ 2 that is a local minimum of cmnd
        // AND where cmnd[τ] < config.threshold.
        // "Local minimum in the dip" is approximated by advancing τ as long as cmnd keeps
        // decreasing after crossing the threshold.
        var tauEstimate = -1
        var tau = 2
        while (tau < halfSize) {
            if (cmnd[tau] < config.threshold) {
                // Entered a dip below threshold; advance to the deepest point in this dip.
                while (tau + 1 < halfSize && cmnd[tau + 1] < cmnd[tau]) {
                    tau++
                }
                tauEstimate = tau
                break
            }
            tau++
        }

        if (tauEstimate == -1) return null

        // Step 4: Parabolic interpolation.
        // Refine the integer estimate to sub-sample accuracy by fitting a parabola through
        // cmnd[τ-1], cmnd[τ], cmnd[τ+1]. The minimum of the parabola is at:
        //   τ_refined = τ + (s0 − s2) / (2 × (s0 − 2×s1 + s2))
        // where s0 = cmnd[τ-1], s1 = cmnd[τ], s2 = cmnd[τ+1].
        // Guard: τ must be at least 1 and at most halfSize-2 for valid neighbours.
        val refinedTau: Double = if (tauEstimate in 1 until halfSize - 1) {
            val s0 = cmnd[tauEstimate - 1]
            val s1 = cmnd[tauEstimate]
            val s2 = cmnd[tauEstimate + 1]
            val denominator = s0 - 2.0 * s1 + s2
            if (denominator != 0.0) {
                tauEstimate + (s0 - s2) / (2.0 * denominator)
            } else {
                tauEstimate.toDouble()
            }
        } else {
            tauEstimate.toDouble()
        }

        if (refinedTau <= 0.0) return null

        val detectedFrequencyHz = sampleRateHz / refinedTau

        if (detectedFrequencyHz < config.absoluteMinFrequencyHz ||
            detectedFrequencyHz > config.absoluteMaxFrequencyHz
        ) return null

        return detectedFrequencyHz
    }
}
