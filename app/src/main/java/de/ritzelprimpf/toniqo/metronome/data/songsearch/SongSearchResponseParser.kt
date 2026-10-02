package de.ritzelprimpf.toniqo.metronome.data.songsearch

import de.ritzelprimpf.toniqo.metronome.domain.model.SongTempo
import de.ritzelprimpf.toniqo.metronome.domain.model.SongTimeSignature
import javax.inject.Inject
import kotlin.math.roundToInt
import org.json.JSONException
import org.json.JSONObject

/**
 * Parses a GetSongBPM `/search/?type=song` response body into [SongTempo]s.
 *
 * Deliberately lenient per field, because the API's documented types don't match what it sends
 * (`tempo` is documented as an integer but returned as a string, `time_sig` is documented as an
 * integer but returned as `"4/4"` and flagged beta):
 * - `tempo` is accepted as a number or a numeric string and rounded; songs without a usable
 *   positive tempo are dropped, since they're useless to the metronome.
 * - `time_sig` is accepted only in `"n/d"` form with positive integers; anything else becomes `null`
 *   (the tempo is still usable on its own).
 * - A `search` value that isn't an array (the API answers "no result" with an object) yields an
 *   empty list.
 * - Duplicate song ids keep the first occurrence (result lists are keyed by id in the UI).
 */
class SongSearchResponseParser @Inject constructor() {

    /** @throws JSONException if [body] is not a JSON object at all. */
    fun parse(body: String): List<SongTempo> {
        val songs = JSONObject(body).optJSONArray(KEY_SEARCH) ?: return emptyList()
        return (0 until songs.length())
            .mapNotNull { index -> songs.optJSONObject(index)?.let(::parseSong) }
            .distinctBy { it.id }
    }

    private fun parseSong(json: JSONObject): SongTempo? {
        val id = json.stringOrNull(KEY_ID) ?: return null
        val title = json.stringOrNull(KEY_TITLE) ?: return null
        val bpm = parseTempo(json.opt(KEY_TEMPO)) ?: return null
        return SongTempo(
            id = id,
            title = title,
            artist = json.optJSONObject(KEY_ARTIST)?.stringOrNull(KEY_ARTIST_NAME),
            bpm = bpm,
            timeSignature = json.stringOrNull(KEY_TIME_SIGNATURE)?.let(::parseTimeSignature),
        )
    }

    private fun parseTempo(raw: Any?): Int? {
        val value = when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.trim().toDoubleOrNull()
            else -> null
        } ?: return null
        if (!value.isFinite()) return null
        return value.roundToInt().takeIf { it > 0 }
    }

    private fun parseTimeSignature(raw: String): SongTimeSignature? {
        val parts = raw.split(TIME_SIGNATURE_SEPARATOR)
        if (parts.size != TIME_SIGNATURE_PARTS) return null
        val numerator = parts[0].trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
        val denominator = parts[1].trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
        return SongTimeSignature(numerator = numerator, denominator = denominator)
    }

    /** Non-blank trimmed string value, treating JSON `null` (which `optString` renders as "null") as absent. */
    private fun JSONObject.stringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() }

    private companion object {
        const val KEY_SEARCH = "search"
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_TEMPO = "tempo"
        const val KEY_TIME_SIGNATURE = "time_sig"
        const val KEY_ARTIST = "artist"
        const val KEY_ARTIST_NAME = "name"
        const val TIME_SIGNATURE_SEPARATOR = "/"
        const val TIME_SIGNATURE_PARTS = 2
    }
}
