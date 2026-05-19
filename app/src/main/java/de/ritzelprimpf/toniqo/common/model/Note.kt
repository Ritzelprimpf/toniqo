package de.ritzelprimpf.toniqo.common.model

import kotlin.math.pow

/**
 * A specific note in scientific pitch notation — a pitch class combined with an octave number.
 *
 * Scientific pitch notation follows the convention that A4 = 440 Hz by default, with octave
 * numbers incrementing at each C (so B3 is one semitone below C4).
 *
 * @property name The pitch class.
 * @property octave The octave number in scientific pitch notation.
 */
data class Note(
    val name: NoteName,
    val octave: Int,
) {

    /**
     * The fundamental frequency of this note in Hertz under equal temperament.
     *
     * Uses `f = referencePitchHz × 2^(n / 12)` where `n` is the signed semitone distance
     * from A4 to this note.
     *
     * @param referencePitchHz The frequency of A4. Defaults to 440 Hz.
     */
    fun frequencyHz(referencePitchHz: Double = DEFAULT_REFERENCE_PITCH_HZ): Double {
        val semitonesFromA4 = (octave - A4_OCTAVE) * SEMITONES_PER_OCTAVE +
                (name.semitonesFromC - A4_SEMITONES_FROM_C)
        return referencePitchHz * 2.0.pow(semitonesFromA4.toDouble() / SEMITONES_PER_OCTAVE)
    }

    /**
     * Returns the display string for this note in scientific pitch notation, e.g. `"E2"`,
     * `"C#4"`, or (with flats) `"Db4"`. The octave number is appended directly to the name.
     *
     * @param useFlats When `true`, accidentals are spelled with flats (`Db` rather than `C#`).
     */
    fun displayName(useFlats: Boolean = false): String {
        val nameStr = if (useFlats) name.flatName else name.sharpName
        return "$nameStr$octave"
    }

    companion object {
        /** International-standard reference pitch for A4, in Hertz. */
        const val DEFAULT_REFERENCE_PITCH_HZ: Double = 440.0

        internal const val SEMITONES_PER_OCTAVE = 12
        internal const val A4_OCTAVE = 4
        internal const val A4_SEMITONES_FROM_C = 9  // A is 9 semitones above C

        /**
         * Parses a note string in scientific pitch notation (e.g. `"E2"`, `"C#4"`, `"Db4"`) into
         * a [Note]. Both sharp and flat spellings are accepted (case-insensitive).
         *
         * @return The parsed [Note], or `null` for unrecognised input.
         */
        fun parse(input: String): Note? {
            val trimmed = input.trim()
            val match = NOTE_REGEX.matchEntire(trimmed) ?: return null
            val noteName = NoteName.parse(match.groupValues[1]) ?: return null
            val octave = match.groupValues[2].toIntOrNull() ?: return null
            return Note(noteName, octave)
        }

        // Matches optional-accidental note name followed by optional-negative integer octave.
        private val NOTE_REGEX = Regex("""^([A-Ga-g][#b]?)(-?\d+)$""")
    }
}
