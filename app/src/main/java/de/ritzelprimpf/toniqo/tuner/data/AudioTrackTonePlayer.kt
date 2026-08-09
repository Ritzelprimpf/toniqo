package de.ritzelprimpf.toniqo.tuner.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import de.ritzelprimpf.toniqo.tuner.data.audio.ToneAudioFormat
import de.ritzelprimpf.toniqo.tuner.data.audio.ToneParameters
import de.ritzelprimpf.toniqo.tuner.data.audio.ToneSynthesizer
import de.ritzelprimpf.toniqo.tuner.domain.repository.TonePlayer
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * [TonePlayer] implementation backed by [AudioTrack] in static (one-shot) mode.
 *
 * Each [play] call builds, plays, and releases its own [AudioTrack] — there is no persistent
 * player instance to manage, since a reference tone is always a short, self-contained one-shot
 * (unlike the metronome's continuous [de.ritzelprimpf.toniqo.metronome.data.AudioTrackMetronomePlayer],
 * which streams for as long as a collector is attached).
 *
 * `USAGE_MEDIA` + `CONTENT_TYPE_MUSIC` — this is a musical reference pitch, not a UI sound effect
 * (contrast with the metronome's `CONTENT_TYPE_SONIFICATION`).
 */
class AudioTrackTonePlayer @Inject constructor(
    private val toneSynthesizer: ToneSynthesizer,
) : TonePlayer {

    override suspend fun play(frequencyHz: Double, durationMs: Long): Unit = withContext(Dispatchers.IO) {
        val buffer = toneSynthesizer.generate(frequencyHz, durationMs)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(ToneAudioFormat.SAMPLE_RATE_HZ)
            .setChannelMask(ToneAudioFormat.CHANNEL_CONFIG)
            .setEncoding(ToneAudioFormat.ENCODING)
            .build()

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(buffer.size * ToneAudioFormat.BYTES_PER_SAMPLE)
            .build()

        // A freshly-built MODE_STATIC AudioTrack reports STATE_NO_STATIC_DATA, not
        // STATE_INITIALIZED — that's its normal, valid pre-write() state, not a failure. Only
        // STATE_UNINITIALIZED means construction genuinely failed.
        if (audioTrack.state == AudioTrack.STATE_UNINITIALIZED) {
            Log.e(TAG, "AudioTrack failed to initialise (state=${audioTrack.state})")
            audioTrack.release()
            return@withContext
        }

        try {
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            // buffer is WARMUP_MS of silence + durationMs of tone (see ToneSynthesizer) — hold
            // the track open for both, not just durationMs, or cleanup would cut the tone itself
            // short. A cancellation here (a new tone starting before this one finishes) propagates
            // straight to the finally block below, so the AudioTrack is always stopped and
            // released promptly rather than left playing.
            delay(ToneParameters.WARMUP_MS + durationMs)
        } finally {
            try {
                audioTrack.stop()
            } catch (t: Throwable) {
                Log.w(TAG, "AudioTrack.stop() threw during cleanup; continuing release", t)
            }
            audioTrack.release()
        }
    }

    private companion object {
        const val TAG = "TonePlayer"
    }
}
