package de.ritzelprimpf.toniqo.keyfinder.domain.model

/**
 * A single ranked result produced by [de.ritzelprimpf.toniqo.keyfinder.domain.usecase.MatchScalesUseCase].
 *
 * Display strings (primary label, subtitle, conventionally-spelled note list) are **not** stored
 * here. The presentation layer derives them from [candidate.type]'s resource keys and
 * [de.ritzelprimpf.toniqo.common.util.ScaleSpeller], keeping the domain free of Context/resources.
 *
 * @property candidate The scale (root + type) that produced this match.
 * @property percent Match score as an integer percentage 0–100, computed by the locked formula
 *   in `Phase7-PLAN.md` (points / maxPoints, rounded half-up).
 * @property isFull `true` iff every input pitch class is contained in [candidate.pitchClasses].
 *   A scale can be FULL at any percentage (e.g. 3 notes that all fit → FULL at 43%).
 * @property isRootMatch `true` iff the user marked a root and [candidate.rootPitchClass] equals it.
 * @property rank 1-based position in the displayed result list (1 = best match, up to 7).
 */
data class ScaleMatch(
    val candidate: ScaleCandidate,
    val percent: Int,
    val isFull: Boolean,
    val isRootMatch: Boolean,
    val rank: Int,
)
