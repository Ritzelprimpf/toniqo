package de.ritzelprimpf.toniqo.ui.navigation

/**
 * All navigation route strings in one place. No raw strings in composables.
 *
 * The Info section is a nested graph whose route is [INFO_GRAPH]. Sub-screens
 * push onto the nested back stack while the bottom bar stays visible.
 */
object Routes {
    const val TUNER         = "tuner"
    const val METRONOME     = "metronome"
    const val KEY_FINDER    = "keyfinder"
    const val CHORD_FINDER  = "chordfinder"

    // Nested info graph
    const val INFO_GRAPH    = "info"
    const val INFO_HOME     = "info_home"
    const val HELP          = "help"
    const val PRIVACY       = "privacy"
    const val LICENSES      = "licenses"
    const val RATE_AND_SHARE = "rate_share"
}
