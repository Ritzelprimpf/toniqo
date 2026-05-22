package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.common.util.Clock
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import de.ritzelprimpf.toniqo.metronome.domain.model.clickKindFor
import de.ritzelprimpf.toniqo.metronome.domain.model.clicksPerBar

/**
 * Pure anchor-based beat scheduling state machine.
 *
 * Tracks when each click should fire and its position within the bar. Contains no `AudioTrack`
 * dependencies — all timing is computed from [Clock.nanoTime] and named math constants. This
 * makes the scheduler fully testable on the JVM with a fake clock.
 *
 * Extracted from [AudioTrackMetronomePlayer] for testability per `Phase6_2-PLAN.md` → "Tests".
 *
 * ## Scheduling model
 *
 * Target time for click at `globalClickIndex`:
 * ```
 * targetNs = anchorNs + globalClickIndex * intervalNanos(config.bpm, config.subdivision)
 * ```
 * Drift is impossible by construction: every target is computed from the fixed anchor, not
 * accumulated from the previous target.
 *
 * ## Re-anchor rules (per `Phase6-Metronome-Decisions.md` Item 2)
 *
 * - **BPM change only:** anchor = now, globalClickIndex = 0, [clickIndexInBar] unchanged.
 * - **Time-signature or subdivision change:** anchor = now, globalClickIndex = 0,
 *   [clickIndexInBar] = 0 (next click is the new downbeat).
 */
internal class BeatScheduler(
    private val clock: Clock,
    initialConfig: MetronomeConfig,
) {
    /** The currently active configuration. Updated by [onBpmChanged] and [onSignatureOrSubdivisionChanged]. */
    var config: MetronomeConfig = initialConfig
        private set

    /**
     * Zero-based position of the current click within the bar.
     * Range: `[0, clicksPerBar(config.timeSignatureNumerator, config.subdivision))`.
     */
    var clickIndexInBar: Int = 0
        private set

    private var anchorNs: Long = clock.nanoTime()
    private var globalClickIndex: Long = 0L

    /**
     * The target time (ns) for the current (not-yet-played) click.
     *
     * When [globalClickIndex] is 0 (immediately after creation or re-anchor), this equals
     * [anchorNs], meaning the first click should fire immediately.
     */
    fun targetNs(): Long =
        anchorNs + globalClickIndex * intervalNanos(config.bpm, config.subdivision)

    /**
     * Returns whether the current click is a main beat (as opposed to a subdivision-only click).
     *
     * A main beat is any click whose [clickIndexInBar] is a multiple of
     * [Subdivision.multiplier]; subdivisions fill the gaps between them.
     */
    fun isMainBeat(): Boolean =
        clickIndexInBar % config.subdivision.multiplier == 0

    /**
     * Returns the zero-based index of the current click's main beat within the bar.
     *
     * Only meaningful when [isMainBeat] is `true`; callers should check that first.
     */
    fun mainBeatIndex(): Int =
        clickIndexInBar / config.subdivision.multiplier

    /**
     * Advances past the current click. Call **after** playing the click sound.
     *
     * Increments [globalClickIndex] and updates [clickIndexInBar] cyclically within the bar.
     */
    fun advance() {
        globalClickIndex++
        clickIndexInBar = (clickIndexInBar + 1) %
            clicksPerBar(config.timeSignatureNumerator, config.subdivision)
    }

    /**
     * Re-anchors the scheduler for a BPM-only change.
     *
     * The anchor is set to now and the global click index resets to 0, so the NEXT click fires
     * exactly one new interval from now. [clickIndexInBar] is preserved — the scheduler continues
     * at its current beat position in the bar, just at the new tempo.
     */
    fun onBpmChanged(newConfig: MetronomeConfig) {
        anchorNs = clock.nanoTime()
        globalClickIndex = 0L
        config = newConfig
        // clickIndexInBar intentionally unchanged
    }

    /**
     * Re-anchors the scheduler for a time-signature or subdivision change.
     *
     * The anchor is set to now, global click index resets, and [clickIndexInBar] resets to 0
     * so the next click is the new bar's downbeat.
     */
    fun onSignatureOrSubdivisionChanged(newConfig: MetronomeConfig) {
        anchorNs = clock.nanoTime()
        globalClickIndex = 0L
        clickIndexInBar = 0
        config = newConfig
    }

    /** Returns the [de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind] for the current click. */
    fun currentClickKind() = clickKindFor(clickIndexInBar, config.subdivision)
}
