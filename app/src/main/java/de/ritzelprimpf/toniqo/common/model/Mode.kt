package de.ritzelprimpf.toniqo.common.model

/**
 * The seven Western diatonic modes, each defined by its interval pattern from the root.
 *
 * Each mode carries a list of [Interval]s describing the *cumulative* semitone offsets of its
 * scale degrees from the root, starting at the unison and ending one octave higher. For
 * example, Ionian (the major scale) follows the pattern `0, 2, 4, 5, 7, 9, 11, 12`.
 *
 * @property displayName Human-readable name suitable for default UI rendering.
 * @property intervalsFromRoot Cumulative semitone offsets of each scale degree from the root.
 *   The first entry is the unison (0); the last is the octave (12). The list always has length 8.
 */
enum class Mode(
    val displayName: String,
    val intervalsFromRoot: List<Interval>,
) {
    /** Ionian — the natural major scale. Pattern: W W H W W W H. */
    IONIAN(
        displayName = "Ionian (Major)",
        intervalsFromRoot = listOf(
            Interval.UNISON,
            Interval.MAJOR_SECOND,
            Interval.MAJOR_THIRD,
            Interval.PERFECT_FOURTH,
            Interval.PERFECT_FIFTH,
            Interval.MAJOR_SIXTH,
            Interval.MAJOR_SEVENTH,
            Interval.OCTAVE,
        ),
    ),

    /** Dorian — minor mode with a raised sixth. Pattern: W H W W W H W. */
    DORIAN(
        displayName = "Dorian",
        intervalsFromRoot = listOf(
            Interval.UNISON,
            Interval.MAJOR_SECOND,
            Interval.MINOR_THIRD,
            Interval.PERFECT_FOURTH,
            Interval.PERFECT_FIFTH,
            Interval.MAJOR_SIXTH,
            Interval.MINOR_SEVENTH,
            Interval.OCTAVE,
        ),
    ),

    /** Phrygian — minor mode with a lowered second. Pattern: H W W W H W W. */
    PHRYGIAN(
        displayName = "Phrygian",
        intervalsFromRoot = listOf(
            Interval.UNISON,
            Interval.MINOR_SECOND,
            Interval.MINOR_THIRD,
            Interval.PERFECT_FOURTH,
            Interval.PERFECT_FIFTH,
            Interval.MINOR_SIXTH,
            Interval.MINOR_SEVENTH,
            Interval.OCTAVE,
        ),
    ),

    /** Lydian — major mode with a raised fourth. Pattern: W W W H W W H. */
    LYDIAN(
        displayName = "Lydian",
        intervalsFromRoot = listOf(
            Interval.UNISON,
            Interval.MAJOR_SECOND,
            Interval.MAJOR_THIRD,
            Interval.TRITONE,
            Interval.PERFECT_FIFTH,
            Interval.MAJOR_SIXTH,
            Interval.MAJOR_SEVENTH,
            Interval.OCTAVE,
        ),
    ),

    /** Mixolydian — major mode with a lowered seventh. Pattern: W W H W W H W. */
    MIXOLYDIAN(
        displayName = "Mixolydian",
        intervalsFromRoot = listOf(
            Interval.UNISON,
            Interval.MAJOR_SECOND,
            Interval.MAJOR_THIRD,
            Interval.PERFECT_FOURTH,
            Interval.PERFECT_FIFTH,
            Interval.MAJOR_SIXTH,
            Interval.MINOR_SEVENTH,
            Interval.OCTAVE,
        ),
    ),

    /** Aeolian — the natural minor scale. Pattern: W H W W H W W. */
    AEOLIAN(
        displayName = "Aeolian (Natural Minor)",
        intervalsFromRoot = listOf(
            Interval.UNISON,
            Interval.MAJOR_SECOND,
            Interval.MINOR_THIRD,
            Interval.PERFECT_FOURTH,
            Interval.PERFECT_FIFTH,
            Interval.MINOR_SIXTH,
            Interval.MINOR_SEVENTH,
            Interval.OCTAVE,
        ),
    ),

    /** Locrian — diminished mode with lowered second and fifth. Pattern: H W W H W W W. */
    LOCRIAN(
        displayName = "Locrian",
        intervalsFromRoot = listOf(
            Interval.UNISON,
            Interval.MINOR_SECOND,
            Interval.MINOR_THIRD,
            Interval.PERFECT_FOURTH,
            Interval.TRITONE,
            Interval.MINOR_SIXTH,
            Interval.MINOR_SEVENTH,
            Interval.OCTAVE,
        ),
    ),
}
