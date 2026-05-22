package de.ritzelprimpf.toniqo.metronome.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import de.ritzelprimpf.toniqo.common.util.Clock
import de.ritzelprimpf.toniqo.metronome.data.audio.ClickKind
import de.ritzelprimpf.toniqo.metronome.data.audio.ClickSynthesizer
import de.ritzelprimpf.toniqo.metronome.data.audio.MetronomeAudioFormat
import de.ritzelprimpf.toniqo.metronome.domain.model.MetronomeConfig
import de.ritzelprimpf.toniqo.metronome.domain.model.PlayerEvent
import de.ritzelprimpf.toniqo.metronome.domain.model.PlayerFailureReason
import de.ritzelprimpf.toniqo.metronome.domain.repository.MetronomePlayer
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * [MetronomePlayer] implementation backed by [AudioTrack] in streaming mode.
 *
 * ## Lifetime
 *
 * The [AudioTrack] instance is created, played, and released entirely within a single [run]
 * invocation. Cancelling the flow's collector is the only way to stop playback — there is no
 * imperative stop API. The `awaitClose` block guarantees [AudioTrack.stop], [AudioTrack.release],
 * and audio-focus abandonment run unconditionally, even if the collector cancels or an exception
 * is thrown.
 *
 * ## Scheduling
 *
 * An anchor-based drift-corrected loop (see [BeatScheduler]) computes each click's target time
 * from a fixed start anchor in nanoseconds. Drift is impossible by construction. Config updates
 * (BPM, time signature, subdivision) are routed in via a conflated [Channel] so the scheduler
 * always sees the latest config without stale reads.
 *
 * ## Audio focus
 *
 * [AudioManager.AUDIOFOCUS_GAIN] is requested on start and abandoned on stop. Any focus-loss
 * event (transient or permanent) closes the flow immediately. No auto-resume.
 *
 * ## Audio attributes
 *
 * `USAGE_MEDIA` + `CONTENT_TYPE_SONIFICATION` — correct for non-musical click sounds;
 * respects system media volume. Per `Phase6-Metronome-Decisions.md` Item 12.
 */
internal class AudioTrackMetronomePlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clickSynthesizer: ClickSynthesizer,
    private val clock: Clock,
) : MetronomePlayer {

    override fun run(
        initialConfig: MetronomeConfig,
        configFlow: Flow<MetronomeConfig>,
    ): Flow<PlayerEvent> = callbackFlow {
        // 1. Pre-generate one click buffer per kind. Done once at player init;
        //    the per-beat hot path is allocation-free.
        val clickBuffers: Map<ClickKind, ShortArray> = mapOf(
            ClickKind.ACCENTED to clickSynthesizer.generate(ClickKind.ACCENTED),
            ClickKind.STANDARD to clickSynthesizer.generate(ClickKind.STANDARD),
            ClickKind.SUBDIVISION to clickSynthesizer.generate(ClickKind.SUBDIVISION),
        )

        // 2. Build AudioTrack in streaming mode with SONIFICATION attributes.
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(MetronomeAudioFormat.SAMPLE_RATE_HZ)
            .setChannelMask(MetronomeAudioFormat.CHANNEL_CONFIG)
            .setEncoding(MetronomeAudioFormat.ENCODING)
            .build()

        val minBufferBytes = AudioTrack.getMinBufferSize(
            MetronomeAudioFormat.SAMPLE_RATE_HZ,
            MetronomeAudioFormat.CHANNEL_CONFIG,
            MetronomeAudioFormat.ENCODING,
        )
        val clickSizeBytes = (clickBuffers[ClickKind.ACCENTED]!!.size) * MetronomeAudioFormat.BYTES_PER_SAMPLE
        val bufferSizeBytes = maxOf(minBufferBytes, clickSizeBytes)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSizeBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "AudioTrack failed to initialise (state=${audioTrack.state})")
            audioTrack.release()
            trySend(PlayerEvent.Failed(PlayerFailureReason.AUDIO_TRACK_INIT_FAILED))
            close()
            return@callbackFlow
        }

        // 3. Request audio focus before any sound is produced.
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { focusChange ->
                // Any focus-loss event stops playback immediately (per Phase6-Metronome-Decisions Item 5).
                // The close() call cancels the scheduler and config jobs, triggers awaitClose.
                if (focusChange != AudioManager.AUDIOFOCUS_GAIN) {
                    Log.i(TAG, "Audio focus lost (focusChange=$focusChange); stopping playback")
                    close()
                }
            }
            .build()

        val focusResult = audioManager.requestAudioFocus(focusRequest)
        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e(TAG, "Audio focus request denied (result=$focusResult)")
            audioTrack.release()
            trySend(PlayerEvent.Failed(PlayerFailureReason.AUDIO_FOCUS_DENIED))
            close()
            return@callbackFlow
        }

        // 4. Start AudioTrack and signal successful launch.
        audioTrack.play()
        trySend(PlayerEvent.Started)
        Log.i(TAG, "Metronome started: bpm=${initialConfig.bpm} sig=" +
            "${initialConfig.timeSignatureNumerator}/${initialConfig.timeSignatureDenominator} " +
            "sub=${initialConfig.subdivision}")

        // 5. Route config updates from configFlow into a conflated channel so the scheduler
        //    always sees the latest config without blocking on config delivery.
        val configChannel = Channel<MetronomeConfig>(Channel.CONFLATED)
        val configJob = launch {
            configFlow.collect { configChannel.trySend(it) }
        }

        // 6. Anchor-based scheduler loop. Runs on IO because AudioTrack.write is blocking.
        val schedulerJob = launch {
            val scheduler = BeatScheduler(clock, initialConfig)

            while (isActive) {
                // Apply any pending config update before playing the current click.
                val newConfig = configChannel.tryReceive().getOrNull()
                if (newConfig != null) {
                    val oldConfig = scheduler.config
                    val signatureOrSubdivisionChanged =
                        newConfig.timeSignatureNumerator != oldConfig.timeSignatureNumerator ||
                        newConfig.timeSignatureDenominator != oldConfig.timeSignatureDenominator ||
                        newConfig.subdivision != oldConfig.subdivision
                    if (signatureOrSubdivisionChanged) {
                        scheduler.onSignatureOrSubdivisionChanged(newConfig)
                    } else if (newConfig.bpm != oldConfig.bpm) {
                        scheduler.onBpmChanged(newConfig)
                    }
                }

                // Write the current click to the AudioTrack buffer.
                val kind = scheduler.currentClickKind()
                val buffer = clickBuffers.getValue(kind)
                audioTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)

                // Emit a BeatTick for every main beat (subdivisions are heard but not surfaced).
                if (scheduler.isMainBeat()) {
                    trySend(PlayerEvent.BeatTick(scheduler.mainBeatIndex()))
                }

                // Advance past this click and sleep until the next target time.
                scheduler.advance()
                val sleepNs = scheduler.targetNs() - clock.nanoTime()
                if (sleepNs > 0) {
                    kotlinx.coroutines.delay(sleepNs / NANOS_PER_MS)
                }
            }
        }

        // 7. Release all resources when the collector cancels or close() is called.
        awaitClose {
            configJob.cancel()
            schedulerJob.cancel()
            try {
                audioTrack.stop()
            } catch (t: Throwable) {
                Log.w(TAG, "AudioTrack.stop() threw during cleanup; continuing release", t)
            }
            audioTrack.release()
            audioManager.abandonAudioFocusRequest(focusRequest)
            Log.i(TAG, "Metronome stopped; AudioTrack released, audio focus abandoned")
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val TAG = "MetronomePlayer"
    }
}
