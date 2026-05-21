package de.ritzelprimpf.toniqo.tuner.fakes

import de.ritzelprimpf.toniqo.tuner.data.TunerPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory test double for [TunerPreferences].
 *
 * Each field is backed by a [MutableStateFlow] so tests can seed starting values and observe
 * writes via the [storedPresetId], [storedAutoAdvance], [storedReferencePitchHz], and
 * [storedHasRequestedAudioPermission] inspection properties.
 *
 * @param initialPresetId Starting value for [lastUsedPresetId]; `null` simulates first launch.
 * @param initialAutoAdvance Starting value for [autoAdvanceEnabled].
 * @param initialReferencePitchHz Starting value for [referencePitchHz].
 * @param initialHasRequestedAudioPermission Starting value for [hasRequestedAudioPermission].
 */
class FakeTunerPreferences(
    initialPresetId: String? = null,
    initialAutoAdvance: Boolean = true,
    initialReferencePitchHz: Double = 440.0,
    initialHasRequestedAudioPermission: Boolean = false,
) : TunerPreferences {

    private val _lastUsedPresetId = MutableStateFlow(initialPresetId)
    private val _autoAdvanceEnabled = MutableStateFlow(initialAutoAdvance)
    private val _referencePitchHz = MutableStateFlow(initialReferencePitchHz)
    private val _hasRequestedAudioPermission = MutableStateFlow(initialHasRequestedAudioPermission)

    override val lastUsedPresetId: Flow<String?> = _lastUsedPresetId.asStateFlow()
    override val autoAdvanceEnabled: Flow<Boolean> = _autoAdvanceEnabled.asStateFlow()
    override val referencePitchHz: Flow<Double> = _referencePitchHz.asStateFlow()
    override val hasRequestedAudioPermission: Flow<Boolean> = _hasRequestedAudioPermission.asStateFlow()

    override suspend fun setLastUsedPresetId(id: String) { _lastUsedPresetId.value = id }
    override suspend fun setAutoAdvanceEnabled(enabled: Boolean) { _autoAdvanceEnabled.value = enabled }
    override suspend fun setReferencePitchHz(hz: Double) { _referencePitchHz.value = hz }
    override suspend fun setHasRequestedAudioPermission(value: Boolean) { _hasRequestedAudioPermission.value = value }

    val storedPresetId: String? get() = _lastUsedPresetId.value
    val storedAutoAdvance: Boolean get() = _autoAdvanceEnabled.value
    val storedReferencePitchHz: Double get() = _referencePitchHz.value
    val storedHasRequestedAudioPermission: Boolean get() = _hasRequestedAudioPermission.value
}
