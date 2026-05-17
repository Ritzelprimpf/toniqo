package de.ritzelprimpf.toniqo.tuner.data

import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset
import de.ritzelprimpf.toniqo.tuner.domain.repository.TunerPresetRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub implementation of [TunerPresetRepository].
 *
 * Per the 2026-05-17 decision in `DECISIONS.md`, the production implementation will expose
 * hardcoded preset constants. Phase 5.1 fills in the catalogue and the lookup logic; until then
 * both methods throw [NotImplementedError].
 */
@Singleton
class TunerPresetRepositoryImpl @Inject constructor() : TunerPresetRepository {

    /**
     * Stub. Returns the hardcoded preset catalogue once Phase 5.1 lands.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    override suspend fun getPresets(): List<TunerPreset> = TODO("Not yet implemented")

    /**
     * Stub. Returns the matching preset (or `null`) once Phase 5.1 lands.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    override suspend fun getPresetById(id: String): TunerPreset? = TODO("Not yet implemented")
}
