package de.ritzelprimpf.toniqo.common.state

import de.ritzelprimpf.toniqo.common.model.ScaleType
import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleCandidate
import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LatestKeyResultStoreTest {

    private val store = LatestKeyResultStore()

    private fun match(rootPc: Int, type: ScaleType) = ScaleMatch(
        candidate = ScaleCandidate(rootPc, type),
        percent = 100,
        isFull = true,
        isRootMatch = false,
        rank = 1,
    )

    @Test
    fun `topResult defaults to null`() {
        assertNull(store.topResult.value)
    }

    @Test
    fun `publish updates topResult`() {
        val m = match(0, ScaleType.IONIAN)
        store.publish(m)
        assertEquals(m, store.topResult.value)
    }

    @Test
    fun `publish null resets topResult`() {
        store.publish(match(0, ScaleType.IONIAN))
        store.publish(null)
        assertNull(store.topResult.value)
    }

    @Test
    fun `successive publishes update topResult to the latest value`() {
        val first = match(0, ScaleType.IONIAN)
        val second = match(9, ScaleType.AEOLIAN)
        store.publish(first)
        store.publish(second)
        assertEquals(second, store.topResult.value)
    }
}
