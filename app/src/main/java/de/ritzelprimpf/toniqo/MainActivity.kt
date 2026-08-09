package de.ritzelprimpf.toniqo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.ritzelprimpf.toniqo.ui.MainScreen
import de.ritzelprimpf.toniqo.ui.theme.ThemeViewModel
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Activity-scoped (this is the root composable, outside any NavHost destination),
            // so every screen that needs the theme choice receives the same ThemeViewModel
            // instance's state, threaded down as plain parameters — see ThemeViewModel's kdoc.
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsStateWithLifecycle()

            ToniqoTheme(useDarkTheme = isDarkTheme) {
                MainScreen(
                    isDarkTheme = isDarkTheme,
                    onDarkThemeChanged = themeViewModel::setDarkTheme,
                )
            }
        }
    }
}
