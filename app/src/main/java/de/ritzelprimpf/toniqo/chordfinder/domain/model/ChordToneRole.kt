package de.ritzelprimpf.toniqo.chordfinder.domain.model

/**
 * The role of the lowest sounding string's note within the chord.
 *
 * [SEVENTH] only ever applies when the chord's [ChordKey.seventhQuality] is non-null — a
 * seventh-chord voicing may have the seventh itself in the bass, same as [THIRD] / [FIFTH] for
 * an inverted triad.
 */
enum class ChordToneRole { ROOT, THIRD, FIFTH, SEVENTH, OTHER }
