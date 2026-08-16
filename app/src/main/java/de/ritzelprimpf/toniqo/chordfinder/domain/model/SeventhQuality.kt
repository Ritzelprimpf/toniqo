package de.ritzelprimpf.toniqo.chordfinder.domain.model

import androidx.annotation.StringRes
import de.ritzelprimpf.toniqo.R

/**
 * The seven seventh-chord types that the 14 [de.ritzelprimpf.toniqo.common.model.ScaleType]s
 * can produce via diatonic harmonisation.
 *
 * Each entry exposes:
 * - [suffixKey] — a string-resource ID for the chord-symbol suffix (e.g. "maj7"), for use in
 *   Composables via `stringResource(seventhQuality.suffixKey)`.
 * - [suffix] — the same suffix text as a compile-time constant, for use in pure-Kotlin contexts
 *   (e.g. building [de.ritzelprimpf.toniqo.chordfinder.domain.model.DegreeChord.symbol] inside
 *   [de.ritzelprimpf.toniqo.chordfinder.domain.usecase.FindChordsUseCase]).
 *
 * Both fields must always contain the same text; [suffix] is the authoritative source — the
 * strings.xml entry mirrors it so the values can never diverge in practice.
 *
 * @property suffixKey String-resource ID for the chord-symbol suffix.
 * @property suffix Chord-symbol suffix as a compile-time constant.
 */
enum class SeventhQuality(
    @StringRes val suffixKey: Int,
    val suffix: String,
    val semitonesFromRoot: Int,
) {
    /** Major seventh: major triad with a major seventh (e.g. Cmaj7). */
    MAJOR_SEVENTH(R.string.cf_suffix_maj7, "maj7", semitonesFromRoot = 11),

    /** Minor seventh: minor triad with a minor seventh (e.g. Dm7). */
    MINOR_SEVENTH(R.string.cf_suffix_m7, "m7", semitonesFromRoot = 10),

    /** Dominant seventh: major triad with a minor seventh (e.g. G7). */
    DOMINANT_SEVENTH(R.string.cf_suffix_dom7, "7", semitonesFromRoot = 10),

    /** Half-diminished (minor seventh flat five): diminished triad with a minor seventh (e.g. Bm7♭5). */
    HALF_DIMINISHED(R.string.cf_suffix_m7b5, "m7♭5", semitonesFromRoot = 10),

    /** Diminished seventh: diminished triad with a diminished seventh (e.g. G♯dim7). */
    DIMINISHED_SEVENTH(R.string.cf_suffix_dim7, "dim7", semitonesFromRoot = 9),

    /** Minor-major seventh: minor triad with a major seventh (e.g. AmMaj7). Appears in harmonic minor. */
    MINOR_MAJOR_SEVENTH(R.string.cf_suffix_mmaj7, "mMaj7", semitonesFromRoot = 11),

    /** Augmented-major seventh: augmented triad with a major seventh (e.g. Cmaj7♯5). Appears in harmonic and melodic minor. */
    AUGMENTED_MAJOR_SEVENTH(R.string.cf_suffix_maj7s5, "maj7♯5", semitonesFromRoot = 11),
}
