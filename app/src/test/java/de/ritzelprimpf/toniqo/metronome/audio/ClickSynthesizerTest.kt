package de.ritzelprimpf.toniqo.metronome.audio

import de.ritzelprimpf.toniqo.metronome.data.audio.ClickParameters
import de.ritzelprimpf.toniqo.metronome.data.audio.ClickSynthesizer
import de.ritzelprimpf.toniqo.metronome.data.audio.MetronomeAudioFormat
import de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickSynthesizerTest {

    private val synthesizer = ClickSynthesizer()

    private val expectedLength =
        ClickParameters.CLICK_DURATION_MS * MetronomeAudioFormat.SAMPLE_RATE_HZ / 1000

    // -------------------------------------------------------------------------
    // Buffer length
    // -------------------------------------------------------------------------

    @Test
    fun `buffer length for ACCENTED equals expected sample count`() {
        assertEquals(expectedLength, synthesizer.generate(ClickKind.ACCENTED).size)
    }

    @Test
    fun `buffer length for STANDARD equals expected sample count`() {
        assertEquals(expectedLength, synthesizer.generate(ClickKind.STANDARD).size)
    }

    @Test
    fun `buffer length for SUBDIVISION equals expected sample count`() {
        assertEquals(expectedLength, synthesizer.generate(ClickKind.SUBDIVISION).size)
    }

    // -------------------------------------------------------------------------
    // No clipping
    // -------------------------------------------------------------------------

    @Test
    fun `no sample in ACCENTED exceeds plus or minus PCM16 full scale`() {
        val buf = synthesizer.generate(ClickKind.ACCENTED)
        assertTrue(buf.all { it.toInt() in -ClickParameters.PCM16_FULL_SCALE..ClickParameters.PCM16_FULL_SCALE })
    }

    @Test
    fun `no sample in STANDARD exceeds plus or minus PCM16 full scale`() {
        val buf = synthesizer.generate(ClickKind.STANDARD)
        assertTrue(buf.all { it.toInt() in -ClickParameters.PCM16_FULL_SCALE..ClickParameters.PCM16_FULL_SCALE })
    }

    @Test
    fun `no sample in SUBDIVISION exceeds plus or minus PCM16 full scale`() {
        val buf = synthesizer.generate(ClickKind.SUBDIVISION)
        assertTrue(buf.all { it.toInt() in -ClickParameters.PCM16_FULL_SCALE..ClickParameters.PCM16_FULL_SCALE })
    }

    // -------------------------------------------------------------------------
    // Amplitude hierarchy
    // -------------------------------------------------------------------------

    @Test
    fun `ACCENTED peak sample is greater than STANDARD peak sample`() {
        val accentedPeak = synthesizer.generate(ClickKind.ACCENTED).maxOf { abs(it.toInt()) }
        val standardPeak = synthesizer.generate(ClickKind.STANDARD).maxOf { abs(it.toInt()) }
        assertTrue(
            "ACCENTED peak ($accentedPeak) should be greater than STANDARD peak ($standardPeak)",
            accentedPeak > standardPeak,
        )
    }

    @Test
    fun `STANDARD peak sample is greater than SUBDIVISION peak sample`() {
        val standardPeak = synthesizer.generate(ClickKind.STANDARD).maxOf { abs(it.toInt()) }
        val subdivisionPeak = synthesizer.generate(ClickKind.SUBDIVISION).maxOf { abs(it.toInt()) }
        assertTrue(
            "STANDARD peak ($standardPeak) should be greater than SUBDIVISION peak ($subdivisionPeak)",
            standardPeak > subdivisionPeak,
        )
    }

    // -------------------------------------------------------------------------
    // Peak amplitude within ±10% of expected
    // -------------------------------------------------------------------------

    @Test
    fun `ACCENTED peak amplitude is within 10 percent of expected`() {
        val buf = synthesizer.generate(ClickKind.ACCENTED)
        val observed = buf.maxOf { abs(it.toInt()) }
        val expected = (ClickParameters.AMPLITUDE_ACCENTED * ClickParameters.PCM16_FULL_SCALE).toInt()
        assertWithinTenPercent("ACCENTED", expected, observed)
    }

    @Test
    fun `STANDARD peak amplitude is within 10 percent of expected`() {
        val buf = synthesizer.generate(ClickKind.STANDARD)
        val observed = buf.maxOf { abs(it.toInt()) }
        val expected = (ClickParameters.AMPLITUDE_STANDARD * ClickParameters.PCM16_FULL_SCALE).toInt()
        assertWithinTenPercent("STANDARD", expected, observed)
    }

    @Test
    fun `SUBDIVISION peak amplitude is within 10 percent of expected`() {
        val buf = synthesizer.generate(ClickKind.SUBDIVISION)
        val observed = buf.maxOf { abs(it.toInt()) }
        val expected = (ClickParameters.AMPLITUDE_SUBDIVISION * ClickParameters.PCM16_FULL_SCALE).toInt()
        assertWithinTenPercent("SUBDIVISION", expected, observed)
    }

    // -------------------------------------------------------------------------
    // First sample is zero (start of linear attack)
    // -------------------------------------------------------------------------

    @Test
    fun `first sample of ACCENTED is exactly zero`() {
        assertEquals(0.toShort(), synthesizer.generate(ClickKind.ACCENTED)[0])
    }

    @Test
    fun `first sample of STANDARD is exactly zero`() {
        assertEquals(0.toShort(), synthesizer.generate(ClickKind.STANDARD)[0])
    }

    @Test
    fun `first sample of SUBDIVISION is exactly zero`() {
        assertEquals(0.toShort(), synthesizer.generate(ClickKind.SUBDIVISION)[0])
    }

    // -------------------------------------------------------------------------
    // Last sample magnitude below 2% of peak (decay reaches near-silence)
    // -------------------------------------------------------------------------

    @Test
    fun `last sample of ACCENTED is below 2 percent of peak`() {
        val buf = synthesizer.generate(ClickKind.ACCENTED)
        val peak = (ClickParameters.AMPLITUDE_ACCENTED * ClickParameters.PCM16_FULL_SCALE).toInt()
        val threshold = peak * 0.02
        assertTrue(
            "last sample magnitude ${abs(buf.last().toInt())} should be < 2% of peak ($threshold)",
            abs(buf.last().toInt()) < threshold,
        )
    }

    @Test
    fun `last sample of STANDARD is below 2 percent of peak`() {
        val buf = synthesizer.generate(ClickKind.STANDARD)
        val peak = (ClickParameters.AMPLITUDE_STANDARD * ClickParameters.PCM16_FULL_SCALE).toInt()
        val threshold = peak * 0.02
        assertTrue(
            "last sample magnitude ${abs(buf.last().toInt())} should be < 2% of peak ($threshold)",
            abs(buf.last().toInt()) < threshold,
        )
    }

    @Test
    fun `last sample of SUBDIVISION is below 2 percent of peak`() {
        val buf = synthesizer.generate(ClickKind.SUBDIVISION)
        val peak = (ClickParameters.AMPLITUDE_SUBDIVISION * ClickParameters.PCM16_FULL_SCALE).toInt()
        val threshold = peak * 0.02
        assertTrue(
            "last sample magnitude ${abs(buf.last().toInt())} should be < 2% of peak ($threshold)",
            abs(buf.last().toInt()) < threshold,
        )
    }

    // -------------------------------------------------------------------------
    // Near-zero DC offset
    // -------------------------------------------------------------------------

    @Test
    fun `ACCENTED buffer has near-zero DC offset`() {
        assertNearZeroDcOffset(ClickKind.ACCENTED)
    }

    @Test
    fun `STANDARD buffer has near-zero DC offset`() {
        assertNearZeroDcOffset(ClickKind.STANDARD)
    }

    @Test
    fun `SUBDIVISION buffer has near-zero DC offset`() {
        assertNearZeroDcOffset(ClickKind.SUBDIVISION)
    }

    // -------------------------------------------------------------------------
    // Deterministic output
    // -------------------------------------------------------------------------

    @Test
    fun `calling generate twice with ACCENTED produces identical buffers`() {
        val first = synthesizer.generate(ClickKind.ACCENTED)
        val second = synthesizer.generate(ClickKind.ACCENTED)
        assertTrue(first.contentEquals(second))
    }

    @Test
    fun `calling generate twice with STANDARD produces identical buffers`() {
        val first = synthesizer.generate(ClickKind.STANDARD)
        val second = synthesizer.generate(ClickKind.STANDARD)
        assertTrue(first.contentEquals(second))
    }

    @Test
    fun `calling generate twice with SUBDIVISION produces identical buffers`() {
        val first = synthesizer.generate(ClickKind.SUBDIVISION)
        val second = synthesizer.generate(ClickKind.SUBDIVISION)
        assertTrue(first.contentEquals(second))
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun assertWithinTenPercent(label: String, expected: Int, observed: Int) {
        val lower = expected * 0.90
        val upper = expected * 1.10
        assertTrue(
            "$label: observed peak $observed should be within ±10% of expected $expected ([$lower, $upper])",
            observed >= lower && observed <= upper,
        )
    }

    private fun assertNearZeroDcOffset(kind: ClickKind) {
        val buf = synthesizer.generate(kind)
        val mean = buf.map { it.toLong() }.sum().toDouble() / buf.size
        val limit = ClickParameters.PCM16_FULL_SCALE / 1000.0
        assertTrue(
            "$kind DC offset |$mean| should be < $limit",
            abs(mean) < limit,
        )
    }
}
