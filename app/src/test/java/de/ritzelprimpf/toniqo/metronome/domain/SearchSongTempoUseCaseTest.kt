package de.ritzelprimpf.toniqo.metronome.domain

import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchFailure
import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchQuery
import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchResult
import de.ritzelprimpf.toniqo.metronome.domain.model.SongTempo
import de.ritzelprimpf.toniqo.metronome.domain.usecase.SearchSongTempoUseCase
import de.ritzelprimpf.toniqo.metronome.fakes.FakeSongTempoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSongTempoUseCaseTest {

    private val repository = FakeSongTempoRepository()
    private val useCase = SearchSongTempoUseCase(repository)

    @Test
    fun `passes a trimmed title with collapsed whitespace to the repository`() = runTest {
        useCase("  Enter \t  Sandman \n")

        assertEquals(listOf(SongSearchQuery("Enter Sandman", artist = null)), repository.requestedQueries)
    }

    @Test
    fun `passes a normalized artist along with the title`() = runTest {
        useCase("Enter Sandman", "  Metal   lica ")

        assertEquals(listOf(SongSearchQuery("Enter Sandman", artist = "Metal lica")), repository.requestedQueries)
    }

    @Test
    fun `treats a blank artist as a title-only search`() = runTest {
        useCase("One", "   ")

        assertEquals(listOf(SongSearchQuery("One", artist = null)), repository.requestedQueries)
    }

    @Test
    fun `does not search by artist alone when the title is blank`() = runTest {
        val result = useCase("  ", "Metallica")

        assertEquals(SongSearchResult.Success(emptyList()), result)
        assertTrue(repository.requestedQueries.isEmpty())
    }

    @Test
    fun `returns an empty success without querying the repository for a blank title`() = runTest {
        val result = useCase("   ")

        assertEquals(SongSearchResult.Success(emptyList()), result)
        assertTrue(repository.requestedQueries.isEmpty())
    }

    @Test
    fun `returns the repository's songs on success`() = runTest {
        val songs = listOf(SongTempo("1", "One", "Metallica", 108, null))
        repository.result = SongSearchResult.Success(songs)

        assertEquals(SongSearchResult.Success(songs), useCase("One"))
    }

    @Test
    fun `returns the repository's failure unchanged`() = runTest {
        val failure = SongSearchResult.Failure(SongSearchFailure.NO_CONNECTION)
        repository.result = failure

        assertEquals(failure, useCase("One"))
    }
}
