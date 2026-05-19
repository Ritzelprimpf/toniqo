package de.ritzelprimpf.toniqo.common.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ritzelprimpf.toniqo.common.permission.AndroidAudioPermissionChecker
import de.ritzelprimpf.toniqo.common.permission.AudioPermissionChecker
import javax.inject.Singleton

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
}
