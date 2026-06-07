package de.ritzelprimpf.toniqo.common.state

import de.ritzelprimpf.toniqo.common.model.GuitarTuning
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pairs a [GuitarTuning] with the human-readable label sourced from [TunerPreset.displayName].
 *
 * @property tuning The guitar tuning (string notes).
 * @property label Display label suitable for UI (e.g. `"E Standard"`, `"E♭ Standard"`).
 */
data class TuningWithLabel(
    val tuning: GuitarTuning,
    val label: String,
)

/**
 * App-scoped in-memory holder of the Guitar Tuner's currently active tuning.
 *
 * Written by [de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerViewModel] whenever the
 * user selects a preset; read by
 * [de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel.ChordVoicingsViewModel] to select
 * the correct voicing tier and to display the active tuning name.
 *
 * Default before the Tuner has been used in the current session is [GuitarTuning.STANDARD_6]
 * labelled `"E Standard"`. In-memory storage suffices — the tuner persists its own last-used
 * preset ID and re-publishes on the next Tuner screen open.
 */
@Singleton
class SelectedTuningStore @Inject constructor() {

    private val _selection = MutableStateFlow(
        TuningWithLabel(GuitarTuning.STANDARD_6, DEFAULT_LABEL),
    )

    /** The active tuning paired with its display label. Defaults to E Standard / [GuitarTuning.STANDARD_6]. */
    val selection: StateFlow<TuningWithLabel> = _selection.asStateFlow()

    /** Replaces the current tuning + label. Called from the Tuner ViewModel on preset load or change. */
    fun publish(tuning: GuitarTuning, label: String) {
        _selection.value = TuningWithLabel(tuning, label)
    }

    private companion object {
        const val DEFAULT_LABEL = "E Standard"
    }
}
