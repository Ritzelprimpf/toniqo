package de.ritzelprimpf.toniqo.chordfinder.presentation.ui

import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.ui.theme.Tq
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Contract test for [DegreeColor.of] per DESIGN.md §2.4.
 *
 * Exhaustive — one case per [ChordQuality] value. If [ChordQuality] gains a new entry the
 * exhaustive `when` in [DegreeColor] will fail to compile, ensuring this test suite is updated.
 */
class DegreeColorTest {

    @Test
    fun `MAJOR maps to signal mint`() =
        assertEquals(Tq.Color.SignalMint, DegreeColor.of(ChordQuality.MAJOR))

    @Test
    fun `MINOR maps to signal cyan`() =
        assertEquals(Tq.Color.SignalCyan, DegreeColor.of(ChordQuality.MINOR))

    @Test
    fun `DIMINISHED maps to signal amber`() =
        assertEquals(Tq.Color.SignalAmber, DegreeColor.of(ChordQuality.DIMINISHED))

    @Test
    fun `AUGMENTED maps to signal violet`() =
        assertEquals(Tq.Color.SignalViolet, DegreeColor.of(ChordQuality.AUGMENTED))

    @Test
    fun `all ChordQuality values are handled — no unmatched entries`() {
        ChordQuality.entries.forEach { quality ->
            // Throws if the when is non-exhaustive at runtime (belt-and-suspenders guard)
            DegreeColor.of(quality)
        }
    }
}
