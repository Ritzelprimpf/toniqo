package de.ritzelprimpf.toniqo.keyfinder.domain.model

import de.ritzelprimpf.toniqo.common.model.Scale

/**
 * A single ranked match returned by the Key Finder.
 *
 * One [KeyFinderResult] is produced per candidate scale that contains at least one input note;
 * scales with zero matching notes are excluded from the result list entirely.
 *
 * @property scale The candidate diatonic scale (root + mode interval pattern).
 * @property modeName Display name of the matched mode (e.g. `A Natural Minor`,
 *   `C Major (Ionian)`).
 * @property matchScore Fraction of the input notes contained in [scale], in `0.0..1.0`. A score
 *   of `1.0` corresponds to a full match.
 * @property isFullMatch `true` iff every input note is contained in [scale].
 * @property matchesTonic `true` iff the user provided a tonic and [scale]'s root matches it. Used
 *   as the primary ranking criterion ahead of [matchScore].
 */
data class KeyFinderResult(
    val scale: Scale,
    val modeName: String,
    val matchScore: Float,
    val isFullMatch: Boolean,
    val matchesTonic: Boolean,
)
