package de.ritzelprimpf.toniqo.metronome.data.audio

import de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Synthesizes PCM click buffers for the metronome.
 *
 * Each call to [generate] allocates exactly one [ShortArray] of length
 * `(CLICK_DURATION_MS * SAMPLE_RATE_HZ) / 1000` (1440 samples at 48 kHz). Buffers are intended
 * to be generated once at player initialization, not per-beat, so the per-beat hot path remains
 * allocation-free.
 *
 * The synthesis formula is a sine wave with a linear attack followed by an exponential decay.
 * All numeric parameters live in [ClickParameters]; all format constants live in
 * [MetronomeAudioFormat]. There are no magic numbers in the implementation.
 *
 * No Android runtime is required — this class is fully testable on the JVM.
 */
class ClickSynthesizer @javax.inject.Inject constructor() {

    /**
     * Generates a 16-bit PCM buffer for the given [kind].
     *
     * Formula for sample at index `i`:
     * ```
     * envelope = i / attackSamples                                   (i < attackSamples, linear)
     *          = exp(-CLICK_DECAY_RATE * (i - attackSamples) / SAMPLE_RATE_HZ)  (i >= attackSamples)
     * sample[i] = (peak * envelope * sin(2π * freqHz * i / SAMPLE_RATE_HZ) * PCM16_FULL_SCALE).toShort()
     * ```
     *
     * @param kind Determines the carrier frequency and peak amplitude (see [ClickParameters]).
     * @return Mono 16-bit PCM buffer at [MetronomeAudioFormat.SAMPLE_RATE_HZ].
     */
    fun generate(kind: ClickKind): ShortArray {
        val totalSamples = ClickParameters.CLICK_DURATION_MS * MetronomeAudioFormat.SAMPLE_RATE_HZ / 1000
        val attackSamples = ClickParameters.CLICK_ATTACK_MS * MetronomeAudioFormat.SAMPLE_RATE_HZ / 1000
        val freqHz = frequencyFor(kind)
        val peak = amplitudeFor(kind)
        val sampleRate = MetronomeAudioFormat.SAMPLE_RATE_HZ.toDouble()

        val out = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val envelope: Double = if (i < attackSamples) {
                i.toDouble() / attackSamples
            } else {
                exp(-ClickParameters.CLICK_DECAY_RATE * (i - attackSamples) / sampleRate)
            }
            val wave = sin(2.0 * PI * freqHz * i / sampleRate)
            out[i] = (peak * envelope * wave * ClickParameters.PCM16_FULL_SCALE).toInt().toShort()
        }
        return out
    }

    private fun frequencyFor(kind: ClickKind): Double = when (kind) {
        ClickKind.ACCENTED -> ClickParameters.FREQUENCY_HZ_ACCENTED
        ClickKind.STANDARD -> ClickParameters.FREQUENCY_HZ_STANDARD
        ClickKind.SUBDIVISION -> ClickParameters.FREQUENCY_HZ_SUBDIVISION
    }

    private fun amplitudeFor(kind: ClickKind): Double = when (kind) {
        ClickKind.ACCENTED -> ClickParameters.AMPLITUDE_ACCENTED
        ClickKind.STANDARD -> ClickParameters.AMPLITUDE_STANDARD
        ClickKind.SUBDIVISION -> ClickParameters.AMPLITUDE_SUBDIVISION
    }
}
