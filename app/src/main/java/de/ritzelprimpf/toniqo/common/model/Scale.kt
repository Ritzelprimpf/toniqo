package de.ritzelprimpf.toniqo.common.model

/**
 * A scale anchored at a root [Note], described by its interval pattern.
 *
 * The [notes] property is a *derived* view of the scale: the result of applying each entry of
 * [intervals] to [root]. It is exposed as a computed property rather than a constructor field
 * so that equality and hash code depend only on `(root, intervals)` — the two values that
 * uniquely identify a scale.
 *
 * @property root The lowest note of the scale (the tonic).
 * @property intervals The cumulative semitone offsets from [root] for each scale degree. The
 *   first entry is the unison; the last entry is typically the octave.
 */
data class Scale(
    val root: Note,
    val intervals: List<Interval>,
) {

    /**
     * The notes that make up this scale, in ascending order from [root]. Each note is obtained
     * by transposing [root] by the corresponding entry of [intervals].
     *
     * Computed on access — not part of equals/hashCode. Throws [NotImplementedError] in Phase 2.
     */
    val notes: List<Note>
        get() = TODO("Not yet implemented")
}
