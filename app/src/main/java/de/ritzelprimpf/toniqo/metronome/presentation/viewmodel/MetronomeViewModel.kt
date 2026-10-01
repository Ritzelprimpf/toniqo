package de.ritzelprimpf.toniqo.metronome.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ritzelprimpf.toniqo.metronome.data.MetronomePreferences
import de.ritzelprimpf.toniqo.metronome.data.TapTempoCalculator
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig.Companion.BPM_MAX
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig.Companion.BPM_MIN
import de.ritzelprimpf.toniqo.metronome.domain.model.PlayerEvent
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import de.ritzelprimpf.toniqo.metronome.domain.model.tempoDescriptorFor
import de.ritzelprimpf.toniqo.metronome.domain.usecase.StartMetronomeUseCase
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MetronomeViewModel @Inject constructor(
    private val preferences: MetronomePreferences,
    private val startMetronome: StartMetronomeUseCase,
    private val tapTempoCalculator: TapTempoCalculator,
) : ViewModel() {

    /**
     * Bridge between the ViewModel's config state and the running player.
     *
     * Updated immediately on every config change so the player always has the latest values
     * without waiting for the DataStore debounce. Initialised to DEFAULT; the `init` block
     * overwrites it with the persisted value on the first DataStore emission.
     */
    private val configFlow = MutableStateFlow(MetronomeConfig.DEFAULT)

    private val _uiState = MutableStateFlow(MetronomeUiState())
    val uiState: StateFlow<MetronomeUiState> = _uiState.asStateFlow()

    /**
     * One-shot error events. Replay is 0 so subscribers that arrive late do not see stale errors.
     * Extra buffer capacity of 1 ensures [tryEmit] never drops in the failure path where there
     * may be no active subscriber yet.
     */
    private val _events = MutableSharedFlow<MetronomeEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<MetronomeEvent> = _events.asSharedFlow()

    private var playerJob: Job? = null
    private var persistJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.config.collect { persisted ->
                configFlow.value = persisted
                _uiState.update {
                    it.copy(
                        config = persisted,
                        tempoDescriptor = tempoDescriptorFor(persisted.bpm),
                        isInitialLoadComplete = true,
                    )
                }
            }
        }
    }

    fun onPlayToggled() {
        if (_uiState.value.isPlaying) stopPlayback() else startPlayback()
    }

    fun onBpmChanged(newBpm: Int) {
        updateConfig { it.copy(bpm = newBpm.coerceIn(BPM_MIN, BPM_MAX)) }
    }

    fun onBpmIncrement() = onBpmChanged(_uiState.value.config.bpm + 1)

    fun onBpmDecrement() = onBpmChanged(_uiState.value.config.bpm - 1)

    fun onTimeSignatureChanged(numerator: Int, denominator: Int) {
        if (!MetronomeConfig.isSupportedTimeSignature(numerator, denominator)) return
        // A custom accent pattern is sized for the previous bar length and doesn't carry over
        // cleanly to a new one, so it resets to the default (beat 1 only) on every signature
        // change — per the user's own call when this feature was scoped.
        updateConfig {
            it.copy(
                timeSignatureNumerator = numerator,
                timeSignatureDenominator = denominator,
                accentedBeats = MetronomeConfig.DEFAULT_ACCENTED_BEATS,
            )
        }
    }

    fun onSubdivisionChanged(subdivision: Subdivision) {
        updateConfig { it.copy(subdivision = subdivision) }
    }

    /**
     * Toggles whether [beatIndex] (a zero-based main-beat index) is accented. Applies immediately
     * to the running player, same as any other config change (takes effect on that beat's next
     * occurrence).
     */
    fun onBeatAccentToggled(beatIndex: Int) {
        updateConfig { config ->
            val updated = if (beatIndex in config.accentedBeats) {
                config.accentedBeats - beatIndex
            } else {
                config.accentedBeats + beatIndex
            }
            config.copy(accentedBeats = updated)
        }
    }

    fun onTapTempo() {
        tapTempoCalculator.onTap()?.let { newBpm -> onBpmChanged(newBpm) }
    }

    private fun updateConfig(transform: (MetronomeConfig) -> MetronomeConfig) {
        val updated = transform(_uiState.value.config)
        configFlow.value = updated
        _uiState.update { it.copy(config = updated, tempoDescriptor = tempoDescriptorFor(updated.bpm)) }
        schedulePersist(updated)
    }

    private fun schedulePersist(config: MetronomeConfig) {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            preferences.setConfig(config)
        }
    }

    private fun startPlayback() {
        if (playerJob?.isActive == true) return
        _uiState.update { it.copy(isPlaying = true, currentBeat = MetronomeUiState.INITIAL_BEAT) }
        playerJob = viewModelScope.launch {
            startMetronome(initialConfig = configFlow.value, configFlow = configFlow)
                .catch {
                    _events.tryEmit(MetronomeEvent.AudioUnavailable)
                    _uiState.update { it.copy(isPlaying = false) }
                }
                .collect { event ->
                    when (event) {
                        PlayerEvent.Started -> { }
                        is PlayerEvent.BeatTick ->
                            _uiState.update { it.copy(currentBeat = event.beatIndexInBar) }
                        is PlayerEvent.Failed -> {
                            _events.tryEmit(MetronomeEvent.AudioUnavailable)
                            _uiState.update { it.copy(isPlaying = false) }
                        }
                    }
                }
            _uiState.update { it.copy(isPlaying = false) }
        }
    }

    private fun stopPlayback() {
        playerJob?.cancel()
        playerJob = null
        tapTempoCalculator.reset()
        _uiState.update { it.copy(isPlaying = false) }
    }

    override fun onCleared() {
        super.onCleared()
        playerJob?.cancel()
        persistJob?.cancel()
    }

    private companion object {
        const val PERSIST_DEBOUNCE_MS = 200L
    }
}
