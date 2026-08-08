package de.ritzelprimpf.toniqo.tuner.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.state.SelectedTuningStore
import de.ritzelprimpf.toniqo.tuner.data.TunerPreferences
import de.ritzelprimpf.toniqo.tuner.data.TuningPresetMapper
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerInput
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerMode
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset
import de.ritzelprimpf.toniqo.tuner.domain.model.TuningStatus
import de.ritzelprimpf.toniqo.tuner.domain.repository.TunerPresetRepository
import de.ritzelprimpf.toniqo.tuner.domain.usecase.DetectTunedStringUseCase
import de.ritzelprimpf.toniqo.tuner.domain.usecase.DetectionEvent
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Tuner screen. Implements [TunerScreenViewModel] so that Compose UI tests
 * can inject a fake without Hilt.
 *
 * Orchestrates the tuner pipeline: preset loading, DataStore persistence, audio capture,
 * pitch detection, sustained-tone state machine, auto-advance, and the preset → chromatic
 * mode transition.
 *
 * ## Pipeline wiring
 *
 * A [MutableStateFlow]`<TunerInput?>` governs what the use case is doing:
 * - When `null` (no preset selected), no use-case collection happens — the mic stays off.
 * - When non-null, [DetectTunedStringUseCase.execute] is collected via [flatMapLatest], so a
 *   new input cancels the previous flow and starts a fresh one with a fresh sustained-tone window.
 *
 * ## Lifecycle
 *
 * [uiState] uses [SharingStarted.WhileSubscribed] with a 5-second grace period so screen
 * rotation does not tear down the audio pipeline.
 *
 * ## Chromatic re-entry
 *
 * When the user explicitly enters chromatic mode via the mode menu, [previousPresetStringIndex]
 * captures the current string index. Tapping "Preset" in the menu restores that index. The
 * snapshot is cleared on preset change, string-pill tap, or the auto-success transition.
 */
@HiltViewModel
class TunerViewModel @Inject constructor(
    private val presetRepository: TunerPresetRepository,
    private val preferences: TunerPreferences,
    private val detectTunedStringUseCase: DetectTunedStringUseCase,
    private val selectedTuningStore: SelectedTuningStore,
) : ViewModel(), TunerScreenViewModel {

    // ── Internal mutable state ────────────────────────────────────────────────────

    private val _state = MutableStateFlow(TunerUiState())
    private val _events = MutableSharedFlow<TunerEvent>()

    /** Current input to the detection pipeline. `null` = mic off. */
    private val tunerInput = MutableStateFlow<TunerInput?>(null)

    /** Job for the in-flight auto-advance hold (STRING_LOCK_HOLD_MS or ALL_TUNED_HOLD_MS). */
    private var autoAdvanceJob: Job? = null

    /**
     * Job that owns the audio pipeline ([tunerInput] → [detectTunedStringUseCase] → events).
     *
     * Kept as a field so it can be cancelled and restarted when the pipeline terminates
     * abnormally (permission denied, capture failure). A plain `viewModelScope.launch`
     * cannot be restarted once its inner flow completes — restarting requires a new job.
     */
    private var detectionJob: Job? = null

    /**
     * Index captured when the user explicitly enters chromatic mode. Restored on user-initiated
     * exit. `null` when chromatic mode was entered automatically (post-success transition) or
     * after the snapshot has been consumed/cleared.
     */
    private var previousPresetStringIndex: Int? = null

    // ── Public API ────────────────────────────────────────────────────────────────

    override val uiState: StateFlow<TunerUiState> = _state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(PIPELINE_GRACE_PERIOD_MS),
            initialValue = TunerUiState(),
        )

    override val events: SharedFlow<TunerEvent> = _events.asSharedFlow()

    // ── Initialization ────────────────────────────────────────────────────────────

    init {
        startDetectionJob()

        // Load presets and restore persisted preferences (runs concurrently with the pipeline).
        viewModelScope.launch {
            val grouped = presetRepository.getPresetsGrouped()
            val savedId = preferences.lastUsedPresetId.first()
            val refHz = preferences.referencePitchHz.first()
            val autoAdvance = preferences.autoAdvanceEnabled.first()
            val hasRequestedPermission = preferences.hasRequestedAudioPermission.first()

            val resolved = (if (savedId != null) presetRepository.getPresetById(savedId) else null)
                ?: presetRepository.getPresetById(DEFAULT_PRESET_ID)

            _state.update { state ->
                state.copy(
                    availablePresets = grouped,
                    selectedPreset = resolved,
                    currentStringIndex = 0,
                    targetNote = resolved?.notes?.getOrNull(0),
                    targetFrequencyHz = resolved?.notes?.getOrNull(0)?.frequencyHz(refHz),
                    status = if (resolved != null) TuningStatus.LISTENING else TuningStatus.IDLE,
                    referencePitchHz = refHz,
                    autoAdvanceEnabled = autoAdvance,
                    hasRequestedAudioPermission = hasRequestedPermission,
                )
            }

            if (resolved != null) {
                tunerInput.value = buildInput(TunerMode.PRESET, resolved.notes[0], refHz)
                selectedTuningStore.publish(TuningPresetMapper.map(resolved), resolved.displayName)
            }
        }

        // Subscribe to live preference changes so the state stays current while the VM is alive.
        viewModelScope.launch {
            combine(
                preferences.autoAdvanceEnabled,
                preferences.referencePitchHz,
                preferences.hasRequestedAudioPermission,
            ) { autoAdvance, refHz, hasPermission ->
                Triple(autoAdvance, refHz, hasPermission)
            }.collect { (autoAdvance, refHz, hasPermission) ->
                _state.update { it.copy(
                    autoAdvanceEnabled = autoAdvance,
                    referencePitchHz = refHz,
                    hasRequestedAudioPermission = hasPermission,
                ) }
            }
        }
    }

    // ── User actions ──────────────────────────────────────────────────────────────

    /**
     * Called when the user selects a preset from the picker sheet.
     *
     * Re-arms preset mode at string 0 with an empty tuned-string set. Cancels any in-flight
     * auto-advance hold. Clears the chromatic-re-entry snapshot.
     */
    override fun onPresetSelected(presetId: String) {
        viewModelScope.launch {
            val preset = presetRepository.getPresetById(presetId) ?: return@launch
            cancelAutoAdvance()
            previousPresetStringIndex = null
            val targetNote = preset.notes[0]
            val refHz = _state.value.referencePitchHz
            _state.update { state ->
                state.copy(
                    mode = TunerMode.PRESET,
                    selectedPreset = preset,
                    currentStringIndex = 0,
                    targetNote = targetNote,
                    targetFrequencyHz = targetNote.frequencyHz(refHz),
                    tunedStringIndices = emptySet(),
                    detectedFrequencyHz = null,
                    detectedNote = null,
                    centsOffTarget = null,
                    status = TuningStatus.LISTENING,
                )
            }
            tunerInput.value = buildInput(TunerMode.PRESET, targetNote, refHz)
            selectedTuningStore.publish(TuningPresetMapper.map(preset), preset.displayName)
            viewModelScope.launch { preferences.setLastUsedPresetId(presetId) }
        }
    }

    /**
     * Called when the user taps a string pill in the selector.
     *
     * Jumps to [stringIndex] in preset mode, resetting the tuned-string set and clearing the
     * chromatic-re-entry snapshot.
     */
    override fun onStringSelected(stringIndex: Int) {
        val preset = _state.value.selectedPreset ?: return
        if (stringIndex < 0 || stringIndex >= preset.notes.size) return
        cancelAutoAdvance()
        previousPresetStringIndex = null
        val targetNote = preset.notes[stringIndex]
        val refHz = _state.value.referencePitchHz
        _state.update { state ->
            state.copy(
                mode = TunerMode.PRESET,
                currentStringIndex = stringIndex,
                targetNote = targetNote,
                targetFrequencyHz = targetNote.frequencyHz(refHz),
                tunedStringIndices = emptySet(),
                detectedFrequencyHz = null,
                detectedNote = null,
                centsOffTarget = null,
                status = TuningStatus.LISTENING,
            )
        }
        tunerInput.value = buildInput(TunerMode.PRESET, targetNote, refHz)
    }

    /**
     * Called when the user taps "Chromatic" in the mode menu.
     *
     * Captures the current string index for later restoration, switches to chromatic mode, and
     * emits [TunerEvent.EnteredChromaticMode].
     */
    override fun onEnterChromaticMode() {
        val current = _state.value
        if (current.mode == TunerMode.CHROMATIC) return
        previousPresetStringIndex = current.currentStringIndex
        _state.update {
            it.copy(
                mode = TunerMode.CHROMATIC,
                tunedStringIndices = emptySet(),
            )
        }
        tunerInput.value = buildInput(TunerMode.CHROMATIC, targetNote = null, current.referencePitchHz)
        emitEvent(TunerEvent.EnteredChromaticMode)
    }

    /**
     * Called when the user taps "Preset" in the mode menu while in chromatic mode.
     *
     * Restores the string index captured in [onEnterChromaticMode] (or 0 if the snapshot is
     * absent). Clears [tunedStringIndices].
     */
    override fun onExitChromaticMode() {
        val current = _state.value
        if (current.mode == TunerMode.PRESET) return
        val restoreIndex = previousPresetStringIndex ?: 0
        previousPresetStringIndex = null
        val preset = current.selectedPreset ?: return
        val targetNote = preset.notes[restoreIndex]
        val refHz = current.referencePitchHz
        _state.update {
            it.copy(
                mode = TunerMode.PRESET,
                currentStringIndex = restoreIndex,
                tunedStringIndices = emptySet(),
                targetNote = targetNote,
                targetFrequencyHz = targetNote.frequencyHz(refHz),
                status = TuningStatus.LISTENING,
                detectedFrequencyHz = null,
                detectedNote = null,
                centsOffTarget = null,
            )
        }
        tunerInput.value = buildInput(TunerMode.PRESET, targetNote, refHz)
    }

    /** Persists the auto-advance [enabled] preference. The combined flow re-emits into uiState. */
    override fun onAutoAdvanceChanged(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoAdvanceEnabled(enabled) }
    }

    /**
     * Persists the reference pitch [hz] and immediately re-emits the current tuner input so
     * target frequencies update live without waiting for the next detection cycle.
     */
    override fun onReferencePitchChanged(hz: Double) {
        viewModelScope.launch { preferences.setReferencePitchHz(hz) }
        val current = _state.value
        val preset = current.selectedPreset
        if (preset != null && current.mode == TunerMode.PRESET) {
            val targetNote = preset.notes[current.currentStringIndex]
            _state.update { it.copy(targetFrequencyHz = targetNote.frequencyHz(hz)) }
            tunerInput.value = buildInput(TunerMode.PRESET, targetNote, hz)
        }
    }

    /**
     * Records that the system permission dialog has been shown and restarts the pipeline.
     *
     * The [MicrophoneAudioSourceImpl] terminates its flow on permission denial, so the pipeline
     * is dead by the time this callback fires. Restarting re-checks the current permission state
     * so the new grant (if any) takes effect immediately.
     */
    override fun onPermissionRequested() {
        viewModelScope.launch { preferences.setHasRequestedAudioPermission(true) }
        startDetectionJob()
    }

    /**
     * Called on each ON_RESUME lifecycle event.
     *
     * Restarts the pipeline when in a terminal error state. Covers the "open app settings,
     * grant RECORD_AUDIO manually, return to the app" path, where the
     * [ActivityResultLauncher] callback never fires.
     */
    override fun onResumed() {
        val status = _state.value.status
        if (status == TuningStatus.PERMISSION_DENIED || status == TuningStatus.CAPTURE_FAILED) {
            startDetectionJob()
        }
    }

    // ── Detection event handler ───────────────────────────────────────────────────

    private fun handleDetectionEvent(event: DetectionEvent) {
        when (event) {
            is DetectionEvent.Listening -> {
                _state.update { state ->
                    // onStringSustainedInTune() sets tunerInput to null to stop the mic during the
                    // ALL_TUNED_HOLD_MS pause, which re-enters this branch via flatMapLatest. Without
                    // this guard, that would immediately downgrade the just-set ALL_STRINGS_TUNED
                    // status back to LISTENING for the whole hold, before the chromatic transition.
                    if (state.selectedPreset == null || state.status == TuningStatus.ALL_STRINGS_TUNED) state
                    else state.copy(
                        status = TuningStatus.LISTENING,
                        detectedFrequencyHz = null,
                        detectedNote = null,
                        centsOffTarget = null,
                    )
                }
            }

            is DetectionEvent.Detection -> {
                val rawCents = event.centsOff
                val newStatus = when {
                    event.isSustainedInTune -> TuningStatus.IN_TUNE
                    rawCents < -IN_TUNE_TOLERANCE_CENTS -> TuningStatus.FLAT
                    rawCents > IN_TUNE_TOLERANCE_CENTS -> TuningStatus.SHARP
                    else -> TuningStatus.LISTENING
                }

                _state.update { state ->
                    state.copy(
                        detectedFrequencyHz = event.detectedFrequencyHz,
                        detectedNote = event.detectedNote,
                        targetNote = event.targetNote,
                        targetFrequencyHz = event.targetFrequencyHz,
                        centsOffTarget = rawCents,
                        status = newStatus,
                    )
                }

                if (event.isSustainedInTune && _state.value.mode == TunerMode.PRESET) {
                    onStringSustainedInTune()
                }
            }

            is DetectionEvent.PermissionDenied -> {
                _state.update { state ->
                    state.copy(
                        status = TuningStatus.PERMISSION_DENIED,
                        detectedFrequencyHz = null,
                        detectedNote = null,
                        centsOffTarget = null,
                    )
                }
            }

            is DetectionEvent.Failed -> {
                _state.update { state ->
                    state.copy(
                        status = TuningStatus.CAPTURE_FAILED,
                        detectedFrequencyHz = null,
                        detectedNote = null,
                        centsOffTarget = null,
                    )
                }
            }
        }
    }

    // ── Auto-advance ──────────────────────────────────────────────────────────────

    private fun onStringSustainedInTune() {
        val currentIndex = _state.value.currentStringIndex
        val preset = _state.value.selectedPreset ?: return

        // Always mark the string tuned and emit the event (haptic fires even when auto-advance is off).
        _state.update { state ->
            state.copy(tunedStringIndices = state.tunedStringIndices + currentIndex)
        }
        emitEvent(TunerEvent.StringTuned(currentIndex))

        cancelAutoAdvance()
        val isLastString = currentIndex >= preset.notes.size - 1

        autoAdvanceJob = viewModelScope.launch {
            delay(STRING_LOCK_HOLD_MS)

            if (isLastString) {
                _state.update { it.copy(status = TuningStatus.ALL_STRINGS_TUNED) }
                emitEvent(TunerEvent.AllStringsTuned)
                tunerInput.value = null

                delay(ALL_TUNED_HOLD_MS)

                // Auto-success transition: clear the chromatic snapshot (success has start-fresh semantics).
                previousPresetStringIndex = null

                val refHz = _state.value.referencePitchHz
                _state.update { state ->
                    state.copy(
                        mode = TunerMode.CHROMATIC,
                        currentStringIndex = 0,
                        targetNote = null,
                        targetFrequencyHz = null,
                        tunedStringIndices = emptySet(),
                        status = TuningStatus.LISTENING,
                        detectedFrequencyHz = null,
                        detectedNote = null,
                        centsOffTarget = null,
                    )
                }
                tunerInput.value = buildInput(TunerMode.CHROMATIC, targetNote = null, refHz)
                emitEvent(TunerEvent.EnteredChromaticMode)
            } else if (_state.value.autoAdvanceEnabled) {
                // Auto-advance is gated on the user preference.
                val nextIndex = currentIndex + 1
                val nextNote = preset.notes[nextIndex]
                val refHz = _state.value.referencePitchHz
                _state.update { state ->
                    state.copy(
                        currentStringIndex = nextIndex,
                        targetNote = nextNote,
                        targetFrequencyHz = nextNote.frequencyHz(refHz),
                        status = TuningStatus.LISTENING,
                        detectedFrequencyHz = null,
                        detectedNote = null,
                        centsOffTarget = null,
                    )
                }
                tunerInput.value = buildInput(TunerMode.PRESET, nextNote, refHz)
                emitEvent(TunerEvent.StringAdvanced(nextIndex))
            }
            // When auto-advance is disabled, the hold fires but does not increment currentStringIndex.
        }
    }

    private fun cancelAutoAdvance() {
        autoAdvanceJob?.cancel()
        autoAdvanceJob = null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /**
     * Cancels any existing pipeline job and starts a new one.
     *
     * The new job subscribes to [tunerInput] via [flatMapLatest], so it immediately begins
     * collecting from [detectTunedStringUseCase.execute] for the current input value (or enters
     * the idle/listening state if [tunerInput] is `null`).
     *
     * Called from [init] (initial start) and from [onPermissionRequested] / [onResumed]
     * (restart after a terminal pipeline failure).
     */
    private fun startDetectionJob() {
        detectionJob?.cancel()
        detectionJob = viewModelScope.launch {
            tunerInput
                .flatMapLatest { input ->
                    if (input == null) flowOf(DetectionEvent.Listening)
                    else detectTunedStringUseCase.execute(input)
                }
                .collect { event -> handleDetectionEvent(event) }
        }
    }

    private fun buildInput(
        mode: TunerMode,
        targetNote: Note?,
        referencePitchHz: Double,
    ) = TunerInput(
        mode = mode,
        targetNote = targetNote,
        referencePitchHz = referencePitchHz,
    )

    private fun emitEvent(event: TunerEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    companion object {
        const val DEFAULT_PRESET_ID = "six_string_standard_e"
        const val STRING_LOCK_HOLD_MS = 200L
        const val ALL_TUNED_HOLD_MS = 1200L
        private const val PIPELINE_GRACE_PERIOD_MS = 5_000L
        private const val IN_TUNE_TOLERANCE_CENTS = 5.0
    }
}
