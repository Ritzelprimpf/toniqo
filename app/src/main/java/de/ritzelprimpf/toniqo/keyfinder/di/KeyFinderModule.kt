package de.ritzelprimpf.toniqo.keyfinder.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ritzelprimpf.toniqo.keyfinder.data.KeyFinderServiceImpl
import de.ritzelprimpf.toniqo.keyfinder.data.StableNoteDetectorImpl
import de.ritzelprimpf.toniqo.keyfinder.domain.repository.KeyFinderService
import de.ritzelprimpf.toniqo.keyfinder.domain.repository.NoteDetector
import javax.inject.Singleton

/**
 * Hilt bindings for the Key Finder feature.
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
}
