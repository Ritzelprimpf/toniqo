package de.ritzelprimpf.toniqo.metronome.domain

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetronomeConfigTest {

    @Test
    fun `every curated preset in SUPPORTED_SIGNATURES is itself a supported time signature`() {
        MetronomeConfig.SUPPORTED_SIGNATURES.forEach { (numerator, denominator) ->
            assertTrue(
                "$numerator/$denominator should be supported",
                MetronomeConfig.isSupportedTimeSignature(numerator, denominator),
            )
        }
    }

    @Test
    fun `a custom non-preset signature within range is supported`() {
        assertTrue(MetronomeConfig.isSupportedTimeSignature(numerator = 11, denominator = 16))
    }

    @Test
    fun `numerator at the minimum bound is supported`() {
        assertTrue(MetronomeConfig.isSupportedTimeSignature(MetronomeConfig.TIME_SIGNATURE_NUMERATOR_MIN, denominator = 4))
    }

    @Test
    fun `numerator at the maximum bound is supported`() {
        assertTrue(MetronomeConfig.isSupportedTimeSignature(MetronomeConfig.TIME_SIGNATURE_NUMERATOR_MAX, denominator = 4))
    }

    @Test
    fun `numerator below the minimum bound is not supported`() {
        assertFalse(
            MetronomeConfig.isSupportedTimeSignature(MetronomeConfig.TIME_SIGNATURE_NUMERATOR_MIN - 1, denominator = 4),
        )
    }

    @Test
    fun `numerator above the maximum bound is not supported`() {
        assertFalse(
            MetronomeConfig.isSupportedTimeSignature(MetronomeConfig.TIME_SIGNATURE_NUMERATOR_MAX + 1, denominator = 4),
        )
    }

    @Test
    fun `every entry in SUPPORTED_DENOMINATORS is individually supported at a fixed numerator`() {
        MetronomeConfig.SUPPORTED_DENOMINATORS.forEach { denominator ->
            assertTrue(
                "denominator $denominator should be supported",
                MetronomeConfig.isSupportedTimeSignature(numerator = 4, denominator = denominator),
            )
        }
    }

    @Test
    fun `a denominator that is not a power of two is not supported`() {
        assertFalse(MetronomeConfig.isSupportedTimeSignature(numerator = 4, denominator = 3))
        assertFalse(MetronomeConfig.isSupportedTimeSignature(numerator = 4, denominator = 5))
        assertFalse(MetronomeConfig.isSupportedTimeSignature(numerator = 4, denominator = 6))
    }

    @Test
    fun `a denominator above SUPPORTED_DENOMINATORS range is not supported even if it is a power of two`() {
        // 64 is a power of two but outside the 1..32 range this project supports.
        assertFalse(MetronomeConfig.isSupportedTimeSignature(numerator = 4, denominator = 64))
    }
}
