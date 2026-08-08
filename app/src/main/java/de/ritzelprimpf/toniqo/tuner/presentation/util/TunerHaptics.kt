package de.ritzelprimpf.toniqo.tuner.presentation.util

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Tactile pulse fired when a string reaches sustained-in-tune: one `LongPress` click.
 *
 * Matches the §8.1 spec: "One tactile pulse via the standard system haptic."
 */
fun HapticFeedback.tunedStringHaptic() = performHapticFeedback(HapticFeedbackType.LongPress)

/**
 * Tactile pulse fired when auto-advance switches the target to the next string.
 *
 * Deliberately a lighter, distinct pattern from [tunedStringHaptic] — that pulse means "this
 * string is in tune," this one means "we moved on." Using the same feel for both would make the
 * advance itself unnoticeable, which is the exact gap this event exists to fix.
 */
fun HapticFeedback.stringAdvancedHaptic() = performHapticFeedback(HapticFeedbackType.TextHandleMove)

/**
 * Tactile pulse fired when all strings are in tune. Same weight as [tunedStringHaptic] in v1;
 * a distinct heavier pattern can be substituted here without touching call sites.
 */
fun HapticFeedback.allTunedHaptic() = performHapticFeedback(HapticFeedbackType.LongPress)
