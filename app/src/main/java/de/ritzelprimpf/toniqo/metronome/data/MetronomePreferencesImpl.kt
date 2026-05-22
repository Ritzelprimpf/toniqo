package de.ritzelprimpf.toniqo.metronome.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.metronomeDataStore by preferencesDataStore(name = "metronome_preferences")

/**
 * DataStore-backed implementation of [MetronomePreferences].
 *
 * Stores BPM, time-signature numerator and denominator, and subdivision name in a dedicated
 * `metronome_preferences` DataStore file — separate from the tuner's (per
 * `Phase6-Metronome-Decisions.md` Item 4).
 *
 * ## Validation and self-healing
 *
 * On each read the raw preferences are validated by [validateOrDefault]. If the stored values are
 * invalid (out-of-range BPM, unsupported signature, unrecognised subdivision name), the entire
 * config is replaced by [MetronomeConfig.DEFAULT] and the corrected values are written back so
 * subsequent reads are clean. First-launch (all keys absent) silently falls through to DEFAULT
 * without triggering a write-back.
 *
 * ## Not directly unit-tested
 *
 * DataStore requires a real [Context]. Unit tests use `FakeMetronomePreferences` instead.
 * This implementation is exercised via the ViewModel integration test in Phase 6.3 and the
 * manual smoke test in Phase 6.2.
 */
internal class MetronomePreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : MetronomePreferences {

    private val keyBpm = intPreferencesKey("bpm")
    private val keyNumerator = intPreferencesKey("time_sig_numerator")
    private val keyDenominator = intPreferencesKey("time_sig_denominator")
    private val keySubdivision = stringPreferencesKey("subdivision")

    override val config: Flow<MetronomeConfig> = context.metronomeDataStore.data
        .map { prefs ->
            val raw = RawMetronomeConfig(
                bpm = prefs[keyBpm],
                numerator = prefs[keyNumerator],
                denominator = prefs[keyDenominator],
                subdivisionName = prefs[keySubdivision],
            )
            val validated = validateOrDefault(raw)
            // Self-healing write-back: only if the persisted form differs from the validated form.
            // Avoids write-back churn on every read of a clean config or a first-launch (all-null)
            // config.
            if (raw.requiresRepair(validated)) {
                context.metronomeDataStore.edit { writeAll(it, validated) }
            }
            validated
        }

    override suspend fun setConfig(config: MetronomeConfig) {
        context.metronomeDataStore.edit { writeAll(it, config) }
    }

    private fun writeAll(prefs: MutablePreferences, config: MetronomeConfig) {
        prefs[keyBpm] = config.bpm
        prefs[keyNumerator] = config.timeSignatureNumerator
        prefs[keyDenominator] = config.timeSignatureDenominator
        prefs[keySubdivision] = config.subdivision.name
    }
}
