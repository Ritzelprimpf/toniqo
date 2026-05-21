package de.ritzelprimpf.toniqo.tuner.domain.usecase

import de.ritzelprimpf.toniqo.common.model.Note

/**
 * Events emitted by [DetectTunedStringUseCase.execute].
 *
 * The ViewModel maps these events to [de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerUiState]
 * fields. Each event corresponds to a single audio buffer cycle.
 */
sealed interface DetectionEvent {

    /**
     * The microphone is active but no usable fundamental has been detected in this cycle.
     *
     * Emitted when [de.ritzelprimpf.toniqo.tuner.data.CaptureEvent.Listening] is received, or
     * when the pitch detector returns `null` for a buffer. In the null-detector case no event is
     * emitted to the ViewModel; the UI stays on the previous status.
     */
    data object Listening : DetectionEvent

    /**
     * A pitch was successfully detected and compared against the target.
     *
     * @property detectedFrequencyHz The fundamental frequency returned by the detector.
     * @property detectedNote The note nearest to [detectedFrequencyHz] (sharp-spelled), or `null`
     *   if [de.ritzelprimpf.toniqo.common.util.MusicTheory.frequencyToNote] returned `null`.
     * @property targetNote The note the tuner is currently comparing against. In PRESET mode this
     *   is the preset string's note; in CHROMATIC mode it equals [detectedNote] (the nearest note
     *   is the target).
     * @property targetFrequencyHz Equal-tempered frequency (Hz) of [targetNote].
     * @property centsOff Signed cents offset of [detectedFrequencyHz] from [targetFrequencyHz].
     *   Raw (not clamped). Positive = sharp, negative = flat.
     * @property isSustainedInTune `true` iff the sliding 6-element window has ≥ 5 in-tolerance
     *   detections. Only `true` after the window is full — never on the very first detection.
     */
    data class Detection(
        val detectedFrequencyHz: Double,
        val detectedNote: Note?,
        val targetNote: Note,
        val targetFrequencyHz: Double,
        val centsOff: Double,
        val isSustainedInTune: Boolean,
    ) : DetectionEvent

    /**
     * `RECORD_AUDIO` permission is not granted. Terminal — the flow completes after this.
     */
    data object PermissionDenied : DetectionEvent

    /**
     * Audio capture failed for a non-permission reason. Terminal — the flow completes after this.
     *
     * @property reason A human-readable description suitable for Logcat.
     */
    data class Failed(val reason: String) : DetectionEvent
}
