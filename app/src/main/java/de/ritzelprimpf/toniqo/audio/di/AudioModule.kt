package de.ritzelprimpf.toniqo.audio.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ritzelprimpf.toniqo.audio.AudioCaptureSource
import de.ritzelprimpf.toniqo.audio.AudioRecordCaptureSource
import de.ritzelprimpf.toniqo.audio.PitchDetector
import de.ritzelprimpf.toniqo.audio.YinConfig
import de.ritzelprimpf.toniqo.audio.YinPitchDetector
import javax.inject.Singleton

/**
 * Hilt bindings for the shared audio pipeline.
 *
 * Provides the [AudioCaptureSource] and [PitchDetector] interfaces that are consumed by both
 * the Guitar Tuner and the Key Finder (and any future feature requiring microphone input).
 *
 * These bindings were previously split between `TunerModule` (capture source + pitch detector)
 * and `CommonModule`. They are now co-located here as part of the Phase 7.2 promotion of the
 * shared audio pipeline to its own top-level module.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    /**
     * Binds [AudioRecordCaptureSource] as the singleton implementation of [AudioCaptureSource].
     *
     * The singleton scope ensures only one `AudioRecord` hardware session is open at a time
     * across the entire application. Sequential use cases (Tuner active, then Key Finder active)
     * share the same source instance; the cold-`Flow` API means the hardware is only held while
     * a collector is active.
     */
    @Binds
    @Singleton
    abstract fun bindAudioCaptureSource(
        impl: AudioRecordCaptureSource,
    ): AudioCaptureSource

    /**
     * Binds [YinPitchDetector] as the singleton implementation of [PitchDetector].
     *
     * [YinPitchDetector] has no mutable state — calls are safe from any coroutine scope.
     * The singleton scope avoids redundant instantiation.
     */
    @Binds
    @Singleton
    abstract fun bindPitchDetector(
        impl: YinPitchDetector,
    ): PitchDetector

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
