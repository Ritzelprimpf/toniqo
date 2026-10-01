package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import de.ritzelprimpf.toniqo.metronome.domain.model.clickKindFor
import de.ritzelprimpf.toniqo.metronome.domain.model.clicksPerBar
import kotlin.math.roundToLong

/**
 * Pure anchor-based beat scheduling state machine, operating in **output-sample position**
 * rather than wall-clock time.
 *
 * Tracks when each click should play (as a position in the continuously-generated PCM stream)
 * and its position within the bar. Contains no `AudioTrack` or `Clock` dependency — all timing is
 * expressed relative to a sample position supplied by the caller, which makes this fully testable
 * on the JVM with plain `Long`s, no fake clock needed.
 *
 * ## Why sample position, not wall-clock time
 *
 * The original design anchored to `Clock.nanoTime()` and the player loop played one click, then
 * `delay()`d until the next click's wall-clock target — writing nothing to `AudioTrack` in
 * between. That gap (silence = no write, not a zero-samples write) let the output path go idle
 * at low BPM, and resuming it after an idle gap produced an audible glitch — see
 * [ClickStreamRenderer] and `AudioTrackMetronomePlayer`'s class doc for the full story and the
 * field evidence that drove this rewrite (`docs/DECISIONS.md`, 2026-10-01 entry).
 *
 * The fix is to keep the `AudioTrack` fed with a *continuous* stream — silence explicitly written
 * as zero-valued samples, never a gap — so the output path never goes idle in the first place.
 * Once every sample is explicitly generated, "when" is naturally expressed as "which sample index"
 * rather than "at which wall-clock instant," and wall-clock jitter in the generating coroutine no
 * longer matters: [ClickStreamRenderer] only cares how many samples it has already written, a
 * number it controls deterministically, not how much real time has passed.
 *
 * ## Scheduling model
 *
 * Target sample for click at `globalClickIndex`:
 * ```
 * targetSample = anchorSample + round(globalClickIndex * samplesPerClick(bpm, subdivision, denominator, sampleRateHz))
 * ```
 * BPM is always a quarter-note pulse; the time-signature denominator scales the actual interval
 * (an /8 signature clicks twice as fast as a /4 signature at the same BPM) — see [samplesPerClick].
 * Drift is impossible by construction: every target is computed fresh from the fixed anchor, not
 * accumulated from the previous target.
 *
 * ## Re-anchor rules (per `Phase6-Metronome-Decisions.md` Item 2)
 *
 * - **BPM change only:** anchor = the sample position at which the change is applied,
 *   globalClickIndex = 0, [clickIndexInBar] unchanged.
 * - **Time-signature or subdivision change:** anchor = the sample position at which the change is
 *   applied, globalClickIndex = 0, [clickIndexInBar] = 0 (next click is the new downbeat).
 *
 * ## What happened to `catchUpIfBehind`
 *
 * The previous nanosecond-anchored version of this class had a `catchUpIfBehind` method that
 * silently skipped clicks if the player's calling thread ever fell behind its own wall-clock
 * schedule. That guarded against *our own* coroutine stalling (GC pause, dispatcher contention) —
 * a real but different failure mode from the output-path idle-gap glitch this rewrite targets.
 * It has no equivalent here: because every sample is explicitly generated into a deterministic
 * position in the stream regardless of how much real time the generating coroutine took, there is
 * no "behind schedule" to catch up on — `AudioTrack.write(..., WRITE_BLOCKING)` simply blocks
 * longer if the hardware is still draining a previous chunk, exactly like any other continuous
 * audio-generation loop.
 */
internal class BeatScheduler(
    private val sampleRateHz: Int,
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

    private var anchorSample: Long = 0L
    private var globalClickIndex: Long = 0L

    /**
     * The target sample position (in the continuous output stream) for the current (not-yet-
     * played) click.
     *
     * When [globalClickIndex] is 0 (immediately after creation or re-anchor), this equals
     * [anchorSample], meaning the first click should start at that exact sample.
     */
    fun targetSample(): Long =
        anchorSample + (globalClickIndex * samplesPerClick(config.bpm, config.subdivision, config.timeSignatureDenominator, sampleRateHz)).roundToLong()

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
     * Advances past the current click. Call **after** placing the click's samples.
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
     * The anchor becomes [atSample] and the global click index resets to 0, so the NEXT click's
     * target is exactly one new interval after [atSample]. [clickIndexInBar] is preserved — the
     * scheduler continues at its current beat position in the bar, just at the new tempo.
     *
     * @param atSample The renderer's current stream position when this change takes effect.
     */
    fun onBpmChanged(newConfig: MetronomeConfig, atSample: Long) {
        anchorSample = atSample
        globalClickIndex = 0L
        config = newConfig
        // clickIndexInBar intentionally unchanged
    }

    /**
     * Re-anchors the scheduler for a time-signature or subdivision change.
     *
     * The anchor becomes [atSample], global click index resets, and [clickIndexInBar] resets to 0
     * so the next click is the new bar's downbeat.
     *
     * @param atSample The renderer's current stream position when this change takes effect.
     */
    fun onSignatureOrSubdivisionChanged(newConfig: MetronomeConfig, atSample: Long) {
        anchorSample = atSample
        globalClickIndex = 0L
        clickIndexInBar = 0
        config = newConfig
    }

    /** Returns the [de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind] for the current click. */
    fun currentClickKind() = clickKindFor(clickIndexInBar, config.subdivision, config.accentedBeats)

    /**
     * Applies [newConfig] without re-anchoring and without touching [clickIndexInBar].
     *
     * Use this for config fields that don't affect click *timing* — currently only
     * [MetronomeConfig.accentedBeats]. A BPM, time-signature, or subdivision change always goes
     * through [onBpmChanged] / [onSignatureOrSubdivisionChanged] instead, since those genuinely
     * need to re-anchor; routing an accent-only change through either of those unconditionally
     * would needlessly reset the anchor on every accent toggle for no timing-related reason.
     */
    fun updateConfig(newConfig: MetronomeConfig) {
        config = newConfig
    }
}
