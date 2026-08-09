package de.ritzelprimpf.toniqo.tuner.data.audio

/**
 * Synthesis parameters for [ToneSynthesizer].
 *
 * **v1 starting values**, expected to be adjusted after a listening pass. Tuning any value here
 * does not change the contract or the test structure — only the named constant's value changes.
 */
internal object ToneParameters {

    /**
     * Linear fade-in/fade-out duration in milliseconds, applied at both ends of the tone.
     * Avoids the audible click/pop a raw, un-enveloped sine buffer produces at its edges.
     */
    const val FADE_MS = 15L

    /**
     * Leading silence, in milliseconds, prepended to every generated buffer before the tone
     * itself — see [ToneSynthesizer]'s "Leading silence" note. Every tap builds a brand-new
     * `AudioTrack` (unlike the metronome's one long-lived track), so this cold-start absorption
     * has to happen on *every* tap, not just once per session. Same value and rationale as the
     * metronome's own `AudioTrackMetronomePlayer.WARMUP_SILENCE_MS`.
     */
    const val WARMUP_MS = 150L

    /**
     * Peak amplitude as a fraction of full-scale.
     *
     * Deliberately close to full scale. A sustained tone at the same peak amplitude as a short
     * percussive click (see [de.ritzelprimpf.toniqo.metronome.data.audio.ClickParameters]'s
     * 0.50–0.70 range) reads far quieter to the ear — a click's loudness comes from its sharp
     * transient, not sustained energy, so matching a click's peak level left this tone barely
     * audible in practice. 0.5 was tried first and confirmed too quiet on-device.
     */
    const val AMPLITUDE = 0.9

    /**
     * `tanh` soft-clip drive applied to the raw sine before scaling — see [ToneSynthesizer]'s
     * "Why not a pure sine" note. `AMPLITUDE` was already near the digital ceiling and still read
     * as quiet on-device (a pure sine's RMS is inherently only ~70.7% of its peak); this raises
     * perceived loudness by shaping the wave toward a higher RMS at the *same* peak, rather than
     * by raising the peak further (there's no headroom left to do that safely).
     *
     * `1.0` ≈ no shaping (still close to a pure sine). Higher values push the waveform's shoulders
     * harder toward full scale — louder, but with more added harmonic content (moving away from a
     * "clean" sine and toward a squarer wave). `2.5` was picked as a first, unmeasured attempt at
     * a noticeable loudness gain that doesn't yet sound like a buzzy square wave; confirm on
     * device and adjust.
     */
    const val DRIVE = 2.5

    /** Maximum magnitude of a 16-bit PCM sample (2^15 − 1). Used to scale [AMPLITUDE] to PCM range. */
    const val PCM16_FULL_SCALE = 32_767
}
