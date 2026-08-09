package de.ritzelprimpf.toniqo.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.ritzelprimpf.toniqo.ui.components.AppNavigationBar
import de.ritzelprimpf.toniqo.ui.navigation.AppNavHost
import de.ritzelprimpf.toniqo.ui.navigation.bottomNavDestinations
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

/**
 * Root composable hosting the navigation shell: a Material 3 [Scaffold] with
 * [AppNavigationBar] as the bottom bar and [AppNavHost] as the content area.
 *
 * State ownership:
 * - [NavHostController] is created and owned here (top-level).
 * - [SnackbarHostState] is created here and threaded to screens that need it.
 * - Tab switching uses `saveState`/`restoreState` to preserve each tab's back
 *   stack across switches (see the `onNavigate` lambda below).
 * - [isDarkTheme]/[onDarkThemeChanged] are owned by `MainActivity`'s
 *   `de.ritzelprimpf.toniqo.ui.theme.ThemeViewModel` and threaded through here rather than
 *   re-fetched inside a nav destination — see that ViewModel's kdoc for why.
 *
 * @param isDarkTheme The user's current dark/light theme choice, surfaced as a toggle in the
 *   Info menu ([de.ritzelprimpf.toniqo.ui.info.InfoHomeScreen]).
 * @param onDarkThemeChanged Called when the user flips that toggle.
 */
@Composable
fun MainScreen(
    isDarkTheme: Boolean = true,
    onDarkThemeChanged: (Boolean) -> Unit = {},
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination: NavDestination? = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Tq.Color.BgBase,
        bottomBar = {
            AppNavigationBar(
                destinations = bottomNavDestinations,
                currentDestination = currentDestination,
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Pop to the graph's start destination so that tapping
                        // another tab doesn't build an unbounded back stack.
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination.
                        launchSingleTop = true
                        // Restore state when re-selecting a previously visited tab.
                        restoreState = true
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            isDarkTheme = isDarkTheme,
            onDarkThemeChanged = onDarkThemeChanged,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(name = "MainScreen — Dark", showSystemUi = true)
@Composable
private fun MainScreenPreviewDark() {
    ToniqoTheme(useDarkTheme = true) {
        MainScreen()
    }
}

@Preview(name = "MainScreen — Light", showSystemUi = true)
@Composable
private fun MainScreenPreviewLight() {
    ToniqoTheme(useDarkTheme = false) {
        MainScreen()
    }
}
