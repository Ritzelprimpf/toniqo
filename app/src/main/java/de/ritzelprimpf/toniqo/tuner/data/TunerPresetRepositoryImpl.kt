package de.ritzelprimpf.toniqo.tuner.data

import de.ritzelprimpf.toniqo.tuner.domain.model.TunerCategory
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset
import de.ritzelprimpf.toniqo.tuner.domain.repository.TunerPresetRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardcoded implementation of [TunerPresetRepository].
 *
 * All data comes from [TunerPresets], which holds the full catalog as compiled-in constants.
 * Functions are `suspend` to satisfy the interface contract and remain compatible with a future
 * Room-backed implementation if user-defined presets are added.
 */
@Singleton
class TunerPresetRepositoryImpl @Inject constructor() : TunerPresetRepository {

    override suspend fun getPresets(): List<TunerPreset> = TunerPresets.all

    override suspend fun getPresetById(id: String): TunerPreset? =
        TunerPresets.all.firstOrNull { it.id == id }

    override suspend fun getPresetsGrouped(): Map<Int, Map<TunerCategory, List<TunerPreset>>> =
        TunerPresets.grouped
}
