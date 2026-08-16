package de.ritzelprimpf.toniqo.keyfinder.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ritzelprimpf.toniqo.keyfinder.data.StableNoteDetectorImpl
import de.ritzelprimpf.toniqo.keyfinder.domain.repository.NoteDetector
import de.ritzelprimpf.toniqo.keyfinder.domain.usecase.MatchScalesUseCase
import javax.inject.Singleton

/**
 * Hilt bindings for the Key Finder feature.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class KeyFinderModule {

    /**
     * Binds [StableNoteDetectorImpl] as the singleton implementation of [NoteDetector].
     *
     * The singleton scope ensures the same capture state is shared between the ViewModel and
     * any collector that subscribes to [NoteDetector.detectedNotes].
     */
    @Binds
    @Singleton
    abstract fun bindNoteDetector(
        impl: StableNoteDetectorImpl,
    ): NoteDetector

    companion object {

        /**
         * Provides the [MatchScalesUseCase] with the production [de.ritzelprimpf.toniqo.keyfinder.domain.ScaleCatalog.DEFAULT]
         * catalog (168 candidates: 12 roots × 14 scale types).
         *
         * Not scoped to singleton — the use case is stateless and cheap to instantiate; a new
         * instance per injection site is fine, but in practice it is only injected into
         * [de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel.KeyFinderViewModel].
         */
        @Provides
        fun provideMatchScalesUseCase(): MatchScalesUseCase = MatchScalesUseCase()
    }
}
