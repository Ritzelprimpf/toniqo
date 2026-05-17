package de.ritzelprimpf.toniqo.keyfinder.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ritzelprimpf.toniqo.keyfinder.data.KeyFinderServiceImpl
import de.ritzelprimpf.toniqo.keyfinder.domain.repository.KeyFinderService
import javax.inject.Singleton

/**
 * Hilt bindings for the Key Finder feature.
 *
 * Binds the domain [KeyFinderService] to its stateless implementation. The use case is a
 * concrete class with `@Inject constructor`, so it needs no explicit binding here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class KeyFinderModule {

    /** Binds [KeyFinderServiceImpl] as the singleton implementation of [KeyFinderService]. */
    @Binds
    @Singleton
    abstract fun bindKeyFinderService(
        impl: KeyFinderServiceImpl,
    ): KeyFinderService
}
