package de.ritzelprimpf.toniqo.metronome

import de.ritzelprimpf.toniqo.metronome.data.AudioTrackMetronomePlayer
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.usecase.StartMetronomeUseCase
import de.ritzelprimpf.toniqo.metronome.domain.usecase.StopMetronomeUseCase
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MetronomeStubsTest {

    @Test
    fun `AudioTrackMetronomePlayer can be constructed and throws on start`() {
        val player = AudioTrackMetronomePlayer()

        assertThrows(NotImplementedError::class.java) { player.start(MetronomeConfig()) }
    }

    @Test
    fun `AudioTrackMetronomePlayer throws on stop`() {
        val player = AudioTrackMetronomePlayer()

        assertThrows(NotImplementedError::class.java) { player.stop() }
    }

    @Test
    fun `AudioTrackMetronomePlayer throws on updateConfig`() {
        val player = AudioTrackMetronomePlayer()

        assertThrows(NotImplementedError::class.java) { player.updateConfig(MetronomeConfig()) }
    }

    @Test
    fun `AudioTrackMetronomePlayer currentBeat emits nothing as its Phase 2 default`() {
        val player = AudioTrackMetronomePlayer()

        val collected = runBlocking { player.currentBeat.toList() }

        assertEquals(emptyList<Int>(), collected)
    }

    @Test
    fun `StartMetronomeUseCase propagates the player stub's NotImplementedError`() {
        val useCase = StartMetronomeUseCase(player = AudioTrackMetronomePlayer())

        assertThrows(NotImplementedError::class.java) { useCase(MetronomeConfig()) }
    }

    @Test
    fun `StopMetronomeUseCase propagates the player stub's NotImplementedError`() {
        val useCase = StopMetronomeUseCase(player = AudioTrackMetronomePlayer())

        assertThrows(NotImplementedError::class.java) { useCase() }
    }

    @Test
    fun `MetronomeConfig data class equality holds for matching fields`() {
        val a = MetronomeConfig()
        val b = MetronomeConfig()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `MetronomeConfig data class differs when BPM differs`() {
        val a = MetronomeConfig()
        val b = MetronomeConfig(bpm = MetronomeConfig.DEFAULT_BPM + 1)

        assertNotEquals(a, b)
    }
}
