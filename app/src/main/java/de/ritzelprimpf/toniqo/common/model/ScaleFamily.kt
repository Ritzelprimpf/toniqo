package de.ritzelprimpf.toniqo.common.model

/**
 * Categorises the fourteen supported scale types into three parent families.
 *
 * Used by [ScaleType] to group scale types in the Key Finder catalog and to document
 * their theoretical derivation.
 */
enum class ScaleFamily {

    /** The seven Western diatonic modes (Ionian through Locrian). */
    DIATONIC,

    /**
     * Scales derived from, or characteristically associated with, the harmonic minor parent
     * scale. Includes Harmonic Minor itself, Phrygian Dominant (mode 5), and Locrian ♮6 (mode 6).
     */
    HARMONIC_MINOR,

    /**
     * Scales derived from, or characteristically associated with, the melodic minor parent
     * scale. Includes Melodic Minor itself, Lydian Dominant (mode 4), Altered/Super Locrian
     * (mode 7), and Dorian ♭2 (mode 2).
     */
    MELODIC_MINOR,
}
