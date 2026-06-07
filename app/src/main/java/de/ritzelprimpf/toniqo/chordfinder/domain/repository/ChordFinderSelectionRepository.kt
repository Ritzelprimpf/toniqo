package de.ritzelprimpf.toniqo.chordfinder.domain.repository

import de.ritzelprimpf.toniqo.common.model.ScaleType
import kotlinx.coroutines.flow.Flow

/**
 * Persists and restores the user's Chord Finder root/mode/seventh selection.
 *
 * The [hasUserSelection] flag distinguishes a seeded selection (from Key Finder or the A-Aeolian
 * fallback) from a user-owned selection. Once set to `true`, subsequent Key Finder result publishes
 * no longer override the persisted values.
 */
interface ChordFinderSelectionRepository {

    /**
     * A [Flow] that emits the persisted selection. Emits [ChordFinderSelection.DEFAULT] on
     * first launch or when the stored data is invalid.
     */
    val selection: Flow<ChordFinderSelection>

    /**
     * Writes [selection] to the backing store.
     *
     * Calls are debounced internally when warranted (mirrors the metronome's 200 ms pattern).
     * The new value is reflected in [selection] on the next emission.
     */
    suspend fun saveSelection(selection: ChordFinderSelection)
}

/**
 * The persisted Chord Finder selection snapshot.
 *
 * @property rootPitchClass Scale root pitch class, 0 (C) through 11 (B).
 * @property scaleType The selected scale type.
 * @property includeSeventhChords Whether the seventh-chord toggle is on.
 * @property hasUserSelection `true` once the user has made at least one explicit change; `false`
 *   on first launch, allowing the seed algorithm to override from Key Finder.
 */
data class ChordFinderSelection(
    val rootPitchClass: Int,
    val scaleType: ScaleType,
    val includeSeventhChords: Boolean,
    val hasUserSelection: Boolean,
) {
    companion object {
        val DEFAULT_SCALE_TYPE = ScaleType.AEOLIAN

        /** A Aeolian, triads, no user selection — the seed fallback. */
        val DEFAULT = ChordFinderSelection(
            rootPitchClass = DEFAULT_ROOT_PITCH_CLASS,
            scaleType = DEFAULT_SCALE_TYPE,
            includeSeventhChords = false,
            hasUserSelection = false,
        )

        const val DEFAULT_ROOT_PITCH_CLASS = 9   // A
    }
}
