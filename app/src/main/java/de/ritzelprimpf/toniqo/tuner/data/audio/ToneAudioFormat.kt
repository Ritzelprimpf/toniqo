package de.ritzelprimpf.toniqo.tuner.data.audio

import android.media.AudioFormat

/**
 * Audio-format constants shared between [ToneSynthesizer] and
 * [de.ritzelprimpf.toniqo.tuner.data.AudioTrackTonePlayer].
 *
 * 44.1 kHz mono 16-bit PCM — matches the sample rate the tuner's own capture pipeline already
 * uses ([de.ritzelprimpf.toniqo.tuner.domain.usecase.DetectTunedStringUseCase]), so this module
 * doesn't introduce a second distinct sample rate for no reason. Playback and capture are
 * otherwise independent; nothing requires them to match.
 */
internal object ToneAudioFormat {
    /** Output sample rate in Hz. */
    const val SAMPLE_RATE_HZ = 44_100

    /** Equals [AudioFormat.CHANNEL_OUT_MONO]. Mono is correct for a single reference tone. */
    val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO

    /** Equals [AudioFormat.ENCODING_PCM_16BIT]. */
    val ENCODING = AudioFormat.ENCODING_PCM_16BIT

    /** Bytes per sample for 16-bit PCM. Used for buffer-size calculations in the player. */
    const val BYTES_PER_SAMPLE = 2
}
