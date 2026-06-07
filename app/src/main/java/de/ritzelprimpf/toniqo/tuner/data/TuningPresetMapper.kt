package de.ritzelprimpf.toniqo.tuner.data

import de.ritzelprimpf.toniqo.common.model.GuitarTuning
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset

/**
 * Pure mapping from a [TunerPreset] to the shared [GuitarTuning] model.
 *
 * Lives in the tuner module because it knows tuner-module internals ([TunerPreset]); outputs
 * the cross-module [GuitarTuning] so that [de.ritzelprimpf.toniqo.common.state.SelectedTuningStore]
 * can hold it without importing tuner types.
 *
 * Stateless and side-effect-free — a top-level `object` per [CLAUDE.md] §4.
 */
object TuningPresetMapper {

    /**
     * Converts a [TunerPreset] into a [GuitarTuning].
     *
     * The preset's [TunerPreset.id] becomes the tuning's [GuitarTuning.id] so that
     * [GuitarTuning.uniformOffsetFrom] comparisons remain stable across module boundaries.
     */
    fun map(preset: TunerPreset): GuitarTuning = GuitarTuning(
        id = preset.id,
        openNotes = preset.notes,
    )
}
