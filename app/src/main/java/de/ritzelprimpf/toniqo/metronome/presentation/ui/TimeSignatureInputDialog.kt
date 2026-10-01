package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * A number-pad input dialog for typing a custom time signature (numerator/denominator) directly,
 * opened from [TimeSignatureDropdown]'s "Custom…" option.
 *
 * Mirrors [BpmInputDialog]'s structure: two [OutlinedTextField]s instead of one, each filtered to
 * digits and capped at [MAX_DIGITS] characters (enough for [MetronomeConfig.TIME_SIGNATURE_NUMERATOR_MAX]
 * == 32). The OK button is disabled until both fields satisfy
 * [MetronomeConfig.isSupportedTimeSignature]; the denominator field additionally shows an inline
 * error hint when its value is syntactically a number but not one of
 * [MetronomeConfig.SUPPORTED_DENOMINATORS], since "not a power of two" isn't obvious the way
 * "out of BPM range" is.
 *
 * @param initialNumerator Pre-populates the numerator field.
 * @param initialDenominator Pre-populates the denominator field.
 * @param onConfirm Called with the validated (numerator, denominator) pair when the user confirms.
 * @param onDismiss Called when the user cancels or dismisses the dialog.
 */
@Composable
internal fun TimeSignatureInputDialog(
    initialNumerator: Int,
    initialDenominator: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var numeratorText by remember { mutableStateOf(initialNumerator.toString()) }
    var denominatorText by remember { mutableStateOf(initialDenominator.toString()) }

    val parsedNumerator = numeratorText.toIntOrNull()
    val parsedDenominator = denominatorText.toIntOrNull()
    val isValid = parsedNumerator != null && parsedDenominator != null &&
        MetronomeConfig.isSupportedTimeSignature(parsedNumerator, parsedDenominator)
    val denominatorHasRangeError = parsedDenominator != null &&
        parsedDenominator !in MetronomeConfig.SUPPORTED_DENOMINATORS

    fun confirmIfValid() {
        if (isValid) onConfirm(parsedNumerator!!, parsedDenominator!!)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.metronome_signature_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = numeratorText,
                    onValueChange = { new -> numeratorText = new.filter { it.isDigit() }.take(MAX_DIGITS) },
                    label = { Text(stringResource(R.string.metronome_signature_dialog_numerator_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                )
                Spacer(Modifier.height(Tq.Sp.s3))
                OutlinedTextField(
                    value = denominatorText,
                    onValueChange = { new -> denominatorText = new.filter { it.isDigit() }.take(MAX_DIGITS) },
                    label = { Text(stringResource(R.string.metronome_signature_dialog_denominator_label)) },
                    isError = denominatorHasRangeError,
                    supportingText = {
                        if (denominatorHasRangeError) {
                            Text(stringResource(R.string.metronome_signature_dialog_denominator_error))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirmIfValid() }),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = ::confirmIfValid, enabled = isValid) {
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

/** Digit cap for both fields — [MetronomeConfig.TIME_SIGNATURE_NUMERATOR_MAX] (32) is 2 digits; 3 leaves headroom without allowing absurd input. */
private const val MAX_DIGITS = 3
