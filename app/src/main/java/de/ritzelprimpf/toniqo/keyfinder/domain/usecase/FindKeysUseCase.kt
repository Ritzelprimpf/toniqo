package de.ritzelprimpf.toniqo.keyfinder.domain.usecase

import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderInput
import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderResult
import de.ritzelprimpf.toniqo.keyfinder.domain.repository.KeyFinderService
import javax.inject.Inject

/**
 * Phase 2 stub use case retained for compilation continuity until Phase 7.3 wires the ViewModel
 * to [MatchScalesUseCase].
 *
 * @property service The matching service. Injected by Hilt.
 */
class FindKeysUseCase @Inject constructor(
    private val service: KeyFinderService,
) {

    /**
     * Delegates to [KeyFinderService.findKeys]. Throws [NotImplementedError] until Phase 7.3.
     *
     * @param input The user's query.
     * @return The ranked match list.
     */
    operator fun invoke(input: KeyFinderInput): List<KeyFinderResult> = service.findKeys(input)
}
