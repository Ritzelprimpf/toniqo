package de.ritzelprimpf.toniqo.keyfinder.domain.usecase

import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderInput
import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderResult
import de.ritzelprimpf.toniqo.keyfinder.domain.repository.KeyFinderService
import javax.inject.Inject

/**
 * Returns the Key Finder's ranked match list for a given input.
 *
 * Thin wrapper around [KeyFinderService.findKeys]; exists so the presentation layer depends on a
 * use case rather than the service directly.
 *
 * @property service The matching service. Injected by Hilt.
 */
class FindKeysUseCase @Inject constructor(
    private val service: KeyFinderService,
) {

    /**
     * Ranks candidate diatonic scales against [input].
     *
     * @param input The user's query.
     * @return The ranked match list.
     */
    operator fun invoke(input: KeyFinderInput): List<KeyFinderResult> = service.findKeys(input)
}
