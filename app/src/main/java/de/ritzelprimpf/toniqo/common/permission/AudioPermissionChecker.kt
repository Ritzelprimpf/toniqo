package de.ritzelprimpf.toniqo.common.permission

/**
 * Checks whether the calling app currently holds the `RECORD_AUDIO` permission.
 *
 * This interface does **not** request the permission — it only reports the current grant state.
 * Permission requesting is the responsibility of the UI layer, via the Activity Result API.
 *
 * The concrete implementation is [AndroidAudioPermissionChecker]; the abstraction exists so that
 * the audio source and tests can use a simple fake without touching Android framework APIs.
 */
interface AudioPermissionChecker {

    /**
     * Returns `true` if `Manifest.permission.RECORD_AUDIO` is currently granted,
     * `false` otherwise.
     */
    fun hasRecordAudioPermission(): Boolean
}
