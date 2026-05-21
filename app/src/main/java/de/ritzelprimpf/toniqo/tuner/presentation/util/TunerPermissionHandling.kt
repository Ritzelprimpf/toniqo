package de.ritzelprimpf.toniqo.tuner.presentation.util

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
 * The `isPermanentlyDenied` heuristic requires [hasRequestedBefore] to distinguish "never asked"
 * (where `shouldShowRequestPermissionRationale` is also false) from "permanently denied".
 * Without this flag, a first-time button tap would incorrectly route to app settings.
 * (See DECISIONS.md entry 13: `hasRequestedAudioPermission` preference.)
 *
 * @param activity Used for `shouldShowRequestPermissionRationale` check. Null-safe; a null
 *   value is a defensive guard — should never occur in a normal Compose screen lifecycle.
 * @param permissionLauncher The `ActivityResultContracts.RequestPermission` launcher.
 * @param hasRequestedBefore `true` when the user has previously seen the system dialog.
 *   Derived from `TunerUiState.hasRequestedAudioPermission`.
 */
fun handleGrantAccess(
    activity: Activity?,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    hasRequestedBefore: Boolean,
) {
    if (activity == null) return
    val canShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.RECORD_AUDIO,
    )
    val isPermanentlyDenied = hasRequestedBefore && !canShowRationale && !hasPermission(activity)
    if (isPermanentlyDenied) {
        openAppSettings(activity)
    } else {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

private fun hasPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

/**
 * Walks the [ContextWrapper] chain to find the underlying [Activity], or returns null if
 * the context does not originate from an Activity (e.g., a service context).
 */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
