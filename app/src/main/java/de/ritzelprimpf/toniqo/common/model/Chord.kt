package de.ritzelprimpf.toniqo.common.model

/**
 * A musical chord defined by its root, quality, and constituent notes.
 *
 * The [notes] list is included in the constructor (rather than derived) because chord-voicing
 * choices — open vs. closed, inversions, doublings — are downstream concerns that may legitimately
 * vary for the same root/quality combination. Callers build a [Chord] from the appropriate
 * notes; equality treats two chords as the same iff all three properties match.
 *
 * @property root The chord's root note.
 * @property quality The chord's harmonic [ChordQuality].
 * @property notes The notes that make up the chord, ordered from lowest to highest pitch.
 */
data class Chord(
    val root: Note,
    val quality: ChordQuality,
    val notes: List<Note>,
) {

    /**
     * Returns the standard chord-symbol display for this chord — for example `C`, `Dm`, `Bdim`,
     * `Cmaj7`, `G7`, `Bm7b5`. The format follows common practice: capital root letter, then a
     * quality suffix (empty for major triads, `m` for minor, `dim` for diminished, `aug` for
     * augmented, `maj7` / `m7` / `7` / `m7b5` for the seventh variants).
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    fun displayName(): String = TODO("Not yet implemented")
}
