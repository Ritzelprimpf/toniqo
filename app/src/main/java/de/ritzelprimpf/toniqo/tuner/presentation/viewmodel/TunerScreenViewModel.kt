package de.ritzelprimpf.toniqo.tuner.presentation.viewmodel

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for the ViewModel consumed by [de.ritzelprimpf.toniqo.tuner.presentation.ui.TunerScreen].
 *
 * Extracted as an interface so Compose UI tests can inject a
 * [de.ritzelprimpf.toniqo.tuner.fakes.FakeTunerScreenViewModel] via
 * `createComposeRule().setContent { TunerScreen(viewModel = fake) }` without requiring Hilt.
 *
 * The production implementation is [TunerViewModel].
 */
interface TunerScreenViewModel {

    /** Observable UI state. Collected in the screen via `collectAsStateWithLifecycle()`. */
    val uiState: StateFlow<TunerUiState>

    /**
     * One-shot side-effect events (haptics, animations). Collected via `repeatOnLifecycle(STARTED)`
     * to prevent duplicate haptics on configuration change.
     */
    val events: SharedFlow<TunerEvent>

    /** Called when the user selects a preset from the picker sheet. */
    fun onPresetSelected(presetId: String)

    /** Called when the user taps a string pill in the selector. */
    fun onStringSelected(stringIndex: Int)

    /** Called when the user taps "Chromatic" in the mode popover. */
    fun onEnterChromaticMode()

    /** Called when the user taps "Preset" in the mode popover while in chromatic mode. */
    fun onExitChromaticMode()

    /** Called when the user toggles auto-advance in the settings sheet. */
    fun onAutoAdvanceChanged(enabled: Boolean)

    /** Called when the user changes the reference pitch in the settings sheet. */
    fun onReferencePitchChanged(hz: Double)

    /**
     * Called when the system permission dialog returns a result (granted or denied).
     * Sets [TunerUiState.hasRequestedAudioPermission] to `true` and restarts the pipeline.
     */
    fun onPermissionRequested()

    /**
     * Called when the screen resumes (ON_RESUME lifecycle event).
     *
     * Restarts the detection pipeline if it is in a terminal error state
     * ([TuningStatus.PERMISSION_DENIED] or [TuningStatus.CAPTURE_FAILED]). This handles
     * the case where the user granted `RECORD_AUDIO` via system app settings — that path
     * does not trigger the `ActivityResultLauncher` callback, so a foreground-resume is the
     * earliest signal that permission state may have changed.
     */
    fun onResumed()
}
