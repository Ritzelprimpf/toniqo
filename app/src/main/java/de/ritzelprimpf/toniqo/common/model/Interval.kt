package de.ritzelprimpf.toniqo.common.model

/**
 * A pitch interval expressed as a number of equal-tempered semitones.
 *
 * Used both as a building block for [Scale] (the interval pattern from the root) and as a
 * way to describe gaps between individual notes. Negative values represent descending
 * intervals; positive values represent ascending intervals.
 *
 * @property semitones The signed semitone distance.
 */
data class Interval(val semitones: Int) {

    companion object {
        /** Perfect unison: 0 semitones. */
        val UNISON: Interval = Interval(SEMITONES_UNISON)

        /** Minor second: 1 semitone. */
        val MINOR_SECOND: Interval = Interval(SEMITONES_MINOR_SECOND)

        /** Major second: 2 semitones. */
        val MAJOR_SECOND: Interval = Interval(SEMITONES_MAJOR_SECOND)

        /** Minor third: 3 semitones. */
        val MINOR_THIRD: Interval = Interval(SEMITONES_MINOR_THIRD)

        /** Major third: 4 semitones. */
        val MAJOR_THIRD: Interval = Interval(SEMITONES_MAJOR_THIRD)

        /** Perfect fourth: 5 semitones. */
        val PERFECT_FOURTH: Interval = Interval(SEMITONES_PERFECT_FOURTH)

        /** Tritone (augmented fourth / diminished fifth): 6 semitones. */
        val TRITONE: Interval = Interval(SEMITONES_TRITONE)

        /** Perfect fifth: 7 semitones. */
        val PERFECT_FIFTH: Interval = Interval(SEMITONES_PERFECT_FIFTH)

        /** Minor sixth: 8 semitones. */
        val MINOR_SIXTH: Interval = Interval(SEMITONES_MINOR_SIXTH)

        /** Major sixth: 9 semitones. */
        val MAJOR_SIXTH: Interval = Interval(SEMITONES_MAJOR_SIXTH)

        /** Minor seventh: 10 semitones. */
        val MINOR_SEVENTH: Interval = Interval(SEMITONES_MINOR_SEVENTH)

        /** Major seventh: 11 semitones. */
        val MAJOR_SEVENTH: Interval = Interval(SEMITONES_MAJOR_SEVENTH)

        /** Perfect octave: 12 semitones. */
        val OCTAVE: Interval = Interval(SEMITONES_OCTAVE)

        private const val SEMITONES_UNISON: Int = 0
        private const val SEMITONES_MINOR_SECOND: Int = 1
        private const val SEMITONES_MAJOR_SECOND: Int = 2
        private const val SEMITONES_MINOR_THIRD: Int = 3
        private const val SEMITONES_MAJOR_THIRD: Int = 4
        private const val SEMITONES_PERFECT_FOURTH: Int = 5
        private const val SEMITONES_TRITONE: Int = 6
        private const val SEMITONES_PERFECT_FIFTH: Int = 7
        private const val SEMITONES_MINOR_SIXTH: Int = 8
        private const val SEMITONES_MAJOR_SIXTH: Int = 9
        private const val SEMITONES_MINOR_SEVENTH: Int = 10
        private const val SEMITONES_MAJOR_SEVENTH: Int = 11
        private const val SEMITONES_OCTAVE: Int = 12
    }
}
