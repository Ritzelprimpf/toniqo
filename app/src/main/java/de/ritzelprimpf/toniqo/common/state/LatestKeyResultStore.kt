package de.ritzelprimpf.toniqo.common.state

import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleMatch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped in-memory holder of the Key Finder's current top result.
 *
 * Written by [de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel.KeyFinderViewModel] on
 * every recompute; read once by [de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel.ChordFinderViewModel]
 * at initialisation to seed the initial root/mode selection.
 *
 * In-memory storage suffices because the seed is a convenience, not durable state — Chord Finder's
 * own [de.ritzelprimpf.toniqo.chordfinder.domain.repository.ChordFinderSelectionRepository]
 * holds the durable selection.
 */
@Singleton
class LatestKeyResultStore @Inject constructor() {

    private val _topResult = MutableStateFlow<ScaleMatch?>(null)

    /** The Key Finder's most recently computed top result, or `null` if no search has run. */
    val topResult: StateFlow<ScaleMatch?> = _topResult.asStateFlow()

    /** Replaces the current top result. Called from the Key Finder ViewModel's recompute path. */
    fun publish(top: ScaleMatch?) {
        _topResult.value = top
    }
}
