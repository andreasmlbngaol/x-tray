package id.andreasmlbngaol.xtray.platform.media

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Smoke test against the live session bus. It passes when no player is
 * running and validates the track shape when one is.
 */
class LiveMediaProbeTest {
    @Test
    fun readsTheCurrentPlayerIfAny() {
        val track = BusctlMediaSource().snapshot()
        println("LIVE MEDIA: " + track)
        if (track != null) {
            assertTrue(track.title.isNotBlank())
            assertTrue(track.busName.startsWith("org.mpris.MediaPlayer2."))
        }
    }
}
