package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind

/**
 * Mixes a continuous silence+click PCM stream, chunk by chunk, by consulting a [BeatScheduler].
 *
 * This is the class that actually eliminates the low-BPM glitch: every sample handed to the
 * caller (for writing to `AudioTrack`) is either an explicit zero (silence) or a click sample —
 * never "nothing." The output path this feeds never sees a gap, so it has nothing to idle during
 * and nothing to recover from.
 *
 * Pure Kotlin, no Android dependency — fully testable on the JVM with fake click buffers.
 *
 * ## Usage
 *
 * Call [renderChunk] repeatedly with reusable chunk-sized buffers, in order, with no gaps between
 * calls — each call picks up exactly where the previous one left off, both in the silence/click
 * mixing and in the underlying [BeatScheduler]'s state. [position] is the total sample count
 * rendered so far; pass it to [BeatScheduler.onBpmChanged] /
 * [BeatScheduler.onSignatureOrSubdivisionChanged] when applying a config change between chunks so
 * the new config re-anchors at the correct stream position.
 *
 * A click can span more than one [renderChunk] call if it is longer than the chunk size — this is
 * handled internally via carryover state, transparent to the caller.
 */
internal class ClickStreamRenderer(
    private val scheduler: BeatScheduler,
    private val clickBuffers: Map<ClickKind, ShortArray>,
) {
    /** Total samples rendered so far — this renderer's own position in the continuous output stream. */
    var position: Long = 0L
        private set

    private var carryoverBuffer: ShortArray? = null
    private var carryoverOffset: Int = 0

    /**
     * Fills all of [chunk] with silence and/or click samples, advancing [scheduler] past every
     * click whose target sample falls within this chunk (including one carried over from a
     * previous call).
     *
     * @return The main-beat index ([BeatScheduler.mainBeatIndex]) of every main-beat click that
     *   *started* in this chunk, in the order they occur. Empty if none started in this chunk.
     *   Subdivision-only clicks are mixed into the audio but never reported here — matching the
     *   existing "ticks surface main beats only" contract.
     */
    fun renderChunk(chunk: ShortArray): List<Int> {
        chunk.fill(0)
        val mainBeatTicks = mutableListOf<Int>()
        var cursor = 0

        val inProgress = carryoverBuffer
        if (inProgress != null) {
            val copied = copyInto(inProgress, carryoverOffset, chunk, cursor)
            cursor += copied
            position += copied
            if (carryoverOffset + copied >= inProgress.size) {
                carryoverBuffer = null
                carryoverOffset = 0
            } else {
                carryoverOffset += copied
            }
        }

        while (cursor < chunk.size) {
            val samplesUntilClick = (scheduler.targetSample() - position).coerceAtLeast(0L)
            val silenceLen = minOf(samplesUntilClick, (chunk.size - cursor).toLong()).toInt()
            cursor += silenceLen
            position += silenceLen
            if (cursor >= chunk.size) break

            // The next click starts exactly here.
            if (scheduler.isMainBeat()) {
                mainBeatTicks += scheduler.mainBeatIndex()
            }
            val buffer = clickBuffers.getValue(scheduler.currentClickKind())
            scheduler.advance()

            val copied = copyInto(buffer, 0, chunk, cursor)
            cursor += copied
            position += copied
            if (copied < buffer.size) {
                carryoverBuffer = buffer
                carryoverOffset = copied
            }
        }

        return mainBeatTicks
    }

    private fun copyInto(source: ShortArray, sourceOffset: Int, dest: ShortArray, destOffset: Int): Int {
        val count = minOf(source.size - sourceOffset, dest.size - destOffset)
        System.arraycopy(source, sourceOffset, dest, destOffset, count)
        return count
    }
}
