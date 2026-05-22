package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision

/** Nanoseconds in one minute. Used to convert BPM to per-beat intervals without precision loss. */
internal const val NANOS_PER_MINUTE = 60_000_000_000L

/** Nanoseconds per millisecond. Used when converting a nanosecond sleep duration to [delay] ms. */
internal const val NANOS_PER_MS = 1_000_000L

/**
 * Returns the nanosecond interval between consecutive clicks at the given [bpm] and [subdivision].
 *
 * Formula: `NANOS_PER_MINUTE / bpm / subdivision.multiplier`.
 *
 * Examples:
 * - 120 BPM, NONE  → 500 000 000 ns (500 ms)
 * - 120 BPM, EIGHTHS → 250 000 000 ns (250 ms)
 * - 60 BPM, TRIPLETS → 333 333 333 ns (~333 ms)
 *
 * @param bpm Beats per minute in [1, 300].
 * @param subdivision Active subdivision; its [Subdivision.multiplier] is used as the divisor.
 * @return Positive nanosecond interval between clicks.
 */
internal fun intervalNanos(bpm: Int, subdivision: Subdivision): Long =
    NANOS_PER_MINUTE / bpm / subdivision.multiplier
