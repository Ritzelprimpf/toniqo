package de.ritzelprimpf.toniqo.tuner.domain.repository

import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset

/**
 * Source of the tuning presets the app exposes to the user.
 *
 * Implementations may load presets from compiled-in constants (the Phase-2 default), a JSON
 * asset, or a database — the rest of the stack does not care. The interface is `suspend` to
 * keep the door open for I/O-backed implementations without changing call sites.
 */
interface TunerPresetRepository {

    /**
     * Returns every preset available to the user, in display order (typically grouped by string
     * count and category).
     *
     * @return The full preset catalogue.
     */
    suspend fun getPresets(): List<TunerPreset>

    /**
     * Returns the preset whose [TunerPreset.id] equals [id], or `null` if no such preset exists.
     *
     * @param id The stable preset identifier.
     * @return The matching preset, or `null` if not found.
     */
    suspend fun getPresetById(id: String): TunerPreset?
}
