package de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel

import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.keyfinder.domain.model.KeyFinderResult

/**
 * Immutable snapshot of everything the Key Finder screen needs to render.
 *
 * Defaults represent the screen's empty state: no notes entered, no tonic selected, no results.
 *
 * @property inputNotes The notes the user has entered so far.
 * @property tonic The tonic the user has selected, or `null` if none has been chosen.
 * @property results The ranked match list returned by the service, or an empty list if no query
 *   has been issued yet (or the current query produced zero matches).
 */
data class KeyFinderUiState(
    val inputNotes: Set<Note> = emptySet(),
    val tonic: Note? = null,
    val results: List<KeyFinderResult> = emptyList(),
)
