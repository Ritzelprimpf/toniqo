package de.ritzelprimpf.toniqo.keyfinder.domain.usecase

import de.ritzelprimpf.toniqo.keyfinder.domain.ScaleCatalog
import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderInput
import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleCandidate
import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleMatch
import kotlin.math.floor
import kotlin.math.max

/**
 * Scores and ranks the Key Finder's scale catalog against a user-supplied set of pitch classes.
 *
 * **Scoring formula** (from `Phase7-PLAN.md`, supersedes `APP_SPECIFICATION.md`):
 * ```
 * covered    = |inputPitchClasses ∩ candidate.pitchClasses|
 * rootBonus  = 1 if (rootPitchClass != null and candidate.root == rootPitchClass) else 0
 * points     = covered + rootBonus
 * maxPoints  = max(SCALE_SIZE, n) + (if rootPitchClass != null then 1 else 0)
 * percent    = round(points / maxPoints × 100)   // round half-up
 * ```
 *
 * **Ranking**: percent descending → [ScaleType.rankOrder][de.ritzelprimpf.toniqo.common.model.ScaleType.rankOrder]
 * ascending → root pitch class ascending. Top [MAX_RESULTS] returned.
 *
 * **Gate**: returns an empty list when the input has fewer than [MIN_NOTES_TO_MATCH] distinct
 * pitch classes. This is a pure synchronous function — matching 168 small sets is microseconds.
 *
 * @property catalog Source of [ScaleCandidate]s to score. Use [ScaleCatalog.DEFAULT] in
 *   production; pass a trimmed catalog in unit tests.
 */
class MatchScalesUseCase(private val catalog: ScaleCatalog = ScaleCatalog.DEFAULT) {

    companion object {
        /** Minimum distinct input pitch classes required before any matches are returned. */
        const val MIN_NOTES_TO_MATCH: Int = 3

        /** Maximum number of results returned; the top-[MAX_RESULTS] by score are kept. */
        const val MAX_RESULTS: Int = 7

        /** Every scale type in this engine has exactly this many pitch classes. */
        const val SCALE_SIZE: Int = 7

        /** Number of distinct pitch classes in the chromatic scale. */
        const val PITCH_CLASSES: Int = 12
    }

    /**
     * Scores and ranks all candidates against [input].
     *
     * @param input Pitch classes and optional root; must already be de-duplicated and octave-free.
     * @return Up to [MAX_RESULTS] [ScaleMatch]es, or an empty list if input size < [MIN_NOTES_TO_MATCH].
     */
    operator fun invoke(input: KeyFinderInput): List<ScaleMatch> {
        val n = input.pitchClasses.size
        if (n < MIN_NOTES_TO_MATCH) return emptyList()

        val rootMarked = input.rootPitchClass != null
        val maxPoints = max(SCALE_SIZE, n) + if (rootMarked) 1 else 0

        return catalog.candidates
            .mapNotNull { candidate -> scoreCandidate(candidate, input, n, maxPoints, rootMarked) }
            .sortedWith(
                compareByDescending<ScaleMatch> { it.percent }
                    .thenBy { it.candidate.type.rankOrder }
                    .thenBy { it.candidate.rootPitchClass },
            )
            .take(MAX_RESULTS)
            .mapIndexed { index, match -> match.copy(rank = index + 1) }
    }

    private fun scoreCandidate(
        candidate: ScaleCandidate,
        input: KeyFinderInput,
        n: Int,
        maxPoints: Int,
        rootMarked: Boolean,
    ): ScaleMatch? {
        val covered = (input.pitchClasses intersect candidate.pitchClasses).size
        if (covered == 0) return null

        val rootBonus = if (rootMarked && candidate.rootPitchClass == input.rootPitchClass) 1 else 0
        val points = covered + rootBonus
        val percent = roundHalfUp(points, maxPoints)

        return ScaleMatch(
            candidate = candidate,
            percent = percent,
            isFull = covered == n,
            isRootMatch = rootMarked && candidate.rootPitchClass == input.rootPitchClass,
            rank = 0, // assigned after sort
        )
    }

    /** Integer percentage rounded half-up: floor(points/maxPoints × 100 + 0.5). */
    private fun roundHalfUp(points: Int, maxPoints: Int): Int =
        floor(points.toDouble() / maxPoints * 100.0 + 0.5).toInt()
}
