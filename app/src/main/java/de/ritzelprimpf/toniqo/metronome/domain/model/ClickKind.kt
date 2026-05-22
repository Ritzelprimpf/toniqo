package de.ritzelprimpf.toniqo.metronome.domain.model

/**
 * Classifies a scheduled metronome click by its musical role within the bar.
 *
 * The synthesizer ([de.ritzelprimpf.toniqo.metronome.data.audio.ClickSynthesizer]) maps each
 * kind to a distinct frequency and amplitude so that the three click types are audibly distinct.
 * The mapping from click index to kind is computed by [clickKindFor].
 */
internal enum class ClickKind {
    /** The downbeat (beat 1 of each bar). Highest frequency and amplitude. */
    ACCENTED,

    /** A non-downbeat main beat. Mid frequency and amplitude. */
    STANDARD,

    /** A between-beats subdivision tick. Lowest frequency and amplitude. */
    SUBDIVISION,
}
