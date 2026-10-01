package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision

/** Seconds in one minute. Used to convert BPM to a per-click sample interval. */
internal const val SECONDS_PER_MINUTE = 60.0

/**
 * The time-signature denominator BPM is defined relative to: a quarter note. At this denominator
 * the scaling term in [samplesPerClick] is 1 (no-op) — exactly the previous (pre-denominator-
 * scaling) behavior. See `docs/DECISIONS.md` 2026-10-01 "time-signature denominator now scales
 * click speed" entry for why this exists.
 */
internal const val BPM_REFERENCE_DENOMINATOR = 4

/**
 * Returns the exact (fractional) number of samples between consecutive clicks at the given [bpm],
 * [subdivision], [denominator], and [sampleRateHz].
 *
 * BPM is always interpreted as quarter notes per minute, regardless of time signature. A
 * denominator smaller than [BPM_REFERENCE_DENOMINATOR] (a half note, 2) lengthens the interval;
 * a larger one (an eighth note, 8) shortens it — e.g. an /8 signature clicks exactly twice as
 * fast as a /4 signature at the same BPM, because an eighth note is half a quarter note's
 * duration. This matches how "BPM" is conventionally understood: a fixed note-value pulse that
 * the time signature's denominator names, not an arbitrary click rate decoupled from it.
 *
 * Returned as a [Double] rather than rounded to a whole sample count so that [BeatScheduler] can
 * compute each click's target sample fresh from its anchor
 * (`anchorSample + clickIndex * samplesPerClick`) without ever accumulating rounding error —
 * the same anti-drift approach the player used when it scheduled in nanoseconds, now applied to
 * the coarser (by comparison) sample-rate grid.
 *
 * Examples at 48 000 Hz, 120 BPM:
 * - denominator 4, NONE → 24 000.0 samples (500 ms)
 * - denominator 4, EIGHTHS → 12 000.0 samples (250 ms)
 * - denominator 8, NONE → 12 000.0 samples (250 ms) — twice as fast as denominator 4
 *
 * @param bpm Beats per minute in [1, 300], always a quarter-note pulse.
 * @param subdivision Active subdivision; its [Subdivision.multiplier] is used as the divisor.
 * @param denominator The time signature's denominator (e.g. 4 or 8).
 * @param sampleRateHz Output sample rate in Hz.
 * @return Positive, exact sample interval between clicks.
 */
internal fun samplesPerClick(bpm: Int, subdivision: Subdivision, denominator: Int, sampleRateHz: Int): Double =
    sampleRateHz * SECONDS_PER_MINUTE * BPM_REFERENCE_DENOMINATOR / (bpm * subdivision.multiplier * denominator)
