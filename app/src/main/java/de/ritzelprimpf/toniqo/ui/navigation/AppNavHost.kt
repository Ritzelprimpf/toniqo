package de.ritzelprimpf.toniqo.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import de.ritzelprimpf.toniqo.chordfinder.presentation.ui.ChordFinderScreen
import de.ritzelprimpf.toniqo.keyfinder.presentation.ui.KeyFinderScreen
import de.ritzelprimpf.toniqo.metronome.presentation.ui.MetronomeScreen
import de.ritzelprimpf.toniqo.tuner.presentation.ui.TunerScreen
import de.ritzelprimpf.toniqo.ui.info.HelpScreen
import de.ritzelprimpf.toniqo.ui.info.InfoHomeScreen
import de.ritzelprimpf.toniqo.ui.info.LicensesScreen
import de.ritzelprimpf.toniqo.ui.info.PrivacyPolicyScreen
import de.ritzelprimpf.toniqo.ui.info.RateAndShareScreen

/**
 * Top-level nav host wiring all five module destinations and the nested Info graph.
 *
 * Navigation structure:
 * ```
 * AppNavHost
 * ├── tuner_route        → TunerScreen
 * ├── metronome_route    → MetronomeScreen    (placeholder)
 * ├── keyfinder_route    → KeyFinderScreen    (placeholder)
 * ├── chordfinder_route  → ChordFinderScreen  (placeholder)
 * └── info (nested graph)
 *     ├── info_home_route → InfoHomeScreen
 *     ├── help_route      → HelpScreen
 *     ├── privacy_route   → PrivacyPolicyScreen
 *     ├── licenses_route  → LicensesScreen
 *     └── rate_share_route→ RateAndShareScreen
 * ```
 *
 * The bottom bar remains visible during Info sub-screen navigation because the
 * nested graph lives inside the Scaffold's content slot (not overlaid on top).
 *
 * @param navController Hoisted nav controller from [MainScreen].
 * @param snackbarHostState Shared snackbar state from the outer Scaffold.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.TUNER,
        modifier = modifier,
    ) {
        composable(Routes.TUNER) {
            TunerScreen()
        }
        composable(Routes.METRONOME) {
            MetronomeScreen()
        }
        composable(Routes.KEY_FINDER) {
            KeyFinderScreen()
        }
        composable(Routes.CHORD_FINDER) {
            ChordFinderScreen(
                onChordSelected = { _, _ -> }, // nav wired in Phase 8.5
            )
        }

        // Nested info graph — back-stack stays within this graph; bottom bar
        // remains visible throughout because the NavHost itself is in the Scaffold
        // content slot, not the bottomBar slot.
        navigation(
            startDestination = Routes.INFO_HOME,
            route = Routes.INFO_GRAPH,
        ) {
            composable(Routes.INFO_HOME) {
                InfoHomeScreen(
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable(Routes.HELP) {
                HelpScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.PRIVACY) {
                PrivacyPolicyScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.LICENSES) {
                LicensesScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.RATE_AND_SHARE) {
                RateAndShareScreen(
                    snackbarHostState = snackbarHostState,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
