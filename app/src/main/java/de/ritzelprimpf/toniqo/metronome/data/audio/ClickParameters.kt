package de.ritzelprimpf.toniqo.metronome.data.audio

/**
 * Synthesis parameters for [ClickSynthesizer].
 *
 * **These are v1 starting values**, expected to be adjusted after the first listening pass during
 * Phase 6.2's manual smoke test. Tuning any value here does not change the contract or the test
 * structure — only the named constant's value changes, and the tests re-pass automatically.
 *
 * See `docs/phases/phase-6-metronome/Phase6-Metronome-Decisions.md` Item 21 for the full
 * parameter rationale (frequency spacing, amplitude hierarchy, envelope shape).
 */
internal object ClickParameters {

    // --- Envelope and timing ---

    /** Total click duration in milliseconds. At 48 kHz this is 1440 samples. */
    const val CLICK_DURATION_MS = 30

    /** Linear attack duration in milliseconds. Avoids click-pop without softening the onset. */
    const val CLICK_ATTACK_MS = 1

    /**
     * Exponential decay rate constant. The envelope at sample [i] after the attack is:
     * `exp(-CLICK_DECAY_RATE * (i - attackSamples) / SAMPLE_RATE_HZ)`.
     * At 160.0 the envelope reaches ~1% of peak by the end of the 30 ms click.
     */
    const val CLICK_DECAY_RATE = 160.0

    // --- Carrier frequencies (Hz) ---

    /** Carrier frequency for the accented downbeat click. */
    const val FREQUENCY_HZ_ACCENTED = 1500.0

    /** Carrier frequency for non-downbeat main-beat clicks. */
    const val FREQUENCY_HZ_STANDARD = 1000.0

    /** Carrier frequency for between-beat subdivision clicks. */
    const val FREQUENCY_HZ_SUBDIVISION = 800.0

    // --- Peak amplitudes (fraction of full-scale) ---

    /** Peak amplitude for [de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind.ACCENTED]. */
    const val AMPLITUDE_ACCENTED = 0.70

    /** Peak amplitude for [de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind.STANDARD]. */
    const val AMPLITUDE_STANDARD = 0.50

    /** Peak amplitude for [de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind.SUBDIVISION]. */
    const val AMPLITUDE_SUBDIVISION = 0.25

    // --- PCM scaling ---

    /** Maximum magnitude of a 16-bit PCM sample (2^15 − 1). Used to scale normalised amplitudes. */
    const val PCM16_FULL_SCALE = 32_767
}
