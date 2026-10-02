package de.ritzelprimpf.toniqo.metronome.data.songsearch

import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchFailure
import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchQuery
import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchResult
import de.ritzelprimpf.toniqo.metronome.domain.model.SongTempo
import de.ritzelprimpf.toniqo.metronome.domain.repository.SongTempoRepository
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import org.json.JSONException

/**
 * [SongTempoRepository] backed by the GetSongBPM web API: `/search/?type=song` for a title-only
 * query, `/search/?type=both` with `lookup=song:<title> artist:<artist>` when an artist is given.
 *
 * Successful results are cached in memory for the process lifetime, keyed case-insensitively by
 * title and artist: the one API key is shared by every install and limited to 3000 requests/hour, so a
 * repeated search should never cost a second request. Failures are not cached, so a retry after
 * reconnecting actually retries. Bound as a singleton so the cache outlives the screen.
 *
 * The API key travels in the `X-API-KEY` header rather than the URL so it doesn't end up in
 * URL-level logs.
 */
class GetSongBpmSongTempoRepository @Inject constructor(
    private val config: GetSongBpmConfig,
    private val transport: HttpTransport,
    private val parser: SongSearchResponseParser,
) : SongTempoRepository {

    private val cache = ConcurrentHashMap<SongSearchQuery, List<SongTempo>>()

    override suspend fun search(query: SongSearchQuery): SongSearchResult {
        val cacheKey = SongSearchQuery(
            title = query.title.lowercase(Locale.ROOT),
            artist = query.artist?.lowercase(Locale.ROOT),
        )
        cache[cacheKey]?.let { return SongSearchResult.Success(it) }

        if (config.apiKey.isBlank()) {
            return SongSearchResult.Failure(
                reason = SongSearchFailure.SERVICE_UNAVAILABLE,
                cause = IllegalStateException(MISSING_KEY_MESSAGE),
            )
        }

        val response = try {
            transport.get(url = buildSearchUrl(query), headers = mapOf(HEADER_API_KEY to config.apiKey))
        } catch (e: IOException) {
            return SongSearchResult.Failure(SongSearchFailure.NO_CONNECTION, e)
        }

        if (response.statusCode != HTTP_OK) {
            return SongSearchResult.Failure(
                reason = SongSearchFailure.SERVICE_UNAVAILABLE,
                cause = IOException("$UNEXPECTED_STATUS_MESSAGE ${response.statusCode}"),
            )
        }

        val songs = try {
            parser.parse(response.body)
        } catch (e: JSONException) {
            return SongSearchResult.Failure(SongSearchFailure.SERVICE_UNAVAILABLE, e)
        }
        cache[cacheKey] = songs
        return SongSearchResult.Success(songs)
    }

    private fun buildSearchUrl(query: SongSearchQuery): String {
        val (type, lookup) = when (val artist = query.artist) {
            null -> TYPE_SONG to query.title
            else -> TYPE_BOTH to "$LOOKUP_SONG_PREFIX${query.title} $LOOKUP_ARTIST_PREFIX$artist"
        }
        return "${config.baseUrl}$SEARCH_PATH?$PARAM_TYPE=$type" +
            "&$PARAM_LOOKUP=${URLEncoder.encode(lookup, CHARSET_UTF_8)}" +
            "&$PARAM_LIMIT=${config.resultLimit}"
    }

    private companion object {
        const val SEARCH_PATH = "search/"
        const val PARAM_TYPE = "type"
        const val TYPE_SONG = "song"
        const val TYPE_BOTH = "both"
        const val LOOKUP_SONG_PREFIX = "song:"
        const val LOOKUP_ARTIST_PREFIX = "artist:"
        const val PARAM_LOOKUP = "lookup"
        const val PARAM_LIMIT = "limit"
        const val HEADER_API_KEY = "X-API-KEY"
        // URLEncoder.encode(String, Charset) needs API 33; minSdk is 31.
        const val CHARSET_UTF_8 = "UTF-8"
        const val HTTP_OK = 200
        const val MISSING_KEY_MESSAGE = "GETSONGBPM_API_KEY is not configured in local.properties"
        const val UNEXPECTED_STATUS_MESSAGE = "GetSongBPM responded with HTTP"
    }
}
