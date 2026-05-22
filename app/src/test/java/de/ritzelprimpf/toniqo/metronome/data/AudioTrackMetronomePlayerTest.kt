package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import de.ritzelprimpf.toniqo.metronome.fakes.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the beat-scheduling logic used by [AudioTrackMetronomePlayer].
 *
 * [AudioTrackMetronomePlayer] requires a real `AudioTrack` and cannot be JVM-tested. Instead,
 * its extracted scheduling state machine ([BeatScheduler]) is tested directly here. All timing
 * is virtualized via [FakeClock]; no real-time delays occur.
 */
class AudioTrackMetronomePlayerTest {

    private val clock = FakeClock(initialNanos = 0L)
    private val defaultConfig = MetronomeConfig.DEFAULT // 120 bpm, 4/4, NONE

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial targetNs equals the clock time at construction`() {
        clock.setNow(1_000_000_000L)
        val scheduler = BeatScheduler(clock, defaultConfig)

        assertEquals(1_000_000_000L, scheduler.targetNs())
    }

    @Test
    fun `initial clickIndexInBar is 0`() {
        val scheduler = BeatScheduler(clock, defaultConfig)

        assertEquals(0, scheduler.clickIndexInBar)
    }

    @Test
    fun `initial click is a main beat`() {
        val scheduler = BeatScheduler(clock, defaultConfig)

        assertTrue(scheduler.isMainBeat())
    }

    @Test
    fun `initial mainBeatIndex is 0`() {
        val scheduler = BeatScheduler(clock, defaultConfig)

        assertEquals(0, scheduler.mainBeatIndex())
    }

    @Test
    fun `initial click kind is ACCENTED`() {
        val scheduler = BeatScheduler(clock, defaultConfig)

        assertEquals(ClickKind.ACCENTED, scheduler.currentClickKind())
    }

    // ── advance — no subdivision ──────────────────────────────────────────────

    @Test
    fun `advance increments clickIndexInBar by 1`() {
        val scheduler = BeatScheduler(clock, defaultConfig)

        scheduler.advance()

        assertEquals(1, scheduler.clickIndexInBar)
    }

    @Test
    fun `advance increments targetNs by one interval`() {
        val scheduler = BeatScheduler(clock, defaultConfig)
        val interval = intervalNanos(defaultConfig.bpm, defaultConfig.subdivision)

        scheduler.advance()

        assertEquals(interval, scheduler.targetNs())
    }

    @Test
    fun `click after advance is STANDARD for beat 2 in 4 4`() {
        val scheduler = BeatScheduler(clock, defaultConfig)
        scheduler.advance()

        assertEquals(ClickKind.STANDARD, scheduler.currentClickKind())
    }

    @Test
    fun `targetNs advances by one interval per advance across multiple beats`() {
        val scheduler = BeatScheduler(clock, defaultConfig)
        val interval = intervalNanos(defaultConfig.bpm, defaultConfig.subdivision)

        repeat(8) { i ->
            assertEquals(interval * i, scheduler.targetNs())
            scheduler.advance()
        }
    }

    @Test
    fun `clickIndexInBar wraps back to 0 after a full bar in 4 4`() {
        val scheduler = BeatScheduler(clock, defaultConfig) // 4 clicks per bar

        repeat(4) { scheduler.advance() }

        assertEquals(0, scheduler.clickIndexInBar)
    }

    @Test
    fun `click kind cycles ACCENTED STANDARD STANDARD STANDARD and back in 4 4`() {
        val scheduler = BeatScheduler(clock, defaultConfig)
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
        val scheduler = BeatScheduler(clock, config)

        scheduler.advance() // index 1 = between beat 1 and beat 2

        assertFalse(scheduler.isMainBeat())
        assertEquals(ClickKind.SUBDIVISION, scheduler.currentClickKind())
    }

    @Test
    fun `second main beat in 4 4 EIGHTHS is at click index 2`() {
        val config = defaultConfig.copy(subdivision = Subdivision.EIGHTHS)
        val scheduler = BeatScheduler(clock, config)

        scheduler.advance() // index 1 (subdivision)
        scheduler.advance() // index 2 (main beat 2)

        assertTrue(scheduler.isMainBeat())
        assertEquals(1, scheduler.mainBeatIndex())
        assertEquals(ClickKind.STANDARD, scheduler.currentClickKind())
    }

    @Test
    fun `clickIndexInBar wraps after a full bar with eighths subdivision`() {
        val config = defaultConfig.copy(subdivision = Subdivision.EIGHTHS) // 8 clicks per bar
        val scheduler = BeatScheduler(clock, config)

        repeat(8) { scheduler.advance() }

        assertEquals(0, scheduler.clickIndexInBar)
        assertEquals(ClickKind.ACCENTED, scheduler.currentClickKind())
    }

    @Test
    fun `clickIndexInBar wraps correctly in 3 4 with triplets subdivision`() {
        // 3/4 TRIPLETS → 3 * 3 = 9 clicks per bar
        val config = MetronomeConfig(bpm = 120, timeSignatureNumerator = 3, timeSignatureDenominator = 4, subdivision = Subdivision.TRIPLETS)
        val scheduler = BeatScheduler(clock, config)

        repeat(9) { scheduler.advance() }

        assertEquals(0, scheduler.clickIndexInBar)
        assertEquals(ClickKind.ACCENTED, scheduler.currentClickKind())
    }

    // ── onBpmChanged ──────────────────────────────────────────────────────────

    @Test
    fun `onBpmChanged re-anchors targetNs to now`() {
        val scheduler = BeatScheduler(clock, defaultConfig)
        repeat(3) { scheduler.advance() }

        clock.setNow(5_000_000_000L)
        scheduler.onBpmChanged(defaultConfig.copy(bpm = 60))

        assertEquals(5_000_000_000L, scheduler.targetNs())
    }

    @Test
    fun `onBpmChanged preserves clickIndexInBar`() {
        val scheduler = BeatScheduler(clock, defaultConfig)
        scheduler.advance()
        scheduler.advance() // clickIndexInBar = 2

        scheduler.onBpmChanged(defaultConfig.copy(bpm = 80))

        assertEquals(2, scheduler.clickIndexInBar)
    }

    @Test
    fun `onBpmChanged updates the config on the scheduler`() {
        val scheduler = BeatScheduler(clock, defaultConfig)

        scheduler.onBpmChanged(defaultConfig.copy(bpm = 80))

        assertEquals(80, scheduler.config.bpm)
    }

    @Test
    fun `after onBpmChanged advance uses new bpm interval`() {
        val scheduler = BeatScheduler(clock, defaultConfig)
        clock.setNow(1_000_000_000L)
        scheduler.onBpmChanged(defaultConfig.copy(bpm = 60))

        scheduler.advance()

        val expectedTarget = 1_000_000_000L + intervalNanos(60, Subdivision.NONE)
        assertEquals(expectedTarget, scheduler.targetNs())
    }

    // ── onSignatureOrSubdivisionChanged ───────────────────────────────────────

    @Test
    fun `onSignatureOrSubdivisionChanged resets clickIndexInBar to 0`() {
        val scheduler = BeatScheduler(clock, defaultConfig)
        scheduler.advance()
        scheduler.advance() // clickIndexInBar = 2

        scheduler.onSignatureOrSubdivisionChanged(defaultConfig.copy(timeSignatureNumerator = 3))

        assertEquals(0, scheduler.clickIndexInBar)
    }

    @Test
    fun `onSignatureOrSubdivisionChanged re-anchors targetNs to now`() {
        val scheduler = BeatScheduler(clock, defaultConfig)
        repeat(3) { scheduler.advance() }

        clock.setNow(9_000_000_000L)
        scheduler.onSignatureOrSubdivisionChanged(defaultConfig.copy(timeSignatureNumerator = 3))

        assertEquals(9_000_000_000L, scheduler.targetNs())
    }

    @Test
    fun `after onSignatureOrSubdivisionChanged current click kind is ACCENTED`() {
        val scheduler = BeatScheduler(clock, defaultConfig)
        scheduler.advance()
        scheduler.advance() // clickIndexInBar = 2 → STANDARD

        scheduler.onSignatureOrSubdivisionChanged(defaultConfig.copy(subdivision = Subdivision.EIGHTHS))

        assertEquals(ClickKind.ACCENTED, scheduler.currentClickKind())
    }

    @Test
    fun `onSignatureOrSubdivisionChanged updates the config on the scheduler`() {
        val scheduler = BeatScheduler(clock, defaultConfig)
        val newConfig = defaultConfig.copy(timeSignatureNumerator = 3, timeSignatureDenominator = 4)

        scheduler.onSignatureOrSubdivisionChanged(newConfig)

        assertEquals(3, scheduler.config.timeSignatureNumerator)
    }
}
