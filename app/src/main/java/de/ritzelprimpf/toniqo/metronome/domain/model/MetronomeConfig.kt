package de.ritzelprimpf.toniqo.metronome.domain.model

/**
 * The tempo, meter, and subdivision settings that fully describe how the metronome should play.
 *
 * Defaults match the values in `APP_SPECIFICATION.md` (Metronome > Parameters): 120 BPM in 4/4
 * with no subdivision. The valid BPM range is [BPM_MIN]..[BPM_MAX]; the time-signature numerator
 * and denominator are not validated at the type level (any positive integer is accepted) — the
 * UI restricts the user-facing choices.
 *
 * @property bpm Beats per minute.
 * @property timeSignatureNumerator The top number of the time signature (beats per measure).
 * @property timeSignatureDenominator The bottom number of the time signature (the note value that
 *   gets the beat — 4 for quarter notes, 8 for eighth notes, etc.).
 * @property subdivision The internal subdivision of each beat.
 */
data class MetronomeConfig(
    val bpm: Int = DEFAULT_BPM,
    val timeSignatureNumerator: Int = DEFAULT_TIME_SIGNATURE_NUMERATOR,
    val timeSignatureDenominator: Int = DEFAULT_TIME_SIGNATURE_DENOMINATOR,
    val subdivision: Subdivision = Subdivision.NONE,
) {
    companion object {
        /** Minimum beats per minute the metronome will accept. */
        const val BPM_MIN: Int = 1

        /** Maximum beats per minute the metronome will accept. */
        const val BPM_MAX: Int = 300

        /** Default BPM when the user has not yet chosen a tempo. */
        const val DEFAULT_BPM: Int = 120

        /** Default time-signature numerator (4 — as in 4/4). */
        const val DEFAULT_TIME_SIGNATURE_NUMERATOR: Int = 4

        /** Default time-signature denominator (4 — as in 4/4). */
        const val DEFAULT_TIME_SIGNATURE_DENOMINATOR: Int = 4
    }
}
