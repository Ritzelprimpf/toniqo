package de.ritzelprimpf.toniqo.tuner.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.tunerDataStore by preferencesDataStore(name = "tuner_preferences")

/**
 * DataStore-backed implementation of [TunerPreferences].
 *
 * Stores all tuner-scoped settings in a single `"tuner_preferences"` DataStore. Defaults for
 * missing keys are applied in each flow's `.map { it[KEY] ?: DEFAULT }`.
 *
 * Not unit-tested directly — DataStore requires a real [Context]. Exercised via
 * [de.ritzelprimpf.toniqo.tuner.fakes.FakeTunerPreferences] in unit tests and end-to-end
 * in Phase 5.4.
 */
class TunerPreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : TunerPreferences {

    override val lastUsedPresetId: Flow<String?> = context.tunerDataStore.data
        .map { it[LAST_USED_PRESET_ID_KEY] }

    override val autoAdvanceEnabled: Flow<Boolean> = context.tunerDataStore.data
        .map { it[AUTO_ADVANCE_ENABLED_KEY] ?: DEFAULT_AUTO_ADVANCE }

    override val referencePitchHz: Flow<Double> = context.tunerDataStore.data
        .map { it[REFERENCE_PITCH_HZ_KEY] ?: DEFAULT_REFERENCE_PITCH_HZ }

    override val hasRequestedAudioPermission: Flow<Boolean> = context.tunerDataStore.data
        .map { it[HAS_REQUESTED_AUDIO_PERMISSION_KEY] ?: false }

    override suspend fun setLastUsedPresetId(id: String) {
        context.tunerDataStore.edit { it[LAST_USED_PRESET_ID_KEY] = id }
    }

    override suspend fun setAutoAdvanceEnabled(enabled: Boolean) {
        context.tunerDataStore.edit { it[AUTO_ADVANCE_ENABLED_KEY] = enabled }
    }

    override suspend fun setReferencePitchHz(hz: Double) {
        context.tunerDataStore.edit { it[REFERENCE_PITCH_HZ_KEY] = hz }
    }

    override suspend fun setHasRequestedAudioPermission(value: Boolean) {
        context.tunerDataStore.edit { it[HAS_REQUESTED_AUDIO_PERMISSION_KEY] = value }
    }

    private companion object {
        val LAST_USED_PRESET_ID_KEY = stringPreferencesKey("last_used_preset_id")
        val AUTO_ADVANCE_ENABLED_KEY = booleanPreferencesKey("auto_advance_enabled")
        val REFERENCE_PITCH_HZ_KEY = doublePreferencesKey("reference_pitch_hz")
        val HAS_REQUESTED_AUDIO_PERMISSION_KEY = booleanPreferencesKey("has_requested_audio_permission")

        const val DEFAULT_AUTO_ADVANCE = true
        const val DEFAULT_REFERENCE_PITCH_HZ = 440.0
    }
}
