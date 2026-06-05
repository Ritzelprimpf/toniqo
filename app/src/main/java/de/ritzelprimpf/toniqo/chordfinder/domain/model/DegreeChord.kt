package de.ritzelprimpf.toniqo.chordfinder.domain.model

import de.ritzelprimpf.toniqo.common.model.ChordQuality

/**
 * A diatonic chord on one degree of a harmonised scale, ready for display.
 *
 * All text fields are pre-computed by [de.ritzelprimpf.toniqo.chordfinder.domain.usecase.FindChordsUseCase]
 * using [de.ritzelprimpf.toniqo.common.util.ScaleSpeller] for conventional spelling.
 *
 * @property degree One-based scale degree (1 = I, 7 = VII).
 * @property romanNumeral Roman numeral with quality symbols: uppercase for major/augmented,
 *   lowercase for minor/diminished; `°` appended for diminished, `+` for augmented
 *   (e.g. `"I"`, `"ii"`, `"vii°"`, `"III+"`).
 * @property triadQuality The quality of the underlying triad.
 * @property seventhQuality The seventh-chord quality when seventh chords are requested; `null`
 *   when the result is triads-only.
 * @property rootName Conventionally-spelled root note name (e.g. `"G♯"`, `"B♭"`).
 * @property noteNames Conventionally-spelled names of each chord tone: 3 entries for triads,
 *   4 entries when [seventhQuality] is present.
 * @property symbol Full chord symbol (e.g. `"Dm"`, `"Bdim"`, `"Cmaj7♯5"`).
 */
data class DegreeChord(
    val degree: Int,
    val romanNumeral: String,
    val triadQuality: ChordQuality,
    val seventhQuality: SeventhQuality?,
    val rootName: String,
    val noteNames: List<String>,
    val symbol: String,
)
