package de.ritzelprimpf.toniqo.metronome.domain.model

/**
 * The internal subdivision of each beat — how many quieter clicks layer between the main beats.
 *
 * Subdivision clicks are *additional* to the main beats, not a replacement. The first beat of
 * every measure still receives its accent regardless of subdivision (see `APP_SPECIFICATION.md`
 * Metronome > Terminology).
 *
 * [multiplier] is a flat rate multiplier applied identically regardless of time signature — it is
 * deliberately **not** tied to an absolute note value (the enum constant names `EIGHTHS`/
 * `SIXTEENTHS` are historical/internal only, unchanged for persisted-config compatibility; the
 * user-facing labels were relabeled away from absolute note names to "Double (×2)"/"Quadruple
 * (×4)" etc. — see `docs/DECISIONS.md`, 2026-10-02 "subdivision labels now describe a multiplier,
 * not a note value" entry for why: an absolute note name is only correct when the bar's beat unit
 * happens to be a quarter note, and is actively wrong for any other time-signature denominator).
 *
 * @property multiplier How many clicks this subdivision produces per main beat. A value of 1
 *   means no additional clicks; 2 means one extra click per beat, etc.
 *   Used by the scheduler to compute click intervals and by [clicksPerBar] for bar-length math.
 */
enum class Subdivision(val multiplier: Int) {
    /** No subdivision — only the main beats click. Multiplier = 1. */
    NONE(1),

    /** One extra click on the off-beat, doubling the click rate. Multiplier = 2. */
    EIGHTHS(2),

    /** Three extra clicks per beat, quadrupling the click rate. Multiplier = 4. */
    SIXTEENTHS(4),

    /** Triplet feel — two extra clicks per beat, tripling the click rate. Multiplier = 3. */
    TRIPLETS(3),
}
