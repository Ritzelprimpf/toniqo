package de.ritzelprimpf.toniqo.ui.theme

import de.ritzelprimpf.toniqo.common.fakes.FakeThemePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
class ThemeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // ThemeViewModel uses viewModelScope, which resolves to Dispatchers.Main — unavailable in a
    // plain JVM unit test unless substituted, matching the pattern used by TunerViewModelTest.
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // isDarkTheme is built via stateIn(WhileSubscribed), which only mirrors the upstream
    // preferences flow while it has an active collector — backgroundScope keeps one alive for
    // the duration of each test, matching the pattern used by TunerViewModelTest.
    private fun TestScope.makeViewModel(
        preferences: FakeThemePreferences = FakeThemePreferences(),
    ): ThemeViewModel {
        val vm = ThemeViewModel(preferences)
        vm.isDarkTheme.onEach { }.launchIn(backgroundScope)
        return vm
    }

    @Test
    fun `isDarkTheme defaults to true when no preference has been stored`() = runTest {
        val vm = makeViewModel(FakeThemePreferences(initialDarkThemeEnabled = true))
        advanceUntilIdle()
        assertTrue(vm.isDarkTheme.value)
    }

    @Test
    fun `isDarkTheme reflects a stored light-theme preference`() = runTest {
        val vm = makeViewModel(FakeThemePreferences(initialDarkThemeEnabled = false))
        advanceUntilIdle()
        assertEquals(false, vm.isDarkTheme.value)
    }

    @Test
    fun `setDarkTheme persists the choice and updates isDarkTheme`() = runTest {
        val preferences = FakeThemePreferences(initialDarkThemeEnabled = true)
        val vm = makeViewModel(preferences)
        advanceUntilIdle()

        vm.setDarkTheme(false)
        advanceUntilIdle()

        assertEquals(false, preferences.storedDarkThemeEnabled)
        assertEquals(false, vm.isDarkTheme.value)
    }

    @Test
    fun `setDarkTheme back to true is persisted`() = runTest {
        val preferences = FakeThemePreferences(initialDarkThemeEnabled = false)
        val vm = makeViewModel(preferences)
        advanceUntilIdle()

        vm.setDarkTheme(true)
        advanceUntilIdle()

        assertEquals(true, preferences.storedDarkThemeEnabled)
        assertTrue(vm.isDarkTheme.value)
    }
}
