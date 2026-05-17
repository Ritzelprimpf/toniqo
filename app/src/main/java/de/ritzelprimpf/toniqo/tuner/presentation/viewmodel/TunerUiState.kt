package de.ritzelprimpf.toniqo.tuner.presentation.viewmodel

import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset
import de.ritzelprimpf.toniqo.tuner.domain.model.TuningStatus

/**
 * Immutable snapshot of everything the Tuner screen needs to render.
 *
 * Defaults represent the screen's "cold" state: no presets loaded yet, no preset selected, the
 * tuner idle, and no detection results. The ViewModel emits this state on initialisation; the
 * UI uses it to drive its placeholder rendering until real data arrives.
 *
 * @property availablePresets Every preset the user can choose from. Empty until the repository
 *   loads.
 * @property selectedPreset The preset currently active, or `null` if the user has not yet picked
 *   one.
 * @property currentStringIndex The zero-based index into `selectedPreset.notes` of the string the
 *   tuner is currently targeting in sequential mode.
 * @property detectedFrequencyHz The most recent fundamental frequency the detector returned, or
 *   `null` if no usable pitch has been detected yet.
 * @property centsOffTarget Signed cents offset from the current target, or `null` if no
 *   comparison has been made yet.
 * @property status The current [TuningStatus]; defaults to [TuningStatus.IDLE].
 */
data class TunerUiState(
    val availablePresets: List<TunerPreset> = emptyList(),
    val selectedPreset: TunerPreset? = null,
    val currentStringIndex: Int = DEFAULT_STRING_INDEX,
    val detectedFrequencyHz: Double? = null,
    val centsOffTarget: Float? = null,
    val status: TuningStatus = TuningStatus.IDLE,
) {
    companion object {
        /** The string index the tuner starts on in sequential mode (lowest string). */
        const val DEFAULT_STRING_INDEX: Int = 0
    }
}
