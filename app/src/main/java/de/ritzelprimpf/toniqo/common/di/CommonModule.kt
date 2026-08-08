package de.ritzelprimpf.toniqo.common.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ritzelprimpf.toniqo.common.permission.AndroidAudioPermissionChecker
import de.ritzelprimpf.toniqo.common.permission.AudioPermissionChecker
import de.ritzelprimpf.toniqo.common.util.Clock
import de.ritzelprimpf.toniqo.common.util.SystemClock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Hilt bindings for cross-feature common utilities.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CommonModule {

    /** Binds [AndroidAudioPermissionChecker] as the singleton implementation of [AudioPermissionChecker]. */
    @Binds
    @Singleton
    abstract fun bindAudioPermissionChecker(
        impl: AndroidAudioPermissionChecker,
    ): AudioPermissionChecker

    /**
     * Binds [SystemClock] as the singleton implementation of [Clock].
     *
     * Injected wherever [System.nanoTime] is needed so that unit tests can substitute a fake clock.
     * See `Phase6_2-PLAN.md` for the rationale (scheduler and tap-tempo logic both require it).
     */
    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemClock): Clock

    companion object {
        /**
         * Provides [Dispatchers.IO] qualified by [IoDispatcher].
         *
         * Injected wherever a `withContext(Dispatchers.IO)`-style hop is needed so unit tests can
         * substitute a `TestDispatcher` instead of hopping onto a real, untestable thread pool.
         */
        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    }
}
