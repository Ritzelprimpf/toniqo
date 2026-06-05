package de.ritzelprimpf.toniqo.common.util

import de.ritzelprimpf.toniqo.common.model.Chord
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.common.model.Mode
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import de.ritzelprimpf.toniqo.common.model.Scale
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * Pure, stateless music-theory utilities shared across modules.
 *
 * `MusicTheory` is the **documented exception** to the no-singletons rule (see `CLAUDE.md` §4):
 * it has no state, no I/O, and no platform dependencies. If any function here ever needs
 * configuration, state, or platform access, it must be converted to a class and injected.
 */
object MusicTheory {

    // ── Constants ────────────────────────────────────────────────────────────────

    private const val SEMITONES_PER_OCTAVE = 12
    private const val CENTS_PER_OCTAVE = 1200.0
    private const val A4_SEMITONES_FROM_C4 = 9  // A is 9 semitones above C in the same octave
    private const val A4_OCTAVE = 4
    private const val MIN_VALID_OCTAVE = 0
    private const val MAX_VALID_OCTAVE = 9

    // ── Frequency ────────────────────────────────────────────────────────────────

    /**
     * Converts a [Note] into its equal-tempered fundamental frequency.
     *
     * Equivalent to calling [Note.frequencyHz] directly; provided here as a single entry point
     * for callers working at the `MusicTheory` level.
     *
     * @param note The note to convert.
     * @param referencePitchHz Reference frequency for A4. Defaults to 440 Hz.
     * @return The fundamental frequency in Hertz.
     */
    fun noteToFrequency(
        note: Note,
        referencePitchHz: Double = Note.DEFAULT_REFERENCE_PITCH_HZ,
    ): Double = note.frequencyHz(referencePitchHz)

    /**
     * Returns the [Note] whose equal-tempered frequency is nearest to [frequencyHz], using
     * sharp spelling for accidentals (e.g. `C#4` rather than `Db4`).
     *
     * Returns `null` when:
     * - [frequencyHz] is ≤ 0, NaN, or infinite.
     * - The computed pitch falls outside the range C0–B9 (covers all musically relevant guitar
     *   pitches and far beyond; filters non-musical input).
     *
     * @param frequencyHz The detected frequency in Hertz.
     * @param referencePitchHz Reference frequency for A4. Defaults to 440 Hz.
     * @return The nearest [Note] (sharp-spelled), or `null` for invalid input.
     */
    fun frequencyToNote(
        frequencyHz: Double,
        referencePitchHz: Double = Note.DEFAULT_REFERENCE_PITCH_HZ,
    ): Note? {
        if (frequencyHz <= 0.0 || frequencyHz.isNaN() || frequencyHz.isInfinite()) return null

        // Semitone offset from A4 (can be negative for notes below A4).
        val semitonesFromA4 = (SEMITONES_PER_OCTAVE * log2(frequencyHz / referencePitchHz))
            .roundToInt()

        // Convert to semitones-from-C4, then split into octave and pitch-class index.
        val semitonesFromC4 = semitonesFromA4 + A4_SEMITONES_FROM_C4
        val octave = A4_OCTAVE + Math.floorDiv(semitonesFromC4, SEMITONES_PER_OCTAVE)
        val noteIndex = Math.floorMod(semitonesFromC4, SEMITONES_PER_OCTAVE)

        if (octave < MIN_VALID_OCTAVE || octave > MAX_VALID_OCTAVE) return null

        return Note(name = NoteName.entries[noteIndex], octave = octave)
    }

    // ── Cents ────────────────────────────────────────────────────────────────────

    /**
     * Computes the cents offset of [detectedFrequencyHz] relative to [referenceFrequencyHz].
     *
     * Formula: `1200 × log₂(detected / reference)`.
     * Positive values mean the detected pitch is sharp; negative values mean flat.
     *
     * @param referenceFrequencyHz The target (in-tune) frequency.
     * @param detectedFrequencyHz The measured frequency.
     * @return Signed cents offset.
     */
    fun centsBetween(referenceFrequencyHz: Double, detectedFrequencyHz: Double): Double =
        CENTS_PER_OCTAVE * log2(detectedFrequencyHz / referenceFrequencyHz)

    // ── Scale & Chord Builders ────────────────────────────────────────────────────

    /**
     * Builds a [Scale] from a root note and a [Mode].
     *
     * This is a thin factory that delegates to `Scale(root, mode)`. It exists so callers in the
     * presentation and use-case layers depend on this utility rather than the data class directly.
     *
     * @param root The tonic of the scale.
     * @param mode The mode whose interval pattern defines the scale.
     * @return The constructed scale; `scale.notes` is already computed.
     */
    fun buildScale(root: Note, mode: Mode): Scale = Scale(root, mode)

    /**
     * Builds the seven diatonic triads of [scale], one per scale degree, in ascending order.
     *
     * For each degree the triad quality is determined by counting the semitones between
     * root → third and third → fifth (both drawn from adjacent scale degrees):
     * - 4+3 → MAJOR, 3+4 → MINOR, 3+3 → DIMINISHED, 4+4 → AUGMENTED.
     *
     * @param scale The scale to harmonise.
     * @return 7 [Chord]s in scale-degree order (I, ii, iii, …).
     */
    fun buildTriads(scale: Scale): List<Chord> {
        val notes = scale.notes
        return List(DIATONIC_DEGREE_COUNT) { i ->
            val gaps = computeGaps(notes, i, toneCount = TRIAD_TONE_COUNT)
            Chord(root = notes[i], quality = classifyTriad(gaps))
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private const val DIATONIC_DEGREE_COUNT = 7
    private const val TRIAD_TONE_COUNT = 3

    /**
     * Returns the absolute semitone position of [note] above C0 (C0 = 0, C#0 = 1, …).
     * Used for gap arithmetic so octave wrapping is handled automatically.
     */
    private fun absoluteSemitones(note: Note): Int =
        note.octave * SEMITONES_PER_OCTAVE + note.name.semitonesFromC

    /**
     * Computes the semitone gaps between consecutive chord tones rooted at scale degree [degree].
     *
     * Chord tones are drawn from scale degree i, i+2, i+4, … (every other scale note). When the
     * index wraps past the 7th degree, the corresponding note is placed one octave above its
     * position in [notes] by adding 12 whenever the absolute pitch would otherwise be ≤ the
     * previous chord tone.
     *
     * @param notes The 7 scale notes in ascending order.
     * @param degree The chord's root scale degree (0-indexed).
     * @param toneCount Number of chord tones (3 for triads, 4 for seventh chords).
     * @return Array of `toneCount - 1` semitone gaps between consecutive chord tones.
     */
    private fun computeGaps(notes: List<Note>, degree: Int, toneCount: Int): IntArray {
        // Gather the absolute semitone position for each chord tone, adjusting octaves.
        var prevAbs = absoluteSemitones(notes[degree])
        val absolutePositions = IntArray(toneCount) { k ->
            if (k == 0) {
                prevAbs
            } else {
                var abs = absoluteSemitones(notes[(degree + k * 2) % DIATONIC_DEGREE_COUNT])
                if (abs <= prevAbs) abs += SEMITONES_PER_OCTAVE
                prevAbs = abs
                abs
            }
        }
        return IntArray(toneCount - 1) { k -> absolutePositions[k + 1] - absolutePositions[k] }
    }

    private fun classifyTriad(gaps: IntArray): ChordQuality = when {
        gaps[0] == 4 && gaps[1] == 3 -> ChordQuality.MAJOR
        gaps[0] == 3 && gaps[1] == 4 -> ChordQuality.MINOR
        gaps[0] == 3 && gaps[1] == 3 -> ChordQuality.DIMINISHED
        gaps[0] == 4 && gaps[1] == 4 -> ChordQuality.AUGMENTED
        else -> error("Unexpected triad interval pattern: ${gaps.toList()}")
    }
}
