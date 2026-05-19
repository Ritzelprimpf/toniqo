package de.ritzelprimpf.toniqo.tuner.data

import kotlinx.coroutines.flow.Flow

/**
 * Streams microphone audio as a flow of capture events.
 *
 * ## Flow contract
 *
 * - The **first emission** is always one of [CaptureEvent.PermissionDenied],
 *   [CaptureEvent.Listening], or [CaptureEvent.Failed].
 * - [CaptureEvent.Samples] emissions only ever follow a [CaptureEvent.Listening] emission.
 * - [CaptureEvent.PermissionDenied] and [CaptureEvent.Failed] are terminal — the flow
 *   completes immediately after emitting either of them.
 *
 * ## Lifecycle
 *
 * The `AudioRecord` lifecycle is fully bound to the collector's coroutine scope. Cancelling
 * the collection releases all audio resources. There are **no** explicit `start()` or
 * `stop()` methods, and the implementation holds **no** mutable state between calls.
 * Each call to [samples] opens its own independent audio session.
 */
interface MicrophoneAudioSource {

    /** Returns a cold [Flow] that begins capturing audio when collected. */
    fun samples(): Flow<CaptureEvent>
}
