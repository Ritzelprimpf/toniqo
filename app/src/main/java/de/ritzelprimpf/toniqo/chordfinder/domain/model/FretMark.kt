package de.ritzelprimpf.toniqo.chordfinder.domain.model

/**
 * The state of one string in a voicing diagram: open, muted, or fretted at a specific position.
 *
 * Sealed so exhaustive `when` expressions are enforced at every callsite.
 */
sealed interface FretMark {

    /** The string is played open (not fretted). Represented as `"o"` or `0` in JSON. */
    data object Open : FretMark

    /** The string is not played (muted). Represented as `"x"` in JSON. */
    data object Muted : FretMark

    /**
     * The string is pressed at [fret].
     *
     * @property fret Fret number ≥ 1.
     */
    data class Fretted(val fret: Int) : FretMark
}
