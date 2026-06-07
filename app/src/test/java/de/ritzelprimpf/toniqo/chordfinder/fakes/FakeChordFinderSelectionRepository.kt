package de.ritzelprimpf.toniqo.chordfinder.fakes

import de.ritzelprimpf.toniqo.chordfinder.domain.repository.ChordFinderSelection
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.ChordFinderSelectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory test double for [ChordFinderSelectionRepository].
 *
 * [selection] emits [initialSelection] immediately (via [MutableStateFlow]) so the ViewModel's
 * `selection.first()` call resolves without needing to emit anything manually.
 *
 * [saveSelection] updates the stored value and the [selection] flow, mimicking the real DataStore
 * behaviour at the contract level.
 */
class FakeChordFinderSelectionRepository(
    initialSelection: ChordFinderSelection = ChordFinderSelection.DEFAULT,
) : ChordFinderSelectionRepository {

    private val _selection = MutableStateFlow(initialSelection)

    override val selection: Flow<ChordFinderSelection> = _selection.asStateFlow()

    /** Returns the most recently saved selection, or the initial selection if none saved yet. */
    val latestSaved: ChordFinderSelection get() = _selection.value

    override suspend fun saveSelection(selection: ChordFinderSelection) {
        _selection.value = selection
    }
}
