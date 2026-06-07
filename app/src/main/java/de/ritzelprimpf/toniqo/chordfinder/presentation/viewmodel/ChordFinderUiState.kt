package de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel

import de.ritzelprimpf.toniqo.chordfinder.domain.model.DegreeChord
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.ChordFinderSelection
import de.ritzelprimpf.toniqo.common.model.ScaleType

/**
 * Immutable snapshot of everything the Chord Finder list screen needs to render.
 *
 * @property rootPitchClass Currently selected root pitch class (0 = C … 11 = B).
 * @property scaleType Currently selected scale type.
 * @property includeSeventhChords State of the TRIADS / 7THS toggle.
 * @property spelledRoot Conventionally-spelled root name (e.g. `"A"`, `"D♭"`) via
 *   [de.ritzelprimpf.toniqo.common.util.ScaleSpeller]. Combined with the string resource
 *   for [scaleType.primaryLabelKey] to produce the screen title.
 * @property chords Ordered list of 7 diatonic chords (I … VII) for the current selection.
 *   Empty before the initial DataStore load completes.
 * @property isInitialLoadComplete `false` until the first DataStore emission arrives;
 *   the screen suppresses content until this is `true`.
 */
data class ChordFinderUiState(
    val rootPitchClass: Int = ChordFinderSelection.DEFAULT_ROOT_PITCH_CLASS,
    val scaleType: ScaleType = ChordFinderSelection.DEFAULT_SCALE_TYPE,
    val includeSeventhChords: Boolean = false,
    val spelledRoot: String = DEFAULT_SPELLED_ROOT,
    val chords: List<DegreeChord> = emptyList(),
    val isInitialLoadComplete: Boolean = false,
) {
    companion object {
        private const val DEFAULT_SPELLED_ROOT = "A"
    }
}
