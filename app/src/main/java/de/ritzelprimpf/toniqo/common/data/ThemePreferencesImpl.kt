package de.ritzelprimpf.toniqo.common.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.themeDataStore by preferencesDataStore(name = "theme_preferences")

/**
 * DataStore-backed implementation of [ThemePreferences].
 *
 * Not unit-tested directly — DataStore requires a real [Context]; a fake implements
 * [ThemePreferences] for unit tests instead.
 */
class ThemePreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ThemePreferences {

    override val darkThemeEnabled: Flow<Boolean> = context.themeDataStore.data
        .map { it[DARK_THEME_ENABLED_KEY] ?: DEFAULT_DARK_THEME_ENABLED }

    override suspend fun setDarkThemeEnabled(enabled: Boolean) {
        context.themeDataStore.edit { it[DARK_THEME_ENABLED_KEY] = enabled }
    }

    private companion object {
        val DARK_THEME_ENABLED_KEY = booleanPreferencesKey("dark_theme_enabled")
        const val DEFAULT_DARK_THEME_ENABLED = true
    }
}
