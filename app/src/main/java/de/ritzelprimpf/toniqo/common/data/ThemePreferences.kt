package de.ritzelprimpf.toniqo.common.data

import kotlinx.coroutines.flow.Flow

/**
 * Persists the user's app-wide dark/light theme choice across app launches.
 *
 * Exposed as a [Flow] so [de.ritzelprimpf.toniqo.ui.theme.ThemeViewModel] can react to changes in
 * real time. The DataStore-backed implementation ([ThemePreferencesImpl]) is the production
 * source; a fake is used in unit tests.
 */
interface ThemePreferences {

    /**
     * Whether the app should use the dark theme. Defaults to `true` — dark is the design target
     * (DESIGN.md §2.1) and the default regardless of the device's system theme setting; the user
     * must explicitly opt into light mode via the toggle in the Info menu.
     */
    val darkThemeEnabled: Flow<Boolean>

    /** Persists [enabled] as the dark-theme choice. */
    suspend fun setDarkThemeEnabled(enabled: Boolean)
}
