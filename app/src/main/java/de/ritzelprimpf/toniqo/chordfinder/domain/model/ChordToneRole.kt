package de.ritzelprimpf.toniqo.chordfinder.domain.model

/**
 * The role of the lowest sounding string's note within the chord — the inversion seam for FP-1.
 *
 * In v1 every shipped voicing is root-position, so all entries are [ROOT]. The field exists so
 * FP-1 (inversions/slash chords) can populate it without a model change.
 */
enum class ChordToneRole { ROOT, THIRD, FIFTH, OTHER }
