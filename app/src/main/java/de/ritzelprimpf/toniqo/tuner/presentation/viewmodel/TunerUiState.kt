package de.ritzelprimpf.toniqo.tuner.presentation.viewmodel

import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerCategory
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerMode
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset
import de.ritzelprimpf.toniqo.tuner.domain.model.TuningStatus

/**
 * Immutable snapshot of everything the Tuner screen needs to render.
 *
 * Redesigned in Phase 5.3 to carry the full pipeline state (mode, detection fields, tuned-string
 * progress). All fields have sensible defaults representing the "cold" state before any audio is
 * captured or any preset is selected.
 *
 * Shape change recorded in `DECISIONS.md`.
 *
 * @property mode Whether the tuner is in sequential preset mode or free chromatic mode.
 * @property availablePresets The full grouped preset catalog; empty until the repository loads.
 * @property selectedPreset The preset currently active, or `null` before the repository loads.
 * @property currentStringIndex Zero-based index into `selectedPreset.notes` for the string being
 *   targeted. Meaningful only in [TunerMode.PRESET]; ignored in [TunerMode.CHROMATIC].
 * @property targetNote The note the tuner is currently comparing against: the preset string in
 *   PRESET mode, or the nearest detected note in CHROMATIC mode. Null when idle.
 * @property targetFrequencyHz Equal-tempered frequency (Hz) of [targetNote]. Null when idle.
 * @property detectedFrequencyHz The most recent fundamental frequency returned by the detector,
 *   or `null` when no usable pitch has been detected.
 * @property detectedNote The note nearest to [detectedFrequencyHz] (sharp-spelled). In PRESET
 *   mode this may differ from [targetNote] (e.g. "you're playing an F# but the target is E").
 *   Null when no pitch is detected.
 * @property centsOffTarget Signed cents offset of [detectedFrequencyHz] from [targetFrequencyHz].
 *   Raw (unclamped), range `[-1200, +1200]`. Null when no detection has been made. The UI is
 *   responsible for visual clamping (needle pegs at ±50 cents).
 * @property status The current [TuningStatus]; defaults to [TuningStatus.IDLE].
 * @property tunedStringIndices String indices that have satisfied the sustained-tune condition in
 *   the current session. Used by the UI to render per-string check marks.
 * @property referencePitchHz Reference frequency of A4 (Hz). Defaults to 440 Hz. The 432 Hz
 *   toggle is exposed via the settings sheet.
 * @property autoAdvanceEnabled Whether the tuner auto-advances to the next string when the current
 *   string is held in tune. Persisted via [de.ritzelprimpf.toniqo.tuner.data.TunerPreferences].
 * @property hasRequestedAudioPermission Whether the system permission dialog has been shown at
 *   least once. Used to distinguish first-launch "never asked" from "permanently denied" when
 *   deciding whether "Grant access" should re-request or open app settings.
 */
data class TunerUiState(
    val mode: TunerMode = TunerMode.PRESET,
    val availablePresets: Map<Int, Map<TunerCategory, List<TunerPreset>>> = emptyMap(),
    val selectedPreset: TunerPreset? = null,
    val currentStringIndex: Int = 0,
    val targetNote: Note? = null,
    val targetFrequencyHz: Double? = null,
    val detectedFrequencyHz: Double? = null,
    val detectedNote: Note? = null,
    val centsOffTarget: Double? = null,
    val status: TuningStatus = TuningStatus.IDLE,
    val tunedStringIndices: Set<Int> = emptySet(),
    val referencePitchHz: Double = DEFAULT_REFERENCE_PITCH_HZ,
    val autoAdvanceEnabled: Boolean = true,
    val hasRequestedAudioPermission: Boolean = false,
) {
    companion object {
        const val DEFAULT_STRING_INDEX: Int = 0
        const val DEFAULT_REFERENCE_PITCH_HZ: Double = 440.0
    }
}
