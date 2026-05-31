package de.ritzelprimpf.toniqo.keyfinder.domain.repository

import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderInput
import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderResult

/**
 * Pure-logic service stub retained for the Phase 2 dependency-graph wiring.
 *
 * Phase 7.3 replaces the ViewModel's dependency on [FindKeysUseCase] / [KeyFinderService] with
 * [de.ritzelprimpf.toniqo.keyfinder.domain.usecase.MatchScalesUseCase]. Until then this
 * interface remains as a compile-time placeholder; [findKeys] still throws in the implementation.
 *
 * Named `Service` (rather than `Repository`) because it performs computation, not persistence.
 */
interface KeyFinderService {

    /**
     * Phase 2 stub. Returns a ranked match list once the matching algorithm is wired in Phase 7.3.
     *
     * @param input The user's query (pitch classes + optional root).
     * @return The ranked match list.
     */
    fun findKeys(input: KeyFinderInput): List<KeyFinderResult>
}
