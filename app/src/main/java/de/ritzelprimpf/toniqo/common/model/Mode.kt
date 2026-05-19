package de.ritzelprimpf.toniqo.common.model

/**
 * The seven Western diatonic modes, each defined by its interval pattern from the root.
 *
 * [intervalsFromRoot] is an [IntArray] of 7 values — the cumulative semitone offsets of each
 * scale degree from the tonic (e.g. Ionian: 0, 2, 4, 5, 7, 9, 11). The octave (12) is not
 * included; the 7 values correspond to the 7 diatonic scale degrees.
 *
 * @property displayName Human-readable name suitable for UI rendering.
 * @property intervalsFromRoot Cumulative semitone offsets of each scale degree from the root.
 *   Always has exactly 7 entries; the first is always 0 (unison).
 */
enum class Mode(
    val displayName: String,
    val intervalsFromRoot: IntArray,
) {
    /** Ionian — the natural major scale. Pattern: W W H W W W H. */
    IONIAN(
        displayName = "Ionian (Major)",
        intervalsFromRoot = intArrayOf(0, 2, 4, 5, 7, 9, 11),
    ),

    /** Dorian — minor mode with a raised sixth. Pattern: W H W W W H W. */
    DORIAN(
        displayName = "Dorian",
        intervalsFromRoot = intArrayOf(0, 2, 3, 5, 7, 9, 10),
    ),

    /** Phrygian — minor mode with a lowered second. Pattern: H W W W H W W. */
    PHRYGIAN(
        displayName = "Phrygian",
        intervalsFromRoot = intArrayOf(0, 1, 3, 5, 7, 8, 10),
    ),

    /** Lydian — major mode with a raised fourth. Pattern: W W W H W W H. */
    LYDIAN(
        displayName = "Lydian",
        intervalsFromRoot = intArrayOf(0, 2, 4, 6, 7, 9, 11),
    ),

    /** Mixolydian — major mode with a lowered seventh. Pattern: W W H W W H W. */
    MIXOLYDIAN(
        displayName = "Mixolydian",
        intervalsFromRoot = intArrayOf(0, 2, 4, 5, 7, 9, 10),
    ),

    /** Aeolian — the natural minor scale. Pattern: W H W W H W W. */
    AEOLIAN(
        displayName = "Aeolian (Natural Minor)",
        intervalsFromRoot = intArrayOf(0, 2, 3, 5, 7, 8, 10),
    ),

    /** Locrian — diminished mode with lowered second and fifth. Pattern: H W W H W W W. */
    LOCRIAN(
        displayName = "Locrian",
        intervalsFromRoot = intArrayOf(0, 1, 3, 5, 6, 8, 10),
    ),
}
