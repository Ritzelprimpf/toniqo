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
    const val ARG_ROOT_PC         = "rootPc"
    const val ARG_QUALITY         = "quality"
    const val ARG_CHORD_NAME      = "chordName"
    const val ARG_SEVENTH_QUALITY = "seventhQuality"
    const val CHORD_VOICINGS      =
        "chordfinder/voicings/{$ARG_ROOT_PC}/{$ARG_QUALITY}/{$ARG_CHORD_NAME}" +
            "?$ARG_SEVENTH_QUALITY={$ARG_SEVENTH_QUALITY}"

    /**
     * Builds a fully-encoded route string for navigating to the voicings screen.
     *
     * [seventhQuality] is the [de.ritzelprimpf.toniqo.chordfinder.domain.model.SeventhQuality]
     * name, or `null` for a plain triad — appended as an optional query parameter so triad
     * navigation (the common case) keeps the same URL shape it always had.
     */
    fun chordVoicingsRoute(rootPc: Int, quality: String, chordName: String, seventhQuality: String? = null): String {
        val base = "chordfinder/voicings/$rootPc/$quality/${Uri.encode(chordName)}"
        return if (seventhQuality != null) "$base?$ARG_SEVENTH_QUALITY=$seventhQuality" else base
    }

    // Nested info graph
    const val INFO_GRAPH       = "info"
    const val INFO_HOME        = "info_home"
    const val HELP             = "help"
    const val LICENSES         = "licenses"
    const val BUG_REPORT       = "bug_report"
    const val FEATURE_REQUEST  = "feature_request"
    const val DATA_PRIVACY     = "data_privacy"
}
