package de.ritzelprimpf.toniqo.common.model

/**
 * The twelve pitch classes of the equal-tempered chromatic scale.
 *
 * Enharmonic spellings (`C#` / `Db`, `D#` / `Eb`, etc.) share a single enum entry —
 * the sharp form — because they refer to the same pitch class. The display and parse
 * helpers in the companion object handle both spellings.
 */
enum class NoteName {
    /** The pitch class C. */
    C,
    /** The pitch class C# / Db. */
    CSharp,
    /** The pitch class D. */
    D,
    /** The pitch class D# / Eb. */
    DSharp,
    /** The pitch class E. */
    E,
    /** The pitch class F. */
    F,
    /** The pitch class F# / Gb. */
    FSharp,
    /** The pitch class G. */
    G,
    /** The pitch class G# / Ab. */
    GSharp,
    /** The pitch class A. */
    A,
    /** The pitch class A# / Bb. */
    ASharp,
    /** The pitch class B. */
    B;

    /** Semitone offset above C (C = 0, C# = 1, …, B = 11). */
    val semitonesFromC: Int
        get() = ordinal

    /**
     * Sharp-spelled display string for this pitch class.
     * Natural notes return their single letter; accidentals use `#` (e.g. `"C#"`, `"F#"`).
     */
    val sharpName: String
        get() = SHARP_NAMES[ordinal]

    /**
     * Flat-spelled display string for this pitch class.
     * Natural notes are identical to [sharpName]; accidentals use `b` (e.g. `"Db"`, `"Gb"`).
     */
    val flatName: String
        get() = FLAT_NAMES[ordinal]

    companion object {
        private val SHARP_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        private val FLAT_NAMES  = arrayOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

        /** Maps every recognised token (both spellings, case-insensitive, trimmed) to a [NoteName]. */
        private val PARSE_MAP: Map<String, NoteName> = buildMap {
            put("C", C)
            put("C#", CSharp); put("DB", CSharp)
            put("D", D)
            put("D#", DSharp); put("EB", DSharp)
            put("E", E)
            put("F", F)
            put("F#", FSharp); put("GB", FSharp)
            put("G", G)
            put("G#", GSharp); put("AB", GSharp)
            put("A", A)
            put("A#", ASharp); put("BB", ASharp)
            put("B", B)
        }

        /**
         * Parses a note-name token (sharp or flat spelling, case-insensitive) into the
         * corresponding [NoteName]. Recognised tokens: `C`, `C#`, `Db`, `D`, `D#`, `Eb`, …, `B`.
         *
         * @return The matching [NoteName], or `null` for unrecognised input.
         */
        fun parse(input: String): NoteName? {
            val normalised = input.trim().uppercase()
            return PARSE_MAP[normalised]
        }
    }
}
