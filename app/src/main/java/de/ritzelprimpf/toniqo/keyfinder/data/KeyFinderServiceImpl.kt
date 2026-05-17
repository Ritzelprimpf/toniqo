package de.ritzelprimpf.toniqo.keyfinder.data

import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderInput
import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderResult
import de.ritzelprimpf.toniqo.keyfinder.domain.repository.KeyFinderService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub implementation of [KeyFinderService].
 *
 * Will iterate the 84 candidate scales (7 modes × 12 roots) and rank them per the criteria in
 * `APP_SPECIFICATION.md` (Key Finder > Matching Logic). Phase 2 only wires the dependency graph;
 * the matching algorithm itself lands in a later phase.
 */
@Singleton
class KeyFinderServiceImpl @Inject constructor() : KeyFinderService {

    /**
     * Stub. Returns the ranked match list once the matching algorithm is implemented.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    override fun findKeys(input: KeyFinderInput): List<KeyFinderResult> =
        TODO("Not yet implemented")
}
