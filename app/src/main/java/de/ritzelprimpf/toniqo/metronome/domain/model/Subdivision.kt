package de.ritzelprimpf.toniqo.metronome.domain.model

/**
 * The internal subdivision of each beat — how many quieter clicks layer between the main beats.
 *
 * Subdivision clicks are *additional* to the main beats, not a replacement. The first beat of
 * every measure still receives its accent regardless of subdivision (see `APP_SPECIFICATION.md`
 * Metronome > Terminology).
 *
 * @property multiplier How many clicks this subdivision produces per main beat. A value of 1
 *   means no additional clicks; 2 means one extra click per beat (eighths), etc.
 *   Used by the scheduler to compute click intervals and by [clicksPerBar] for bar-length math.
 */
enum class Subdivision(val multiplier: Int) {
    /** No subdivision — only the main beats click. Multiplier = 1. */
    NONE(1),

    /** Eighth notes — one extra click on the off-beat. Multiplier = 2. */
    EIGHTHS(2),

    /** Sixteenth notes — three extra clicks per beat. Multiplier = 4. */
    SIXTEENTHS(4),

    /** Eighth-note triplets — two extra clicks per beat. Multiplier = 3. */
    TRIPLETS(3),
}
