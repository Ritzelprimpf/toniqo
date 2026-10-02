package de.ritzelprimpf.toniqo.metronome.domain.model

/**
 * What to search for in a song-tempo search.
 *
 * @property title The song title — always required; a title-only search is the default.
 * @property artist Optional artist name that narrows the results, or `null` to search by title only.
 */
data class SongSearchQuery(
    val title: String,
    val artist: String? = null,
)
