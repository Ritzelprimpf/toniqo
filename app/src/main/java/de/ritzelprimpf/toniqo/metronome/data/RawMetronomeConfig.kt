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
    val accentedBeats: String?,
) {
    /**
     * Returns `true` if at least one field was persisted (non-null) **and** the stored values
     * differ from [validated].
     *
     * A `false` result for an all-null config (first launch) avoids unnecessary write-back to
     * a fresh DataStore file. A `true` result for any non-null + mismatching field indicates
     * corruption or an out-of-range value that needs repair.
     *
     * [accentedBeats] is included in the mismatch check even though it's excluded from the
     * all-null first-launch guard below: an **existing** config (the other four fields present)
     * from before the accent-customization field existed has `accentedBeats == null`, which
     * mismatches the validated default and triggers exactly one self-healing write-back — adding
     * the new key without disturbing the other, already-matching fields. See
     * [parseAccentedBeats]'s doc for why this field's repair never cascades into a full reset.
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
            subdivisionName != validated.subdivision.name ||
            accentedBeats != encodeAccentedBeats(validated.accentedBeats)
    }
}

/** Encodes an accent pattern as ascending, comma-joined indices (e.g. `{0, 3}` → `"0,3"`). */
internal fun encodeAccentedBeats(accentedBeats: Set<Int>): String =
    accentedBeats.sorted().joinToString(separator = ",")

/**
 * Parses [raw] into an accent pattern, falling back to [MetronomeConfig.DEFAULT_ACCENTED_BEATS]
 * if it is absent, malformed, or contains an index outside `[0, numerator)`.
 *
 * Unlike every other field in [validateOrDefault], an invalid or missing [raw] here does **not**
 * reset the rest of the config. This field is purely additive, self-contained state with an
 * obvious safe default — and critically, a user upgrading from a version that predates this field
 * will have `raw == null` for it while every other field is a perfectly valid, already-persisted
 * config; resetting their BPM/signature/subdivision just because this one new key hasn't been
 * written yet would be a real regression, not a safety net.
 */
internal fun parseAccentedBeats(raw: String?, numerator: Int): Set<Int> {
    if (raw == null) return MetronomeConfig.DEFAULT_ACCENTED_BEATS
    if (raw.isEmpty()) return emptySet()
    val indices = raw.split(",").map { it.toIntOrNull() ?: return MetronomeConfig.DEFAULT_ACCENTED_BEATS }
    if (indices.any { it !in 0 until numerator }) return MetronomeConfig.DEFAULT_ACCENTED_BEATS
    return indices.toSet()
}

/**
 * Validates [raw] and returns a [MetronomeConfig], or [MetronomeConfig.DEFAULT] if any field is
 * missing or out of range.
 *
 * Validation rules (per `Phase6-Metronome-Decisions.md` Item 17):
 * - [bpm], [numerator], [denominator], and [subdivisionName] must all be non-null.
 * - [bpm] must be in [[MetronomeConfig.BPM_MIN], [MetronomeConfig.BPM_MAX]].
 * - `(numerator, denominator)` must satisfy [MetronomeConfig.isSupportedTimeSignature].
 * - [subdivisionName] must decode to a [Subdivision] enum value.
 *
 * Any single invalid field among those four triggers **whole-config replacement** — partial
 * repair is not performed. [accentedBeats] is validated and repaired independently via
 * [parseAccentedBeats] — see that function's doc for why it's excluded from the whole-config-reset
 * rule above.
 */
internal fun validateOrDefault(raw: RawMetronomeConfig): MetronomeConfig {
    val bpm = raw.bpm ?: return MetronomeConfig.DEFAULT
    val numerator = raw.numerator ?: return MetronomeConfig.DEFAULT
    val denominator = raw.denominator ?: return MetronomeConfig.DEFAULT
    val subdivisionName = raw.subdivisionName ?: return MetronomeConfig.DEFAULT

    val subdivision = Subdivision.entries.firstOrNull { it.name == subdivisionName }
        ?: return MetronomeConfig.DEFAULT

    val bpmOk = bpm in MetronomeConfig.BPM_MIN..MetronomeConfig.BPM_MAX
    val sigOk = MetronomeConfig.isSupportedTimeSignature(numerator, denominator)

    if (!bpmOk || !sigOk) return MetronomeConfig.DEFAULT

    return MetronomeConfig(
        bpm = bpm,
        timeSignatureNumerator = numerator,
        timeSignatureDenominator = denominator,
        subdivision = subdivision,
        accentedBeats = parseAccentedBeats(raw.accentedBeats, numerator),
    )
}
