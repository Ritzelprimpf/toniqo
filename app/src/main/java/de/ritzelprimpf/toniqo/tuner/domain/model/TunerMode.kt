package de.ritzelprimpf.toniqo.tuner.domain.model

/**
 * The active operating mode of the tuner.
 *
 * The mode determines how the target note for each detection cycle is resolved and whether
 * auto-advance is enabled.
 */
enum class TunerMode {
    /**
     * The tuner targets the notes of the selected preset one string at a time, starting from the
     * lowest. Auto-advance fires when the sustained-tone condition is met. After all strings are
     * tuned the ViewModel transitions to [CHROMATIC].
     *
     * This is the default mode when a preset is selected.
     */
    PRESET,

    /**
     * The tuner identifies the target each frame by finding the nearest note to the detected
     * frequency (via [de.ritzelprimpf.toniqo.common.util.MusicTheory.frequencyToNote]). No
     * auto-advance. The user can pluck any string and the tuner figures out which target to
     * compare against.
     *
     * Entered automatically after [ALL_STRINGS_TUNED][TuningStatus.ALL_STRINGS_TUNED], or
     * explicitly via the mode toggle in the UI (Phase 5.4).
     */
    CHROMATIC,
}
