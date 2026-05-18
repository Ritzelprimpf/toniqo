package de.ritzelprimpf.toniqo.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector
import de.ritzelprimpf.toniqo.R

/**
 * Describes one destination in the bottom navigation bar.
 *
 * [route] is the navigation route to pop/restore to (the top-level route
 * or the nested graph route for the Info section).
 */
data class BottomNavDestination(
    val route: String,
    @StringRes val labelResId: Int,
    @StringRes val contentDescriptionResId: Int,
    /** Icon used when this tab is not selected. */
    val outlinedIcon: ImageVector,
    /** Icon used when this tab is selected (per DESIGN.md §7: filled for active nav). */
    val filledIcon: ImageVector,
)

/** The five bottom-nav destinations, in display order. */
val bottomNavDestinations: List<BottomNavDestination> = listOf(
    BottomNavDestination(
        route                    = Routes.TUNER,
        labelResId               = R.string.nav_label_tuner,
        contentDescriptionResId  = R.string.nav_cd_tuner,
        // TODO: Replace with custom `tuner` icon from DESIGN.md §7
        outlinedIcon             = Icons.Outlined.GraphicEq,
        filledIcon               = Icons.Filled.GraphicEq,
    ),
    BottomNavDestination(
        route                    = Routes.METRONOME,
        labelResId               = R.string.nav_label_metronome,
        contentDescriptionResId  = R.string.nav_cd_metronome,
        // TODO: Replace with custom `metronome` icon from DESIGN.md §7
        outlinedIcon             = Icons.Outlined.Alarm,
        filledIcon               = Icons.Filled.Alarm,
    ),
    BottomNavDestination(
        route                    = Routes.KEY_FINDER,
        labelResId               = R.string.nav_label_keyfinder,
        contentDescriptionResId  = R.string.nav_cd_keyfinder,
        // TODO: Replace with custom `key` icon from DESIGN.md §7
        outlinedIcon             = Icons.Outlined.MusicNote,
        filledIcon               = Icons.Filled.MusicNote,
    ),
    BottomNavDestination(
        route                    = Routes.CHORD_FINDER,
        labelResId               = R.string.nav_label_chordfinder,
        contentDescriptionResId  = R.string.nav_cd_chordfinder,
        // TODO: Replace with custom `chord` icon from DESIGN.md §7
        outlinedIcon             = Icons.Outlined.LibraryMusic,
        filledIcon               = Icons.Filled.LibraryMusic,
    ),
    BottomNavDestination(
        route                    = Routes.INFO_GRAPH,
        labelResId               = R.string.nav_label_more,
        contentDescriptionResId  = R.string.nav_cd_more,
        // TODO: Replace with custom `more` icon from DESIGN.md §7
        outlinedIcon             = Icons.Outlined.MoreHoriz,
        filledIcon               = Icons.Filled.MoreHoriz,
    ),
)
