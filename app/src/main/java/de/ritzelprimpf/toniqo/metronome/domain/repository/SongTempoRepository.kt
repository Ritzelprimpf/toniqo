package de.ritzelprimpf.toniqo.metronome.domain.repository

import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchQuery
import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchResult

/** Looks up song tempos from an external song database. */
interface SongTempoRepository {

    /**
     * Searches songs matching [query]'s title and, if given, its artist.
     *
     * Never throws for expected failures (network, service errors) — those are reported as
     * [SongSearchResult.Failure].
     *
     * @param query A query with a non-blank, trimmed title and a non-blank, trimmed artist or `null`.
     */
    suspend fun search(query: SongSearchQuery): SongSearchResult
}
