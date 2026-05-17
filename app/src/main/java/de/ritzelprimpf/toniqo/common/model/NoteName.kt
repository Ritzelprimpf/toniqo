package de.ritzelprimpf.toniqo.common.model

/**
 * The twelve pitch classes of the equal-tempered chromatic scale.
 *
 * Enharmonic spellings (`C#` / `Db`, `D#` / `Eb`, etc.) share a single enum entry —
 * the sharp form — because they refer to the same pitch class. The display and parse
 * helpers in the companion object handle both spellings.
 */
enum class NoteName {
    /** The pitch class C, also known as B# in some contexts. */
    C,

    /** The pitch class C# / Db. */
    CSharp,

    /** The pitch class D. */
    D,

    /** The pitch class D# / Eb. */
    DSharp,

    /** The pitch class E, also known as Fb in some contexts. */
    E,

    /** The pitch class F, also known as E# in some contexts. */
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

    /** The pitch class B, also known as Cb in some contexts. */
    B;

    /**
     * The semitone offset of this pitch class relative to C (C = 0, C# = 1, …, B = 11).
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    val semitonesFromC: Int
        get() = TODO("Not yet implemented")

    /**
     * Returns the sharp-form display string for this pitch class (e.g. `C`, `C#`, `D`).
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    fun displaySharp(): String = TODO("Not yet implemented")

    /**
     * Returns the flat-form display string for this pitch class (e.g. `C`, `Db`, `D`).
     *
     * Naturals are returned unchanged; only accidentals differ between the two forms.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    fun displayFlat(): String = TODO("Not yet implemented")

    companion object {
        /**
         * Parses a note-name token (either sharp or flat spelling, case-insensitive) into the
         * corresponding [NoteName]. Recognised tokens include `C`, `C#`, `Db`, `D`, `D#`, `Eb`, …, `B`.
         *
         * Returns `null` for unrecognised input rather than throwing.
         *
         * Throws [NotImplementedError] in Phase 2.
         */
        fun fromString(token: String): NoteName? = TODO("Not yet implemented")
    }
}
