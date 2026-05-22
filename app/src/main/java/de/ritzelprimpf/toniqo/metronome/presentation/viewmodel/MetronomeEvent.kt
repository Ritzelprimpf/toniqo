package de.ritzelprimpf.toniqo.metronome.presentation.viewmodel

/**
 * One-shot events emitted by [MetronomeViewModel] that must not be modelled as persistent state.
 *
 * Collected via [MetronomeViewModel.events] ([kotlinx.coroutines.flow.SharedFlow]). The screen
 * consumes each event exactly once and must not replay it after re-composition.
 */
sealed interface MetronomeEvent {

    /** Audio could not be started or was lost mid-session (focus denied, AudioTrack init failure). */
    data object AudioUnavailable : MetronomeEvent
}
