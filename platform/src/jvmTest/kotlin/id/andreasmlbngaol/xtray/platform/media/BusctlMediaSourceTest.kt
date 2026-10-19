package id.andreasmlbngaol.xtray.platform.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BusctlMediaSourceTest {
    private val getAll = """{"type":"a{sv}","data":[{"Metadata":{"type":"a{sv}","data":{"xesam:title":{"type":"s","data":"Midnight City"},"xesam:artist":{"type":"as","data":["M83","Guest"]},"xesam:album":{"type":"s","data":"Hurry Up"},"mpris:length":{"type":"x","data":243000000},"mpris:artUrl":{"type":"s","data":"file:///tmp/a.png"}}},"Position":{"type":"x","data":42000000},"PlaybackStatus":{"type":"s","data":"Playing"}}]}"""

    @Test
    fun parsesMetadataAndProgress() {
        val track = parseMediaTrack(getAll, "Vlc")!!
        assertEquals("Midnight City", track.title)
        assertEquals("M83, Guest", track.artist)
        assertEquals("Hurry Up", track.album)
        assertEquals("file:///tmp/a.png", track.artUrl)
        assertTrue(track.playing)
        assertEquals(42_000L, track.positionMs)
        assertEquals(243_000L, track.lengthMs)
        assertEquals(0.172f, track.progress, 0.01f)
    }

    @Test
    fun ignoresPlayersWithoutTitle() {
        val empty = """{"type":"a{sv}","data":[{"PlaybackStatus":{"type":"s","data":"Playing"}}]}"""
        assertNull(parseMediaTrack(empty, "Vlc"))
        assertNull(parseMediaTrack("not json", "Vlc"))
    }

    @Test
    fun filtersNonMprisNames() {
        val list = """[{"name":"org.mpris.MediaPlayer2.vlc"},{"name":"org.freedesktop.Notifications"},{"name":"org.mpris.MediaPlayer2.playerctld"}]"""
        assertEquals(listOf("org.mpris.MediaPlayer2.vlc"), parsePlayerNames(list))
        assertTrue(parsePlayerNames("broken").isEmpty())
    }

    @Test
    fun pausedTrackIsNotPlaying() {
        val paused = getAll.replace("\"Playing\"", "\"Paused\"")
        assertFalse(parseMediaTrack(paused, "Vlc")!!.playing)
    }
}
