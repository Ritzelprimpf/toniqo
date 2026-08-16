package de.ritzelprimpf.toniqo.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * Shared "About this screen" dialog: an [Tq.Type.H2] [title], a [Tq.Type.Body] [body], and a
 * single mint "Got it" confirm button — used by every top-level screen's info button (Key
 * Finder, Chord Finder, Metronome) behind [ScreenHeader]'s `trailingAction` slot.
 *
 * Only [title] and [body] are screen-specific; the dialog chrome and the confirm button's
 * shared `common_info_dialog_ok` text are identical everywhere, so they live here once instead
 * of being copy-pasted per screen — see the 2026-08-15 DECISIONS.md entry.
 *
 * @param title The dialog's heading, e.g. "About Chord Finder".
 * @param body The explanatory body text.
 * @param onDismiss Called when the dialog is dismissed (backdrop tap, back gesture, or the
 *   confirm button).
 */
@Composable
fun InfoDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = Tq.Type.H2,
                color = Tq.Color.FgPrimary,
            )
        },
        text = {
            Text(
                text = body,
                style = Tq.Type.Body,
                color = Tq.Color.FgSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.common_info_dialog_ok),
                    style = Tq.Type.Body,
                    color = Tq.Color.SignalMint,
                )
            }
        },
    )
}
