package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig.Companion.BPM_MAX
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig.Companion.BPM_MIN

/**
 * A number-pad input dialog for typing a BPM value directly.
 *
 * The OK button is disabled until the entered value is within [BPM_MIN]..[BPM_MAX].
 * Accepts only digit characters; silently filters non-digits and caps input at 3 characters.
 *
 * @param initialBpm Pre-populates the text field.
 * @param onConfirm Called with the validated parsed integer when the user confirms.
 * @param onDismiss Called when the user cancels or dismisses the dialog.
 */
@Composable
internal fun BpmInputDialog(
    initialBpm: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialBpm.toString()) }
    val parsedValue = text.toIntOrNull()
    val isValid = parsedValue != null && parsedValue in BPM_MIN..BPM_MAX

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.metronome_bpm_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new ->
                    text = new.filter { it.isDigit() }.take(3)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (isValid) onConfirm(parsedValue!!) },
                ),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(parsedValue!!) },
                enabled = isValid,
            ) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
