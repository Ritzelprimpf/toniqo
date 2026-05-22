package de.ritzelprimpf.toniqo.metronome.data.audio

import android.media.AudioFormat

/**
 * Audio-format constants shared between [ClickSynthesizer] and the [AudioTrackMetronomePlayer]
 * (Phase 6.2). All values target 48 kHz mono 16-bit PCM, which matches the native output format
 * of essentially every modern Android device and avoids OS-level resampling.
 *
 * See `docs/phases/phase-6-metronome/Phase6-Metronome-Decisions.md` Item 15 for the full
 * rationale behind these choices.
 */
internal object MetronomeAudioFormat {
    /** Output sample rate in Hz. Matches the native rate of modern Android audio hardware. */
    const val SAMPLE_RATE_HZ = 48_000

    /**
     * Channel configuration for [android.media.AudioTrack].
     * Equals [AudioFormat.CHANNEL_OUT_MONO] (4). Mono is correct for a point-source click sound.
     */
    val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO

    /**
     * PCM encoding for [android.media.AudioTrack].
     * Equals [AudioFormat.ENCODING_PCM_16BIT] (2). 16-bit provides ~96 dB of dynamic range —
     * far more than needed for a metronome click.
     */
    val ENCODING = AudioFormat.ENCODING_PCM_16BIT

    /** Bytes per sample for 16-bit PCM. Used for buffer-size calculations in the player. */
    const val BYTES_PER_SAMPLE = 2
}
