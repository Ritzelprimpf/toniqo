package de.ritzelprimpf.toniqo.metronome.domain.usecase

import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchQuery
import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchResult
import de.ritzelprimpf.toniqo.metronome.domain.repository.SongTempoRepository
import javax.inject.Inject

/**
 * Searches songs by title, optionally narrowed by artist, for the metronome's song-tempo lookup.
 *
 * Normalizes the user's input (trims, collapses runs of whitespace) so that trivially different
 * spellings of the same query hit the repository's cache instead of the rate-limited service.
 * The title is required: a blank title never reaches the repository and yields an empty
 * [SongSearchResult.Success], even if an artist was entered. A blank artist means "title only".
 */
class SearchSongTempoUseCase @Inject constructor(
    private val repository: SongTempoRepository,
) {

    suspend operator fun invoke(rawTitle: String, rawArtist: String = ""): SongSearchResult {
        val title = normalize(rawTitle)
        if (title.isEmpty()) return SongSearchResult.Success(emptyList())
        val artist = normalize(rawArtist).takeIf { it.isNotEmpty() }
        return repository.search(SongSearchQuery(title = title, artist = artist))
    }

    private fun normalize(raw: String): String = raw.trim().replace(WHITESPACE_RUN, SINGLE_SPACE)

    private companion object {
        val WHITESPACE_RUN = Regex("\\s+")
        const val SINGLE_SPACE = " "
    }
}
