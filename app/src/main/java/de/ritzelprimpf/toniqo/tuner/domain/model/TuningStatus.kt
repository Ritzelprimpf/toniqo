package de.ritzelprimpf.toniqo.tuner.domain.model

/**
 * The state of the tuner with respect to the currently targeted string.
 *
 * The full lifecycle for a sequential-mode tuning session:
 * `IDLE` → (preset selected) → `LISTENING` → `FLAT`/`SHARP` → `IN_TUNE` → … → `ALL_STRINGS_TUNED`.
 * After `ALL_STRINGS_TUNED` the ViewModel automatically transitions to chromatic mode.
 *
 * `PERMISSION_DENIED` and `CAPTURE_FAILED` are error states that halt capture. Added in Phase 5.3
 * so the use case can surface what [de.ritzelprimpf.toniqo.tuner.data.MicrophoneAudioSource]
 * can emit. Recorded in `DECISIONS.md`.
 */
enum class TuningStatus {
    /**
     * No detection is running — either no preset is selected, or the tuner screen has not yet
     * started. UI shows the idle readout placeholder.
     */
    IDLE,

    /**
     * The microphone is active and capture has started, but no usable fundamental frequency has
     * been detected yet. UI shows the "listening" animation and a blank needle.
     */
    LISTENING,

    /**
     * The detected pitch is within ±5 cents of the target **and** the sustained-tone window is
     * satisfied (≥ 5 of the last 6 detections were in tolerance). UI shows the in-tune color
     * (mint) and triggers the auto-advance hold timer.
     */
    IN_TUNE,

    /**
     * The detected pitch is more than 5 cents below the target. UI shows the flat color and
     * deflects the needle left.
     */
    FLAT,

    /**
     * The detected pitch is more than 5 cents above the target. UI shows the sharp color and
     * deflects the needle right.
     */
    SHARP,

    /**
     * Transitional state (~1.2 s) emitted when the last string in the current preset has been
     * brought in tune. UI plays the success animation. The ViewModel transitions to chromatic
     * mode when this hold expires.
     */
    ALL_STRINGS_TUNED,

    /**
     * `RECORD_AUDIO` permission is not granted. UI shows the permission-denied card with a
     * "Grant access" button (implemented in Phase 5.4).
     */
    PERMISSION_DENIED,

    /**
     * Audio capture failed for a non-permission reason (e.g. hardware unavailable). UI shows an
     * error message. The user can retry by tapping a preset or restarting the screen.
     */
    CAPTURE_FAILED,
}
