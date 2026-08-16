package de.ritzelprimpf.toniqo.chordfinder.data

import de.ritzelprimpf.toniqo.chordfinder.domain.model.Barre
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordToneRole
import de.ritzelprimpf.toniqo.chordfinder.domain.model.FretMark
import de.ritzelprimpf.toniqo.chordfinder.domain.model.SeventhQuality
import de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing
import de.ritzelprimpf.toniqo.chordfinder.domain.model.classifyToneRole
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.common.model.GuitarTuning
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses the curated `voicings_standard_6.json` asset into a map of [ChordKey] → [List<Voicing>].
 *
 * Uses Android's built-in `org.json` — no additional dependency required.
 *
 * Schema handled (see `assets/chordfinder/voicings_standard_6.json`):
 * ```json
 * { "tuningId": "standard_6", "version": 1,
 *   "chords": [
 *     { "rootPitchClass": 0, "quality": "MAJOR",
 *       "voicings": [
 *         { "frets": ["x",3,2,0,1,0], "fingers": [0,3,2,0,1,0], "barre": null }
 *       ] }
 *   ] }
 * ```
 *
 * `seventhQuality` is an optional field on each chord entry (omitted or JSON `null` for a plain
 * triad). When present, it names a [SeventhQuality] and the entry's voicings are validated
 * against the 4-tone chord (triad + seventh), keyed separately from the parent triad — see
 * `voicings_standard_6_seventh.json` / `voicings_drop_d_6_seventh.json`.
 *
 * `frets` values: `"x"` → Muted, `"o"` or `0` (integer zero) → Open, positive integer → Fretted.
 *
 * This is a pure stateless utility object — no I/O or Android context dependency.
 */
internal object VoicingJsonParser {

    private const val KEY_CHORDS = "chords"
    private const val KEY_ROOT_PC = "rootPitchClass"
    private const val KEY_QUALITY = "quality"
    private const val KEY_SEVENTH_QUALITY = "seventhQuality"
    private const val KEY_VOICINGS = "voicings"
    private const val KEY_FRETS = "frets"
    private const val KEY_FINGERS = "fingers"
    private const val KEY_BARRE = "barre"
    private const val KEY_BARRE_FRET = "fret"
    private const val KEY_BARRE_FROM = "from"
    private const val KEY_BARRE_TO = "to"

    private const val FRET_TOKEN_MUTED = "x"
    private const val FRET_TOKEN_OPEN = "o"
    private const val PITCH_CLASSES = 12

    /**
     * Parses [json] and returns all voicings keyed by [ChordKey].
     *
     * Voicings are ordered by ascending [Voicing.baseFret] within each key.
     * [rootStringIndices] is computed from [tuning] and the chord root — not stored in the JSON.
     *
     * @param json The full JSON string.
     * @param tuning The tuning the voicings are authored for (used to compute pitch classes).
     * @throws org.json.JSONException on malformed JSON.
     * @throws IllegalArgumentException if any voicing fails validation.
     */
    fun parse(json: String, tuning: GuitarTuning): Map<ChordKey, List<Voicing>> {
        val root = JSONObject(json)
        val chordsArray = root.getJSONArray(KEY_CHORDS)

        val openPcs = tuning.openNotes.map { it.name.semitonesFromC }

        val result = mutableMapOf<ChordKey, MutableList<Voicing>>()

        for (chordIdx in 0 until chordsArray.length()) {
            val chordObj = chordsArray.getJSONObject(chordIdx)
            val rootPc = chordObj.getInt(KEY_ROOT_PC)
            val quality = ChordQuality.valueOf(chordObj.getString(KEY_QUALITY))
            val seventhQuality = if (chordObj.isNull(KEY_SEVENTH_QUALITY) || !chordObj.has(KEY_SEVENTH_QUALITY)) {
                null
            } else {
                SeventhQuality.valueOf(chordObj.getString(KEY_SEVENTH_QUALITY))
            }
            val key = ChordKey(rootPc, quality, seventhQuality)

            val voicingsArray = chordObj.getJSONArray(KEY_VOICINGS)
            val voicingList = result.getOrPut(key) { mutableListOf() }

            for (vIdx in 0 until voicingsArray.length()) {
                val vObj = voicingsArray.getJSONObject(vIdx)
                val marks = parseFrets(vObj.getJSONArray(KEY_FRETS))
                val fingers = parseFingers(vObj.getJSONArray(KEY_FINGERS))
                val barre = if (vObj.isNull(KEY_BARRE)) null else parseBarre(vObj.getJSONObject(KEY_BARRE))

                // Compute rootStringIndices from tuning + chord root
                val rootStringIndices = computeRootStringIndices(marks, openPcs, rootPc)
                // bassDegree is derived from the shape itself, not assumed to be ROOT — the JSON
                // is free to contain inversions (bass = third or fifth), and Voicing.validated()
                // requires whatever's passed in to be the truth, not just a convenient default.
                val bassDegree = computeBassDegree(marks, openPcs, key)

                val voicing = Voicing.validated(
                    labelKey = voicingList.size + 1, // 1-based within this chord key
                    marks = marks,
                    fingers = fingers,
                    barre = barre,
                    rootStringIndices = rootStringIndices,
                    bassDegree = bassDegree,
                    chordKey = key,
                    openNotes = openPcs,
                )
                voicingList.add(voicing)
            }
        }

        // Sort each list by baseFret ascending
        return result.mapValues { (_, list) -> list.sortedBy { it.baseFret } }
    }

    private fun parseFrets(array: JSONArray): List<FretMark> =
        List(array.length()) { i ->
            when (val token = array.get(i)) {
                is String -> when (token.lowercase()) {
                    FRET_TOKEN_MUTED -> FretMark.Muted
                    FRET_TOKEN_OPEN -> FretMark.Open
                    else -> FretMark.Fretted(token.toInt())
                }
                is Int -> if (token == 0) FretMark.Open else FretMark.Fretted(token)
                else -> throw IllegalArgumentException("Unexpected fret token: $token")
            }
        }

    private fun parseFingers(array: JSONArray): List<Int> =
        List(array.length()) { i -> array.getInt(i) }

    private fun parseBarre(obj: JSONObject): Barre =
        Barre(
            fret = obj.getInt(KEY_BARRE_FRET),
            fromString = obj.getInt(KEY_BARRE_FROM),
            toString = obj.getInt(KEY_BARRE_TO),
        )

    private fun computeRootStringIndices(
        marks: List<FretMark>,
        openPcs: List<Int>,
        rootPc: Int,
    ): Set<Int> = marks.indices.filter { i ->
        when (val m = marks[i]) {
            is FretMark.Muted -> false
            is FretMark.Open -> openPcs[i] == rootPc
            is FretMark.Fretted -> (openPcs[i] + m.fret) % PITCH_CLASSES == rootPc
        }
    }.toSet()

    /**
     * Classifies the lowest-index sounded string's pitch class against [key]'s actual third/fifth
     * (by interval value, via [classifyToneRole] — not by array index, which breaks for a
     * 2-element quality like [ChordQuality.POWER] that has no third at all).
     */
    private fun computeBassDegree(
        marks: List<FretMark>,
        openPcs: List<Int>,
        key: ChordKey,
    ): ChordToneRole {
        val bassIndex = marks.indices.first { marks[it] != FretMark.Muted }
        val bassPc = when (val m = marks[bassIndex]) {
            is FretMark.Muted -> error("unreachable — filtered out by indexOfFirst above")
            is FretMark.Open -> openPcs[bassIndex]
            is FretMark.Fretted -> (openPcs[bassIndex] + m.fret) % PITCH_CLASSES
        }
        return key.classifyToneRole(bassPc)
    }
}
