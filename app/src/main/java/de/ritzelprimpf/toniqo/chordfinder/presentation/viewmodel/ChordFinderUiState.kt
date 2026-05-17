package de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel

import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordFinderResult
import de.ritzelprimpf.toniqo.common.model.Mode
import de.ritzelprimpf.toniqo.common.model.Note

/**
 * Immutable snapshot of everything the Chord Finder screen needs to render.
 *
 * Defaults represent the screen's empty state: no root or mode selected, seventh toggle off, no
 * result computed.
 *
 * @property selectedRoot The chosen root note, or `null` if none has been selected yet.
 * @property selectedMode The chosen mode, or `null` if none has been selected yet.
 * @property includeSeventhChords State of the seventh-chord toggle. Defaults to `false`.
 * @property result The most recently computed chord list, or `null` if no query has been issued
 *   yet.
 */
data class ChordFinderUiState(
    val selectedRoot: Note? = null,
    val selectedMode: Mode? = null,
    val includeSeventhChords: Boolean = false,
    val result: ChordFinderResult? = null,
)
