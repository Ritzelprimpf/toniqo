package de.ritzelprimpf.toniqo.chordfinder.presentation.ui

import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.ui.theme.TqPalette
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Contract test for [DegreeColor.of] per DESIGN.md §2.4.
 *
 * Exhaustive — one case per [ChordQuality] value. If [ChordQuality] gains a new entry the
 * exhaustive `when` in [DegreeColor] will fail to compile, ensuring this test suite is updated.
 * Uses [TqPalette.Dark] as a stand-in palette — the mapping itself is palette-agnostic.
 */
class DegreeColorTest {

    private val palette = TqPalette.Dark

    @Test
    fun `MAJOR maps to signal mint`() =
        assertEquals(palette.signalMint, DegreeColor.of(ChordQuality.MAJOR, palette))

    @Test
    fun `MINOR maps to signal cyan`() =
        assertEquals(palette.signalCyan, DegreeColor.of(ChordQuality.MINOR, palette))

    @Test
    fun `DIMINISHED maps to signal amber`() =
        assertEquals(palette.signalAmber, DegreeColor.of(ChordQuality.DIMINISHED, palette))

    @Test
    fun `AUGMENTED maps to signal violet`() =
        assertEquals(palette.signalViolet, DegreeColor.of(ChordQuality.AUGMENTED, palette))

    @Test
    fun `all ChordQuality values are handled — no unmatched entries`() {
        ChordQuality.entries.forEach { quality ->
            // Throws if the when is non-exhaustive at runtime (belt-and-suspenders guard)
            DegreeColor.of(quality, palette)
        }
    }
}
