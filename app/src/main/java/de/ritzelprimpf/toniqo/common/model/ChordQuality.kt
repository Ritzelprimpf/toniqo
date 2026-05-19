package de.ritzelprimpf.toniqo.common.model

/**
 * The harmonic quality of a chord — the colour produced by the combination of its intervals.
 *
 * Each value carries:
 * - [intervalsFromRoot] — the semitone offsets of every chord tone from the root (e.g. major
 *   triad = [0, 4, 7]).
 * - [symbol] — the conventional chord-notation suffix appended to the root name (e.g. `""` for
 *   major, `"m"` for minor, `"maj7"`, `"7"`, `"m7♭5"`, etc.).
 *
 * @property intervalsFromRoot Semitone offsets of each chord tone from the root.
 * @property symbol Chord-notation suffix; empty string for major triads.
 */
enum class ChordQuality(
    val intervalsFromRoot: IntArray,
    val symbol: String,
) {
    /** Major triad: root, major third, perfect fifth. */
    MAJOR(intArrayOf(0, 4, 7), ""),

    /** Minor triad: root, minor third, perfect fifth. */
    MINOR(intArrayOf(0, 3, 7), "m"),

    /** Diminished triad: root, minor third, diminished fifth. */
    DIMINISHED(intArrayOf(0, 3, 6), "dim"),

    /** Augmented triad: root, major third, augmented fifth. */
    AUGMENTED(intArrayOf(0, 4, 8), "aug"),

    /** Major seventh chord: major triad with a major seventh (e.g. Cmaj7). */
    MAJOR_SEVENTH(intArrayOf(0, 4, 7, 11), "maj7"),

    /** Minor seventh chord: minor triad with a minor seventh (e.g. Dm7). */
    MINOR_SEVENTH(intArrayOf(0, 3, 7, 10), "m7"),

    /** Dominant seventh chord: major triad with a minor seventh (e.g. G7). */
    DOMINANT_SEVENTH(intArrayOf(0, 4, 7, 10), "7"),

    /** Half-diminished seventh chord: diminished triad with a minor seventh (e.g. Bm7♭5). */
    HALF_DIMINISHED(intArrayOf(0, 3, 6, 10), "m7♭5"),

    /** Fully diminished seventh chord: diminished triad with a diminished seventh (e.g. Bdim7). */
    DIMINISHED_SEVENTH(intArrayOf(0, 3, 6, 9), "dim7"),
}
