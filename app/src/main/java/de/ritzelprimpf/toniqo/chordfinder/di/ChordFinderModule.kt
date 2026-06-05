package de.ritzelprimpf.toniqo.chordfinder.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ritzelprimpf.toniqo.chordfinder.data.VoicingRepositoryImpl
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.VoicingRepository
import javax.inject.Singleton

/**
 * Hilt bindings for the Chord Finder feature.
 *
 * [VoicingRepositoryImpl] is a singleton so the JSON asset is loaded at most once.
 * [de.ritzelprimpf.toniqo.chordfinder.domain.usecase.FindChordsUseCase] has an
 * `@Inject constructor()` and needs no explicit binding.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ChordFinderModule {

    /** Binds [VoicingRepositoryImpl] as the singleton implementation of [VoicingRepository]. */
    @Binds
    @Singleton
    abstract fun bindVoicingRepository(impl: VoicingRepositoryImpl): VoicingRepository
}
