package de.ritzelprimpf.toniqo.tuner.data.audio

import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Synthesizes a PCM reference tone for [de.ritzelprimpf.toniqo.tuner.data.AudioTrackTonePlayer].
 *
 * Unlike the metronome's [de.ritzelprimpf.toniqo.metronome.data.audio.ClickSynthesizer] (a fixed
 * set of pre-defined click sounds), frequency and duration are runtime parameters here — one
 * buffer per string-pill tap, generated on demand rather than cached, since the target frequency
 * changes with every tap.
 *
 * Linear fade-in and fade-out (see [ToneParameters.FADE_MS]) avoid the audible click/pop a raw
 * sine buffer would produce at its start/end edges. No Android runtime required — fully testable
 * on the JVM.
 *
 * ### Why not a pure sine
 *
 * A pure sine wave is the *quietest* waveform possible for a given peak amplitude — its RMS
 * (perceived loudness) is only ~70.7% of its peak, and [ToneParameters.AMPLITUDE] is already
 * pinned close to the 16-bit ceiling, so there's no headroom left to simply turn the peak up
 * further. [ToneParameters.DRIVE] applies a `tanh` soft-clip to the raw sine before scaling: it
 * pushes the waveform's shoulders closer to full scale (raising RMS energy, i.e. perceived
 * loudness) while still hitting exactly the same peak, at the cost of adding some (mild, at this
 * drive level) harmonic content. `tanh` is odd and monotonic, so it doesn't touch zero-crossings,
 * DC offset, or where the envelope reads zero — it only reshapes the wave *between* those points.
 *
 * ### Leading silence
 *
 * Every tap builds and plays a brand-new `AudioTrack` (see [ToneParameters.WARMUP_MS]'s kdoc), and
 * a freshly-started `AudioTrack` has a real, audible cold-start artifact — the mixer/HAL output
 * path spinning up produces a "crack" right as playback begins, landing squarely on the tone's own
 * attack if nothing absorbs it first. [ToneParameters.WARMUP_MS] of true silence (`0`-valued
 * samples — [ShortArray]'s default) is prepended to the buffer so that artifact happens during
 * silence instead of during the tone. [AudioTrackTonePlayer] accounts for the extra leading time
 * when deciding how long to hold the `AudioTrack` open.
 */
class ToneSynthesizer @Inject constructor() {

    /**
     * Generates a mono 16-bit PCM buffer: [ToneParameters.WARMUP_MS] of leading silence, followed
     * by a [frequencyHz] tone lasting [durationMs], all at [ToneAudioFormat.SAMPLE_RATE_HZ].
     *
     * Formula for the tone portion at index `i` relative to its own start (i.e. after the
     * `warmupSamples` leading silence; `fadeSamples` = [ToneParameters.FADE_MS] in samples):
     * ```
     * envelope = i / fadeSamples                                    (i < fadeSamples)
     *          = (toneSamples - i) / fadeSamples                    (i >= toneSamples - fadeSamples)
     *          = 1.0                                                (otherwise)
     * rawWave    = sin(2π * freqHz * i / SAMPLE_RATE_HZ)
     * shapedWave = tanh(DRIVE * rawWave) / tanh(DRIVE)               (peak-preserving soft-clip)
     * sample[warmupSamples + i] = (peak * envelope * shapedWave * PCM16_FULL_SCALE).toShort()
     * ```
     */
    fun generate(frequencyHz: Double, durationMs: Long): ShortArray {
        val sampleRate = ToneAudioFormat.SAMPLE_RATE_HZ
        val warmupSamples = (ToneParameters.WARMUP_MS * sampleRate / MILLIS_PER_SECOND).toInt()
        val toneSamples = (durationMs * sampleRate / MILLIS_PER_SECOND).toInt()
        val fadeSamples = (ToneParameters.FADE_MS * sampleRate / MILLIS_PER_SECOND).toInt()
        val driveNormalizer = tanh(ToneParameters.DRIVE)

        // The leading warmupSamples entries stay at ShortArray's default value of 0 (silence).
        val out = ShortArray(warmupSamples + toneSamples)
        for (i in 0 until toneSamples) {
            val envelope: Double = when {
                i < fadeSamples -> i.toDouble() / fadeSamples
                i >= toneSamples - fadeSamples -> (toneSamples - i).toDouble() / fadeSamples
                else -> 1.0
            }
            val rawWave = sin(2.0 * PI * frequencyHz * i / sampleRate)
            val shapedWave = tanh(ToneParameters.DRIVE * rawWave) / driveNormalizer
            out[warmupSamples + i] =
                (ToneParameters.AMPLITUDE * envelope * shapedWave * ToneParameters.PCM16_FULL_SCALE)
                    .toInt().toShort()
        }
        return out
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
