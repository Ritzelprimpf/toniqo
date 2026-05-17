package de.ritzelprimpf.toniqo.chordfinder.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ritzelprimpf.toniqo.chordfinder.data.ChordFinderServiceImpl
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.ChordFinderService
import javax.inject.Singleton

/**
 * Hilt bindings for the Chord Finder feature.
 *
 * Binds the domain [ChordFinderService] to its stateless implementation. The use case is a
 * concrete class with `@Inject constructor`, so it needs no explicit binding here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ChordFinderModule {

    /** Binds [ChordFinderServiceImpl] as the singleton implementation of [ChordFinderService]. */
    @Binds
    @Singleton
    abstract fun bindChordFinderService(
        impl: ChordFinderServiceImpl,
    ): ChordFinderService
}
