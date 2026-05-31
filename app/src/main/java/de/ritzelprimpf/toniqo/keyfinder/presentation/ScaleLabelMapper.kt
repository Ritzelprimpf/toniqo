package de.ritzelprimpf.toniqo.keyfinder.presentation

import de.ritzelprimpf.toniqo.common.util.ScaleSpeller
import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleMatch

/**
 * Pure mapping of a [ScaleMatch] to the display-string keys and spelled root used to build a
 * result card's primary label and subtitle. No Android [android.content.Context] required.
 *
 * This is **the authoritative place** where Key Finder display strings are assembled. The actual
 * resource lookup (formatting the `%s` placeholder with [spelledRoot]) is done in the Compose
 * layer via [android.content.res.Resources.getString].
 *
 * @property primaryLabelKey String-resource key, e.g. `"scale_type_label_major"`.
 * @property subtitleKey String-resource key, e.g. `"scale_type_subtitle_ionian"`.
 * @property spelledRoot Conventionally-spelled root for the `%s` placeholder, e.g. `"A"`, `"D♭"`.
 */
data class ScaleLabelData(
    val primaryLabelKey: String,
    val subtitleKey: String,
    val spelledRoot: String,
)

/** Returns the [ScaleLabelData] for [match], a pure computation with no Android dependencies. */
fun scaleLabelData(match: ScaleMatch): ScaleLabelData = ScaleLabelData(
    primaryLabelKey = match.candidate.type.primaryLabelKey,
    subtitleKey = match.candidate.type.subtitleKey,
    spelledRoot = ScaleSpeller.rootName(match.candidate.rootPitchClass, match.candidate.type),
)

/**
 * Returns the standard scale-degree label for a given degree position and its semitone interval.
 *
 * Degree numbers are 1-indexed (1–7). Accidentals are expressed relative to the diatonic major
 * scale: a lowered third is "♭3", a raised fourth is "♯4", and so on.
 *
 * @param degreePosition 0-based position in the scale (0 = root, 6 = seventh degree).
 * @param scaleInterval The scale type's semitone offset at this position (0–11).
 */
fun scaleDegreeLabel(degreePosition: Int, scaleInterval: Int): String {
    val majorIntervals = intArrayOf(0, 2, 4, 5, 7, 9, 11)
    val degreeNumber = degreePosition + 1
    val diff = scaleInterval - majorIntervals[degreePosition]
    val prefix = when (diff) {
        0    -> ""
        -1   -> "♭"
        1    -> "♯"
        -2   -> "♭♭"
        2    -> "𝄪"
        else -> "?"
    }
    return "$prefix$degreeNumber"
}
