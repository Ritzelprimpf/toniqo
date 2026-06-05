package de.ritzelprimpf.toniqo.common.model

/**
 * The harmonic quality of a triad — the colour produced by the combination of its intervals.
 *
 * Exactly four values; these are the only triads two stacked thirds can produce. Seventh-chord
 * types are modelled separately in `chordfinder/domain/model/SeventhQuality`, which was
 * introduced in Phase 8.1 when the diatonic chord engine was built.
 *
 * Each value carries:
 * - [intervalsFromRoot] — semitone offsets of every chord tone from the root (e.g. major
 *   triad = [0, 4, 7]).
 * - [symbol] — the conventional chord-notation suffix appended to the root name (e.g. `""` for
 *   major, `"m"` for minor).
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
}
