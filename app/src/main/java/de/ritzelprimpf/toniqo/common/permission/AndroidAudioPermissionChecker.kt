package de.ritzelprimpf.toniqo.common.permission

import android.Manifest
import android.content.Context
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-platform implementation of [AudioPermissionChecker].
 *
 * Uses [ContextCompat.checkSelfPermission] with the application context to query the
 * current `RECORD_AUDIO` grant state. The application context is used (not an Activity
 * context) because this instance is a singleton — it must not hold a reference to any
 * Activity, which would cause a memory leak.
 */
@Singleton
class AndroidAudioPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioPermissionChecker {

    override fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
}
