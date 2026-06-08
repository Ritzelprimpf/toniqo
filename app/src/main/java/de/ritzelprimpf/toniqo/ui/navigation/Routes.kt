package de.ritzelprimpf.toniqo.ui.navigation

import android.net.Uri

/**
 * All navigation route strings in one place. No raw strings in composables.
 *
 * The Info section is a nested graph whose route is [INFO_GRAPH]. Sub-screens
 * push onto the nested back stack while the bottom bar stays visible.
 *
 * The Chord Finder voicings screen lives outside the bottom-nav graph so the
 * back arrow pops back to [CHORD_FINDER] correctly.
 */
object Routes {
    const val TUNER         = "tuner"
    const val METRONOME     = "metronome"
    const val KEY_FINDER    = "keyfinder"
    const val CHORD_FINDER  = "chordfinder"

    // Chord Finder — voicings detail screen
    const val ARG_ROOT_PC     = "rootPc"
    const val ARG_QUALITY     = "quality"
    const val ARG_CHORD_NAME  = "chordName"
    const val CHORD_VOICINGS  =
        "chordfinder/voicings/{$ARG_ROOT_PC}/{$ARG_QUALITY}/{$ARG_CHORD_NAME}"

    /** Builds a fully-encoded route string for navigating to the voicings screen. */
    fun chordVoicingsRoute(rootPc: Int, quality: String, chordName: String): String =
        "chordfinder/voicings/$rootPc/$quality/${Uri.encode(chordName)}"

    // Nested info graph
    const val INFO_GRAPH    = "info"
    const val INFO_HOME     = "info_home"
    const val HELP          = "help"
    const val PRIVACY       = "privacy"
    const val LICENSES      = "licenses"
    const val RATE_AND_SHARE = "rate_share"
}
