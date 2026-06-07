package de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.VoicingLookupResult
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.VoicingRepository
import de.ritzelprimpf.toniqo.common.model.GuitarTuning
import de.ritzelprimpf.toniqo.common.state.SelectedTuningStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the Chord Voicings screen.
 *
 * Reacts to the active tuning from [SelectedTuningStore] and calls [VoicingRepository.lookup]
 * for the chord identified by [chordKey]. Maps the three-tier result
 * ([VoicingLookupResult.Standard], [VoicingLookupResult.UniformOffset],
 * [VoicingLookupResult.Unsupported]) to [ChordVoicingsUiState].
 *
 * For [VoicingLookupResult.Unsupported] (non-uniform or non-6-string tunings), a fallback
 * lookup against [GuitarTuning.STANDARD_6] is performed so the screen can render standard-tuning
 * diagrams with a "shown for standard tuning" indicator.
 *
 * This ViewModel is **not** annotated with `@HiltViewModel` in Phase 8.3 — it has no nav arg
 * wiring yet. Phase 8.5 will convert it to a proper `@HiltViewModel` + `SavedStateHandle` pattern.
 *
 * @param chordKey Identity key used for voicing lookup.
 * @param chordName Full chord symbol for display (e.g. `"Am"`, `"Cmaj7"`).
 * @param chordNoteNames Conventionally-spelled chord tones in degree order; first entry is root.
 * @param voicingRepository Repository providing [VoicingLookupResult]s.
 * @param selectedTuningStore App-scoped source of the active [GuitarTuning].
 */
class ChordVoicingsViewModel(
    private val chordKey: ChordKey,
    chordName: String,
    chordNoteNames: List<String>,
    private val voicingRepository: VoicingRepository,
    private val selectedTuningStore: SelectedTuningStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChordVoicingsUiState(
            chordName = chordName,
            noteNames = chordNoteNames,
            rootNoteName = chordNoteNames.firstOrNull().orEmpty(),
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
            tuningLabel = tuningLabel,
            tier = VoicingTier.STANDARD,
            voicings = result.voicings,
            offsetSemitones = null,
            isLoading = false,
        )

        is VoicingLookupResult.UniformOffset -> prev.copy(
            tuningLabel = tuningLabel,
            tier = VoicingTier.UNIFORM_OFFSET,
            voicings = result.voicings,
            offsetSemitones = result.offsetSemitones,
            isLoading = false,
        )

        is VoicingLookupResult.Unsupported -> {
            // Fallback: look up voicings for standard tuning so the screen can render them
            // with a "shown for standard tuning" indicator (Phase 8.5 presentation).
            val standardResult = withContext(Dispatchers.IO) {
                voicingRepository.lookup(chordKey, GuitarTuning.STANDARD_6)
            }
            val standardVoicings = (standardResult as? VoicingLookupResult.Standard)?.voicings
                ?: emptyList()
            prev.copy(
                tuningLabel = tuningLabel,
                tier = VoicingTier.UNSUPPORTED,
                voicings = standardVoicings,
                offsetSemitones = null,
                isLoading = false,
            )
        }
    }
}
