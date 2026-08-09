package de.ritzelprimpf.toniqo.tuner.fakes

import de.ritzelprimpf.toniqo.tuner.domain.repository.TonePlayer
import kotlinx.coroutines.delay

/**
 * In-memory test double for [TonePlayer].
 *
 * Records every [play] call's arguments in [playedTones] for assertions. Actually suspends via
 * [delay] rather than returning immediately, so tests exercising cancellation (a new tone
 * interrupting an in-flight one) or "the mic stays muted until playback finishes" timing behave
 * the same as production — `kotlinx-coroutines-test`'s virtual clock means this costs no real
 * wall-clock time.
 */
class FakeTonePlayer : TonePlayer {

    data class PlayedTone(val frequencyHz: Double, val durationMs: Long)

    private val _playedTones = mutableListOf<PlayedTone>()
    val playedTones: List<PlayedTone> get() = _playedTones

    override suspend fun play(frequencyHz: Double, durationMs: Long) {
        _playedTones += PlayedTone(frequencyHz, durationMs)
        delay(durationMs)
    }
}
