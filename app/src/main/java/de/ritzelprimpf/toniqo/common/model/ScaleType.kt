package de.ritzelprimpf.toniqo.common.model

/**
 * The fourteen scale types supported by the Key Finder.
 *
 * This enum is a **superset** of [Mode]: the seven diatonic types (IONIAN through LOCRIAN)
 * carry the same interval patterns as the corresponding [Mode] entries. A test asserts this
 * equality so the interval data has a single conceptual source of truth. [Mode] is retained
 * unchanged for the tuner and Chord Finder modules.
 *
 * Together with twelve roots (pitch classes 0–11), these fourteen types yield the
 * 168 candidates in the Key Finder catalog (12 × 14 = 168).
 *
 * Each entry exposes:
 * - [family] — the parent scale family.
 * - [intervalsFromRoot] — 7 cumulative semitone offsets from the root (always starts at 0,
 *   strictly ascending, all values 0–11).
 * - [rankOrder] — 0-based position in the "common-first" tie-break ordering used by the
 *   matching engine when two scales score the same percentage.
 * - [primaryLabelKey] / [subtitleKey] — string-resource keys (matching entries in
 *   `res/values/strings.xml`); the caller fills the `%s` root placeholder at display time.
 *   No display text is hardcoded in this enum.
 */
enum class ScaleType(
    val family: ScaleFamily,
    val intervalsFromRoot: IntArray,
    val rankOrder: Int,
    val primaryLabelKey: String,
    val subtitleKey: String,
) {

    // ──────────────────────────── Diatonic modes ────────────────────────────

    /** Ionian — the natural major scale. Diatonic mode 1. */
    IONIAN(
        family = ScaleFamily.DIATONIC,
        intervalsFromRoot = intArrayOf(0, 2, 4, 5, 7, 9, 11),
        rankOrder = 0,
        primaryLabelKey = "scale_type_label_major",
        subtitleKey = "scale_type_subtitle_ionian",
    ),

    /** Aeolian — the natural minor scale. Diatonic mode 6. */
    AEOLIAN(
        family = ScaleFamily.DIATONIC,
        intervalsFromRoot = intArrayOf(0, 2, 3, 5, 7, 8, 10),
        rankOrder = 1,
        primaryLabelKey = "scale_type_label_natural_minor",
        subtitleKey = "scale_type_subtitle_aeolian",
    ),

    /** Dorian — minor mode with a raised sixth. Diatonic mode 2. */
    DORIAN(
        family = ScaleFamily.DIATONIC,
        intervalsFromRoot = intArrayOf(0, 2, 3, 5, 7, 9, 10),
        rankOrder = 2,
        primaryLabelKey = "scale_type_label_dorian",
        subtitleKey = "scale_type_subtitle_dorian",
    ),

    /** Phrygian — minor mode with a lowered second. Diatonic mode 3. */
    PHRYGIAN(
        family = ScaleFamily.DIATONIC,
        intervalsFromRoot = intArrayOf(0, 1, 3, 5, 7, 8, 10),
        rankOrder = 3,
        primaryLabelKey = "scale_type_label_phrygian",
        subtitleKey = "scale_type_subtitle_phrygian",
    ),

    /** Lydian — major mode with a raised fourth. Diatonic mode 4. */
    LYDIAN(
        family = ScaleFamily.DIATONIC,
        intervalsFromRoot = intArrayOf(0, 2, 4, 6, 7, 9, 11),
        rankOrder = 4,
        primaryLabelKey = "scale_type_label_lydian",
        subtitleKey = "scale_type_subtitle_lydian",
    ),

    /** Mixolydian — major mode with a lowered seventh. Diatonic mode 5. */
    MIXOLYDIAN(
        family = ScaleFamily.DIATONIC,
        intervalsFromRoot = intArrayOf(0, 2, 4, 5, 7, 9, 10),
        rankOrder = 5,
        primaryLabelKey = "scale_type_label_mixolydian",
        subtitleKey = "scale_type_subtitle_mixolydian",
    ),

    /** Locrian — diminished mode with lowered second and fifth. Diatonic mode 7. */
    LOCRIAN(
        family = ScaleFamily.DIATONIC,
        intervalsFromRoot = intArrayOf(0, 1, 3, 5, 6, 8, 10),
        rankOrder = 6,
        primaryLabelKey = "scale_type_label_locrian",
        subtitleKey = "scale_type_subtitle_locrian",
    ),

    // ────────────────────────── Harmonic-minor family ───────────────────────

    /** Harmonic Minor — natural minor with a raised seventh degree. */
    HARMONIC_MINOR(
        family = ScaleFamily.HARMONIC_MINOR,
        intervalsFromRoot = intArrayOf(0, 2, 3, 5, 7, 8, 11),
        rankOrder = 7,
        primaryLabelKey = "scale_type_label_harmonic_minor",
        subtitleKey = "scale_type_subtitle_harmonic_minor",
    ),

    /**
     * Phrygian Dominant — mode 5 of harmonic minor; a major scale with a lowered second and sixth.
     * Common in flamenco and Middle Eastern music.
     */
    PHRYGIAN_DOMINANT(
        family = ScaleFamily.HARMONIC_MINOR,
        intervalsFromRoot = intArrayOf(0, 1, 4, 5, 7, 8, 10),
        rankOrder = 8,
        primaryLabelKey = "scale_type_label_phrygian_dominant",
        subtitleKey = "scale_type_subtitle_phrygian_dominant",
    ),

    /**
     * Locrian ♮6 — mode 2 of harmonic minor; Locrian with a natural (raised) sixth.
     * Also known as the "Half-Diminished" or "Super Locrian ♮6" scale in jazz contexts.
     */
    LOCRIAN_NATURAL_6(
        family = ScaleFamily.HARMONIC_MINOR,
        intervalsFromRoot = intArrayOf(0, 1, 3, 5, 6, 9, 10),
        rankOrder = 9,
        primaryLabelKey = "scale_type_label_locrian_natural6",
        subtitleKey = "scale_type_subtitle_locrian_natural6",
    ),

    // ────────────────────────── Melodic-minor family ────────────────────────

    /** Melodic Minor — natural minor with raised sixth and seventh degrees. */
    MELODIC_MINOR(
        family = ScaleFamily.MELODIC_MINOR,
        intervalsFromRoot = intArrayOf(0, 2, 3, 5, 7, 9, 11),
        rankOrder = 10,
        primaryLabelKey = "scale_type_label_melodic_minor",
        subtitleKey = "scale_type_subtitle_melodic_minor",
    ),

    /**
     * Lydian Dominant — mode 4 of melodic minor; Lydian with a lowered seventh.
     * Widely used over dominant seventh chords in jazz.
     */
    LYDIAN_DOMINANT(
        family = ScaleFamily.MELODIC_MINOR,
        intervalsFromRoot = intArrayOf(0, 2, 4, 6, 7, 9, 10),
        rankOrder = 11,
        primaryLabelKey = "scale_type_label_lydian_dominant",
        subtitleKey = "scale_type_subtitle_lydian_dominant",
    ),

    /**
     * Altered — mode 7 of melodic minor; also called Super Locrian.
     * Contains all four alterations (♭9, ♯9, ♭5/♯11, ♭13) and is used over altered dominant chords.
     */
    ALTERED(
        family = ScaleFamily.MELODIC_MINOR,
        intervalsFromRoot = intArrayOf(0, 1, 3, 4, 6, 8, 10),
        rankOrder = 12,
        primaryLabelKey = "scale_type_label_altered",
        subtitleKey = "scale_type_subtitle_altered",
    ),

    /**
     * Dorian ♭2 — mode 2 of melodic minor; Dorian with a lowered second.
     * Also called Phrygian ♮6 or Assyrian.
     */
    DORIAN_FLAT_2(
        family = ScaleFamily.MELODIC_MINOR,
        intervalsFromRoot = intArrayOf(0, 1, 3, 5, 7, 9, 10),
        rankOrder = 13,
        primaryLabelKey = "scale_type_label_dorian_flat2",
        subtitleKey = "scale_type_subtitle_dorian_flat2",
    );

    companion object {

        /**
         * The seven diatonic scale types in [rankOrder] — a convenience for callers that need
         * only the diatonic subset (e.g., Chord Finder, tuner preset display).
         */
        val DIATONIC: List<ScaleType> = listOf(
            IONIAN, AEOLIAN, DORIAN, PHRYGIAN, LYDIAN, MIXOLYDIAN, LOCRIAN,
        )
    }
}
