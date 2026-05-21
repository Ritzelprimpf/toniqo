package de.ritzelprimpf.toniqo.tuner.domain.model

import de.ritzelprimpf.toniqo.common.model.Note

/**
 * The configuration passed to [de.ritzelprimpf.toniqo.tuner.domain.usecase.DetectTunedStringUseCase]
 * for a single detection session.
 *
 * Each call to `execute(input)` returns a fresh flow with a fresh sustained-tone window.
 * Changing any field (e.g. switching strings or entering chromatic mode) means creating a new
 * [TunerInput] and restarting the use case.
 *
 * @property mode Determines how the target note is resolved each frame.
 * @property targetNote The note to compare against in [TunerMode.PRESET] mode. **Must be
 *   non-null when `mode == PRESET`; must be `null` when `mode == CHROMATIC`** (the use case
 *   resolves the chromatic target per-frame). Violating this contract is a programming error;
 *   the use case will throw if a null `targetNote` is provided in PRESET mode.
 * @property referencePitchHz The reference frequency for A4 in Hertz. Used both for computing
 *   note frequencies (via [de.ritzelprimpf.toniqo.common.util.MusicTheory.frequencyToNote]) and
 *   for computing cents offsets. Defaults to the international standard, 440 Hz.
 */
data class TunerInput(
    val mode: TunerMode,
    val targetNote: Note?,
    val referencePitchHz: Double,
)
