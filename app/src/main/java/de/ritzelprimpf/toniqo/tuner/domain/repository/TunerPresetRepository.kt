package de.ritzelprimpf.toniqo.tuner.domain.repository

import de.ritzelprimpf.toniqo.tuner.domain.model.TunerCategory
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset

/**
 * Source of the tuning presets the app exposes to the user.
 *
 * All functions are `suspend` to keep the interface stable for future I/O-backed
 * implementations (e.g. user-defined presets in Room) without requiring call-site changes.
 */
interface TunerPresetRepository {

    /**
     * Returns every preset in display order (grouped by string count, then category).
     */
    suspend fun getPresets(): List<TunerPreset>

    /**
     * Returns the preset whose [TunerPreset.id] equals [id], or `null` if not found.
     */
    suspend fun getPresetById(id: String): TunerPreset?

    /**
     * Returns the full catalog pre-grouped for the preset picker.
     *
     * Outer key: string count (6, 7, or 8). Inner key: [TunerCategory]. Each inner list is in
     * display order. This mirrors [TunerPresets.grouped] and is provided here so callers in the
     * presentation layer do not need to repeat the grouping logic.
     */
    suspend fun getPresetsGrouped(): Map<Int, Map<TunerCategory, List<TunerPreset>>>
}
