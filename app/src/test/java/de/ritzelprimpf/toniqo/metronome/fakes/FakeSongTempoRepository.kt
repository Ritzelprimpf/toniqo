package de.ritzelprimpf.toniqo.metronome.fakes

import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchQuery
import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchResult
import de.ritzelprimpf.toniqo.metronome.domain.repository.SongTempoRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * In-memory test double for [SongTempoRepository].
 *
 * Returns [result] for every search and records each query in [requestedQueries]. Set
 * [gate] to suspend searches until the test completes it, to observe in-flight states.
 */
class FakeSongTempoRepository(
    var result: SongSearchResult = SongSearchResult.Success(emptyList()),
) : SongTempoRepository {

    val requestedQueries = mutableListOf<SongSearchQuery>()

    var gate: CompletableDeferred<Unit>? = null

    override suspend fun search(query: SongSearchQuery): SongSearchResult {
        requestedQueries += query
        gate?.await()
        return result
    }
}
