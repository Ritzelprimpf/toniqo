package de.ritzelprimpf.toniqo.chordfinder.domain.model

/**
 * A barre: the first finger laid flat across a group of strings at a single fret.
 *
 * @property fret The fret at which the barre is applied (1-indexed).
 * @property fromString The lowest string index covered by the barre (0-indexed, inclusive).
 * @property toString The highest string index covered by the barre (0-indexed, inclusive).
 */
data class Barre(
    val fret: Int,
    val fromString: Int,
    val toString: Int,
)
