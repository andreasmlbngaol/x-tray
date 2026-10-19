package id.andreasmlbngaol.islanddesk.core.activity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiveActivityTest {
    @Test
    fun countsDownWithInjectedClock() {
        var now = 1_000L
        val controller = LiveActivityController { now }
        controller.start("Write", minutes = 1)
        assertEquals(60_000L, controller.remainingMs())
        assertEquals(0f, controller.progress())

        now += 30_000L
        assertEquals(30_000L, controller.remainingMs())
        assertEquals(0.5f, controller.progress())

        now += 30_000L
        assertEquals(0L, controller.remainingMs())
        assertTrue(controller.isFinished())

        controller.stop()
        assertNull(controller.current)
        assertFalse(controller.isFinished())
    }

    @Test
    fun acceptsCustomHoursMinutesSeconds() {
        var now = 0L
        val controller = LiveActivityController { now }
        controller.startSeconds("Deep work", 1 * 3_600L + 2 * 60L + 3L)
        assertEquals(3_723_000L, controller.current!!.durationMs)
        assertEquals("01:02:03", LiveActivityController.format(controller.remainingMs()))
    }

    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("25:00", LiveActivityController.format(25 * 60_000L))
        assertEquals("00:01", LiveActivityController.format(1L))
        assertEquals("00:00", LiveActivityController.format(-5L))
        assertEquals("01:00:00", LiveActivityController.format(3_600_000L))
    }
}
