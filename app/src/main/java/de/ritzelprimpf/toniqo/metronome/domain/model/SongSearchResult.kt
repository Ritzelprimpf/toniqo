package de.ritzelprimpf.toniqo.metronome.domain.model

/**
 * Outcome of a song-tempo search.
 *
 * - [Success] — the search completed; [Success.songs] may be empty when nothing matched.
 * - [Failure] — the search could not be completed; [Failure.reason] tells the UI which message
 *   to show, [Failure.cause] keeps the underlying error for debugging.
 */
sealed interface SongSearchResult {

    /** @property songs Matching songs in the source's relevance order. */
    data class Success(val songs: List<SongTempo>) : SongSearchResult

    /**
     * @property reason The user-facing failure category.
     * @property cause The underlying error, if one was thrown or synthesized (e.g. an HTTP status).
     */
    data class Failure(
        val reason: SongSearchFailure,
        val cause: Throwable? = null,
    ) : SongSearchResult
}

/** User-facing categories of a failed song-tempo search. */
enum class SongSearchFailure {

    /** The service could not be reached (no connection, DNS failure, timeout). */
    NO_CONNECTION,

    /**
     * The service was reached but refused or could not answer the request — missing/rejected
     * API key, rate limit hit, server error, or an unparseable response. The user cannot fix any
     * of these, so they share one message.
     */
    SERVICE_UNAVAILABLE,
}
