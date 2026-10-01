package de.ritzelprimpf.toniqo.metronome.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind
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
 * ## Scheduling — continuous stream, not gap-based writes
 *
 * **This is the second generation of this player.** The first wrote one click buffer per beat and
 * wrote nothing in between, sleeping via wall-clock `delay()` until the next click was due. That
 * let the `AudioTrack` output path go idle during the (long, at low BPM) silence gaps; field
 * testing on multiple Pixel devices (not reproducible on a Sony Xperia 10 IV) confirmed the
 * output path doesn't resume cleanly from that idle gap — clicks 2–3 of a 4/4 bar would go
 * missing and then fire back-to-back right before the next downbeat, with the app's own clock
 * never falling behind (ruling out a GC/dispatcher stall as the cause; see `docs/DECISIONS.md`,
 * 2026-10-01 entry, for the full diagnostic trail).
 *
 * This version never writes "nothing." [ClickStreamRenderer] mixes a continuous PCM stream —
 * explicit zero-valued silence samples and click samples, chunk by chunk — and the scheduler
 * loop below does nothing but keep calling it and writing the result. The output path is always
 * receiving real audio, so it has no idle gap to stall on. [BeatScheduler] tracks *which* click
 * plays next and *where* (as a sample position in that stream) rather than *when* (a wall-clock
 * instant) — see its class doc for why that distinction is what makes the fix work. Config
 * updates (BPM, time signature, subdivision) are routed in via a conflated [Channel] and applied
 * between chunks, re-anchoring the scheduler at the renderer's current stream position.
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
class AudioTrackMetronomePlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clickSynthesizer: ClickSynthesizer,
) : MetronomePlayer {

    override fun run(
        initialConfig: MetronomeConfig,
        configFlow: Flow<MetronomeConfig>,
    ): Flow<PlayerEvent> = callbackFlow {
        // 1. Pre-generate one click buffer per kind. Done once at player init;
        //    the per-chunk hot path is allocation-free.
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
        val chunkBytes = CHUNK_SAMPLES * MetronomeAudioFormat.BYTES_PER_SAMPLE
        val bufferSizeBytes = maxOf(minBufferBytes, chunkBytes * BUFFER_DEPTH_CHUNKS)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSizeBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            // Deep-buffer path: tolerates this player's steady throughput at the cost of higher
            // but constant latency. Kept from the previous generation as defense in depth — this
            // rewrite's continuous feed is the fix for the idle-gap glitch itself, but there's no
            // reason to also opt back into the fast mixer's much smaller underrun margin.
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_POWER_SAVING)
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

        // No warmup-silence write here (the previous generation had one). That existed to absorb
        // AudioTrack's cold-start latency before the wall-clock anchor was set, so the first click
        // wouldn't land early relative to the steady-state latency every later click experienced.
        // With a continuous stream, sample 0 of the stream (silence or click) is generated and
        // written exactly like every later sample — cold-start latency shifts the whole stream's
        // audible onset by one constant amount, not just the first click relative to the rest —
        // so there's no first-click-vs-later-clicks differential left to absorb.

        // 5. Route config updates from configFlow into a conflated channel so the scheduler
        //    always sees the latest config without blocking on config delivery.
        val configChannel = Channel<MetronomeConfig>(Channel.CONFLATED)
        val configJob = launch {
            configFlow.collect { configChannel.trySend(it) }
        }

        // 6. Continuous-stream generation loop. Runs on IO because AudioTrack.write is blocking;
        //    those blocking writes are this loop's only pacing mechanism — there is no delay().
        val schedulerJob = launch {
            val scheduler = BeatScheduler(MetronomeAudioFormat.SAMPLE_RATE_HZ, initialConfig)
            val renderer = ClickStreamRenderer(scheduler, clickBuffers)
            val chunk = ShortArray(CHUNK_SAMPLES)

            while (isActive) {
                // Apply any pending config update before rendering the next chunk.
                val newConfig = configChannel.tryReceive().getOrNull()
                if (newConfig != null) {
                    val oldConfig = scheduler.config
                    val signatureOrSubdivisionChanged =
                        newConfig.timeSignatureNumerator != oldConfig.timeSignatureNumerator ||
                        newConfig.timeSignatureDenominator != oldConfig.timeSignatureDenominator ||
                        newConfig.subdivision != oldConfig.subdivision
                    when {
                        signatureOrSubdivisionChanged ->
                            scheduler.onSignatureOrSubdivisionChanged(newConfig, renderer.position)
                        newConfig.bpm != oldConfig.bpm ->
                            scheduler.onBpmChanged(newConfig, renderer.position)
                        // Covers every other config change (currently only accentedBeats) — applied
                        // immediately, in place, with no re-anchor: it doesn't affect click timing.
                        else -> scheduler.updateConfig(newConfig)
                    }
                }

                // Mix the next chunk of silence/click samples and write it. This write is the
                // loop's entire pacing mechanism: it blocks until AudioTrack has room, which
                // happens at the real output rate once its buffer is full.
                val ticks = renderer.renderChunk(chunk)
                audioTrack.write(chunk, 0, chunk.size, AudioTrack.WRITE_BLOCKING)

                // Emit a BeatTick for every main beat that started in this chunk (subdivisions
                // are heard but not surfaced).
                for (beatIndex in ticks) {
                    trySend(PlayerEvent.BeatTick(beatIndex))
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

        /**
         * Size, in samples, of each chunk generated and written per loop iteration. 480 samples
         * at 48 kHz = 10 ms — fine enough that BPM/signature/subdivision changes feel instant,
         * coarse enough to keep write() overhead (and therefore CPU wakeups) low. Click placement
         * itself is sample-accurate regardless of this value; it only affects write granularity
         * and how quickly a config change takes effect (see [ClickStreamRenderer]).
         */
        const val CHUNK_SAMPLES = 480

        /**
         * How many chunks' worth of buffer depth to request beyond the platform minimum, giving
         * the generation loop a little slack against its own scheduling jitter (GC, dispatcher
         * contention) without needing any wall-clock catch-up logic — the same role a deep output
         * buffer plays for any continuous audio-generation loop.
         */
        const val BUFFER_DEPTH_CHUNKS = 4
    }
}
