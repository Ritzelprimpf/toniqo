package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchFailure
import de.ritzelprimpf.toniqo.metronome.domain.model.SongTempo
import de.ritzelprimpf.toniqo.metronome.domain.model.SongTimeSignature
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.SongSearchStatus
import de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.SongSearchUiState
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme
import java.util.Locale

/** GetSongBPM's terms require a visible link back to their site wherever their data is used. */
private const val ATTRIBUTION_URL = "https://getsongbpm.com"

// Minimum tap target per DESIGN.md §13.4 for the attribution link row.
private val MIN_TAP_TARGET = 44.dp

/**
 * Bottom sheet for looking up a song's tempo (DESIGN.md §8.2 "Song search sheet").
 *
 * Follows the existing sheet pattern ([de.ritzelprimpf.toniqo.tuner.presentation.ui.components.PresetPickerSheet]):
 * `bg.elev1` container, kicker title + close button header. Below it: a required title field and
 * an optional artist field that narrows the results — both search on the keyboard's search
 * action only — a status area (hint / spinner / results / message), and a permanent GetSongBPM
 * attribution link.
 *
 * Stateless — [state] and the callbacks come from [de.ritzelprimpf.toniqo.metronome.presentation.viewmodel.SongSearchViewModel].
 *
 * @param onSongSelected Called with the tapped song; the caller applies it and closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SongSearchSheet(
    state: SongSearchUiState,
    onQueryChanged: (String) -> Unit,
    onArtistQueryChanged: (String) -> Unit,
    onSearchSubmitted: () -> Unit,
    onSongSelected: (SongTempo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Tq.Color.BgElev1,
    ) {
        SongSearchSheetContent(
            state = state,
            onQueryChanged = onQueryChanged,
            onArtistQueryChanged = onArtistQueryChanged,
            onSearchSubmitted = onSearchSubmitted,
            onSongSelected = onSongSelected,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun SongSearchSheetContent(
    state: SongSearchUiState,
    onQueryChanged: (String) -> Unit,
    onArtistQueryChanged: (String) -> Unit,
    onSearchSubmitted: () -> Unit,
    onSongSelected: (SongTempo) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Opening the sheet is an explicit "I want to search" — put the cursor in the title field.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val submit = {
        keyboard?.hide()
        onSearchSubmitted()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Tq.Sp.s5, end = Tq.Sp.s2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.metronome_song_sheet_title).uppercase(Locale.ROOT),
                style = Tq.Type.Kicker,
                color = Tq.Color.FgTertiary,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.metronome_song_cd_close),
                    tint = Tq.Color.FgSecondary,
                )
            }
        }

        SearchField(
            value = state.query,
            onValueChange = onQueryChanged,
            placeholder = stringResource(R.string.metronome_song_search_placeholder),
            clearContentDescription = stringResource(R.string.metronome_song_cd_clear_title),
            icon = Icons.Outlined.Search,
            onSearch = submit,
            modifier = Modifier.focusRequester(focusRequester),
        )
        Spacer(Modifier.height(Tq.Sp.s2))
        SearchField(
            value = state.artistQuery,
            onValueChange = onArtistQueryChanged,
            placeholder = stringResource(R.string.metronome_song_artist_placeholder),
            clearContentDescription = stringResource(R.string.metronome_song_cd_clear_artist),
            icon = Icons.Outlined.Person,
            onSearch = submit,
        )
        Spacer(Modifier.height(Tq.Sp.s3))

        SongSearchStatusArea(status = state.status, onSongSelected = onSongSelected)

        AttributionLink()
    }
}

/**
 * One of the sheet's single-line search inputs; the keyboard's search action submits. Shows a
 * trailing × that empties the field while it has text.
 */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearContentDescription: String,
    icon: ImageVector,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,  // the placeholder describes the field
                tint = Tq.Color.FgTertiary,
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = clearContentDescription,
                        tint = Tq.Color.FgTertiary,
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        textStyle = Tq.Type.Body,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Tq.Sp.s5),
    )
}

@Composable
private fun ColumnScope.SongSearchStatusArea(
    status: SongSearchStatus,
    onSongSelected: (SongTempo) -> Unit,
) {
    when (status) {
        SongSearchStatus.Idle -> StatusMessage(stringResource(R.string.metronome_song_search_hint))
        SongSearchStatus.Loading -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Tq.Sp.s6),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = Tq.Color.SignalMint,
                trackColor = Tq.Color.BgElev2,
            )
        }
        SongSearchStatus.NoResults -> StatusMessage(stringResource(R.string.metronome_song_no_results))
        is SongSearchStatus.Failed -> StatusMessage(failureMessage(status.reason))
        is SongSearchStatus.Results -> LazyColumn(
            // weight(fill = false): the list takes only the height it needs, but shrinks to keep
            // the attribution link visible when results overflow the sheet.
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
        ) {
            items(status.songs, key = { it.id }) { song ->
                SongRow(song = song, onClick = { onSongSelected(song) })
            }
        }
    }
}

@Composable
private fun failureMessage(reason: SongSearchFailure): String = when (reason) {
    SongSearchFailure.NO_CONNECTION -> stringResource(R.string.metronome_song_error_connection)
    SongSearchFailure.SERVICE_UNAVAILABLE -> stringResource(R.string.metronome_song_error_unavailable)
}

@Composable
private fun StatusMessage(text: String) {
    Text(
        text = text,
        style = Tq.Type.Body,
        color = Tq.Color.FgTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Tq.Sp.s5, vertical = Tq.Sp.s6),
    )
}

@Composable
private fun SongRow(
    song: SongTempo,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Tq.Sp.s5, vertical = Tq.Sp.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = Tq.Type.BodyStrong,
                color = Tq.Color.FgPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            song.artist?.let { artist ->
                Text(
                    text = artist,
                    style = Tq.Type.Caption,
                    color = Tq.Color.FgTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(Tq.Sp.s3))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.metronome_song_bpm_format, song.bpm),
                style = Tq.Type.NumericM,
                color = Tq.Color.FgPrimary,
            )
            song.timeSignature?.let { signature ->
                Text(
                    text = stringResource(
                        R.string.metronome_song_time_signature_format,
                        signature.numerator,
                        signature.denominator,
                    ),
                    style = Tq.Type.KickerS,
                    color = Tq.Color.FgTertiary,
                )
            }
        }
    }
}

@Composable
private fun AttributionLink() {
    val uriHandler = LocalUriHandler.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TAP_TARGET)
            .clickable { uriHandler.openUri(ATTRIBUTION_URL) }
            .padding(horizontal = Tq.Sp.s5, vertical = Tq.Sp.s3),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.metronome_song_attribution),
            style = Tq.Type.Caption,
            color = Tq.Color.FgTertiary,
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────
// Previews render the sheet content directly: ModalBottomSheet opens a separate window that
// the preview pane doesn't show.

@Preview(name = "SongSearchSheet — Results", showBackground = true, backgroundColor = 0xFF222729)
@Composable
private fun SongSearchSheetPreviewResults() {
    ToniqoTheme(useDarkTheme = true) {
        SongSearchSheetContent(
            state = SongSearchUiState(
                query = "Master of Puppets",
                artistQuery = "Metallica",
                status = SongSearchStatus.Results(
                    listOf(
                        SongTempo("o2r0L", "Master of Puppets", "Metallica", 220, SongTimeSignature(4, 4)),
                        SongTempo("x1", "Master of Puppets (Live)", "Metallica", 212, null),
                    ),
                ),
            ),
            onQueryChanged = {}, onArtistQueryChanged = {}, onSearchSubmitted = {}, onSongSelected = {}, onDismiss = {},
        )
    }
}

@Preview(name = "SongSearchSheet — Idle", showBackground = true, backgroundColor = 0xFF222729)
@Composable
private fun SongSearchSheetPreviewIdle() {
    ToniqoTheme(useDarkTheme = true) {
        SongSearchSheetContent(
            state = SongSearchUiState(),
            onQueryChanged = {}, onArtistQueryChanged = {}, onSearchSubmitted = {}, onSongSelected = {}, onDismiss = {},
        )
    }
}

@Preview(name = "SongSearchSheet — No connection", showBackground = true, backgroundColor = 0xFF222729)
@Composable
private fun SongSearchSheetPreviewError() {
    ToniqoTheme(useDarkTheme = true) {
        SongSearchSheetContent(
            state = SongSearchUiState(
                query = "Enter Sandman",
                status = SongSearchStatus.Failed(SongSearchFailure.NO_CONNECTION),
            ),
            onQueryChanged = {}, onArtistQueryChanged = {}, onSearchSubmitted = {}, onSongSelected = {}, onDismiss = {},
        )
    }
}
