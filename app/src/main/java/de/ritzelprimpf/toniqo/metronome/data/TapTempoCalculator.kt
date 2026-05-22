package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.common.util.Clock
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Computes BPM from tap timestamps using a rolling window of the most recent taps.
 *
 * ## Algorithm (per `Phase6-Metronome-Decisions.md` Item 6)
 *
 * - Maintains a rolling window of the last [WINDOW_SIZE] tap timestamps.
 * - After every tap, computes the simple mean of the intervals between consecutive taps.
 * - Returns `null` on the first tap of a session (only one timestamp — no interval yet).
 * - If the gap since the previous tap exceeds [RESET_TIMEOUT_MS], the window is cleared before
 *   recording the new tap and `null` is returned (the user has paused; this is the first tap
 *   of a new session).
 * - The resulting BPM is clamped to [[MetronomeConfig.BPM_MIN], [MetronomeConfig.BPM_MAX]] and
 *   rounded to the nearest integer.
 *
 * ## Testability
 *
 * [Clock] is injected so that unit tests can supply deterministic timestamps without sleeping.
 */
internal class TapTempoCalculator @Inject constructor(
    private val clock: Clock,
) {
    private val windowMs: ArrayDeque<Long> = ArrayDeque()

    /**
     * Records a tap at the current clock time.
     *
     * @return The estimated BPM in [[MetronomeConfig.BPM_MIN], [MetronomeConfig.BPM_MAX]],
     *   or `null` if this is the first tap of the current session (not enough taps yet).
     */
    fun onTap(): Int? {
        val nowMs = clock.nanoTime() / NANOS_PER_MS

        // If the previous tap was more than RESET_TIMEOUT_MS ago, begin a new session.
        if (windowMs.isNotEmpty() && nowMs - windowMs.last() > RESET_TIMEOUT_MS) {
            windowMs.clear()
        }

        windowMs.addLast(nowMs)
        // Keep only the most recent WINDOW_SIZE taps.
        while (windowMs.size > WINDOW_SIZE) windowMs.removeFirst()

        if (windowMs.size < MIN_TAPS_FOR_BPM) return null

        val intervals = windowMs.zipWithNext { a, b -> (b - a).toDouble() }
        val meanIntervalMs = intervals.average()
        val bpm = (MILLIS_PER_MINUTE / meanIntervalMs).roundToInt()
        return bpm.coerceIn(MetronomeConfig.BPM_MIN, MetronomeConfig.BPM_MAX)
    }

    /** Clears the rolling window. Call when the user leaves the metronome screen. */
    fun reset() {
        windowMs.clear()
    }

    private companion object {
        /** Number of tap timestamps kept in the rolling window (4 intervals available). */
        const val WINDOW_SIZE = 5

        /** Gap between taps that signals a new tap-tempo session, in milliseconds. */
        const val RESET_TIMEOUT_MS = 2_000L

        /** Minimum number of taps needed before a BPM can be emitted. */
        const val MIN_TAPS_FOR_BPM = 2

        /** Milliseconds per minute; used to convert interval lengths to BPM. */
        const val MILLIS_PER_MINUTE = 60_000.0

        /** Nanoseconds per millisecond; used to convert [Clock.nanoTime] to ms. */
        private const val NANOS_PER_MS = 1_000_000L
    }
}
