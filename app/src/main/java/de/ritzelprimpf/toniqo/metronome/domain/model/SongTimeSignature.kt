package de.ritzelprimpf.toniqo.metronome.domain.model

/**
 * A song's published time signature, as reported by an external song database.
 *
 * Deliberately not validated against the metronome's supported range — the source data is
 * third-party (and, for GetSongBPM, explicitly "beta"), so whether a given signature can actually
 * be applied is decided where it is applied, via
 * [MetronomeConfig.isSupportedTimeSignature].
 *
 * @property numerator Beats per bar (top number).
 * @property denominator Note value of one beat (bottom number).
 */
data class SongTimeSignature(
    val numerator: Int,
    val denominator: Int,
)
