package de.ritzelprimpf.toniqo.chordfinder.data

import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordFinderInput
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordFinderResult
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.ChordFinderService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub implementation of [ChordFinderService].
 *
 * Will delegate to `MusicTheory.buildScale` followed by `buildTriads` (or `buildSeventhChords`)
 * once those are implemented. Phase 2 only wires the dependency graph.
 */
@Singleton
class ChordFinderServiceImpl @Inject constructor() : ChordFinderService {

    /**
     * Stub. Returns the diatonic-chord list once the chord-building logic is implemented.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    override fun findChords(input: ChordFinderInput): ChordFinderResult =
        TODO("Not yet implemented")
}
