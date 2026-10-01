package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ClickStreamRenderer] — the class that mixes the continuous silence+click PCM stream
 * `AudioTrackMetronomePlayer` writes to `AudioTrack`.
 *
 * This is the coverage that directly proves the low-BPM glitch fix: every sample the renderer
 * hands back is explicit (a real zero for silence, or a real click sample) — there is never a
 * "nothing written" gap for the output path to idle during. Uses small synthetic click buffers
 * and round sample rates/intervals (not real audio constants) so expected output can be written
 * out by hand.
 */
class ClickStreamRendererTest {

    private val accented = shortArrayOf(9)
    private val standard = shortArrayOf(8)
    private val subdivisionClick = shortArrayOf(1)
    private val tinyBuffers: Map<ClickKind, ShortArray> = mapOf(
        ClickKind.ACCENTED to accented,
        ClickKind.STANDARD to standard,
        ClickKind.SUBDIVISION to subdivisionClick,
    )

    // ── Basic silence/click placement ─────────────────────────────────────────

    @Test
    fun `first chunk starts with the downbeat click at offset 0 and reports its tick`() {
        // sampleRateHz=10, bpm=60 → 10 samples/click, far longer than the 5-sample chunk below.
        val scheduler = BeatScheduler(sampleRateHz = 10, MetronomeConfig(bpm = 60))
        val renderer = ClickStreamRenderer(scheduler, tinyBuffers)
        val chunk = ShortArray(5)

        val ticks = renderer.renderChunk(chunk)

        assertArrayEquals(shortArrayOf(9, 0, 0, 0, 0), chunk)
        assertEquals(listOf(0), ticks)
    }

    @Test
    fun `a chunk entirely between clicks is pure silence with no ticks`() {
        val scheduler = BeatScheduler(sampleRateHz = 10, MetronomeConfig(bpm = 60)) // 10 samples/click
        val renderer = ClickStreamRenderer(scheduler, tinyBuffers)
        renderer.renderChunk(ShortArray(5)) // consumes samples 0..4 (includes the downbeat click)

        val chunk = ShortArray(5)
        val ticks = renderer.renderChunk(chunk) // samples 5..9 — next click isn't due until sample 10

        assertArrayEquals(ShortArray(5), chunk) // all zero — a real silence write, not a gap
        assertTrue(ticks.isEmpty())
    }

    @Test
    fun `position advances by exactly the chunk size on every call`() {
        val scheduler = BeatScheduler(sampleRateHz = 10, MetronomeConfig(bpm = 60))
        val renderer = ClickStreamRenderer(scheduler, tinyBuffers)

        assertEquals(0L, renderer.position)
        renderer.renderChunk(ShortArray(5))
        assertEquals(5L, renderer.position)
        renderer.renderChunk(ShortArray(5))
        assertEquals(10L, renderer.position)
    }

    // ── Carryover across multiple chunks ──────────────────────────────────────

    @Test
    fun `a click longer than the chunk size carries over correctly and is ticked only once`() {
        val longBuffers: Map<ClickKind, ShortArray> = mapOf(
            ClickKind.ACCENTED to shortArrayOf(1, 2, 3, 4, 5),
            ClickKind.STANDARD to shortArrayOf(9, 9),
            ClickKind.SUBDIVISION to shortArrayOf(7),
        )
        // sampleRateHz=10, bpm=60 → 10 samples/click; chunk size 3 < click length 5.
        val scheduler = BeatScheduler(sampleRateHz = 10, MetronomeConfig(bpm = 60))
        val renderer = ClickStreamRenderer(scheduler, longBuffers)

        val chunk1 = ShortArray(3)
        val ticks1 = renderer.renderChunk(chunk1)
        val chunk2 = ShortArray(3)
        val ticks2 = renderer.renderChunk(chunk2)
        val chunk3 = ShortArray(3)
        val ticks3 = renderer.renderChunk(chunk3)
        val chunk4 = ShortArray(3)
        val ticks4 = renderer.renderChunk(chunk4)

        assertArrayEquals(shortArrayOf(1, 2, 3), chunk1)
        assertArrayEquals(shortArrayOf(4, 5, 0), chunk2)
        assertArrayEquals(shortArrayOf(0, 0, 0), chunk3)
        assertArrayEquals(shortArrayOf(0, 9, 9), chunk4) // beat 2's STANDARD click starts at sample 10

        assertEquals(listOf(0), ticks1) // downbeat tick fires once, in the chunk it starts in
        assertTrue(ticks2.isEmpty())
        assertTrue(ticks3.isEmpty())
        assertEquals(listOf(1), ticks4)
        assertEquals(12L, renderer.position)
    }

    // ── Subdivisions ───────────────────────────────────────────────────────────

    @Test
    fun `subdivision clicks are mixed into the audio but never reported as ticks`() {
        // sampleRateHz=10, bpm=60, EIGHTHS → 5 samples/click (one extra click between main beats).
        val scheduler = BeatScheduler(sampleRateHz = 10, MetronomeConfig(bpm = 60, subdivision = Subdivision.EIGHTHS))
        val renderer = ClickStreamRenderer(scheduler, tinyBuffers)

        val chunk1 = ShortArray(5)
        val ticks1 = renderer.renderChunk(chunk1)
        val chunk2 = ShortArray(5)
        val ticks2 = renderer.renderChunk(chunk2)

        assertArrayEquals(shortArrayOf(9, 0, 0, 0, 0), chunk1) // downbeat at sample 0
        assertArrayEquals(shortArrayOf(1, 0, 0, 0, 0), chunk2) // subdivision click at sample 5
        assertEquals(listOf(0), ticks1)
        assertTrue(ticks2.isEmpty()) // subdivision click heard, not ticked
    }

    // ── Full-bar sequencing ────────────────────────────────────────────────────

    @Test
    fun `a full 4 4 bar produces ACCENTED then three STANDARD ticks in order`() {
        val scheduler = BeatScheduler(sampleRateHz = 10, MetronomeConfig(bpm = 60)) // 10 samples/click
        val renderer = ClickStreamRenderer(scheduler, tinyBuffers)

        val allTicks = mutableListOf<Int>()
        repeat(5) { // 5 chunks of 10 samples = 50 samples = one full bar (40) plus the next downbeat
            allTicks += renderer.renderChunk(ShortArray(10))
        }

        assertEquals(listOf(0, 1, 2, 3, 0), allTicks)
    }

    // ── Denominator scaling ────────────────────────────────────────────────────

    @Test
    fun `a 12 8 bar renders twice as many clicks in the same number of samples as 4 4`() {
        // Same BPM, same sampleRateHz: 4/4 clicks every 10 samples; 12/8 (denominator 8) clicks
        // every 5 samples — double time, per the project's BPM-is-a-quarter-note-pulse decision.
        val fourFour = ClickStreamRenderer(
            BeatScheduler(sampleRateHz = 10, MetronomeConfig(bpm = 60, timeSignatureNumerator = 4, timeSignatureDenominator = 4)),
            tinyBuffers,
        )
        val twelveEight = ClickStreamRenderer(
            BeatScheduler(sampleRateHz = 10, MetronomeConfig(bpm = 60, timeSignatureNumerator = 12, timeSignatureDenominator = 8)),
            tinyBuffers,
        )

        val fourFourTicks = fourFour.renderChunk(ShortArray(40)) // one full 4/4 bar's worth of samples
        val twelveEightTicks = twelveEight.renderChunk(ShortArray(40))

        assertEquals(listOf(0, 1, 2, 3), fourFourTicks)
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7), twelveEightTicks) // twice as many clicks, same window
    }

    // ── Mid-stream re-anchoring ────────────────────────────────────────────────

    @Test
    fun `applying a bpm change between chunks shifts subsequent clicks with no sample gap or overlap`() {
        val scheduler = BeatScheduler(sampleRateHz = 10, MetronomeConfig(bpm = 60)) // 10 samples/click
        val renderer = ClickStreamRenderer(scheduler, tinyBuffers)

        renderer.renderChunk(ShortArray(10)) // samples 0..9: downbeat click at 0, tick 0 consumed

        // Double the tempo right at the chunk boundary (position 10).
        scheduler.onBpmChanged(MetronomeConfig(bpm = 120), atSample = renderer.position)
        assertEquals(10L, scheduler.targetSample()) // next click still due immediately at the new anchor

        val chunk = ShortArray(5) // 120 bpm → 5 samples/click
        val ticks = renderer.renderChunk(chunk)

        assertArrayEquals(shortArrayOf(8, 0, 0, 0, 0), chunk) // beat 2's STANDARD click, right at the anchor
        assertEquals(listOf(1), ticks)
        assertEquals(15L, renderer.position)
    }
}
