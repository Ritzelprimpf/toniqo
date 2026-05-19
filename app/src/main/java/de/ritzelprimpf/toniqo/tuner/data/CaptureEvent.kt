package de.ritzelprimpf.toniqo.tuner.data

/**
 * Events emitted by [MicrophoneAudioSource].
 *
 * The first emission from a [MicrophoneAudioSource.samples] flow is always one of:
 * [PermissionDenied], [Listening], or [Failed]. While listening, subsequent emissions
 * are [Samples] until the collector cancels or an error occurs.
 *
 * [PermissionDenied] and [Failed] are terminal — the flow completes immediately after
 * emitting either of them.
 */
sealed interface CaptureEvent {

    /**
     * The `RECORD_AUDIO` permission is not granted. The flow completes after this emission.
     *
     * The UI layer should direct the user to grant the permission via the system settings or
     * by requesting it again through the Activity Result API.
     */
    data object PermissionDenied : CaptureEvent

    /**
     * Capture has started successfully.
     *
     * @property sampleRateHz The actual sample rate in use (typically 44100 Hz).
     * @property bufferFrames The number of audio frames in each [Samples] buffer.
     * @property source Which audio source was activated (UNPROCESSED preferred, MIC fallback).
     */
    data class Listening(
        val sampleRateHz: Int,
        val bufferFrames: Int,
        val source: AudioSourceKind,
    ) : CaptureEvent

    /**
     * A buffer of audio samples normalised to approximately `[-1.0f, 1.0f]`.
     *
     * **Array equality note.** This is a `data class` containing a [FloatArray]. Kotlin's
     * generated `equals` and `hashCode` use [FloatArray]'s default `Object.equals`, which
     * compares array instances by **reference**, not by content. This is intentional and
     * deliberate: comparing buffer contents on every emission would be extremely expensive.
     * Do **not** "fix" this by overriding `equals`/`hashCode` with `contentEquals`/
     * `contentHashCode` — the performance cost would be prohibitive. Buffer identity is all
     * that matters for downstream consumers, which process and discard each buffer in sequence.
     *
     * @property buffer The audio frames for this capture slice.
     */
    data class Samples(val buffer: FloatArray) : CaptureEvent

    /**
     * Capture has failed for a non-permission reason. The flow completes after this emission.
     *
     * @property reason A human-readable description of the failure, suitable for Logcat.
     * @property cause The originating exception, if available.
     */
    data class Failed(val reason: String, val cause: Throwable? = null) : CaptureEvent
}

/** Identifies which Android audio source [MicrophoneAudioSource] actually opened. */
enum class AudioSourceKind {
    /** `MediaRecorder.AudioSource.UNPROCESSED` — raw microphone signal without AGC/NR. */
    UNPROCESSED,

    /** `MediaRecorder.AudioSource.MIC` — standard microphone source, used as a fallback. */
    MIC,
}
