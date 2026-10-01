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
 * active [subdivision] and which main beats are accented ([accentedBeats]).
 *
 * Rules (in priority order; accent customization added per `docs/DECISIONS.md` 2026-10-01
 * "per-beat accent customization" entry, superseding the beat-1-only hardcoding in
 * `Phase6-Metronome-Decisions.md` Item 8/11):
 * 1. An index that is **not** a multiple of [Subdivision.multiplier] (a between-beat subdivision
 *    tick) → [ClickKind.SUBDIVISION], always, regardless of [accentedBeats].
 * 2. A main beat (index that is a multiple of the multiplier) whose main-beat index
 *    (`clickIndexInBar / subdivision.multiplier`) is in [accentedBeats] → [ClickKind.ACCENTED].
 * 3. Any other main beat → [ClickKind.STANDARD].
 *
 * Main beats always "win" at collision points — subdivision clicks only fill gaps, and
 * [accentedBeats] only ever retargets which *main* beats are accented, never a subdivision tick.
 *
 * @param clickIndexInBar Zero-based position within the bar. Valid range:
 *   `[0, clicksPerBar(numerator, subdivision))`.
 * @param subdivision Active subdivision; its [Subdivision.multiplier] determines the main-beat
 *   stride.
 * @param accentedBeats Zero-based main-beat indices that should be accented. Defaults to
 *   [MetronomeConfig.DEFAULT_ACCENTED_BEATS] (beat 1 only) — the behavior before accent
 *   customization existed.
 */
internal fun clickKindFor(
    clickIndexInBar: Int,
    subdivision: Subdivision,
    accentedBeats: Set<Int> = MetronomeConfig.DEFAULT_ACCENTED_BEATS,
): ClickKind {
    if (clickIndexInBar % subdivision.multiplier != 0) return ClickKind.SUBDIVISION
    val mainBeatIndex = clickIndexInBar / subdivision.multiplier
    return if (mainBeatIndex in accentedBeats) ClickKind.ACCENTED else ClickKind.STANDARD
}
