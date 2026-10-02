package de.ritzelprimpf.toniqo.metronome.data.songsearch

/**
 * Connection settings for the GetSongBPM API.
 *
 * Provided by Hilt from `BuildConfig` in production (see `MetronomeModule`); constructed directly
 * in tests.
 *
 * @property baseUrl API root, ending in `/`.
 * @property apiKey The API key from `local.properties` (`GETSONGBPM_API_KEY`). Blank when the
 *   build had no key — searches then fail as [de.ritzelprimpf.toniqo.metronome.domain.model.SongSearchFailure.SERVICE_UNAVAILABLE]
 *   without sending a request.
 * @property resultLimit Maximum number of songs requested per search.
 */
data class GetSongBpmConfig(
    val baseUrl: String,
    val apiKey: String,
    val resultLimit: Int,
) {
    companion object {
        const val BASE_URL: String = "https://api.getsong.co/"
        const val DEFAULT_RESULT_LIMIT: Int = 25
    }
}
