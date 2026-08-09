package de.ritzelprimpf.toniqo.common.fakes

import de.ritzelprimpf.toniqo.common.data.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory test double for [ThemePreferences].
 *
 * Backed by a [MutableStateFlow] so tests can seed the initial choice and observe writes via the
 * [storedDarkThemeEnabled] inspection property.
 *
 * @param initialDarkThemeEnabled Starting value for [darkThemeEnabled]; defaults to `true`,
 *   matching the production default (dark is always the starting point, per DESIGN.md §2.1).
 */
class FakeThemePreferences(
    initialDarkThemeEnabled: Boolean = true,
) : ThemePreferences {

    private val _darkThemeEnabled = MutableStateFlow(initialDarkThemeEnabled)

    override val darkThemeEnabled: Flow<Boolean> = _darkThemeEnabled.asStateFlow()

    override suspend fun setDarkThemeEnabled(enabled: Boolean) {
        _darkThemeEnabled.value = enabled
    }

    /** Inspection property: the last value written via [setDarkThemeEnabled], or the initial one. */
    val storedDarkThemeEnabled: Boolean get() = _darkThemeEnabled.value
}
