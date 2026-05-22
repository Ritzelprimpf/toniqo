package de.ritzelprimpf.toniqo.common.util

import javax.inject.Inject

/**
 * A monotonic nanosecond clock.
 *
 * Injected wherever time-dependent code lives so that unit tests can virtualize time.
 * [System.nanoTime] is not virtualized by `kotlinx-coroutines-test`, making this abstraction
 * mandatory for any test that verifies timing logic.
 *
 * The production implementation wraps [System.nanoTime]; test implementations return controlled
 * values via a fake clock.
 */
interface Clock {
    /** Returns the current value of the JVM's high-resolution time source, in nanoseconds. */
    fun nanoTime(): Long
}

/** Production [Clock] backed by [System.nanoTime]. */
internal class SystemClock @Inject constructor() : Clock {
    override fun nanoTime(): Long = System.nanoTime()
}
