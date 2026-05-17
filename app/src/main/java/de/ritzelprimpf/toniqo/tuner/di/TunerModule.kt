package de.ritzelprimpf.toniqo.tuner.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ritzelprimpf.toniqo.tuner.data.TunerPresetRepositoryImpl
import de.ritzelprimpf.toniqo.tuner.data.YinPitchDetector
import de.ritzelprimpf.toniqo.tuner.domain.repository.PitchDetector
import de.ritzelprimpf.toniqo.tuner.domain.repository.TunerPresetRepository
import javax.inject.Singleton

/**
 * Hilt bindings for the Tuner feature.
 *
 * All concrete implementations use `@Inject constructor`, so this module's only job is to bind
 * their domain interfaces to those implementations via `@Binds` — no `@Provides` needed.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TunerModule {

    /** Binds [TunerPresetRepositoryImpl] as the singleton implementation of [TunerPresetRepository]. */
    @Binds
    @Singleton
    abstract fun bindTunerPresetRepository(
        impl: TunerPresetRepositoryImpl,
    ): TunerPresetRepository

    /** Binds [YinPitchDetector] as the singleton implementation of [PitchDetector]. */
    @Binds
    @Singleton
    abstract fun bindPitchDetector(
        impl: YinPitchDetector,
    ): PitchDetector
}
