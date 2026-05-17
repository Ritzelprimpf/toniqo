package de.ritzelprimpf.toniqo.chordfinder.domain.model

import de.ritzelprimpf.toniqo.common.model.Chord

/**
 * A chord paired with its position in the parent scale.
 *
 * The Roman numeral conveys both the scale degree and the chord quality in standard notation —
 * uppercase for major (`I`, `IV`, `V`), lowercase for minor (`ii`, `iii`, `vi`), uppercase with
 * `°` for diminished (`vii°` in major), and so on.
 *
 * @property degree The one-based scale degree of [chord] within the parent scale.
 * @property romanNumeral The Roman-numeral label (e.g. `I`, `ii`, `vii°`, `IVmaj7`).
 * @property chord The chord itself, fully described by its root, quality, and notes.
 */
data class DegreeChord(
    val degree: Int,
    val romanNumeral: String,
    val chord: Chord,
)
