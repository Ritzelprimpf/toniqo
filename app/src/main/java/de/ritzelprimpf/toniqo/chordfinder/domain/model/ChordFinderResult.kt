package de.ritzelprimpf.toniqo.chordfinder.domain.model

/**
 * The output of the Chord Finder: the diatonic chords of the queried key, in scale-degree order.
 *
 * @property chords The chords for each scale degree, ordered I, ii, iii, … When the input
 *   requested seventh chords, every chord here is a seventh chord; otherwise every chord is a
 *   triad. (Mixing is not supported — the toggle applies uniformly across the result.)
 */
data class ChordFinderResult(
    val chords: List<DegreeChord>,
)
