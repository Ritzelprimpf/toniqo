package de.ritzelprimpf.toniqo.metronome.domain.model

/**
 * The internal subdivision of each beat — how many quieter clicks layer between the main beats.
 *
 * Subdivision clicks are *additional* to the main beats, not a replacement. The first beat of
 * every measure still receives its accent regardless of subdivision (see `APP_SPECIFICATION.md`
 * Metronome > Terminology).
 */
enum class Subdivision {
    /** No subdivision — only the main beats click. */
    NONE,

    /** Eighth notes — one extra click on the off-beat. */
    EIGHTHS,

    /** Sixteenth notes — three extra clicks per beat. */
    SIXTEENTHS,

    /** Eighth-note triplets — two extra clicks per beat. */
    TRIPLETS,
}
