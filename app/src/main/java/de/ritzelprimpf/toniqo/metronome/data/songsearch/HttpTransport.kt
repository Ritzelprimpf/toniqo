package de.ritzelprimpf.toniqo.metronome.data.songsearch

import java.io.IOException

/**
 * Minimal HTTP GET abstraction so the song-search repository can be tested without a network.
 *
 * Implementations handle threading themselves; callers may invoke [get] from any dispatcher.
 */
interface HttpTransport {

    /**
     * Performs a GET request.
     *
     * Any HTTP status (including 4xx/5xx) is a normal return value — only transport-level
     * failures throw.
     *
     * @throws IOException if the server could not be reached or the connection failed mid-read.
     */
    suspend fun get(url: String, headers: Map<String, String>): HttpResponse
}

/**
 * @property statusCode The HTTP status code.
 * @property body The response body decoded as UTF-8 (the error body for non-2xx responses; empty
 *   if the server sent none).
 */
data class HttpResponse(
    val statusCode: Int,
    val body: String,
)
