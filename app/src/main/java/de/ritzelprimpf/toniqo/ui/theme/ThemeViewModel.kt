package de.ritzelprimpf.toniqo.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ritzelprimpf.toniqo.common.data.ThemePreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the app-wide dark/light theme choice. Activity-scoped: created once from
 * `MainActivity`'s root composable and threaded down to every screen that needs it (the toggle
 * itself lives in the Info menu), rather than re-fetched via `hiltViewModel()` from inside a nav
 * destination — that would scope it to the destination's back-stack entry instead, giving each
 * screen its own separate instance.
 *
 * @property themePreferences Persists the choice across launches; see `DECISIONS.md`,
 *   "Runtime light/dark theme toggle".
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    /** `true` = dark (the default), `false` = light. */
    val isDarkTheme: StateFlow<Boolean> = themePreferences.darkThemeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(THEME_GRACE_PERIOD_MS),
            initialValue = true,
        )

    /** Persists the user's [enabled] choice; `isDarkTheme` updates once the write completes. */
    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setDarkThemeEnabled(enabled) }
    }

    private companion object {
        const val THEME_GRACE_PERIOD_MS = 5_000L
    }
}
