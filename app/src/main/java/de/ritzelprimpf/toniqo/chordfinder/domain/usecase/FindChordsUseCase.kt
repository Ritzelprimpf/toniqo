package de.ritzelprimpf.toniqo.chordfinder.domain.usecase

import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordFinderInput
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordFinderResult
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.ChordFinderService
import javax.inject.Inject

/**
 * Returns the diatonic chords of the queried key.
 *
 * Thin wrapper around [ChordFinderService.findChords]; exists so the presentation layer depends
 * on a use case rather than the service directly.
 *
 * @property service The chord-finding service. Injected by Hilt.
 */
class FindChordsUseCase @Inject constructor(
    private val service: ChordFinderService,
) {

    /**
     * Returns the diatonic chords of the key described by [input].
     *
     * @param input The user's query.
     * @return The chord list.
     */
    operator fun invoke(input: ChordFinderInput): ChordFinderResult = service.findChords(input)
}
