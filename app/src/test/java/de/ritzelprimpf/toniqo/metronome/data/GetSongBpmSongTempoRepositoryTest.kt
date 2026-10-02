package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.data.songsearch.GetSongBpmConfig
import de.ritzelprimpf.toniqo.metronome.data.songsearch.GetSongBpmSongTempoRepository
import de.ritzelprimpf.toniqo.metronome.data.songsearch.HttpResponse
import de.ritzelprimpf.toniqo.metronome.data.songsearch.SongSearchResponseParser
import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchFailure
import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchQuery
import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchResult
import de.ritzelprimpf.toniqo.metronome.domain.model.SongTempo
import de.ritzelprimpf.toniqo.metronome.domain.model.SongTimeSignature
import de.ritzelprimpf.toniqo.metronome.fakes.FakeHttpTransport
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetSongBpmSongTempoRepositoryTest {

    private val transport = FakeHttpTransport()

    private fun repository(apiKey: String = "test-key") = GetSongBpmSongTempoRepository(
        config = GetSongBpmConfig(baseUrl = "https://api.example/", apiKey = apiKey, resultLimit = 25),
        transport = transport,
        parser = SongSearchResponseParser(),
    )

    private val oneSongBody =
        """{"search":[{"id":"o2r0L","title":"Master of Puppets","tempo":"220","time_sig":"4/4","artist":{"name":"Metallica"}}]}"""

    private fun failureReason(result: SongSearchResult): SongSearchFailure =
        (result as SongSearchResult.Failure).reason

    @Test
    fun `requests the song search endpoint with the encoded title and result limit`() = runTest {
        repository().search(SongSearchQuery("Enter Sandman & Co"))

        assertEquals(
            "https://api.example/search/?type=song&lookup=Enter+Sandman+%26+Co&limit=25",
            transport.requests.single().url,
        )
    }

    @Test
    fun `sends the api key in the X-API-KEY header and not in the url`() = runTest {
        repository(apiKey = "secret").search(SongSearchQuery("One"))

        val request = transport.requests.single()
        assertEquals(mapOf("X-API-KEY" to "secret"), request.headers)
        assertTrue("secret" !in request.url)
    }

    @Test
    fun `returns parsed songs on HTTP 200`() = runTest {
        transport.response = HttpResponse(200, oneSongBody)

        val result = repository().search(SongSearchQuery("Master of Puppets"))

        assertEquals(
            SongSearchResult.Success(
                listOf(SongTempo("o2r0L", "Master of Puppets", "Metallica", 220, SongTimeSignature(4, 4))),
            ),
            result,
        )
    }

    @Test
    fun `serves a repeated search from the cache without a second request, ignoring case`() = runTest {
        transport.response = HttpResponse(200, oneSongBody)
        val repo = repository()

        val first = repo.search(SongSearchQuery("Master of Puppets"))
        val second = repo.search(SongSearchQuery("MASTER OF PUPPETS"))

        assertEquals(1, transport.requests.size)
        assertEquals(first, second)
    }

    @Test
    fun `caches empty results too`() = runTest {
        transport.response = HttpResponse(200, """{"search":{"error":"no result"}}""")
        val repo = repository()

        repo.search(SongSearchQuery("zzz"))
        val second = repo.search(SongSearchQuery("zzz"))

        assertEquals(1, transport.requests.size)
        assertEquals(SongSearchResult.Success(emptyList()), second)
    }

    @Test
    fun `does not cache failures so a retry sends a new request`() = runTest {
        val repo = repository()
        transport.failure = IOException("offline")
        repo.search(SongSearchQuery("One"))

        transport.failure = null
        transport.response = HttpResponse(200, oneSongBody)
        val retry = repo.search(SongSearchQuery("One"))

        assertEquals(2, transport.requests.size)
        assertTrue(retry is SongSearchResult.Success)
    }

    @Test
    fun `maps an IOException to NO_CONNECTION`() = runTest {
        transport.failure = IOException("unreachable")

        assertEquals(SongSearchFailure.NO_CONNECTION, failureReason(repository().search(SongSearchQuery("One"))))
    }

    @Test
    fun `maps a timeout to NO_CONNECTION`() = runTest {
        transport.failure = SocketTimeoutException("timed out")

        assertEquals(SongSearchFailure.NO_CONNECTION, failureReason(repository().search(SongSearchQuery("One"))))
    }

    @Test
    fun `maps rejected key, rate limit and server errors to SERVICE_UNAVAILABLE`() = runTest {
        listOf(401, 403, 429, 500, 503).forEach { status ->
            transport.response = HttpResponse(status, """{"error":"x"}""")

            assertEquals(
                "HTTP $status",
                SongSearchFailure.SERVICE_UNAVAILABLE,
                failureReason(repository().search(SongSearchQuery("One $status"))),
            )
        }
    }

    @Test
    fun `maps an unparseable 200 body to SERVICE_UNAVAILABLE`() = runTest {
        transport.response = HttpResponse(200, "<html>oops</html>")

        assertEquals(SongSearchFailure.SERVICE_UNAVAILABLE, failureReason(repository().search(SongSearchQuery("One"))))
    }

    @Test
    fun `reports SERVICE_UNAVAILABLE without sending a request when no api key is configured`() = runTest {
        val result = repository(apiKey = " ").search(SongSearchQuery("One"))

        assertEquals(SongSearchFailure.SERVICE_UNAVAILABLE, failureReason(result))
        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun `uses the combined song and artist lookup when an artist is given`() = runTest {
        repository().search(SongSearchQuery(title = "Enter Sandman", artist = "Metallica"))

        assertEquals(
            "https://api.example/search/?type=both&lookup=song%3AEnter+Sandman+artist%3AMetallica&limit=25",
            transport.requests.single().url,
        )
    }

    @Test
    fun `caches title-only and title-plus-artist searches separately`() = runTest {
        transport.response = HttpResponse(200, oneSongBody)
        val repo = repository()

        repo.search(SongSearchQuery("Master of Puppets"))
        repo.search(SongSearchQuery("Master of Puppets", artist = "Metallica"))
        repo.search(SongSearchQuery("master of puppets", artist = "METALLICA"))

        assertEquals(2, transport.requests.size)
    }
}
