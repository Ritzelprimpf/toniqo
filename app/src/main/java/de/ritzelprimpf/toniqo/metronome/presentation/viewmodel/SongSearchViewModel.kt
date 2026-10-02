package de.ritzelprimpf.toniqo.metronome.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchResult
import de.ritzelprimpf.toniqo.metronome.domain.usecase.SearchSongTempoUseCase
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the Metronome's song-search sheet.
 *
 * Searches run only when the user submits (keyboard search action), never while typing — every
 * request counts against the API key's shared hourly limit (see `docs/DECISIONS.md`, 2026-10-02
 * "Song BPM search"). Applying a chosen song to the metronome is [MetronomeViewModel]'s job, not
 * this one's.
 */
@HiltViewModel
class SongSearchViewModel @Inject constructor(
    private val searchSongTempo: SearchSongTempoUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongSearchUiState())
    val uiState: StateFlow<SongSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        resetIfBothFieldsEmpty()
    }

    fun onArtistQueryChanged(artistQuery: String) {
        _uiState.update { it.copy(artistQuery = artistQuery) }
        resetIfBothFieldsEmpty()
    }

    /**
     * Once both fields are empty (cleared via their × buttons or by deleting the text), the sheet
     * returns to its hint and any in-flight search is dropped. While either field still has text,
     * the last results stay visible — e.g. clearing only the artist keeps them until the next search.
     */
    private fun resetIfBothFieldsEmpty() {
        val state = _uiState.value
        if (state.query.isNotEmpty() || state.artistQuery.isNotEmpty()) return
        searchJob?.cancel()
        searchJob = null
        _uiState.update { it.copy(status = SongSearchStatus.Idle) }
    }

    /**
     * Runs a search for the current title (narrowed by the artist, if one is entered). Ignored
     * while the title is blank — the artist alone isn't searchable. A newer submit supersedes an
     * in-flight one.
     */
    fun onSearchSubmitted() {
        val (query, artistQuery) = _uiState.value.let { it.query to it.artistQuery }
        if (query.isBlank()) return

        searchJob?.cancel()
        _uiState.update { it.copy(status = SongSearchStatus.Loading) }
        searchJob = viewModelScope.launch {
            val status = when (val result = searchSongTempo(query, artistQuery)) {
                is SongSearchResult.Success ->
                    if (result.songs.isEmpty()) SongSearchStatus.NoResults else SongSearchStatus.Results(result.songs)
                is SongSearchResult.Failure -> SongSearchStatus.Failed(result.reason)
            }
            _uiState.update { it.copy(status = status) }
        }
    }
}
