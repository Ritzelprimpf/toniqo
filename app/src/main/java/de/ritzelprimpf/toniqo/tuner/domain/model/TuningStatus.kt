package de.ritzelprimpf.toniqo.tuner.domain.model

/**
 * The state of the tuner with respect to the currently targeted string.
 *
 * `ALL_STRINGS_TUNED` is included for completeness — sequential mode emits it after the final
 * string is brought in tune so the UI can play its success animation. Per the 2026-05-17 decision
 * in `DECISIONS.md`, it is *not* a terminal state: the tuner transitions into chromatic mode
 * immediately afterwards. Phase 2 only defines the value; the transition behaviour is implemented
 * in Phase 5.3.
 */
enum class TuningStatus {
    /** No detection is running (e.g. before the user grants microphone permission). */
    IDLE,

    /** The microphone is active and no usable pitch has been detected yet. */
    LISTENING,

    /** The detected pitch is within tolerance of the target for the required hold duration. */
    IN_TUNE,

    /** The detected pitch is below the target. */
    FLAT,

    /** The detected pitch is above the target. */
    SHARP,

    /** Every string in the current preset has been brought in tune at least once. */
    ALL_STRINGS_TUNED,
}
