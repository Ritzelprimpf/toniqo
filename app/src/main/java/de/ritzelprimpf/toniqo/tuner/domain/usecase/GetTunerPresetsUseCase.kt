package de.ritzelprimpf.toniqo.tuner.domain.usecase

import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset
import de.ritzelprimpf.toniqo.tuner.domain.repository.TunerPresetRepository
import javax.inject.Inject

/**
 * Returns every tuning preset known to the app.
 *
 * Thin wrapper around [TunerPresetRepository.getPresets]; exists so the presentation layer
 * depends on a use case rather than the repository directly, in line with the project's
 * Clean Architecture conventions.
 *
 * @property repository The preset source. Injected by Hilt.
 */
class GetTunerPresetsUseCase @Inject constructor(
    private val repository: TunerPresetRepository,
) {

    /**
     * Returns the full preset catalogue, in display order.
     *
     * @return The list of presets the user may choose from.
     */
    suspend operator fun invoke(): List<TunerPreset> = repository.getPresets()
}
