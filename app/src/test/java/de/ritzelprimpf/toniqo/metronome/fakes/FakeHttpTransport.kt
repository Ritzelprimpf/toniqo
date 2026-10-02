package de.ritzelprimpf.toniqo.metronome.fakes

import de.ritzelprimpf.toniqo.metronome.data.songsearch.HttpResponse
import de.ritzelprimpf.toniqo.metronome.data.songsearch.HttpTransport
import java.io.IOException

/**
 * In-memory test double for [HttpTransport].
 *
 * Answers every request with [response], or throws [failure] when set. Records each request's URL
 * and headers in [requests].
 */
class FakeHttpTransport(
    var response: HttpResponse = HttpResponse(statusCode = 200, body = """{"search":[]}"""),
    var failure: IOException? = null,
) : HttpTransport {

    data class Request(val url: String, val headers: Map<String, String>)

    val requests = mutableListOf<Request>()

    override suspend fun get(url: String, headers: Map<String, String>): HttpResponse {
        requests += Request(url, headers)
        failure?.let { throw it }
        return response
    }
}
