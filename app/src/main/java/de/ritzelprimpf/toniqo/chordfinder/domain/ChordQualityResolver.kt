package de.ritzelprimpf.toniqo.chordfinder.domain

import de.ritzelprimpf.toniqo.chordfinder.domain.model.SeventhQuality
import de.ritzelprimpf.toniqo.common.model.ChordQuality

/**
 * Maps diatonic interval distances to triad and seventh-chord qualities.
 *
 * All logic is pure and stateless. Interval inputs are semitone distances above the chord root,
 * each in the range 0–11 (i.e. already reduced mod 12). Any combination outside the valid sets
 * indicates a programming error in the calling scale data and is treated as such — an
 * [IllegalArgumentException] is thrown rather than silently returning a default.
 *
 * This is the documented pure-utility exception to the no-singleton rule (`CLAUDE.md` §4): it
 * has no state, no I/O, and no Android dependencies.
 */
object ChordQualityResolver {

    // ── Triad interval constants ──────────────────────────────────────────────────

    private const val MAJOR_THIRD_SEMITONES = 4
    private const val MINOR_THIRD_SEMITONES = 3
    private const val PERFECT_FIFTH_SEMITONES = 7
    private const val DIMINISHED_FIFTH_SEMITONES = 6
    private const val AUGMENTED_FIFTH_SEMITONES = 8

    // ── Seventh interval constants ────────────────────────────────────────────────

    private const val MAJOR_SEVENTH_SEMITONES = 11
    private const val MINOR_SEVENTH_SEMITONES = 10
    private const val DIMINISHED_SEVENTH_SEMITONES = 9

    // ── Public API ────────────────────────────────────────────────────────────────

    /**
     * Resolves a triad quality from the intervals of its third and fifth above the root.
     *
     * Valid combinations and their results:
     * - (4, 7) → [ChordQuality.MAJOR]
     * - (3, 7) → [ChordQuality.MINOR]
     * - (3, 6) → [ChordQuality.DIMINISHED]
     * - (4, 8) → [ChordQuality.AUGMENTED]
     *
     * @param thirdInterval Semitones from root to the third (0–11).
     * @param fifthInterval Semitones from root to the fifth (0–11).
     * @return The resolved [ChordQuality].
     * @throws IllegalArgumentException if the combination is not one of the four valid triads.
     */
    fun triad(thirdInterval: Int, fifthInterval: Int): ChordQuality = when {
        thirdInterval == MAJOR_THIRD_SEMITONES && fifthInterval == PERFECT_FIFTH_SEMITONES ->
            ChordQuality.MAJOR
        thirdInterval == MINOR_THIRD_SEMITONES && fifthInterval == PERFECT_FIFTH_SEMITONES ->
            ChordQuality.MINOR
        thirdInterval == MINOR_THIRD_SEMITONES && fifthInterval == DIMINISHED_FIFTH_SEMITONES ->
            ChordQuality.DIMINISHED
        thirdInterval == MAJOR_THIRD_SEMITONES && fifthInterval == AUGMENTED_FIFTH_SEMITONES ->
            ChordQuality.AUGMENTED
        else -> throw IllegalArgumentException(
            "Invalid triad interval pair: third=$thirdInterval, fifth=$fifthInterval. " +
                "Expected one of (4,7)=MAJOR, (3,7)=MINOR, (3,6)=DIMINISHED, (4,8)=AUGMENTED.",
        )
    }

    /**
     * Resolves a seventh-chord quality from the triad quality and the seventh interval above the root.
     *
     * Valid combinations and their results:
     * - (MAJOR, 11)     → [SeventhQuality.MAJOR_SEVENTH]
     * - (MINOR, 10)     → [SeventhQuality.MINOR_SEVENTH]
     * - (MAJOR, 10)     → [SeventhQuality.DOMINANT_SEVENTH]
     * - (DIMINISHED, 10)→ [SeventhQuality.HALF_DIMINISHED]
     * - (DIMINISHED, 9) → [SeventhQuality.DIMINISHED_SEVENTH]
     * - (MINOR, 11)     → [SeventhQuality.MINOR_MAJOR_SEVENTH]
     * - (AUGMENTED, 11) → [SeventhQuality.AUGMENTED_MAJOR_SEVENTH]
     *
     * @param triad The triad quality of the chord.
     * @param seventhInterval Semitones from root to the seventh (0–11).
     * @return The resolved [SeventhQuality].
     * @throws IllegalArgumentException if the combination is not one of the seven valid types.
     */
    fun seventh(triad: ChordQuality, seventhInterval: Int): SeventhQuality = when {
        triad == ChordQuality.MAJOR && seventhInterval == MAJOR_SEVENTH_SEMITONES ->
            SeventhQuality.MAJOR_SEVENTH
        triad == ChordQuality.MINOR && seventhInterval == MINOR_SEVENTH_SEMITONES ->
            SeventhQuality.MINOR_SEVENTH
        triad == ChordQuality.MAJOR && seventhInterval == MINOR_SEVENTH_SEMITONES ->
            SeventhQuality.DOMINANT_SEVENTH
        triad == ChordQuality.DIMINISHED && seventhInterval == MINOR_SEVENTH_SEMITONES ->
            SeventhQuality.HALF_DIMINISHED
        triad == ChordQuality.DIMINISHED && seventhInterval == DIMINISHED_SEVENTH_SEMITONES ->
            SeventhQuality.DIMINISHED_SEVENTH
        triad == ChordQuality.MINOR && seventhInterval == MAJOR_SEVENTH_SEMITONES ->
            SeventhQuality.MINOR_MAJOR_SEVENTH
        triad == ChordQuality.AUGMENTED && seventhInterval == MAJOR_SEVENTH_SEMITONES ->
            SeventhQuality.AUGMENTED_MAJOR_SEVENTH
        else -> throw IllegalArgumentException(
            "Invalid seventh-chord combination: triad=$triad, seventh=$seventhInterval. " +
                "Expected one of: (MAJOR,11), (MINOR,10), (MAJOR,10), (DIMINISHED,10), " +
                "(DIMINISHED,9), (MINOR,11), (AUGMENTED,11).",
        )
    }
}
