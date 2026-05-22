package de.ritzelprimpf.toniqo.metronome.fakes

import de.ritzelprimpf.toniqo.common.util.Clock

/**
 * Test double for [Clock] that returns a manually controlled time value.
 *
 * Call [advanceBy] to step time forward before each interaction under test. Unlike
 * [System.nanoTime], this fake is fully deterministic — no real-time delays to wait for.
 *
 * @param initialNanos Starting clock value in nanoseconds.
 */
class FakeClock(initialNanos: Long = 0L) : Clock {

    private var nowNanos: Long = initialNanos

    override fun nanoTime(): Long = nowNanos

    /** Sets the clock to an absolute [nanos] value. */
    fun setNow(nanos: Long) {
        nowNanos = nanos
    }

    /** Advances the clock forward by [deltaNanos] nanoseconds. */
    fun advanceBy(deltaNanos: Long) {
        nowNanos += deltaNanos
    }
}
