package de.ritzelprimpf.toniqo.metronome

import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchFailure
import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchQuery
import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchResult
import de.ritzelprimpf.toniqo.metronome.domain.model.SongTempo
import de.ritzelprimpf.toniqo.metronome.domain.usecase.SearchSongTempoUseCase
import de.ritzelprimpf.toniqo.metronome.fakes.FakeSongTempoRepository
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.SongSearchStatus
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.SongSearchUiState
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.SongSearchViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeSongTempoRepository()
    private lateinit var viewModel: SongSearchViewModel

    private val song = SongTempo("o2r0L", "Master of Puppets", "Metallica", 220, null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SongSearchViewModel(SearchSongTempoUseCase(repository))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts idle with an empty query`() {
        assertEquals(SongSearchUiState(), viewModel.uiState.value)
    }

    @Test
    fun `typing updates the query without searching`() = runTest {
        viewModel.onQueryChanged("Master")
        advanceUntilIdle()

        assertEquals("Master", viewModel.uiState.value.query)
        assertEquals(SongSearchStatus.Idle, viewModel.uiState.value.status)
        assertTrue(repository.requestedQueries.isEmpty())
    }

    @Test
    fun `submitting a blank query does nothing`() = runTest {
        viewModel.onQueryChanged("   ")
        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        assertEquals(SongSearchStatus.Idle, viewModel.uiState.value.status)
        assertTrue(repository.requestedQueries.isEmpty())
    }

    @Test
    fun `shows loading while a search is in flight`() = runTest {
        repository.gate = CompletableDeferred()
        viewModel.onQueryChanged("Master")

        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        assertEquals(SongSearchStatus.Loading, viewModel.uiState.value.status)
    }

    @Test
    fun `shows results when the search finds songs`() = runTest {
        repository.result = SongSearchResult.Success(listOf(song))
        viewModel.onQueryChanged("Master")

        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        assertEquals(SongSearchStatus.Results(listOf(song)), viewModel.uiState.value.status)
    }

    @Test
    fun `shows no results when the search finds nothing`() = runTest {
        repository.result = SongSearchResult.Success(emptyList())
        viewModel.onQueryChanged("zzz")

        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        assertEquals(SongSearchStatus.NoResults, viewModel.uiState.value.status)
    }

    @Test
    fun `shows the failure reason when the search fails`() = runTest {
        repository.result = SongSearchResult.Failure(SongSearchFailure.SERVICE_UNAVAILABLE)
        viewModel.onQueryChanged("Master")

        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        assertEquals(SongSearchStatus.Failed(SongSearchFailure.SERVICE_UNAVAILABLE), viewModel.uiState.value.status)
    }

    @Test
    fun `a newer submit supersedes an in-flight search`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        viewModel.onQueryChanged("first")
        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        repository.result = SongSearchResult.Success(listOf(song))
        viewModel.onQueryChanged("second")
        viewModel.onSearchSubmitted()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("first", "second"), repository.requestedQueries.map { it.title })
        assertEquals(SongSearchStatus.Results(listOf(song)), viewModel.uiState.value.status)
    }

    @Test
    fun `typing after a search keeps the previous results visible`() = runTest {
        repository.result = SongSearchResult.Success(listOf(song))
        viewModel.onQueryChanged("Master")
        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        viewModel.onQueryChanged("Master of")

        assertEquals(SongSearchStatus.Results(listOf(song)), viewModel.uiState.value.status)
    }

    @Test
    fun `typing an artist updates the artist query without searching`() = runTest {
        viewModel.onArtistQueryChanged("Metallica")
        advanceUntilIdle()

        assertEquals("Metallica", viewModel.uiState.value.artistQuery)
        assertTrue(repository.requestedQueries.isEmpty())
    }

    @Test
    fun `submitting searches by title narrowed by the entered artist`() = runTest {
        viewModel.onQueryChanged("Enter Sandman")
        viewModel.onArtistQueryChanged("Metallica")

        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        assertEquals(listOf(SongSearchQuery("Enter Sandman", "Metallica")), repository.requestedQueries)
    }

    @Test
    fun `submitting with only an artist does nothing`() = runTest {
        viewModel.onArtistQueryChanged("Metallica")

        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        assertEquals(SongSearchStatus.Idle, viewModel.uiState.value.status)
        assertTrue(repository.requestedQueries.isEmpty())
    }

    private fun TestScope.searchAndSettle(title: String, artist: String = "") {
        repository.result = SongSearchResult.Success(listOf(song))
        viewModel.onQueryChanged(title)
        viewModel.onArtistQueryChanged(artist)
        viewModel.onSearchSubmitted()
        advanceUntilIdle()
    }

    @Test
    fun `clearing only the artist keeps the results`() = runTest {
        searchAndSettle("Master of Puppets", "Metallica")

        viewModel.onArtistQueryChanged("")

        assertEquals(SongSearchStatus.Results(listOf(song)), viewModel.uiState.value.status)
    }

    @Test
    fun `clearing the title while the artist has text keeps the results`() = runTest {
        searchAndSettle("Master of Puppets", "Metallica")

        viewModel.onQueryChanged("")

        assertEquals(SongSearchStatus.Results(listOf(song)), viewModel.uiState.value.status)
    }

    @Test
    fun `clearing both fields returns to the idle hint`() = runTest {
        searchAndSettle("Master of Puppets", "Metallica")

        viewModel.onArtistQueryChanged("")
        viewModel.onQueryChanged("")

        assertEquals(SongSearchUiState(), viewModel.uiState.value)
    }

    @Test
    fun `clearing the only filled field returns to the idle hint`() = runTest {
        searchAndSettle("Master of Puppets")

        viewModel.onQueryChanged("")

        assertEquals(SongSearchStatus.Idle, viewModel.uiState.value.status)
    }

    @Test
    fun `clearing both fields drops an in-flight search`() = runTest {
        repository.gate = CompletableDeferred()
        viewModel.onQueryChanged("Master")
        viewModel.onSearchSubmitted()
        advanceUntilIdle()

        viewModel.onQueryChanged("")
        repository.gate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(SongSearchStatus.Idle, viewModel.uiState.value.status)
    }
}
