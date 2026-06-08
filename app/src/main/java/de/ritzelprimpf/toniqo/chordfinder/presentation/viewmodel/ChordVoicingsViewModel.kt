package de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.VoicingLookupResult
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.VoicingRepository
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.common.model.GuitarTuning
import de.ritzelprimpf.toniqo.common.state.SelectedTuningStore
import de.ritzelprimpf.toniqo.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the Chord Voicings screen.
 *
 * Receives [ChordKey] identity via [SavedStateHandle] navigation arguments
 * ([Routes.ARG_ROOT_PC], [Routes.ARG_QUALITY], [Routes.ARG_CHORD_NAME]).
 * Derives chord tone names internally from the key's intervals.
 *
 * Reacts to the active tuning from [SelectedTuningStore] and calls [VoicingRepository.lookup]
 * for the resolved [ChordKey]. Maps the three-tier result to [ChordVoicingsUiState]:
 *
 * - [VoicingLookupResult.Standard]       → [VoicingTier.STANDARD], no offset
 * - [VoicingLookupResult.UniformOffset]  → [VoicingTier.UNIFORM_OFFSET], offset in semitones
 * - [VoicingLookupResult.Unsupported]    → [VoicingTier.UNSUPPORTED], fallback to standard tuning
 */
@HiltViewModel
class ChordVoicingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val voicingRepository: VoicingRepository,
    private val selectedTuningStore: SelectedTuningStore,
) : ViewModel() {

    private val rootPc: Int =
        checkNotNull(savedStateHandle[Routes.ARG_ROOT_PC]) { "Missing nav arg: ${Routes.ARG_ROOT_PC}" }

    private val quality: ChordQuality =
        ChordQuality.valueOf(
            checkNotNull(savedStateHandle[Routes.ARG_QUALITY]) { "Missing nav arg: ${Routes.ARG_QUALITY}" }
        )

    private val chordName: String =
        checkNotNull(savedStateHandle[Routes.ARG_CHORD_NAME]) { "Missing nav arg: ${Routes.ARG_CHORD_NAME}" }

    private val chordKey = ChordKey(rootPitchClass = rootPc, quality = quality)

    private val noteNames: List<String> = deriveNoteNames(rootPc, quality)

    private val _uiState = MutableStateFlow(
        ChordVoicingsUiState(
            chordName     = chordName,
            noteNames     = noteNames,
            rootNoteName  = noteNames.firstOrNull().orEmpty(),
        ),
    )

    /** The screen's current UI state. Collect with `collectAsStateWithLifecycle`. */
    val uiState: StateFlow<ChordVoicingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            selectedTuningStore.selection.collect { (tuning, label) ->
                val result = withContext(Dispatchers.IO) {
                    voicingRepository.lookup(chordKey, tuning)
                }
                _uiState.update { prev -> mapResult(prev, result, label) }
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private suspend fun mapResult(
        prev: ChordVoicingsUiState,
        result: VoicingLookupResult,
        tuningLabel: String,
    ): ChordVoicingsUiState = when (result) {
        is VoicingLookupResult.Standard -> prev.copy(
            tuningLabel      = tuningLabel,
            tier             = VoicingTier.STANDARD,
            voicings         = result.voicings,
            offsetSemitones  = null,
            isLoading        = false,
        )

        is VoicingLookupResult.UniformOffset -> prev.copy(
            tuningLabel      = tuningLabel,
            tier             = VoicingTier.UNIFORM_OFFSET,
            voicings         = result.voicings,
            offsetSemitones  = result.offsetSemitones,
            isLoading        = false,
        )

        is VoicingLookupResult.Unsupported -> {
            val standardResult = withContext(Dispatchers.IO) {
                voicingRepository.lookup(chordKey, GuitarTuning.STANDARD_6)
            }
            val fallbackVoicings = (standardResult as? VoicingLookupResult.Standard)?.voicings
                ?: emptyList()
            prev.copy(
                tuningLabel      = tuningLabel,
                tier             = VoicingTier.UNSUPPORTED,
                voicings         = fallbackVoicings,
                offsetSemitones  = null,
                isLoading        = false,
            )
        }
    }

    companion object {
        private val NOTE_NAMES = arrayOf(
            "C", "D♭", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B",
        )

        /** Derives spelled chord-tone names (root → last interval) from [rootPitchClass] + [quality]. */
        internal fun deriveNoteNames(rootPitchClass: Int, quality: ChordQuality): List<String> =
            quality.intervalsFromRoot.map { interval ->
                NOTE_NAMES[(rootPitchClass + interval) % 12]
            }
    }
}
