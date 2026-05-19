package de.ritzelprimpf.toniqo.tuner.domain.model

/**
 * High-level grouping for tuning presets, used to organise the preset picker into sections.
 *
 * Each value carries a [urlSlug] — a lower-snake-case form used in preset ID generation
 * (e.g. `"six_string_standard_e"` includes `"standard"` from [STANDARD.urlSlug]).
 *
 * @property urlSlug Lower-snake-case identifier fragment used in preset IDs.
 */
enum class TunerCategory(val urlSlug: String) {
    /** Conventional tunings used as the default for their string count (E Standard, B Standard, …). */
    STANDARD("standard"),

    /** Tunings whose open strings sound a chord (Open D, Open G, DADGAD, …). */
    OPEN("open"),

    /** Tunings that lower one or more specific strings below the standard (Drop D, Drop C, …). */
    DROPPED("dropped"),
}
