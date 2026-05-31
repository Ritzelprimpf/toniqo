package de.ritzelprimpf.toniqo.keyfinder.presentation.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Requests `RECORD_AUDIO` permission or opens system app settings after a permanent denial.
 *
 * Duplicated from `tuner.presentation.util` to avoid a cross-module dependency. The logic is
 * identical: `RECORD_AUDIO` is the same permission, and the permanently-denied heuristic requires
 * the [hasRequestedBefore] flag to distinguish "never asked" from "permanently denied" — without
 * it, a first-time tap would incorrectly route straight to app settings.
 *
 * @param activity Used for `shouldShowRequestPermissionRationale`. Null → no-op (defensive guard).
 * @param permissionLauncher The `RequestPermission` launcher from the composable.
 * @param hasRequestedBefore `true` once the system dialog has been shown at least once.
 */
fun handleKeyFinderMicAccess(
    activity: Activity?,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    hasRequestedBefore: Boolean,
) {
    if (activity == null) return
    val canShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.RECORD_AUDIO,
    )
    val isPermanentlyDenied = hasRequestedBefore && !canShowRationale && !hasMicPermission(activity)
    if (isPermanentlyDenied) {
        openAppSettings(activity)
    } else {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

/** Returns `true` when `RECORD_AUDIO` is granted in the given [context]. */
fun hasMicPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

/** Walks the [ContextWrapper] chain to find the underlying [Activity], or `null` if not found. */
fun Context.findActivity(): Activity? = when (this) {
    is Activity      -> this
    is ContextWrapper -> baseContext.findActivity()
    else             -> null
}
