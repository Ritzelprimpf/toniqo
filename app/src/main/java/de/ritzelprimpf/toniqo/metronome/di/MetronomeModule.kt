package de.ritzelprimpf.toniqo.metronome.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ritzelprimpf.toniqo.BuildConfig
import de.ritzelprimpf.toniqo.metronome.data.AudioTrackMetronomePlayer
import de.ritzelprimpf.toniqo.metronome.data.MetronomePreferences
import de.ritzelprimpf.toniqo.metronome.data.MetronomePreferencesImpl
import de.ritzelprimpf.toniqo.metronome.data.songsearch.GetSongBpmConfig
import de.ritzelprimpf.toniqo.metronome.data.songsearch.GetSongBpmSongTempoRepository
import de.ritzelprimpf.toniqo.metronome.data.songsearch.HttpTransport
import de.ritzelprimpf.toniqo.metronome.data.songsearch.UrlConnectionHttpTransport
import de.ritzelprimpf.toniqo.metronome.domain.repository.MetronomePlayer
import de.ritzelprimpf.toniqo.metronome.domain.repository.SongTempoRepository
import javax.inject.Singleton

/**
 * Hilt bindings for the Metronome feature.
 *
 * [MetronomePlayer] is bound to the `AudioTrack`-backed implementation introduced in Phase 6.2.
 * [MetronomePreferences] is bound to the DataStore-backed implementation (also Phase 6.2).
 * [SongTempoRepository] is bound to the GetSongBPM-backed implementation (song BPM search).
 *
 * Constructor-injected types need no explicit binding:
 * - [de.ritzelprimpf.toniqo.metronome.data.audio.ClickSynthesizer] — `@Inject constructor()`
 * - [de.ritzelprimpf.toniqo.metronome.data.TapTempoCalculator] — `@Inject constructor(Clock)`
 * - Use cases — `@Inject constructor(...)`
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MetronomeModule {

    /**
     * Binds [AudioTrackMetronomePlayer] as the singleton implementation of [MetronomePlayer].
     *
     * The player is stateless — it is a factory for `callbackFlow`-based playback sessions.
     * Binding it as a singleton avoids redundant construction of the synthesizer buffers on
     * each [MetronomePlayer.run] call.
     */
    @Binds
    @Singleton
    abstract fun bindMetronomePlayer(
        impl: AudioTrackMetronomePlayer,
    ): MetronomePlayer

    /**
     * Binds [MetronomePreferencesImpl] as the singleton implementation of [MetronomePreferences].
     *
     * Backed by a `metronome_preferences` DataStore file separate from the tuner's (per
     * `Phase6-Metronome-Decisions.md` Item 4).
     */
    @Binds
    @Singleton
    abstract fun bindMetronomePreferences(
        impl: MetronomePreferencesImpl,
    ): MetronomePreferences

    /**
     * Binds [GetSongBpmSongTempoRepository] as the singleton [SongTempoRepository]. Singleton so
     * its in-memory result cache survives the Metronome screen and saves rate-limited requests.
     */
    @Binds
    @Singleton
    abstract fun bindSongTempoRepository(
        impl: GetSongBpmSongTempoRepository,
    ): SongTempoRepository

    /** Binds the platform `HttpURLConnection` transport as the [HttpTransport]. */
    @Binds
    abstract fun bindHttpTransport(
        impl: UrlConnectionHttpTransport,
    ): HttpTransport

    companion object {
        /** GetSongBPM settings; the API key comes from `local.properties` via `BuildConfig`. */
        @Provides
        fun provideGetSongBpmConfig(): GetSongBpmConfig = GetSongBpmConfig(
            baseUrl = GetSongBpmConfig.BASE_URL,
            apiKey = BuildConfig.GETSONGBPM_API_KEY,
            resultLimit = GetSongBpmConfig.DEFAULT_RESULT_LIMIT,
        )
    }
}
