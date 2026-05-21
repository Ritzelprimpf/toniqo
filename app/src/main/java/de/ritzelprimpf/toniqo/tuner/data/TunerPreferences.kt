package de.ritzelprimpf.toniqo.tuner.data

import kotlinx.coroutines.flow.Flow

/**
 * Persists tuner-scoped user preferences across app launches.
 *
 * All values are exposed as [Flow]s so the ViewModel can react to changes in real time.
 * The DataStore-backed implementation ([TunerPreferencesImpl]) is the production source;
 * [de.ritzelprimpf.toniqo.tuner.fakes.FakeTunerPreferences] is used in unit tests.
 */
interface TunerPreferences {

    /**
     * The stable [de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset.id] of the last preset
     * the user selected, or `null` on first launch (before any selection has been made).
     */
    val lastUsedPresetId: Flow<String?>

    /**
     * Whether the tuner should automatically advance to the next string when the current string
     * is held in tune. Defaults to `true`. Persisted across launches.
     */
    val autoAdvanceEnabled: Flow<Boolean>

    /**
     * The reference frequency of A4 in Hz. Defaults to `440.0`. Only `440.0` and `432.0` are
     * supported values; other values may be stored but are not produced by the UI.
     */
    val referencePitchHz: Flow<Double>

    /**
     * Whether the `RECORD_AUDIO` permission has been requested at least once. Used to distinguish
     * "never asked" (first launch) from "permanently denied" — see `TunerPermissionHandling.kt`.
     * Defaults to `false`.
     */
    val hasRequestedAudioPermission: Flow<Boolean>

    /** Persists [id] as the last-used preset. */
    suspend fun setLastUsedPresetId(id: String)

    /** Persists the auto-advance [enabled] state. */
    suspend fun setAutoAdvanceEnabled(enabled: Boolean)

    /** Persists the reference pitch [hz] value. */
    suspend fun setReferencePitchHz(hz: Double)

    /**
     * Records that the system permission dialog has been shown at least once.
     * Call this from the `RequestPermission` launcher's `onResult` callback.
     */
    suspend fun setHasRequestedAudioPermission(value: Boolean)
}
