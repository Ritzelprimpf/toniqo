package de.ritzelprimpf.toniqo.common.model

/**
 * The harmonic quality of a chord — the colour produced by the combination of its intervals.
 *
 * The four triad values are the only triads two stacked thirds can produce. [POWER] is not a
 * triad (it has no third, so it is neither major nor minor) — it is included here rather than
 * as a triad variant because [de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing]'s
 * "every chord tone must sound" invariant is keyed off exactly this list, and a power chord's
 * tone set genuinely is just {root, fifth}. It is never produced by diatonic harmonization
 * (`MusicTheory`, `ChordQualityResolver.triad`) — those only ever resolve to the four triads
 * below — so it is reachable only where a caller deliberately asks the voicing repository for
 * it. Seventh-chord types are modelled separately in `chordfinder/domain/model/SeventhQuality`,
 * which was introduced in Phase 8.1 when the diatonic chord engine was built.
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

    /** Power chord: root, perfect fifth. No third — neither major nor minor. */
    POWER(intArrayOf(0, 7), "5"),
}
