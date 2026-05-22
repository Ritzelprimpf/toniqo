package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.PlayerEvent
import de.ritzelprimpf.toniqo.metronome.domain.usecase.StartMetronomeUseCase
import de.ritzelprimpf.toniqo.metronome.fakes.FakeMetronomePlayer
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StartMetronomeUseCaseTest {

    private val player = FakeMetronomePlayer()
    private val useCase = StartMetronomeUseCase(player)

    @Test
    fun `invoke delegates to player run and returns its event flow`() = runTest {
        val config = MetronomeConfig.DEFAULT

        val firstEvent = useCase(initialConfig = config, configFlow = emptyFlow()).first()

        assertEquals(PlayerEvent.Started, firstEvent)
        assertEquals(config, player.receivedInitialConfig)
    }
}
