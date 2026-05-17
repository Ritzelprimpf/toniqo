package de.ritzelprimpf.toniqo.tuner.domain.model

/**
 * High-level grouping for tuning presets, used to organise the preset picker into sections.
 *
 * Categories are coarse-grained on purpose: every preset shipped by the app falls into exactly
 * one of these three buckets. Finer subdivisions (e.g. "modern metal") are not modelled —
 * they live in the display ordering, not in the type system.
 */
enum class TunerCategory {
    /** Conventional tunings used as the default for their string count (E Standard, B Standard, …). */
    STANDARD,

    /** Tunings whose open strings sound a chord (Open D, Open G, DADGAD, …). */
    OPEN,

    /** Tunings that lower one or more specific strings below the standard (Drop D, Drop C, …). */
    DROPPED,
}
