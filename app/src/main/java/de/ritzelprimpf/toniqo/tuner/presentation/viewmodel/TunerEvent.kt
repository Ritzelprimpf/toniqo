package de.ritzelprimpf.toniqo.tuner.presentation.viewmodel

/**
 * One-shot side-effect events emitted by [TunerViewModel].
 *
 * Exposed via a `SharedFlow` rather than `StateFlow` because these are single-consumption
 * side-effects (haptic feedback, animations, UI transitions) that do not represent persistent
 * state. `StateFlow`'s replay-1 semantics would re-deliver a stale event after screen rotation,
 * which would spuriously re-trigger haptics or animations.
 *
 * Each event is consumed once by the composable (Phase 5.4) and discarded.
 */
sealed interface TunerEvent {

    /**
     * A string has just been brought in tune (sustained-tone condition satisfied).
     *
     * The UI should trigger haptic feedback and the mint-ring fade-in animation for this string
     * pill (per `DESIGN.md` §8.1).
     *
     * @property stringIndex The zero-based index of the string that was tuned.
     */
    data class StringTuned(val stringIndex: Int) : TunerEvent

    /**
     * Auto-advance just switched the target to a new string, [STRING_LOCK_HOLD_MS][de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerViewModel.STRING_LOCK_HOLD_MS]
     * after [StringTuned] fired for the previous string.
     *
     * Distinct from [StringTuned]: that event marks "this string is now in tune"; this one marks
     * "we moved on to the next string" — the moment the readout well silently re-targets. The UI
     * should trigger a haptic pulse distinct from [StringTuned]'s and animate the active-pill
     * highlight onto the new string, so the advance itself is noticeable, not just the tuning.
     *
     * @property stringIndex The zero-based index of the string auto-advance just moved to.
     */
    data class StringAdvanced(val stringIndex: Int) : TunerEvent

    /**
     * All strings in the current preset have been brought in tune.
     *
     * Drives the 1.2 s success ring animation (`DESIGN.md` §8.1). After 1.2 s the ViewModel
     * transitions to [de.ritzelprimpf.toniqo.tuner.domain.model.TunerMode.CHROMATIC] and emits
     * [EnteredChromaticMode].
     */
    data object AllStringsTuned : TunerEvent

    /**
     * The tuner has transitioned from preset mode into chromatic mode.
     *
     * Emitted both after the all-strings-tuned hold and when the user explicitly taps the
     * chromatic-mode toggle (Phase 5.4). The UI can use this to animate the mode change.
     */
    data object EnteredChromaticMode : TunerEvent
}
