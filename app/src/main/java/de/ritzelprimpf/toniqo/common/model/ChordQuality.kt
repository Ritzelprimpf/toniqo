package de.ritzelprimpf.toniqo.common.model

/**
 * The harmonic quality of a chord — the colour produced by the combination of its intervals.
 *
 * The four triadic qualities cover every diatonic chord built on the seven [Mode]s. The
 * three seventh-chord qualities extend the set when the chord-finder seventh toggle is on.
 */
enum class ChordQuality {
    /** Major triad: root, major third, perfect fifth. */
    MAJOR,

    /** Minor triad: root, minor third, perfect fifth. */
    MINOR,

    /** Diminished triad: root, minor third, diminished fifth (tritone above root). */
    DIMINISHED,

    /** Augmented triad: root, major third, augmented fifth (minor sixth above root). */
    AUGMENTED,

    /** Major seventh chord: major triad with a major seventh (e.g. Cmaj7). */
    MAJOR_SEVENTH,

    /** Minor seventh chord: minor triad with a minor seventh (e.g. Dm7). */
    MINOR_SEVENTH,

    /** Dominant seventh chord: major triad with a minor seventh (e.g. G7). */
    DOMINANT_SEVENTH,

    /** Half-diminished seventh chord: diminished triad with a minor seventh (e.g. Bm7b5). */
    HALF_DIMINISHED_SEVENTH,
}
