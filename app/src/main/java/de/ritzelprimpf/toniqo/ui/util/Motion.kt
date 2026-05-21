package de.ritzelprimpf.toniqo.ui.util

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Returns `true` when the system's transition animation scale is set to zero, indicating that
 * the user has enabled the "Remove animations" accessibility setting or set the developer-option
 * "Animator duration scale" to "Animation off".
 *
 * Read once on composition (`remember` with no keys). Re-reading on every frame is unnecessary
 * because the developer-mode setting does not change during an active session; any change would
 * require a relaunch. (DESIGN.md §9)
 *
 * Callers should use this to swap 200ms cubic-bezier animations for 80ms linear ones (tuner
 * needle), or instant state changes for fade animations (success ring).
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            1f,
        ) == 0f
    }
}
