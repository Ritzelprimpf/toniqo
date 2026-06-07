package de.ritzelprimpf.toniqo.chordfinder.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ritzelprimpf.toniqo.chordfinder.data.ChordFinderSelectionRepositoryImpl
import de.ritzelprimpf.toniqo.chordfinder.data.VoicingRepositoryImpl
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.ChordFinderSelectionRepository
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.VoicingRepository
import javax.inject.Singleton

/**
 * Hilt bindings for the Chord Finder feature.
 *
 * - [VoicingRepositoryImpl] — singleton so the JSON asset is loaded at most once.
 * - [ChordFinderSelectionRepositoryImpl] — singleton so DataStore is a single instance.
 * - [de.ritzelprimpf.toniqo.chordfinder.domain.usecase.FindChordsUseCase] has an
 *   `@Inject constructor()` and needs no explicit binding.
 * - [de.ritzelprimpf.toniqo.common.state.LatestKeyResultStore] and
 *   [de.ritzelprimpf.toniqo.common.state.SelectedTuningStore] use `@Inject constructor()` +
 *   `@Singleton` and are auto-provided; they need no explicit bindings here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ChordFinderModule {

    /** Binds [VoicingRepositoryImpl] as the singleton implementation of [VoicingRepository]. */
    @Binds
    @Singleton
    abstract fun bindVoicingRepository(impl: VoicingRepositoryImpl): VoicingRepository

    /**
     * Binds [ChordFinderSelectionRepositoryImpl] as the singleton implementation of
     * [ChordFinderSelectionRepository].
     */
    @Binds
    @Singleton
    abstract fun bindChordFinderSelectionRepository(
        impl: ChordFinderSelectionRepositoryImpl,
    ): ChordFinderSelectionRepository
}
