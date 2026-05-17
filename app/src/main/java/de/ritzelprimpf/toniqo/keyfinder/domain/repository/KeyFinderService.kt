package de.ritzelprimpf.toniqo.keyfinder.domain.repository

import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderInput
import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderResult

/**
 * Pure-logic service that ranks the 84 candidate diatonic scales against the user's input.
 *
 * Named `Service` (rather than `Repository`) because it performs computation, not persistence —
 * no I/O is involved. The interface lives in the domain layer so the rest of the stack depends
 * on the abstraction; the implementation lives in `data/` to keep the layer split consistent
 * across modules.
 */
interface KeyFinderService {

    /**
     * Ranks the candidate scales against [input]. Scales with zero matching notes are excluded.
     *
     * Ranking order: tonic-matching scales first; within each group, scales with higher
     * [KeyFinderResult.matchScore] rank higher; ties are broken by alphabetical mode name.
     *
     * @param input The user's query.
     * @return The ranked match list — best first.
     */
    fun findKeys(input: KeyFinderInput): List<KeyFinderResult>
}
