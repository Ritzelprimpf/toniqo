package de.ritzelprimpf.toniqo.metronome.data

import de.ritzelprimpf.toniqo.metronome.data.songsearch.SongSearchResponseParser
import de.ritzelprimpf.toniqo.metronome.domain.model.SongTempo
import de.ritzelprimpf.toniqo.metronome.domain.model.SongTimeSignature
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SongSearchResponseParserTest {

    private val parser = SongSearchResponseParser()

    private fun song(
        id: String = "o2r0L",
        title: String = "\"Master of Puppets\"",
        tempo: String = "\"220\"",
        timeSig: String = "\"4/4\"",
        artist: String = """{"id":"nZR","name":"Metallica"}""",
    ) = """{"id":"$id","title":$title,"tempo":$tempo,"time_sig":$timeSig,"artist":$artist}"""

    private fun response(vararg songs: String) = """{"search":[${songs.joinToString(",")}]}"""

    @Test
    fun `parses the documented song example`() {
        val body = """
            {"search":[{
              "id":"o2r0L","title":"Master of Puppets",
              "uri":"https://getsongbpm.com/song/master-of-puppets/o2r0L",
              "tempo":"220","time_sig":"4/4","key_of":"Em","open_key":"2m",
              "danceability":55,"acousticness":0,
              "artist":{"id":"nZR","name":"Metallica","uri":"https://getsongbpm.com/artist/metallica/nZR",
                        "genres":["heavy metal","rock"],"from":"US","mbid":"65f4f0c5"},
              "album":{"title":"Master of Puppets","uri":"x","year":1986}
            }]}
        """.trimIndent()

        assertEquals(
            listOf(SongTempo("o2r0L", "Master of Puppets", "Metallica", 220, SongTimeSignature(4, 4))),
            parser.parse(body),
        )
    }

    @Test
    fun `accepts tempo sent as a JSON number`() {
        assertEquals(128, parser.parse(response(song(tempo = "128"))).single().bpm)
    }

    @Test
    fun `rounds a fractional tempo to the nearest integer`() {
        assertEquals(121, parser.parse(response(song(tempo = "\"120.6\""))).single().bpm)
    }

    @Test
    fun `drops songs whose tempo is missing, empty, zero, or not numeric`() {
        val body = response(
            song(id = "a", tempo = "null"),
            song(id = "b", tempo = "\"\""),
            song(id = "c", tempo = "\"0\""),
            song(id = "d", tempo = "\"fast\""),
            song(id = "e", tempo = "\"90\""),
        )

        assertEquals(listOf("e"), parser.parse(body).map { it.id })
    }

    @Test
    fun `drops songs without an id or title`() {
        val body = response(song(id = ""), song(id = "ok", title = "null"), song(id = "keep"))

        assertEquals(listOf("keep"), parser.parse(body).map { it.id })
    }

    @Test
    fun `parses a non-quarter time signature with surrounding spaces`() {
        assertEquals(SongTimeSignature(7, 8), parser.parse(response(song(timeSig = "\" 7 / 8 \""))).single().timeSignature)
    }

    @Test
    fun `leaves time signature null when absent or malformed but keeps the song`() {
        val body = response(
            song(id = "a", timeSig = "null"),
            song(id = "b", timeSig = "\"4\""),
            song(id = "c", timeSig = "\"4/0\""),
            song(id = "d", timeSig = "\"x/4\""),
            song(id = "e", timeSig = "\"3/4/4\""),
        )

        val songs = parser.parse(body)

        assertEquals(5, songs.size)
        assertTrue(songs.all { it.timeSignature == null })
    }

    @Test
    fun `leaves artist null when the artist object or its name is missing`() {
        val body = response(song(id = "a", artist = "null"), song(id = "b", artist = """{"id":"x"}"""))

        assertTrue(parser.parse(body).all { it.artist == null })
    }

    @Test
    fun `keeps the first occurrence of a duplicate song id`() {
        val body = response(song(id = "dup", tempo = "\"100\""), song(id = "dup", tempo = "\"200\""))

        assertEquals(100, parser.parse(body).single().bpm)
    }

    @Test
    fun `returns empty list when search is an error object instead of an array`() {
        assertTrue(parser.parse("""{"search":{"error":"no result"}}""").isEmpty())
    }

    @Test
    fun `returns empty list when the search key is missing`() {
        assertTrue(parser.parse("""{}""").isEmpty())
    }

    @Test
    fun `skips array entries that are not objects`() {
        assertEquals(1, parser.parse("""{"search":[42,"x",null,${song()}]}""").size)
    }

    @Test(expected = JSONException::class)
    fun `throws when the body is not a JSON object`() {
        parser.parse("<html>Bad gateway</html>")
    }

    @Test
    fun `trims title and artist whitespace`() {
        val parsed = parser.parse(response(song(title = "\"  One  \"", artist = """{"name":"  Metallica "}"""))).single()

        assertEquals("One", parsed.title)
        assertEquals("Metallica", parsed.artist)
        assertNull(parser.parse(response(song(artist = """{"name":"   "}"""))).single().artist)
    }
}
