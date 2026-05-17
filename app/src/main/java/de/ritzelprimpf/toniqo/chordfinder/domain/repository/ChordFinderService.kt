package de.ritzelprimpf.toniqo.chordfinder.domain.repository

import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordFinderInput
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordFinderResult

/**
 * Pure-logic service that returns the diatonic chords of a given key/mode.
 *
 * Named `Service` (rather than `Repository`) because it performs computation, not persistence.
 * The interface lives in the domain layer; the implementation lives in `data/` to keep the layer
 * split consistent across modules.
 */
interface ChordFinderService {

    /**
     * Returns the diatonic chords of the key described by [input], in scale-degree order.
     *
     * @param input The user's query.
     * @return The chord list.
     */
    fun findChords(input: ChordFinderInput): ChordFinderResult
}
