package de.ritzelprimpf.toniqo.metronome.data.songsearch

import de.ritzelprimpf.toniqo.common.di.IoDispatcher
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runInterruptible

/**
 * [HttpTransport] backed by the platform's [HttpURLConnection] — no third-party HTTP client (see
 * `docs/DECISIONS.md`, 2026-10-02 "Song BPM search" entry).
 *
 * Runs on the injected I/O dispatcher via [runInterruptible], so cancelling the calling coroutine
 * interrupts a blocked read instead of leaking it until the timeout fires.
 */
class UrlConnectionHttpTransport @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : HttpTransport {

    override suspend fun get(url: String, headers: Map<String, String>): HttpResponse =
        runInterruptible(ioDispatcher) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = METHOD_GET
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }

                val statusCode = connection.responseCode
                val stream = if (statusCode in SUCCESS_RANGE) connection.inputStream else connection.errorStream
                HttpResponse(statusCode = statusCode, body = stream?.readUtf8().orEmpty())
            } finally {
                connection.disconnect()
            }
        }

    private fun InputStream.readUtf8(): String = use { it.readBytes().toString(Charsets.UTF_8) }

    private companion object {
        const val METHOD_GET = "GET"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 10_000
        val SUCCESS_RANGE = 200..299
    }
}
