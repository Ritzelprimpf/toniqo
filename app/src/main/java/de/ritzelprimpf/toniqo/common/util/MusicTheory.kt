package de.ritzelprimpf.toniqo.common.util

import de.ritzelprimpf.toniqo.common.model.Chord
import de.ritzelprimpf.toniqo.common.model.Mode
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.Scale

/**
 * Pure, stateless music-theory utilities shared across modules.
 *
 * `MusicTheory` is the **documented exception** to the no-singletons rule (see `CLAUDE.md` §4):
 * it has no state, no I/O, and no platform dependencies, so it is a top-level `object`. If any
 * function here ever needs configuration, state, or platform access, it must be converted to a
 * class and injected.
 */
object MusicTheory {

    /**
     * Converts a [Note] into its equal-tempered fundamental frequency, given a reference pitch.
     *
     * Equivalent to calling [Note.frequencyHz] directly; provided here as a single entry point
     * for code that does not already hold a [Note] instance.
     *
     * @param note The note to convert.
     * @param referencePitchHz The reference frequency of A4. Defaults to 440 Hz; pass 432 Hz
     *   (or any other value) for an alternative tuning standard.
     * @return The fundamental frequency in Hertz.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    fun noteToFrequency(
        note: Note,
        referencePitchHz: Double = Note.DEFAULT_REFERENCE_PITCH_HZ,
    ): Double = TODO("Not yet implemented")

    /**
     * Returns the [Note] whose equal-tempered fundamental frequency is closest to [frequencyHz].
     *
     * The lookup is symmetric to [noteToFrequency]: feeding the output of one into the other
     * recovers the original input (modulo enharmonic spelling) under the same reference pitch.
     *
     * @param frequencyHz The detected frequency in Hertz.
     * @param referencePitchHz The reference frequency of A4. Defaults to 440 Hz.
     * @return The nearest [Note] to [frequencyHz].
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    fun frequencyToNote(
        frequencyHz: Double,
        referencePitchHz: Double = Note.DEFAULT_REFERENCE_PITCH_HZ,
    ): Note = TODO("Not yet implemented")

    /**
     * Builds a [Scale] from a root note and a [Mode] by combining the root with the mode's
     * interval pattern.
     *
     * @param root The tonic of the scale.
     * @param mode The mode whose interval pattern defines the scale.
     * @return The constructed scale.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    fun buildScale(root: Note, mode: Mode): Scale = TODO("Not yet implemented")

    /**
     * Builds the seven diatonic triads of [scale], one per scale degree.
     *
     * The returned list is ordered by scale degree (I, ii, iii, …), and each triad's quality
     * is determined by the intervals between its root, third, and fifth within the scale.
     *
     * @param scale The scale to harmonise.
     * @return The list of triadic chords, ordered by scale degree.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    fun buildTriads(scale: Scale): List<Chord> = TODO("Not yet implemented")

    /**
     * Builds the seven diatonic seventh chords of [scale], one per scale degree.
     *
     * Each chord extends the corresponding triad from [buildTriads] with a seventh drawn from
     * the scale, producing qualities such as `MAJOR_SEVENTH`, `DOMINANT_SEVENTH`,
     * `MINOR_SEVENTH`, and `HALF_DIMINISHED_SEVENTH`.
     *
     * @param scale The scale to harmonise.
     * @return The list of seventh chords, ordered by scale degree.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    fun buildSeventhChords(scale: Scale): List<Chord> = TODO("Not yet implemented")
}
