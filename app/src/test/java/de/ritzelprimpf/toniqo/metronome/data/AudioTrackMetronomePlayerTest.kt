package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToLong

/**
 * Tests for the beat-scheduling logic used by [AudioTrackMetronomePlayer].
 *
 * [AudioTrackMetronomePlayer] requires a real `AudioTrack` and cannot be JVM-tested. Instead, its
 * extracted scheduling state machine ([BeatScheduler]) is tested directly here, and its sample
 * mixing ([ClickStreamRenderer]) is tested in `ClickStreamRendererTest`. [BeatScheduler] has no
 * `Clock` dependency — it operates purely on sample positions the caller supplies, so these tests
 * use plain `Long`s with no fake clock.
 */
class AudioTrackMetronomePlayerTest {

    private val sampleRateHz = 48_000
    private val defaultConfig = MetronomeConfig.DEFAULT // 120 bpm, 4/4, NONE

    private fun interval(config: MetronomeConfig = defaultConfig): Long =
        samplesPerClick(config.bpm, config.subdivision, config.timeSignatureDenominator, sampleRateHz).roundToLong()

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial targetSample is 0`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)

        assertEquals(0L, scheduler.targetSample())
    }

    @Test
    fun `initial clickIndexInBar is 0`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)

        assertEquals(0, scheduler.clickIndexInBar)
    }

    @Test
    fun `initial click is a main beat`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)

        assertTrue(scheduler.isMainBeat())
    }

    @Test
    fun `initial mainBeatIndex is 0`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)

        assertEquals(0, scheduler.mainBeatIndex())
    }

    @Test
    fun `initial click kind is ACCENTED`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)

        assertEquals(ClickKind.ACCENTED, scheduler.currentClickKind())
    }

    // ── advance — no subdivision ──────────────────────────────────────────────

    @Test
    fun `advance increments clickIndexInBar by 1`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)

        scheduler.advance()

        assertEquals(1, scheduler.clickIndexInBar)
    }

    @Test
    fun `advance increments targetSample by one interval`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)

        scheduler.advance()

        assertEquals(interval(), scheduler.targetSample())
    }

    @Test
    fun `click after advance is STANDARD for beat 2 in 4 4`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)
        scheduler.advance()

        assertEquals(ClickKind.STANDARD, scheduler.currentClickKind())
    }

    @Test
    fun `targetSample advances by one interval per advance across multiple beats`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)

        repeat(8) { i ->
            assertEquals(interval() * i, scheduler.targetSample())
            scheduler.advance()
        }
    }

    @Test
    fun `targetSample stays exact (no cumulative drift) at a bpm with a fractional sample interval`() {
        // 117 bpm → 48000*60/117 = 24615.384... samples/click, deliberately not a whole number.
        val config = defaultConfig.copy(bpm = 117)
        val scheduler = BeatScheduler(sampleRateHz, config)
        val exactInterval = samplesPerClick(117, Subdivision.NONE, denominator = 4, sampleRateHz)

        repeat(500) { i ->
            val expected = (i * exactInterval).roundToLong()
            assertEquals(expected, scheduler.targetSample())
            scheduler.advance()
        }
    }

    @Test
    fun `clickIndexInBar wraps back to 0 after a full bar in 4 4`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig) // 4 clicks per bar

        repeat(4) { scheduler.advance() }

        assertEquals(0, scheduler.clickIndexInBar)
    }

    @Test
    fun `click kind cycles ACCENTED STANDARD STANDARD STANDARD and back in 4 4`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)
        val expected = listOf(
            ClickKind.ACCENTED,
            ClickKind.STANDARD,
            ClickKind.STANDARD,
            ClickKind.STANDARD,
            ClickKind.ACCENTED, // bar 2 downbeat
        )

        val actual = List(5) {
            val kind = scheduler.currentClickKind()
            scheduler.advance()
            kind
        }

        assertEquals(expected, actual)
    }

    // ── advance — with subdivision ────────────────────────────────────────────

    @Test
    fun `subdivision click between main beats is not a main beat`() {
        val config = defaultConfig.copy(subdivision = Subdivision.EIGHTHS)
        val scheduler = BeatScheduler(sampleRateHz, config)

        scheduler.advance() // index 1 = between beat 1 and beat 2

        assertFalse(scheduler.isMainBeat())
        assertEquals(ClickKind.SUBDIVISION, scheduler.currentClickKind())
    }

    @Test
    fun `second main beat in 4 4 EIGHTHS is at click index 2`() {
        val config = defaultConfig.copy(subdivision = Subdivision.EIGHTHS)
        val scheduler = BeatScheduler(sampleRateHz, config)

        scheduler.advance() // index 1 (subdivision)
        scheduler.advance() // index 2 (main beat 2)

        assertTrue(scheduler.isMainBeat())
        assertEquals(1, scheduler.mainBeatIndex())
        assertEquals(ClickKind.STANDARD, scheduler.currentClickKind())
    }

    @Test
    fun `clickIndexInBar wraps after a full bar with eighths subdivision`() {
        val config = defaultConfig.copy(subdivision = Subdivision.EIGHTHS) // 8 clicks per bar
        val scheduler = BeatScheduler(sampleRateHz, config)

        repeat(8) { scheduler.advance() }

        assertEquals(0, scheduler.clickIndexInBar)
        assertEquals(ClickKind.ACCENTED, scheduler.currentClickKind())
    }

    @Test
    fun `clickIndexInBar wraps correctly in 3 4 with triplets subdivision`() {
        // 3/4 TRIPLETS → 3 * 3 = 9 clicks per bar
        val config = MetronomeConfig(bpm = 120, timeSignatureNumerator = 3, timeSignatureDenominator = 4, subdivision = Subdivision.TRIPLETS)
        val scheduler = BeatScheduler(sampleRateHz, config)

        repeat(9) { scheduler.advance() }

        assertEquals(0, scheduler.clickIndexInBar)
        assertEquals(ClickKind.ACCENTED, scheduler.currentClickKind())
    }

    // ── onBpmChanged ──────────────────────────────────────────────────────────

    @Test
    fun `onBpmChanged re-anchors targetSample to the given position`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)
        repeat(3) { scheduler.advance() }

        scheduler.onBpmChanged(defaultConfig.copy(bpm = 60), atSample = 500_000L)

        assertEquals(500_000L, scheduler.targetSample())
    }

    @Test
    fun `onBpmChanged preserves clickIndexInBar`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)
        scheduler.advance()
        scheduler.advance() // clickIndexInBar = 2

        scheduler.onBpmChanged(defaultConfig.copy(bpm = 80), atSample = 1_000L)

        assertEquals(2, scheduler.clickIndexInBar)
    }

    @Test
    fun `onBpmChanged updates the config on the scheduler`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)

        scheduler.onBpmChanged(defaultConfig.copy(bpm = 80), atSample = 0L)

        assertEquals(80, scheduler.config.bpm)
    }

    @Test
    fun `after onBpmChanged advance uses new bpm interval`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)
        scheduler.onBpmChanged(defaultConfig.copy(bpm = 60), atSample = 1_000_000L)

        scheduler.advance()

        val expectedTarget = 1_000_000L + interval(defaultConfig.copy(bpm = 60))
        assertEquals(expectedTarget, scheduler.targetSample())
    }

    // ── onSignatureOrSubdivisionChanged ───────────────────────────────────────

    @Test
    fun `onSignatureOrSubdivisionChanged resets clickIndexInBar to 0`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)
        scheduler.advance()
        scheduler.advance() // clickIndexInBar = 2

        scheduler.onSignatureOrSubdivisionChanged(defaultConfig.copy(timeSignatureNumerator = 3), atSample = 1_000L)

        assertEquals(0, scheduler.clickIndexInBar)
    }

    @Test
    fun `onSignatureOrSubdivisionChanged re-anchors targetSample to the given position`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)
        repeat(3) { scheduler.advance() }

        scheduler.onSignatureOrSubdivisionChanged(defaultConfig.copy(timeSignatureNumerator = 3), atSample = 900_000L)

        assertEquals(900_000L, scheduler.targetSample())
    }

    @Test
    fun `after onSignatureOrSubdivisionChanged current click kind is ACCENTED`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)
        scheduler.advance()
        scheduler.advance() // clickIndexInBar = 2 → STANDARD

        scheduler.onSignatureOrSubdivisionChanged(defaultConfig.copy(subdivision = Subdivision.EIGHTHS), atSample = 0L)

        assertEquals(ClickKind.ACCENTED, scheduler.currentClickKind())
    }

    @Test
    fun `onSignatureOrSubdivisionChanged updates the config on the scheduler`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)
        val newConfig = defaultConfig.copy(timeSignatureNumerator = 3, timeSignatureDenominator = 4)

        scheduler.onSignatureOrSubdivisionChanged(newConfig, atSample = 0L)

        assertEquals(3, scheduler.config.timeSignatureNumerator)
    }

    // ── Denominator scaling ───────────────────────────────────────────────────

    @Test
    fun `12 8 clicks exactly twice as fast as 4 4 at the same bpm`() {
        val fourFour = BeatScheduler(sampleRateHz, MetronomeConfig(bpm = 120, timeSignatureNumerator = 4, timeSignatureDenominator = 4))
        val twelveEight = BeatScheduler(sampleRateHz, MetronomeConfig(bpm = 120, timeSignatureNumerator = 12, timeSignatureDenominator = 8))

        fourFour.advance()
        twelveEight.advance()

        assertEquals(fourFour.targetSample() / 2, twelveEight.targetSample())
    }

    @Test
    fun `12 8 still accents only once every 12 clicks, not every beat`() {
        val scheduler = BeatScheduler(sampleRateHz, MetronomeConfig(bpm = 120, timeSignatureNumerator = 12, timeSignatureDenominator = 8))

        val kinds = List(13) {
            val kind = scheduler.currentClickKind()
            scheduler.advance()
            kind
        }

        assertEquals(ClickKind.ACCENTED, kinds[0])
        assertTrue(kinds.subList(1, 12).all { it == ClickKind.STANDARD })
        assertEquals(ClickKind.ACCENTED, kinds[12]) // next bar's downbeat
    }

    // ── Custom accent patterns (currentClickKind wiring) ──────────────────────

    @Test
    fun `currentClickKind reflects the scheduler's config accentedBeats, not just beat 1`() {
        val config = defaultConfig.copy(accentedBeats = setOf(1, 3))
        val scheduler = BeatScheduler(sampleRateHz, config)

        val kinds = List(4) {
            val kind = scheduler.currentClickKind()
            scheduler.advance()
            kind
        }

        assertEquals(listOf(ClickKind.STANDARD, ClickKind.ACCENTED, ClickKind.STANDARD, ClickKind.ACCENTED), kinds)
    }

    @Test
    fun `updateConfig applies a new accentedBeats without re-anchoring or resetting clickIndexInBar`() {
        val scheduler = BeatScheduler(sampleRateHz, defaultConfig)
        repeat(2) { scheduler.advance() } // clickIndexInBar = 2, some non-zero global index
        val targetBeforeUpdate = scheduler.targetSample()

        scheduler.updateConfig(defaultConfig.copy(accentedBeats = setOf(2)))

        assertEquals(2, scheduler.clickIndexInBar)
        assertEquals(targetBeforeUpdate, scheduler.targetSample())
        assertEquals(ClickKind.ACCENTED, scheduler.currentClickKind()) // picks up the new pattern immediately
    }

    @Test
    fun `onSignatureOrSubdivisionChanged does not by itself alter accentedBeats`() {
        // BeatScheduler only re-anchors timing and clickIndexInBar; resetting the accent pattern
        // to the default on a real signature change is the ViewModel's responsibility (it passes
        // the already-reset config in), not BeatScheduler's.
        val config = defaultConfig.copy(accentedBeats = setOf(2))
        val scheduler = BeatScheduler(sampleRateHz, config)

        scheduler.onSignatureOrSubdivisionChanged(config.copy(timeSignatureNumerator = 3), atSample = 0L)

        assertEquals(setOf(2), scheduler.config.accentedBeats)
    }
}
