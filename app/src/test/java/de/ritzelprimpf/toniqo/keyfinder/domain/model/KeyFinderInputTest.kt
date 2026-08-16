package de.ritzelprimpf.toniqo.keyfinder.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class KeyFinderInputTest {

    private val sampleInput = KeyFinderInput(
        pitchClasses = setOf(0, 4, 7), // C, E, G
        rootPitchClass = 0,            // C
    )

    @Test
    fun `data class equality holds for matching fields`() {
        val a = sampleInput
        val b = sampleInput.copy()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `data class differs when rootPitchClass differs`() {
        val a = sampleInput
        val b = sampleInput.copy(rootPitchClass = null)

        assertNotEquals(a, b)
    }
}
