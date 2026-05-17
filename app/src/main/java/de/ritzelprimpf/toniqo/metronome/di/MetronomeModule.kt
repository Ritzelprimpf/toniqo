package de.ritzelprimpf.toniqo.metronome.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ritzelprimpf.toniqo.metronome.data.AudioTrackMetronomePlayer
import de.ritzelprimpf.toniqo.metronome.domain.repository.MetronomePlayer
import javax.inject.Singleton

/**
 * Hilt bindings for the Metronome feature.
 *
 * The only domain interface is [MetronomePlayer], bound to the `AudioTrack`-backed
 * implementation. Use cases are concrete classes with `@Inject constructor`, so they need no
 * explicit binding here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MetronomeModule {

    /** Binds [AudioTrackMetronomePlayer] as the singleton implementation of [MetronomePlayer]. */
    @Binds
    @Singleton
    abstract fun bindMetronomePlayer(
        impl: AudioTrackMetronomePlayer,
    ): MetronomePlayer
}
