package de.ritzelprimpf.toniqo.tuner.data

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import de.ritzelprimpf.toniqo.common.permission.AudioPermissionChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `callbackFlow`-based implementation of [MicrophoneAudioSource].
 *
 * ## AudioRecord lifecycle
 *
 * The `AudioRecord` instance is created, started, read from, and released entirely within
 * a single `callbackFlow` invocation. No `AudioRecord` reference escapes this class. The
 * `try/finally` around the read loop guarantees `stop()` and `release()` always run, even
 * if the collector cancels or an exception is thrown.
 *
 * ## Source selection
 *
 * `UNPROCESSED` is attempted first. On devices that support it, `UNPROCESSED` delivers a
 * raw microphone signal without AGC or noise reduction — important for accurate pitch
 * detection. If `UNPROCESSED` fails to initialise (`STATE_INITIALIZED` check), the
 * implementation falls back to `MIC`. The source actually used is reported in
 * [CaptureEvent.Listening] and logged at INFO level for debugging (no PII).
 *
 * ## Dispatcher
 *
 * The entire flow runs on [Dispatchers.IO] via `.flowOn`. `AudioRecord.read()` is a
 * blocking call and must not run on the main thread.
 *
 * @property permissionChecker Checked as the very first step inside the flow body.
 */
@Singleton
class MicrophoneAudioSourceImpl @Inject constructor(
    private val permissionChecker: AudioPermissionChecker,
) : MicrophoneAudioSource {

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun samples(): Flow<CaptureEvent> = callbackFlow {
        // Permission check is the first gate. If denied, the flow is terminal.
        if (!permissionChecker.hasRecordAudioPermission()) {
            trySend(CaptureEvent.PermissionDenied)
            close()
            return@callbackFlow
        }

        // Allow any previous AudioRecord instance to finish its current blocking read() call
        // and release hardware resources. Without this pause the new AudioRecord's read()
        // returns AudioRecord.ERROR_INVALID_OPERATION continuously, producing no audio.
        // A single 4096-frame buffer at 44 100 Hz takes ~93 ms to fill; 150 ms is a safe margin.
        delay(AUDIO_RECORD_SETTLE_MS)

        val minBufferBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferBytes <= 0) {
            trySend(CaptureEvent.Failed("AudioRecord.getMinBufferSize returned $minBufferBytes"))
            close()
            return@callbackFlow
        }

        // Use the larger of our preferred frame count and the hardware minimum.
        val frames = maxOf(BUFFER_FRAMES_DEFAULT, minBufferBytes / BYTES_PER_SAMPLE)

        val (record, sourceUsed) = buildRecorder(frames)
            ?: run {
                trySend(CaptureEvent.Failed("AudioRecord failed to initialise for both UNPROCESSED and MIC sources"))
                close()
                return@callbackFlow
            }

        Log.i(TAG, "AudioRecord opened: source=$sourceUsed sampleRate=${SAMPLE_RATE_HZ}Hz frames=$frames")

        try {
            record.startRecording()
            trySend(CaptureEvent.Listening(SAMPLE_RATE_HZ, frames, sourceUsed))

            val shortBuffer = ShortArray(frames)
            val floatBuffer = FloatArray(frames)

            while (isActive) {
                val read = record.read(shortBuffer, 0, frames)
                if (read <= 0) continue  // Negative values are AudioRecord error codes; skip.

                for (i in 0 until read) {
                    floatBuffer[i] = shortBuffer[i] / SHORT_MAX_AS_FLOAT
                }

                // Defensive copy: downstream consumers may collect on a different dispatcher, and
                // the read loop immediately reuses shortBuffer/floatBuffer on the next iteration.
                val emitted = floatBuffer.copyOf(read)
                trySend(CaptureEvent.Samples(emitted))
            }
        } catch (e: Throwable) {
            trySend(CaptureEvent.Failed(e.message ?: "audio capture error", e))
        } finally {
            // record.stop() can throw if the recorder was never started or is already stopped.
            // The empty catch is safe and intentional: we always want release() to run.
            try { record.stop() } catch (_: Throwable) { }
            record.release()
        }

        // awaitClose is reached after the finally block. If the collector cancelled, the
        // coroutine cancellation is observed here. The cleanup is already done above.
        awaitClose { /* AudioRecord already released in the finally block above */ }
    }.flowOn(Dispatchers.IO)

    /**
     * Attempts to construct a ready [AudioRecord], preferring [MediaRecorder.AudioSource.UNPROCESSED]
     * and falling back to [MediaRecorder.AudioSource.MIC].
     *
     * @return A pair of the initialised recorder and the source kind used, or `null` if both
     *   attempts fail to produce a recorder in [AudioRecord.STATE_INITIALIZED].
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    private fun buildRecorder(frames: Int): Pair<AudioRecord, AudioSourceKind>? {
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE_HZ)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()
        val bufferSizeBytes = frames * BYTES_PER_SAMPLE

        // First attempt: UNPROCESSED (no AGC / noise reduction — preferred for pitch detection).
        runCatching {
            val r = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSizeBytes)
                .build()
            if (r.state == AudioRecord.STATE_INITIALIZED) {
                return r to AudioSourceKind.UNPROCESSED
            }
            r.release()
        }

        // Fallback: MIC (standard microphone source).
        runCatching {
            val r = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSizeBytes)
                .build()
            if (r.state == AudioRecord.STATE_INITIALIZED) {
                return r to AudioSourceKind.MIC
            }
            r.release()
        }

        return null
    }

    private companion object {
        const val TAG = "MicrophoneAudioSource"
        const val SAMPLE_RATE_HZ = 44_100
        const val BUFFER_FRAMES_DEFAULT = 4_096
        const val BYTES_PER_SAMPLE = 2           // PCM 16-bit = 2 bytes per sample
        const val SHORT_MAX_AS_FLOAT = 32_768.0f // Short.MAX_VALUE + 1 for normalisation to [-1, 1]
        const val AUDIO_RECORD_SETTLE_MS = 300L  // safety margin for previous AudioRecord to release hardware
    }
}
