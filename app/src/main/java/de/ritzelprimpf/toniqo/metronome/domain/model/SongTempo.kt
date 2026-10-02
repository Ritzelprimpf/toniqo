package de.ritzelprimpf.toniqo.metronome.domain.model

/**
 * A song found by a song-tempo search, carrying what the metronome can adopt from it.
 *
 * @property id The source database's stable identifier for the song (unique within one result list).
 * @property title The song title.
 * @property artist The performing artist's name, or `null` if the source didn't provide one.
 * @property bpm The song's tempo in beats per minute, always positive. Not clamped to the
 *   metronome's range — clamping happens when the tempo is applied.
 * @property timeSignature The song's time signature, or `null` if unknown or unparseable.
 */
data class SongTempo(
    val id: String,
    val title: String,
    val artist: String?,
    val bpm: Int,
    val timeSignature: SongTimeSignature?,
)
