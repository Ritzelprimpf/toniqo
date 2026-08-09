package de.ritzelprimpf.toniqo.tuner.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ritzelprimpf.toniqo.tuner.data.AudioTrackTonePlayer
import de.ritzelprimpf.toniqo.tuner.data.TunerPreferences
import de.ritzelprimpf.toniqo.tuner.data.TunerPreferencesImpl
import de.ritzelprimpf.toniqo.tuner.data.TunerPresetRepositoryImpl
import de.ritzelprimpf.toniqo.tuner.domain.repository.TonePlayer
import de.ritzelprimpf.toniqo.tuner.domain.repository.TunerPresetRepository
import javax.inject.Singleton

/**
 * Hilt bindings for the Tuner feature.
 *
 * Audio capture ([AudioCaptureSource]) and pitch detection ([PitchDetector] / [YinConfig])
 * are now bound in `audio/di/AudioModule` — they are shared with Key Finder and must not be
 * duplicated here.
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

    /** Binds [AudioTrackTonePlayer] as the implementation of [TonePlayer]. */
    @Binds
    abstract fun bindTonePlayer(
        impl: AudioTrackTonePlayer,
    ): TonePlayer
}
