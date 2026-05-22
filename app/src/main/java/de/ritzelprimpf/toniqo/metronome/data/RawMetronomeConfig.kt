package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision

/**
 * Nullable raw values read directly from DataStore preferences keys.
 *
 * All fields are nullable because DataStore returns `null` for missing keys — which happens on
 * first launch or when new keys are introduced by an app update. [validateOrDefault] converts
 * this raw form into a fully validated [MetronomeConfig].
 */
internal data class RawMetronomeConfig(
    val bpm: Int?,
    val numerator: Int?,
    val denominator: Int?,
    val subdivisionName: String?,
) {
    /**
     * Returns `true` if at least one field was persisted (non-null) **and** the stored values
     * differ from [validated].
     *
     * A `false` result for an all-null config (first launch) avoids unnecessary write-back to
     * a fresh DataStore file. A `true` result for any non-null + mismatching field indicates
     * corruption or an out-of-range value that needs repair.
     */
    fun requiresRepair(validated: MetronomeConfig): Boolean {
        // All nulls = first launch. Nothing was ever written; nothing to repair.
        if (bpm == null && numerator == null && denominator == null && subdivisionName == null) {
            return false
        }
        // If any stored field doesn't match the validated config, repair is needed.
        return bpm != validated.bpm ||
            numerator != validated.timeSignatureNumerator ||
            denominator != validated.timeSignatureDenominator ||
            subdivisionName != validated.subdivision.name
    }
}

/**
 * Validates [raw] and returns a [MetronomeConfig], or [MetronomeConfig.DEFAULT] if any field is
 * missing or out of range.
 *
 * Validation rules (per `Phase6-Metronome-Decisions.md` Item 17):
 * - Every field must be non-null.
 * - [bpm] must be in [[MetronomeConfig.BPM_MIN], [MetronomeConfig.BPM_MAX]].
 * - `(numerator, denominator)` must be one of [MetronomeConfig.SUPPORTED_SIGNATURES].
 * - [subdivisionName] must decode to a [Subdivision] enum value.
 *
 * Any single invalid field triggers **whole-config replacement** — partial repair is not performed.
 */
internal fun validateOrDefault(raw: RawMetronomeConfig): MetronomeConfig {
    val bpm = raw.bpm ?: return MetronomeConfig.DEFAULT
    val numerator = raw.numerator ?: return MetronomeConfig.DEFAULT
    val denominator = raw.denominator ?: return MetronomeConfig.DEFAULT
    val subdivisionName = raw.subdivisionName ?: return MetronomeConfig.DEFAULT

    val subdivision = Subdivision.entries.firstOrNull { it.name == subdivisionName }
        ?: return MetronomeConfig.DEFAULT

    val bpmOk = bpm in MetronomeConfig.BPM_MIN..MetronomeConfig.BPM_MAX
    val sigOk = (numerator to denominator) in MetronomeConfig.SUPPORTED_SIGNATURES

    if (!bpmOk || !sigOk) return MetronomeConfig.DEFAULT

    return MetronomeConfig(
        bpm = bpm,
        timeSignatureNumerator = numerator,
        timeSignatureDenominator = denominator,
        subdivision = subdivision,
    )
}
