package de.ritzelprimpf.toniqo.common.model

/**
 * A diatonic scale anchored at a root [Note] and shaped by a [Mode].
 *
 * [notes] is a derived property: the 7 pitches produced by applying [mode.intervalsFromRoot] to
 * [root], with automatic octave wrapping (when a cumulative offset crosses 12, the note rises
 * into the next octave).
 *
 * Equality and hash code depend only on [root] and [mode].
 *
 * @property root The tonic of the scale.
 * @property mode The diatonic mode whose interval pattern defines the scale.
 */
data class Scale(
    val root: Note,
    val mode: Mode,
) {

    /**
     * The 7 notes that make up this scale, in ascending order from [root].
     *
     * Each note is computed by adding the corresponding entry from [mode.intervalsFromRoot] to
     * [root] chromatically, advancing the octave whenever the cumulative semitone offset causes
     * the pitch class to cross the C boundary.
     */
    val notes: List<Note> = mode.intervalsFromRoot.map { semitones ->
        val totalSemitonesFromC = root.name.semitonesFromC + semitones
        Note(
            name = NoteName.entries[totalSemitonesFromC % Note.SEMITONES_PER_OCTAVE],
            octave = root.octave + totalSemitonesFromC / Note.SEMITONES_PER_OCTAVE,
        )
    }
}
