package de.ritzelprimpf.toniqo.metronome.data

import com.sun.net.httpserver.HttpServer
import de.ritzelprimpf.toniqo.metronome.data.songsearch.HttpResponse
import de.ritzelprimpf.toniqo.metronome.data.songsearch.UrlConnectionHttpTransport
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Exercises the real [java.net.HttpURLConnection] path against the JDK's built-in local
 * [HttpServer] — no network and no extra test dependency.
 */
class UrlConnectionHttpTransportTest {

    private lateinit var server: HttpServer
    private var receivedApiKey: String? = null
    private var receivedQuery: String? = null

    private val transport = UrlConnectionHttpTransport(Dispatchers.IO)

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/ok") { exchange ->
            receivedApiKey = exchange.requestHeaders.getFirst("X-API-KEY")
            receivedQuery = exchange.requestURI.rawQuery
            val body = """{"search":["ü"]}""".toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.createContext("/limited") { exchange ->
            val body = """{"error":"rate limit"}""".toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(429, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.createContext("/empty-error") { exchange ->
            exchange.sendResponseHeaders(500, -1)
            exchange.close()
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    private fun url(path: String) = "http://127.0.0.1:${server.address.port}$path"

    @Test
    fun `returns status and UTF-8 body of a successful response`() = runTest {
        val response = transport.get(url("/ok?type=song&lookup=one"), headers = emptyMap())

        assertEquals(HttpResponse(200, """{"search":["ü"]}"""), response)
        assertEquals("type=song&lookup=one", receivedQuery)
    }

    @Test
    fun `sends the given request headers`() = runTest {
        transport.get(url("/ok"), headers = mapOf("X-API-KEY" to "abc"))

        assertEquals("abc", receivedApiKey)
    }

    @Test
    fun `returns error status and error body instead of throwing`() = runTest {
        assertEquals(HttpResponse(429, """{"error":"rate limit"}"""), transport.get(url("/limited"), emptyMap()))
    }

    @Test
    fun `returns an empty body when an error response has none`() = runTest {
        assertEquals(HttpResponse(500, ""), transport.get(url("/empty-error"), emptyMap()))
    }

    @Test(expected = IOException::class)
    fun `throws IOException when nothing is listening`() = runTest {
        val closedPort = ServerSocket(0, 0, InetAddress.getLoopbackAddress()).use { it.localPort }

        transport.get("http://127.0.0.1:$closedPort/", emptyMap())
    }
}
