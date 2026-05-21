package de.ritzelprimpf.toniqo.tuner.presentation.mapping

import de.ritzelprimpf.toniqo.tuner.domain.model.TuningStatus
import de.ritzelprimpf.toniqo.ui.theme.Tq
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Contract test for [TuningStatus.toSignalColor] per DESIGN.md §2.4.
 *
 * This is the single source of truth for the cents → colour mapping; if the mapping changes,
 * this test must be updated alongside [TuningStatusExt].
 */
class TuningStatusExtTest {

    @Test
    fun `FLAT maps to signal cyan`() =
        assertEquals(Tq.Color.SignalCyan, TuningStatus.FLAT.toSignalColor())

    @Test
    fun `IN_TUNE maps to signal mint`() =
        assertEquals(Tq.Color.SignalMint, TuningStatus.IN_TUNE.toSignalColor())

    @Test
    fun `SHARP maps to signal amber`() =
        assertEquals(Tq.Color.SignalAmber, TuningStatus.SHARP.toSignalColor())

    @Test
    fun `ALL_STRINGS_TUNED maps to signal mint`() =
        assertEquals(Tq.Color.SignalMint, TuningStatus.ALL_STRINGS_TUNED.toSignalColor())

    @Test
    fun `LISTENING maps to fg quaternary`() =
        assertEquals(Tq.Color.FgQuaternary, TuningStatus.LISTENING.toSignalColor())

    @Test
    fun `IDLE maps to fg quaternary`() =
        assertEquals(Tq.Color.FgQuaternary, TuningStatus.IDLE.toSignalColor())

    @Test
    fun `PERMISSION_DENIED maps to fg quaternary`() =
        assertEquals(Tq.Color.FgQuaternary, TuningStatus.PERMISSION_DENIED.toSignalColor())

    @Test
    fun `CAPTURE_FAILED maps to fg quaternary`() =
        assertEquals(Tq.Color.FgQuaternary, TuningStatus.CAPTURE_FAILED.toSignalColor())
}
