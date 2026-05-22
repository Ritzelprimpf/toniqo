package de.ritzelprimpf.toniqo.metronome.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Side-effect composable that adds [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON] while
 * [isPlaying] is true and removes it on disposal or when [isPlaying] becomes false.
 *
 * Keyed on [isPlaying] so the effect re-fires on each state transition. The [onDispose]
 * block always clears the flag — covering both the `isPlaying = false` transition and the
 * composable leaving the composition entirely (e.g., user navigates away while playing).
 *
 * Per Phase6-Metronome-Decisions.md Item 14.
 */
@Composable
internal fun KeepScreenOnWhilePlaying(isPlaying: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(isPlaying) {
        if (isPlaying) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
