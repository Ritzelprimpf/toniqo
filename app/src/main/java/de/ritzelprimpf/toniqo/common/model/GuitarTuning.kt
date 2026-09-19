package de.ritzelprimpf.toniqo.common.model

/**
 * A guitar tuning: an ordered list of open-string notes from lowest to highest.
 *
 * Placed in `common/` because the Tuner and Chord Finder both consume it (the
 * "promote on second consumer" trigger from Phase 7.2 precedent).
 *
 * @property id Stable identifier used as the JSON key (e.g. `"standard_6"`).
 * @property openNotes Open-string notes, lowest string first.
 */
data class GuitarTuning(
    val id: String,
    val openNotes: List<Note>,
) {

    /** Number of strings; derived from [openNotes]. */
    val stringCount: Int get() = openNotes.size

    /**
     * Returns the uniform semitone offset Δ between this tuning and [base], or `null` if
     * the tunings cannot be related by a single offset.
     *
     * A uniform offset exists when:
     * 1. Both tunings have the same string count, **and**
     * 2. Every string in this tuning is the same number of semitones above (or below) the
     *    corresponding string in [base].
     *
     * The returned value is negative when this tuning is lower than [base] (e.g. E♭ standard
     * returns −1 relative to standard). A value of 0 means the tunings are identical.
     *
     * @param base The reference tuning to compare against.
     * @return The signed semitone offset, or `null` if not applicable.
     */
    fun uniformOffsetFrom(base: GuitarTuning): Int? {
        if (stringCount != base.stringCount) return null
        val firstDelta = openNotes[0].absoluteSemitones - base.openNotes[0].absoluteSemitones
        for (i in 1 until stringCount) {
            val delta = openNotes[i].absoluteSemitones - base.openNotes[i].absoluteSemitones
            if (delta != firstDelta) return null
        }
        return firstDelta
    }

    companion object {

        /**
         * Standard 6-string guitar tuning: E2 A2 D3 G3 B3 E4.
         *
         * This is the reference tuning for the v1 voicing library. All curated voicings are
         * keyed to this tuning; tier-2 (uniform-offset) tunings derive from it by fret shifting.
         */
        val STANDARD_6: GuitarTuning = GuitarTuning(
            id = "standard_6",
            openNotes = listOf(
                Note(NoteName.E, octave = 2),
                Note(NoteName.A, octave = 2),
                Note(NoteName.D, octave = 3),
                Note(NoteName.G, octave = 3),
                Note(NoteName.B, octave = 3),
                Note(NoteName.E, octave = 4),
            ),
        )

        /**
         * Drop D 6-string tuning: D2 A2 D3 G3 B3 E4.
         *
         * The reference tuning for the drop-tuning voicing library
         * (`voicings_drop_d_6.json`). Not a uniform offset of [STANDARD_6] (only the lowest
         * string moves), so it needs its own curated library — but every other 6-string drop
         * tuning (Drop C#, Drop C, Drop B, Drop Bb, Drop A, …) *is* a uniform offset of this one,
         * so they all reach that library via [VoicingRepositoryImpl]'s tier-2 fret-shifting,
         * exactly like Eb/D/C#/C standard reach [STANDARD_6]'s library today.
         */
        val DROP_D_6: GuitarTuning = GuitarTuning(
            id = "drop_d_6",
            openNotes = listOf(
                Note(NoteName.D, octave = 2),
                Note(NoteName.A, octave = 2),
                Note(NoteName.D, octave = 3),
                Note(NoteName.G, octave = 3),
                Note(NoteName.B, octave = 3),
                Note(NoteName.E, octave = 4),
            ),
        )

        /**
         * Standard 7-string guitar tuning: B1 E2 A2 D3 G3 B3 E4 — [STANDARD_6] with a low B added
         * a perfect fourth below the low E.
         *
         * The reference tuning for the (not yet shipped) 7-string voicing library —
         * `voicings_standard_7.json` / `voicings_standard_7_seventh.json`, produced by
         * `tools/voicing-generator/generate_voicings_7.py`. Registered here and in
         * [de.ritzelprimpf.toniqo.chordfinder.data.VoicingRepositoryImpl]'s `FAMILIES` ahead of
         * that asset existing, exactly as [DROP_D_6] was — an unregistered family falls back to
         * an empty voicing list rather than crashing (see `VoicingRepositoryImpl.loadFamily`)
         * until the curated JSON is placed under `assets/chordfinder/`.
         *
         * No Chord Finder UI currently lets a user select this tuning — that's an open product
         * question tracked in `FUTURE_PLANS.md` (FP-3: does 7-string selection live in a
         * Chord-Finder-local picker, or inherit from the Tuner's tuning?), deliberately left
         * unresolved here rather than pre-empted.
         */
        val STANDARD_7: GuitarTuning = GuitarTuning(
            id = "standard_7",
            openNotes = listOf(
                Note(NoteName.B, octave = 1),
                Note(NoteName.E, octave = 2),
                Note(NoteName.A, octave = 2),
                Note(NoteName.D, octave = 3),
                Note(NoteName.G, octave = 3),
                Note(NoteName.B, octave = 3),
                Note(NoteName.E, octave = 4),
            ),
        )
    }
}

/** Absolute semitone index from C0 (C0 = 0, C#0 = 1, …). Used for offset arithmetic. */
private val Note.absoluteSemitones: Int
    get() = octave * Note.SEMITONES_PER_OCTAVE + name.semitonesFromC
