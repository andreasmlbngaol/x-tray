package id.andreasmlbngaol.xtray.core.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeToolsTest {
    @Test
    fun stopwatchAccumulatesAcrossPauses() {
        val watch = Stopwatch(clock = { 0L })
        watch.start(1_000)
        assertEquals(4_000L, watch.elapsedMs(5_000))

        watch.pause(5_000)
        assertEquals(4_000L, watch.elapsedMs(60_000))

        watch.start(60_000)
        assertEquals(5_000L, watch.elapsedMs(61_000))

        watch.reset()
        assertEquals(0L, watch.elapsedMs(61_000))
    }

    @Test
    fun countdownFinishesExactlyOnce() {
        val timer = CountdownTimer(clock = { 0L })
        timer.start(10_000L, nowMs = 1_000)
        assertEquals(6_000L, timer.remainingMs(5_000))

        assertFalse(timer.tick(10_000))
        assertTrue(timer.tick(11_000))
        assertFalse(timer.tick(12_000))
        assertTrue(timer.isFinished)
        assertEquals(0L, timer.remainingMs(12_000))
    }

    @Test
    fun countdownPauseAndResume() {
        val timer = CountdownTimer(clock = { 0L })
        timer.start(10_000L, nowMs = 0)
        timer.pause(4_000)
        assertTrue(timer.isPaused)
        assertEquals(6_000L, timer.remainingMs(20_000))

        timer.resume(20_000)
        assertFalse(timer.isPaused)
        assertEquals(6_000L, timer.remainingMs(20_000))
        assertEquals(1_000L, timer.remainingMs(25_000))
    }

    @Test
    fun elapsedFormatAddsHoursOnlyWhenNeeded() {
        assertEquals("00:05", TimeFormat.elapsed(5_000))
        assertEquals("01:05", TimeFormat.elapsed(65_000))
        assertEquals("01:00:00", TimeFormat.elapsed(3_600_000))
    }
}
