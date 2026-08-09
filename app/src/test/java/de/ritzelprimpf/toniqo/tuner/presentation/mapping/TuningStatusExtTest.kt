package de.ritzelprimpf.toniqo.tuner.presentation.mapping

import de.ritzelprimpf.toniqo.tuner.domain.model.TuningStatus
import de.ritzelprimpf.toniqo.ui.theme.TqPalette
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Contract test for [TuningStatus.toSignalColor] per DESIGN.md §2.4.
 *
 * This is the single source of truth for the cents → colour mapping; if the mapping changes,
 * this test must be updated alongside [TuningStatusExt]. Uses [TqPalette.Dark] as a stand-in
 * palette — the mapping itself is palette-agnostic (same relative tokens regardless of theme),
 * so any concrete palette would assert the same relationships.
 */
class TuningStatusExtTest {

    private val palette = TqPalette.Dark

    @Test
    fun `FLAT maps to signal cyan`() =
        assertEquals(palette.signalCyan, TuningStatus.FLAT.toSignalColor(palette))

    @Test
    fun `IN_TUNE maps to signal mint`() =
        assertEquals(palette.signalMint, TuningStatus.IN_TUNE.toSignalColor(palette))

    @Test
    fun `SHARP maps to signal amber`() =
        assertEquals(palette.signalAmber, TuningStatus.SHARP.toSignalColor(palette))

    @Test
    fun `ALL_STRINGS_TUNED maps to signal mint`() =
        assertEquals(palette.signalMint, TuningStatus.ALL_STRINGS_TUNED.toSignalColor(palette))

    @Test
    fun `LISTENING maps to fg quaternary`() =
        assertEquals(palette.fgQuaternary, TuningStatus.LISTENING.toSignalColor(palette))

    @Test
    fun `IDLE maps to fg quaternary`() =
        assertEquals(palette.fgQuaternary, TuningStatus.IDLE.toSignalColor(palette))

    @Test
    fun `PERMISSION_DENIED maps to fg quaternary`() =
        assertEquals(palette.fgQuaternary, TuningStatus.PERMISSION_DENIED.toSignalColor(palette))

    @Test
    fun `CAPTURE_FAILED maps to fg quaternary`() =
        assertEquals(palette.fgQuaternary, TuningStatus.CAPTURE_FAILED.toSignalColor(palette))
}
