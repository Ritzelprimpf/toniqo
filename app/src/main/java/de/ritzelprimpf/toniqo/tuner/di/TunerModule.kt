package de.ritzelprimpf.toniqo.tuner.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ritzelprimpf.toniqo.common.util.PitchDetector
import de.ritzelprimpf.toniqo.common.util.YinConfig
import de.ritzelprimpf.toniqo.common.util.YinPitchDetector
import de.ritzelprimpf.toniqo.tuner.data.MicrophoneAudioSource
import de.ritzelprimpf.toniqo.tuner.data.MicrophoneAudioSourceImpl
import de.ritzelprimpf.toniqo.tuner.data.TunerPreferences
import de.ritzelprimpf.toniqo.tuner.data.TunerPreferencesImpl
import de.ritzelprimpf.toniqo.tuner.data.TunerPresetRepositoryImpl
import de.ritzelprimpf.toniqo.tuner.domain.repository.TunerPresetRepository
import javax.inject.Singleton

/**
 * Hilt bindings and providers for the Tuner feature.
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

    /**
     * Binds [YinPitchDetector] as the singleton implementation of [PitchDetector].
     *
     * The interface moved to `common/util/` in Phase 5.2; the concrete implementation also lives
     * there. The decision is recorded in `DECISIONS.md`.
     */
    @Binds
    @Singleton
    abstract fun bindPitchDetector(
        impl: YinPitchDetector,
    ): PitchDetector

    /** Binds [MicrophoneAudioSourceImpl] as the singleton implementation of [MicrophoneAudioSource]. */
    @Binds
    @Singleton
    abstract fun bindMicrophoneAudioSource(
        impl: MicrophoneAudioSourceImpl,
    ): MicrophoneAudioSource

    /**
     * Binds [TunerPreferencesImpl] as the singleton implementation of [TunerPreferences].
     *
     * Added in Phase 5.3. [TunerPreferencesImpl] is backed by DataStore and requires the
     * application context (injected via `@ApplicationContext` in its constructor).
     */
    @Binds
    @Singleton
    abstract fun bindTunerPreferences(
        impl: TunerPreferencesImpl,
    ): TunerPreferences

    companion object {

        /**
         * Provides the production [YinConfig].
         *
         * Defaults are the values locked in `DECISIONS.md` during Phase 5.2:
         * threshold = 0.15, min = 30 Hz, max = 2000 Hz.
         */
        @Provides
        @Singleton
        fun provideYinConfig(): YinConfig = YinConfig()
    }
}
