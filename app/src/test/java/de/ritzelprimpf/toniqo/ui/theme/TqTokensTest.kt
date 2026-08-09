package de.ritzelprimpf.toniqo.ui.theme

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Sanity tests that [Tq] token constants match the hex values specified in DESIGN.md.
 * These tests guard against accidental refactors that silently change a token value.
 */
class TqTokensTest {

    // ── Signal colours (dark) ────────────────────────────────────────────────

    @Test
    fun `SignalMint dark matches design spec hex 9CFF8B`() {
        assertEquals(0xFF9CFF8B.toInt(), Tq.DarkColor.SignalMint.toArgb())
    }

    @Test
    fun `SignalCyan dark matches design spec hex 73B8E0`() {
        assertEquals(0xFF73B8E0.toInt(), Tq.DarkColor.SignalCyan.toArgb())
    }

    @Test
    fun `SignalAmber dark matches design spec hex E1B065`() {
        assertEquals(0xFFE1B065.toInt(), Tq.DarkColor.SignalAmber.toArgb())
    }

    @Test
    fun `SignalViolet dark matches design spec hex B6A0E0`() {
        assertEquals(0xFFB6A0E0.toInt(), Tq.DarkColor.SignalViolet.toArgb())
    }

    // ── Surface colours (dark) ───────────────────────────────────────────────

    @Test
    fun `BgBase dark matches design spec hex 1A1F22`() {
        assertEquals(0xFF1A1F22.toInt(), Tq.DarkColor.BgBase.toArgb())
    }

    @Test
    fun `BgInset dark matches design spec hex 161A1C`() {
        assertEquals(0xFF161A1C.toInt(), Tq.DarkColor.BgInset.toArgb())
    }

    // ── Text colours (dark) ──────────────────────────────────────────────────

    @Test
    fun `FgPrimary dark matches design spec hex F2F4F5`() {
        assertEquals(0xFFF2F4F5.toInt(), Tq.DarkColor.FgPrimary.toArgb())
    }

    @Test
    fun `FgQuaternary dark matches design spec hex 5E6468`() {
        assertEquals(0xFF5E6468.toInt(), Tq.DarkColor.FgQuaternary.toArgb())
    }

    // ── Light-theme colours ──────────────────────────────────────────────────

    @Test
    fun `BgBase light matches design spec hex F8F9FA`() {
        assertEquals(0xFFF8F9FA.toInt(), Tq.LightColor.BgBase.toArgb())
    }

    @Test
    fun `SignalMint light matches design spec hex 37A85F`() {
        assertEquals(0xFF37A85F.toInt(), Tq.LightColor.SignalMint.toArgb())
    }

    // ── Type tokens ──────────────────────────────────────────────────────────

    @Test
    fun `Body fontSize is 14sp`() {
        assertEquals(14.sp, Tq.Type.Body.fontSize)
    }

    @Test
    fun `Body letterSpacing type is em`() {
        assertEquals(TextUnitType.Em, Tq.Type.Body.letterSpacing.type)
    }

    @Test
    fun `Body letterSpacing value is minus 0_005`() {
        assertEquals(-0.005f, Tq.Type.Body.letterSpacing.value, 0.0001f)
    }

    @Test
    fun `DisplayXl fontSize is 96sp`() {
        assertEquals(96.sp, Tq.Type.DisplayXl.fontSize)
    }

    @Test
    fun `Kicker letterSpacing value is 0_16`() {
        assertEquals(0.16f, Tq.Type.Kicker.letterSpacing.value, 0.0001f)
    }

    // ── Spacing ──────────────────────────────────────────────────────────────

    @Test
    fun `Sp s4 is 16dp`() {
        assertEquals(16f, Tq.Sp.s4.value, 0.01f)
    }

    @Test
    fun `Sp s5 is 20dp`() {
        assertEquals(20f, Tq.Sp.s5.value, 0.01f)
    }

    @Test
    fun `Sp s0 is 0dp`() {
        assertEquals(0f, Tq.Sp.s0.value, 0.01f)
    }

    // ── Radii ────────────────────────────────────────────────────────────────

    @Test
    fun `Radius Lg is 16dp`() {
        assertEquals(16f, Tq.Radius.Lg.value, 0.01f)
    }

    @Test
    fun `Radius Pill is 999dp`() {
        assertEquals(999f, Tq.Radius.Pill.value, 0.01f)
    }

    @Test
    fun `Radius Xs is 4dp`() {
        assertEquals(4f, Tq.Radius.Xs.value, 0.01f)
    }
}
