package de.ritzelprimpf.toniqo.metronome.domain.model

/**
 * Returns the total number of clicks per bar for a time signature with the given [numerator] and
 * the given [subdivision] setting.
 *
 * Formula: `numerator * subdivision.multiplier`. Examples:
 * - 4/4 with NONE → 4 clicks (one per main beat)
 * - 4/4 with EIGHTHS → 8 clicks (two per main beat)
 * - 6/8 with TRIPLETS → 18 clicks (three per main beat)
 *
 * See `Phase6-Metronome-Decisions.md` Item 8 for the full subdivision-multiplier model.
 */
internal fun clicksPerBar(numerator: Int, subdivision: Subdivision): Int =
    numerator * subdivision.multiplier

/**
 * Returns the [ClickKind] that should play at [clickIndexInBar] within the current bar, given the
 * active [subdivision].
 *
 * Rules (in priority order, per `Phase6-Metronome-Decisions.md` Item 8):
 * 1. Index 0 (the bar's downbeat) → [ClickKind.ACCENTED].
 * 2. Non-zero index that is a multiple of [Subdivision.multiplier] (a main beat other than beat
 *    1) → [ClickKind.STANDARD].
 * 3. All other indices (between-beat subdivision ticks) → [ClickKind.SUBDIVISION].
 *
 * Main beats always "win" at collision points — subdivision clicks only fill gaps. This means the
 * downbeat is always ACCENTED and all other main beats are always STANDARD, regardless of the
 * subdivision setting.
 *
 * @param clickIndexInBar Zero-based position within the bar. Valid range:
 *   `[0, clicksPerBar(numerator, subdivision))`.
 * @param subdivision Active subdivision; its [Subdivision.multiplier] determines the main-beat
 *   stride.
 */
internal fun clickKindFor(clickIndexInBar: Int, subdivision: Subdivision): ClickKind = when {
    clickIndexInBar == 0 -> ClickKind.ACCENTED
    clickIndexInBar % subdivision.multiplier == 0 -> ClickKind.STANDARD
    else -> ClickKind.SUBDIVISION
}
