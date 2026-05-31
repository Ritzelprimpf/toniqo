package de.ritzelprimpf.toniqo.common.util

import de.ritzelprimpf.toniqo.common.model.ScaleType

/**
 * Produces conventionally-spelled note names for scale roots and their seven degrees.
 *
 * Conventional spelling rules:
 * 1. The **root** is taken from [ROOT_DISPLAY_NAMES], a canonical table that chooses the
 *    musically standard accidental for each pitch class (e.g. B♭ not A♯, F♯ not G♭).
 * 2. Each of the **7 degrees** is assigned the next letter cyclically from the root's letter
 *    (A–G, wrapping G→A). The accidental for each degree is the signed semitone difference
 *    between its actual pitch class and the natural pitch class of its assigned letter.
 *
 * This guarantees exactly one of each letter name A–G per scale and correct accidentals for
 * every degree, including exotic cases (e.g. G Altered → G A♭ B♭ C♭ D♭ E♭ F).
 *
 * Stateless; may be used as a top-level object per the `CLAUDE.md` §4 pure-utility exception.
 */
object ScaleSpeller {

    /** Unicode music sharp sign (♯). */
    const val SHARP: String = "♯"

    /** Unicode music flat sign (♭). */
    const val FLAT: String = "♭"

    /** Double-sharp (𝄪). */
    const val DOUBLE_SHARP: String = "𝄪"

    /** Double-flat (two flat signs). */
    const val DOUBLE_FLAT: String = "♭♭"

    /**
     * The seven letter names in C-major ascending order.
     * Index 0 = C, 1 = D, 2 = E, 3 = F, 4 = G, 5 = A, 6 = B.
     */
    private val LETTERS = arrayOf("C", "D", "E", "F", "G", "A", "B")

    /**
     * The natural (unaccidentaled) pitch class for each letter.
     * Parallel to [LETTERS]: C=0, D=2, E=4, F=5, G=7, A=9, B=11.
     */
    private val NATURAL_PCS = intArrayOf(0, 2, 4, 5, 7, 9, 11)

    /** Maps a letter character to its index in [LETTERS]. */
    private val LETTER_INDEX: Map<Char, Int> = mapOf(
        'C' to 0, 'D' to 1, 'E' to 2, 'F' to 3, 'G' to 4, 'A' to 5, 'B' to 6,
    )

    /**
     * Canonical display name for each pitch class (0 = C, 11 = B).
     *
     * Chosen to minimise accidentals across the most common keys and to prefer the
     * musically standard spelling (B♭ not A♯, F♯ not G♭, etc.).
     * This table is the authoritative source for [rootName].
     */
    val ROOT_DISPLAY_NAMES: Array<String> = arrayOf(
        "C",          // 0
        "D♭",    // 1  D♭
        "D",          // 2
        "E♭",    // 3  E♭
        "E",          // 4
        "F",          // 5
        "F♯",    // 6  F♯
        "G",          // 7
        "A♭",    // 8  A♭
        "A",          // 9
        "B♭",    // 10 B♭
        "B",          // 11
    )

    /**
     * Returns the conventional display spelling of the root for the given pitch class.
     *
     * The result comes from [ROOT_DISPLAY_NAMES] and is the same for all scale types
     * (no per-type overrides are required for the current 14-type inventory).
     *
     * @param rootPitchClass Pitch class of the root, 0 (C) through 11 (B).
     * @param type The scale type (unused in the current implementation; reserved for
     *   potential per-family root-spelling overrides in a future phase).
     */
    fun rootName(rootPitchClass: Int, @Suppress("UNUSED_PARAMETER") type: ScaleType): String =
        ROOT_DISPLAY_NAMES[rootPitchClass]

    /**
     * Returns the seven conventionally-spelled note names of the scale, in ascending degree order.
     *
     * Each degree is assigned the next letter name cyclically from the root's letter (A–G,
     * wrapping G→A). The accidental for each degree is computed as the signed semitone
     * difference between the degree's actual pitch class and the natural pitch class of its
     * assigned letter, keeping the result in the range −2…+2.
     *
     * @param rootPitchClass Pitch class of the root, 0–11.
     * @param type The scale type whose [ScaleType.intervalsFromRoot] defines the seven degrees.
     * @return A list of 7 strings, one per degree, each using a unique letter A–G.
     */
    fun scaleNoteNames(rootPitchClass: Int, type: ScaleType): List<String> {
        val rootDisplay = rootName(rootPitchClass, type)
        val rootLetter = rootDisplay[0]
        val rootLetterIndex = LETTER_INDEX[rootLetter]
            ?: error("Unexpected root letter '$rootLetter' from root display '$rootDisplay'")

        return type.intervalsFromRoot.mapIndexed { degree, semitones ->
            val letterIndex = (rootLetterIndex + degree) % 7
            val letter = LETTERS[letterIndex]
            val naturalPc = NATURAL_PCS[letterIndex]
            val actualPc = (rootPitchClass + semitones) % 12
            val diff = signedDiff(actualPc, naturalPc)
            letter + accidental(diff)
        }
    }

    /**
     * Returns the signed semitone difference (actual − natural) in the range −2…+2.
     *
     * Computes (actual − natural) mod 12 then maps values > 6 to their negative equivalent
     * (e.g. 11 → −1, 10 → −2).
     */
    private fun signedDiff(actualPc: Int, naturalPc: Int): Int {
        val raw = (actualPc - naturalPc + 12) % 12
        return if (raw > 6) raw - 12 else raw
    }

    /** Maps a signed semitone difference to its accidental string. */
    private fun accidental(diff: Int): String = when (diff) {
        0 -> ""
        1 -> SHARP
        -1 -> FLAT
        2 -> DOUBLE_SHARP
        -2 -> DOUBLE_FLAT
        else -> error("Unexpected accidental diff $diff — scale requires a triple accidental")
    }
}
