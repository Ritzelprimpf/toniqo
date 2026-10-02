package de.ritzelprimpf.toniqo.metronome.presentation.viewmodel

import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchFailure
import de.ritzelprimpf.toniqo.metronome.domain.model.SongTempo

/**
 * State of the Metronome's song-search sheet.
 *
 * @property query The text currently in the title field (searches only run on submit).
 * @property artistQuery The text currently in the optional artist field; blank = title-only search.
 * @property status What the area below the fields shows.
 */
data class SongSearchUiState(
    val query: String = "",
    val artistQuery: String = "",
    val status: SongSearchStatus = SongSearchStatus.Idle,
)

/** What the song-search sheet shows below its search field. */
sealed interface SongSearchStatus {

    /** No search submitted yet — show the usage hint. */
    data object Idle : SongSearchStatus

    /** A search request is in flight. */
    data object Loading : SongSearchStatus

    /** The search succeeded with at least one song. */
    data class Results(val songs: List<SongTempo>) : SongSearchStatus

    /** The search succeeded but matched nothing. */
    data object NoResults : SongSearchStatus

    /** The search failed; [reason] picks the message. */
    data class Failed(val reason: SongSearchFailure) : SongSearchStatus
}
