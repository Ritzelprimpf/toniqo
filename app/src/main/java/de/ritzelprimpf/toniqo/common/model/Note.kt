package de.ritzelprimpf.toniqo.common.model

/**
 * A specific note in scientific pitch notation — a pitch class combined with an octave number.
 *
 * Scientific pitch notation follows the convention that A4 = 440 Hz by default, with octave
 * numbers incrementing at each C (so B3 is one semitone below C4). The octave value is stored
 * as a plain integer; sub-contra and double-contra octaves use negative or zero values.
 *
 * @property name The pitch class (one of the twelve chromatic [NoteName]s).
 * @property octave The octave number in scientific pitch notation.
 */
data class Note(
    val name: NoteName,
    val octave: Int,
) {

    /**
     * The fundamental frequency of this note in Hertz under equal temperament.
     *
     * The frequency is derived from the reference pitch A4 = [referencePitchHz] using the
     * standard formula `f = referencePitchHz * 2^(n / 12)`, where `n` is the signed semitone
     * distance from A4 to this note.
     *
     * @param referencePitchHz The frequency of A4. Defaults to the international standard 440 Hz;
     *   pass `432.0` (or any other value) to retune the chromatic scale.
     * @return The fundamental frequency in Hertz.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    fun frequencyHz(referencePitchHz: Double = DEFAULT_REFERENCE_PITCH_HZ): Double =
        TODO("Not yet implemented")

    companion object {
        /** The international-standard reference pitch for A4, in Hertz. */
        const val DEFAULT_REFERENCE_PITCH_HZ: Double = 440.0
    }
}
