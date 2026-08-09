package de.ritzelprimpf.toniqo.chordfinder.data

import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.common.model.GuitarTuning
import org.junit.Assert.assertEquals
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
}
