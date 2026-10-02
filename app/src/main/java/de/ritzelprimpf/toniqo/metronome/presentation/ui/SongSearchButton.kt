package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.ritzelprimpf.toniqo.R

/**
 * A 60dp circle button next to TAP that opens the song-tempo search sheet.
 *
 * Shows a search icon above the uppercase "SONG" label (see [CircleLabelButton]).
 */
@Composable
internal fun SongSearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CircleLabelButton(
        icon = Icons.Outlined.Search,
        label = stringResource(R.string.metronome_song_search),
        onClick = onClick,
        modifier = modifier,
    )
}
