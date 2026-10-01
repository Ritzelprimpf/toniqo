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
        accentedBeats: String? = "0",
    ) = RawMetronomeConfig(bpm, numerator, denominator, subdivisionName, accentedBeats)

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
        // denominator 3 is not a power of two — never valid, preset or custom
        assertEquals(MetronomeConfig.DEFAULT, validateOrDefault(raw(numerator = 4, denominator = 3)))
    }

    @Test
    fun `custom non-preset signature within range is accepted`() {
        // 5/8 is not one of the 8 curated SUPPORTED_SIGNATURES presets, but is a valid custom signature.
        val result = validateOrDefault(raw(numerator = 5, denominator = 8, accentedBeats = "0"))

        assertEquals(5, result.timeSignatureNumerator)
        assertEquals(8, result.timeSignatureDenominator)
    }

    @Test
    fun `numerator at TIME_SIGNATURE_NUMERATOR_MAX is valid`() {
        val result = validateOrDefault(raw(numerator = MetronomeConfig.TIME_SIGNATURE_NUMERATOR_MAX, denominator = 4, accentedBeats = "0"))

        assertEquals(MetronomeConfig.TIME_SIGNATURE_NUMERATOR_MAX, result.timeSignatureNumerator)
    }

    @Test
    fun `numerator one above TIME_SIGNATURE_NUMERATOR_MAX returns DEFAULT`() {
        val result = validateOrDefault(
            raw(numerator = MetronomeConfig.TIME_SIGNATURE_NUMERATOR_MAX + 1, denominator = 4, accentedBeats = "0"),
        )

        assertEquals(MetronomeConfig.DEFAULT, result)
    }

    @Test
    fun `every supported denominator is individually valid at a fixed numerator`() {
        MetronomeConfig.SUPPORTED_DENOMINATORS.forEach { denominator ->
            val result = validateOrDefault(raw(numerator = 4, denominator = denominator, accentedBeats = "0"))
            assertEquals("denominator $denominator should round-trip", denominator, result.timeSignatureDenominator)
        }
    }

    @Test
    fun `a denominator that is not a power of two returns DEFAULT`() {
        assertEquals(MetronomeConfig.DEFAULT, validateOrDefault(raw(numerator = 4, denominator = 6)))
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
        val allNull = raw(null, null, null, null, null)
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
            accentedBeats = encodeAccentedBeats(config.accentedBeats),
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
            accentedBeats = encodeAccentedBeats(config.accentedBeats),
        )
        assertEquals(true, mismatched.requiresRepair(config))
    }

    // ── accentedBeats parsing ─────────────────────────────────────────────────

    @Test
    fun `valid accentedBeats string round-trips to the matching set`() {
        val result = validateOrDefault(raw(numerator = 4, accentedBeats = "0,2"))

        assertEquals(setOf(0, 2), result.accentedBeats)
    }

    @Test
    fun `empty accentedBeats string means no beat is accented`() {
        val result = validateOrDefault(raw(accentedBeats = ""))

        assertEquals(emptySet<Int>(), result.accentedBeats)
    }

    @Test
    fun `null accentedBeats falls back to the default pattern without resetting the rest of the config`() {
        // Simulates a config persisted before this field existed: every other field is valid.
        val result = validateOrDefault(raw(bpm = 90, numerator = 3, denominator = 4, accentedBeats = null))

        assertEquals(MetronomeConfig.DEFAULT_ACCENTED_BEATS, result.accentedBeats)
        assertEquals(90, result.bpm) // other fields are untouched, not reset to MetronomeConfig.DEFAULT
        assertEquals(3, result.timeSignatureNumerator)
    }

    @Test
    fun `malformed accentedBeats falls back to the default pattern without resetting the rest of the config`() {
        val result = validateOrDefault(raw(bpm = 90, accentedBeats = "not,a,number"))

        assertEquals(MetronomeConfig.DEFAULT_ACCENTED_BEATS, result.accentedBeats)
        assertEquals(90, result.bpm)
    }

    @Test
    fun `accentedBeats index out of range for the current numerator falls back to the default`() {
        // numerator=4 → valid indices are 0..3; index 5 is out of range.
        val result = validateOrDefault(raw(numerator = 4, accentedBeats = "0,5"))

        assertEquals(MetronomeConfig.DEFAULT_ACCENTED_BEATS, result.accentedBeats)
    }

    @Test
    fun `requiresRepair is true when accentedBeats is absent but every other field matches`() {
        val config = MetronomeConfig.DEFAULT
        val migrating = RawMetronomeConfig(
            bpm = config.bpm,
            numerator = config.timeSignatureNumerator,
            denominator = config.timeSignatureDenominator,
            subdivisionName = config.subdivision.name,
            accentedBeats = null,
        )

        assertEquals(true, migrating.requiresRepair(config))
    }
}
