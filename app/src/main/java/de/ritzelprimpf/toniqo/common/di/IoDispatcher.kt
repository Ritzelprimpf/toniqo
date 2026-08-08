package de.ritzelprimpf.toniqo.common.di

import javax.inject.Qualifier

/**
 * Qualifies the injected `CoroutineDispatcher` backed by [kotlinx.coroutines.Dispatchers.IO].
 *
 * Injecting this instead of referencing `Dispatchers.IO` directly lets unit tests substitute a
 * `TestDispatcher` so `kotlinx-coroutines-test`'s virtual time can control I/O-bound `withContext`
 * hops deterministically.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
