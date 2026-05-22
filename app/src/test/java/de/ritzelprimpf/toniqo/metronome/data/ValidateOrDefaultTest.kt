package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.Subdivision
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateOrDefaultTest {

    private fun raw(
        bpm: Int? = 120,
        numerator: Int? = 4,
        denominator: Int? = 4,
        subdivisionName: String? = Subdivision.NONE.name,
    ) = RawMetronomeConfig(bpm, numerator, denominator, subdivisionName)

    // ── Valid input round-trips ───────────────────────────────────────────────

    @Test
    fun `valid default raw config round-trips to MetronomeConfig DEFAULT`() {
        assertEquals(MetronomeConfig.DEFAULT, validateOrDefault(raw()))
    }

    @Test
    fun `each supported time signature produces the correct output config`() {
        MetronomeConfig.SUPPORTED_SIGNATURES.forEach { (num, den) ->
            val result = validateOrDefault(raw(numerator = num, denominator = den))
            assertEquals("numerator mismatch for $num/$den", num, result.timeSignatureNumerator)
            assertEquals("denominator mismatch for $num/$den", den, result.timeSignatureDenominator)
        }
    }

    @Test
    fun `each supported subdivision decodes correctly`() {
        Subdivision.entries.forEach { sub ->
            val result = validateOrDefault(raw(subdivisionName = sub.name))
            assertEquals(sub, result.subdivision)
        }
    }

    @Test
    fun `bpm at BPM_MIN boundary is valid`() {
        assertEquals(MetronomeConfig.BPM_MIN, validateOrDefault(raw(bpm = MetronomeConfig.BPM_MIN)).bpm)
    }

    @Test
    fun `bpm at BPM_MAX boundary is valid`() {
        assertEquals(MetronomeConfig.BPM_MAX, validateOrDefault(raw(bpm = MetronomeConfig.BPM_MAX)).bpm)
    }

    // ── Null fields → DEFAULT ─────────────────────────────────────────────────

    @Test
    fun `null bpm returns DEFAULT`() {
        assertEquals(MetronomeConfig.DEFAULT, validateOrDefault(raw(bpm = null)))
    }

    @Test
    fun `null numerator returns DEFAULT`() {
        assertEquals(MetronomeConfig.DEFAULT, validateOrDefault(raw(numerator = null)))
    }

    @Test
    fun `null denominator returns DEFAULT`() {
        assertEquals(MetronomeConfig.DEFAULT, validateOrDefault(raw(denominator = null)))
    }

    @Test
    fun `null subdivisionName returns DEFAULT`() {
        assertEquals(MetronomeConfig.DEFAULT, validateOrDefault(raw(subdivisionName = null)))
    }

    @Test
    fun `all-null raw config returns DEFAULT`() {
        assertEquals(MetronomeConfig.DEFAULT, validateOrDefault(raw(null, null, null, null)))
    }

    // ── Out-of-range fields → DEFAULT (whole-config replacement) ─────────────

    @Test
    fun `bpm one below minimum returns DEFAULT`() {
        assertEquals(MetronomeConfig.DEFAULT, validateOrDefault(raw(bpm = MetronomeConfig.BPM_MIN - 1)))
    }

    @Test
    fun `bpm one above maximum returns DEFAULT`() {
        assertEquals(MetronomeConfig.DEFAULT, validateOrDefault(raw(bpm = MetronomeConfig.BPM_MAX + 1)))
    }

    @Test
    fun `unsupported time signature returns DEFAULT`() {
        // 5/8 is not in SUPPORTED_SIGNATURES
        assertEquals(MetronomeConfig.DEFAULT, validateOrDefault(raw(numerator = 5, denominator = 8)))
    }

    @Test
    fun `unrecognized subdivision name returns DEFAULT`() {
        assertEquals(MetronomeConfig.DEFAULT, validateOrDefault(raw(subdivisionName = "UNKNOWN")))
    }

    @Test
    fun `invalid bpm overrides even when signature and subdivision are valid`() {
        // Whole-config replacement: one bad field → everything goes back to DEFAULT.
        assertEquals(MetronomeConfig.DEFAULT, validateOrDefault(raw(bpm = 9999, numerator = 4, denominator = 4)))
    }

    // ── requiresRepair ────────────────────────────────────────────────────────

    @Test
    fun `all-null raw config does not require repair`() {
        val allNull = raw(null, null, null, null)
        assertEquals(false, allNull.requiresRepair(MetronomeConfig.DEFAULT))
    }

    @Test
    fun `raw config that matches validated config does not require repair`() {
        val config = MetronomeConfig.DEFAULT
        val matching = RawMetronomeConfig(
            bpm = config.bpm,
            numerator = config.timeSignatureNumerator,
            denominator = config.timeSignatureDenominator,
            subdivisionName = config.subdivision.name,
        )
        assertEquals(false, matching.requiresRepair(config))
    }

    @Test
    fun `raw config with out-of-range bpm requires repair`() {
        val corrupted = raw(bpm = 9999)
        assertEquals(true, corrupted.requiresRepair(MetronomeConfig.DEFAULT))
    }

    @Test
    fun `raw config with unrecognized subdivision name requires repair`() {
        val corrupted = raw(subdivisionName = "CORRUPTED")
        assertEquals(true, corrupted.requiresRepair(MetronomeConfig.DEFAULT))
    }

    @Test
    fun `raw config with mismatched bpm requires repair even if subdivision matches`() {
        val config = MetronomeConfig.DEFAULT
        val mismatched = RawMetronomeConfig(
            bpm = config.bpm + 10,
            numerator = config.timeSignatureNumerator,
            denominator = config.timeSignatureDenominator,
            subdivisionName = config.subdivision.name,
        )
        assertEquals(true, mismatched.requiresRepair(config))
    }
}
