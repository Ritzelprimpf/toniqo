package de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordFinderInput
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.model.DegreeChord
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.ChordFinderSelection
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.ChordFinderSelectionRepository
import de.ritzelprimpf.toniqo.chordfinder.domain.usecase.FindChordsUseCase
import de.ritzelprimpf.toniqo.common.model.ScaleType
import de.ritzelprimpf.toniqo.common.state.LatestKeyResultStore
import de.ritzelprimpf.toniqo.common.util.ScaleSpeller
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Chord Finder list screen.
 *
 * ## Seed algorithm (seed-once, then user-owned)
 *
 * On init, the ViewModel reads the first emission from [ChordFinderSelectionRepository.selection]:
 * 1. If [ChordFinderSelection.hasUserSelection] is `true` — use the persisted values as-is.
 * 2. Else if [LatestKeyResultStore.topResult] is non-null — seed `{root, scaleType}` from the
 *    Key Finder's top result (reads the store **once**; later publishes never override).
 * 3. Else — default to A Aeolian (`pitchClass = 9`, [ScaleType.AEOLIAN]).
 *
 * Once any user intent ([setRoot], [setScaleType], [toggleSevenths]) fires, the selection is
 * saved with `hasUserSelection = true` and the store is permanently ignored.
 *
 * ## Navigation
 *
 * [selectChord] emits a one-shot [ChordNavEvent.NavigateToVoicings] that the UI layer consumes
 * to navigate to the voicings screen (Phase 8.5).
 */
@HiltViewModel
class ChordFinderViewModel @Inject constructor(
    private val findChords: FindChordsUseCase,
    private val selectionRepository: ChordFinderSelectionRepository,
    private val latestKeyResultStore: LatestKeyResultStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChordFinderUiState())

    /** The screen's current UI state. Collect with `collectAsStateWithLifecycle`. */
    val uiState: StateFlow<ChordFinderUiState> = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<ChordNavEvent>(replay = 0, extraBufferCapacity = 1)

    /**
     * One-shot navigation events. The UI layer collects this and calls `navController.navigate()`
     * when it receives a [ChordNavEvent.NavigateToVoicings].
     */
    val navEvents: SharedFlow<ChordNavEvent> = _navEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            val persisted = selectionRepository.selection.first()

            val (rootPc, type, sevenths) = if (persisted.hasUserSelection) {
                Triple(persisted.rootPitchClass, persisted.scaleType, persisted.includeSeventhChords)
            } else {
                val top = latestKeyResultStore.topResult.value
                if (top != null) {
                    Triple(top.candidate.rootPitchClass, top.candidate.type, false)
                } else {
                    Triple(
                        ChordFinderSelection.DEFAULT_ROOT_PITCH_CLASS,
                        ChordFinderSelection.DEFAULT_SCALE_TYPE,
                        false,
                    )
                }
            }

            applySelection(rootPc, type, sevenths, markLoadComplete = true)
        }
    }

    // ── User intents ──────────────────────────────────────────────────────────────

    /** Changes the root pitch class, recomputes the chord list, and persists as user-owned. */
    fun setRoot(rootPitchClass: Int) {
        val current = _uiState.value
        applySelection(rootPitchClass, current.scaleType, current.includeSeventhChords)
        persist(rootPitchClass, current.scaleType, current.includeSeventhChords)
    }

    /** Changes the scale type, recomputes the chord list, and persists as user-owned. */
    fun setScaleType(scaleType: ScaleType) {
        val current = _uiState.value
        applySelection(current.rootPitchClass, scaleType, current.includeSeventhChords)
        persist(current.rootPitchClass, scaleType, current.includeSeventhChords)
    }

    /** Flips the TRIADS / 7THS toggle, recomputes the chord list, and persists as user-owned. */
    fun toggleSevenths() {
        val current = _uiState.value
        val newSevenths = !current.includeSeventhChords
        applySelection(current.rootPitchClass, current.scaleType, newSevenths)
        persist(current.rootPitchClass, current.scaleType, newSevenths)
    }

    /**
     * Emits a [ChordNavEvent.NavigateToVoicings] so the UI layer can navigate to the voicings
     * screen for [degreeChord].
     *
     * The chord root's pitch class is derived from the current state's root plus the scale
     * interval at [DegreeChord.degree] − 1, avoiding any dependency on canonical-name lookup.
     */
    fun selectChord(degreeChord: DegreeChord) {
        val state = _uiState.value
        val chordRootPc = (state.rootPitchClass + state.scaleType.intervalsFromRoot[degreeChord.degree - 1]) % PITCH_CLASSES
        val key = ChordKey(rootPitchClass = chordRootPc, quality = degreeChord.triadQuality)
        viewModelScope.launch {
            _navEvents.emit(ChordNavEvent.NavigateToVoicings(key, degreeChord.symbol, degreeChord.noteNames))
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private fun applySelection(
        rootPitchClass: Int,
        scaleType: ScaleType,
        includeSeventhChords: Boolean,
        markLoadComplete: Boolean = false,
    ) {
        val result = findChords(ChordFinderInput(rootPitchClass, scaleType, includeSeventhChords))
        _uiState.update { prev ->
            prev.copy(
                rootPitchClass = rootPitchClass,
                scaleType = scaleType,
                includeSeventhChords = includeSeventhChords,
                spelledRoot = ScaleSpeller.rootName(rootPitchClass, scaleType),
                chords = result.chords,
                isInitialLoadComplete = if (markLoadComplete) true else prev.isInitialLoadComplete,
            )
        }
    }

    private fun persist(rootPitchClass: Int, scaleType: ScaleType, includeSeventhChords: Boolean) {
        viewModelScope.launch {
            selectionRepository.saveSelection(
                ChordFinderSelection(
                    rootPitchClass = rootPitchClass,
                    scaleType = scaleType,
                    includeSeventhChords = includeSeventhChords,
                    hasUserSelection = true,
                ),
            )
        }
    }

    private companion object {
        const val PITCH_CLASSES = 12
    }
}

/** One-shot navigation event emitted by [ChordFinderViewModel.navEvents]. */
sealed interface ChordNavEvent {
    /**
     * Navigate to the voicings screen for [chordKey].
     *
     * @property chordKey Identity key used to look up voicings.
     * @property chordName Full chord symbol (e.g. `"Am"`, `"Cmaj7"`); used as the screen title.
     * @property noteNames Conventionally-spelled chord tones in degree order; first entry is
     *   the root (highlighted with a mint pill on the voicings screen).
     */
    data class NavigateToVoicings(
        val chordKey: ChordKey,
        val chordName: String,
        val noteNames: List<String>,
    ) : ChordNavEvent
}
