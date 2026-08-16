package de.ritzelprimpf.toniqo.chordfinder.data

import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordToneRole
import de.ritzelprimpf.toniqo.chordfinder.domain.model.SeventhQuality
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.common.model.GuitarTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression coverage for [VoicingJsonParser] against schema-legal shapes that were absent from
 * the original curated library (see [de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing]'s
 * `MAX_FRET` and [VoicingLibraryValidationTest] for the corresponding invariant coverage).
 */
class VoicingJsonParserTest {

    private val tuning = GuitarTuning.STANDARD_6

    @Test
    fun `a chord may carry more than six voicings`() {
        val voicingJson = """{ "frets": ["x",3,2,0,1,0], "fingers": [0,3,2,0,1,0], "barre": null }"""
        val json = """
            {
              "tuningId": "standard_6",
              "version": 1,
              "chords": [
                {
                  "rootPitchClass": 0,
                  "quality": "MAJOR",
                  "voicings": [${List(10) { voicingJson }.joinToString(",")}]
                }
              ]
            }
        """.trimIndent()

        val result = VoicingJsonParser.parse(json, tuning)
        val voicings = result.getValue(ChordKey(0, ChordQuality.MAJOR))

        assertEquals(10, voicings.size)
        assertEquals((1..10).toList(), voicings.map { it.labelKey }.sorted())
    }

    // ── seventhQuality: optional field, distinct key from the parent triad ─────────

    @Test
    fun `a chord entry without seventhQuality parses to a plain-triad key`() {
        val json = """
            {
              "tuningId": "standard_6",
              "version": 1,
              "chords": [
                {
                  "rootPitchClass": 0,
                  "quality": "MAJOR",
                  "voicings": [
                    { "frets": ["x",3,2,0,1,0], "fingers": [0,3,2,0,1,0], "barre": null }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = VoicingJsonParser.parse(json, tuning)

        assertNull(result.keys.single().seventhQuality)
    }

    @Test
    fun `a chord entry with seventhQuality parses to a distinct key from the plain triad`() {
        val json = """
            {
              "tuningId": "standard_6",
              "version": 1,
              "chords": [
                {
                  "rootPitchClass": 0,
                  "quality": "MAJOR",
                  "voicings": [
                    { "frets": ["x",3,2,0,1,0], "fingers": [0,3,2,0,1,0], "barre": null }
                  ]
                },
                {
                  "rootPitchClass": 0,
                  "quality": "MAJOR",
                  "seventhQuality": "MAJOR_SEVENTH",
                  "voicings": [
                    { "frets": ["x",3,2,0,0,0], "fingers": [0,3,2,0,0,0], "barre": null }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = VoicingJsonParser.parse(json, tuning)
        val triadKey = ChordKey(0, ChordQuality.MAJOR)
        val seventhKey = ChordKey(0, ChordQuality.MAJOR, SeventhQuality.MAJOR_SEVENTH)

        assertEquals(2, result.size)
        assertEquals(1, result.getValue(triadKey).size)
        assertEquals(1, result.getValue(seventhKey).size)
        assertEquals(ChordToneRole.ROOT, result.getValue(seventhKey).single().bassDegree)
    }
}
