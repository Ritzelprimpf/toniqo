package de.ritzelprimpf.toniqo.common.model

/**
 * A musical chord defined by its root note and harmonic quality.
 *
 * [notes] is a derived property computed from [root] and [quality.intervalsFromRoot]. The chord
 * is always in root position; inversion and voicing choices belong to a higher layer.
 *
 * Equality and hash code depend only on [root] and [quality].
 *
 * @property root The chord's root note.
 * @property quality The chord's harmonic [ChordQuality].
 */
data class Chord(
    val root: Note,
    val quality: ChordQuality,
) {

    /**
     * The notes that make up this chord in root position, from lowest to highest.
     *
     * Derived by adding each entry of [quality.intervalsFromRoot] to [root] chromatically,
     * with octave wrapping when the semitone offset causes the pitch class to cross the C boundary.
     */
    val notes: List<Note> = quality.intervalsFromRoot.map { semitones ->
        val totalSemitonesFromC = root.name.semitonesFromC + semitones
        Note(
            name = NoteName.entries[totalSemitonesFromC % Note.SEMITONES_PER_OCTAVE],
            octave = root.octave + totalSemitonesFromC / Note.SEMITONES_PER_OCTAVE,
        )
    }

    /**
     * Returns the standard chord-symbol display string — for example `"C"`, `"Dm"`, `"Bdim"`,
     * `"Cmaj7"`, `"G7"`, `"Bm7♭5"`. The format is the root pitch class (sharp-spelled, no
     * octave number) followed by [quality.symbol].
     */
    fun displayName(): String = "${root.name.sharpName}${quality.symbol}"
}
