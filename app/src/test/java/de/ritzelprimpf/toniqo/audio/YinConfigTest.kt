package de.ritzelprimpf.toniqo.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class YinConfigTest {

    @Test
    fun `default YinConfig has expected threshold, min and max frequency values`() {
        val config = YinConfig()
        assertEquals(0.15, config.threshold, 0.0)
        assertEquals(30.0, config.absoluteMinFrequencyHz, 0.0)
        assertEquals(2000.0, config.absoluteMaxFrequencyHz, 0.0)
    }
}
