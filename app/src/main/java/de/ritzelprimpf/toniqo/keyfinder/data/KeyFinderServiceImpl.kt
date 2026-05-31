package de.ritzelprimpf.toniqo.keyfinder.data

import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderInput
import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderResult
import de.ritzelprimpf.toniqo.keyfinder.domain.repository.KeyFinderService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 2 stub implementation of [KeyFinderService], retained for compilation continuity.
 *
 * Phase 7.3 replaces the ViewModel dependency on this service with [MatchScalesUseCase].
 * Until then [findKeys] throws [NotImplementedError].
 */
@Singleton
class KeyFinderServiceImpl @Inject constructor() : KeyFinderService {

    override fun findKeys(input: KeyFinderInput): List<KeyFinderResult> =
        TODO("Not yet implemented — replaced by MatchScalesUseCase in Phase 7.3")
}
