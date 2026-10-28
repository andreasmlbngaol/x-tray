package id.andreasmlbngaol.xtray.core.time

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Counts up from zero. It lives in the services graph rather than in the tab,
 * so closing the panel does not throw away a running measurement.
 */
class Stopwatch(private val clock: () -> Long = { System.currentTimeMillis() }) {
    var startedAtMs: Long? by mutableStateOf(null)
        private set

    var accumulatedMs: Long by mutableLongStateOf(0L)
        private set

    val isRunning: Boolean get() = startedAtMs != null

    fun elapsedMs(nowMs: Long = clock()): Long =
        accumulatedMs + (startedAtMs?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L)

    fun start(nowMs: Long = clock()) {
        if (startedAtMs == null) startedAtMs = nowMs
    }

    fun pause(nowMs: Long = clock()) {
        val started = startedAtMs ?: return
        accumulatedMs += (nowMs - started).coerceAtLeast(0L)
        startedAtMs = null
    }

    fun toggle(nowMs: Long = clock()) {
        if (isRunning) pause(nowMs) else start(nowMs)
    }

    fun reset() {
        startedAtMs = null
        accumulatedMs = 0L
    }
}

/**
 * Plain countdown, separate from the focus session so the two can run
 * independently. [tick] is what the app layer calls to learn that it ended,
 * which is also how the completion notification fires while the panel is shut.
 */
class CountdownTimer(private val clock: () -> Long = { System.currentTimeMillis() }) {
    var durationMs: Long by mutableLongStateOf(0L)
        private set

    var endsAtMs: Long? by mutableStateOf(null)
        private set

    var pausedRemainingMs: Long by mutableLongStateOf(0L)
        private set

    val isRunning: Boolean get() = endsAtMs != null

    val isPaused: Boolean get() = endsAtMs == null && pausedRemainingMs > 0L

    val isFinished: Boolean
        get() = endsAtMs == null && durationMs > 0L && pausedRemainingMs == 0L

    fun start(durationMs: Long, nowMs: Long = clock()) {
        val total = durationMs.coerceAtLeast(0L)
        if (total <= 0L) return
        this.durationMs = total
        this.endsAtMs = nowMs + total
        this.pausedRemainingMs = total
    }

    fun pause(nowMs: Long = clock()) {
        val ends = endsAtMs ?: return
        pausedRemainingMs = (ends - nowMs).coerceAtLeast(0L)
        endsAtMs = null
    }

    fun resume(nowMs: Long = clock()) {
        if (endsAtMs != null || pausedRemainingMs <= 0L) return
        endsAtMs = nowMs + pausedRemainingMs
    }

    fun reset() {
        endsAtMs = null
        pausedRemainingMs = 0L
        durationMs = 0L
    }

    fun remainingMs(nowMs: Long = clock()): Long =
        endsAtMs?.let { (it - nowMs).coerceAtLeast(0L) } ?: pausedRemainingMs

    fun progress(nowMs: Long = clock()): Float {
        if (durationMs <= 0L) return 0f
        val elapsed = (durationMs - remainingMs(nowMs)).coerceIn(0L, durationMs)
        return elapsed.toFloat() / durationMs.toFloat()
    }

    /** Stops the countdown at zero; true exactly once per run. */
    fun tick(nowMs: Long = clock()): Boolean {
        val ends = endsAtMs ?: return false
        if (nowMs < ends) return false
        endsAtMs = null
        pausedRemainingMs = 0L
        return true
    }
}

object TimeFormat {
    /** 00:00 or 00:00:00, with the hour block only when it is needed. */
    fun elapsed(ms: Long): String {
        val totalSeconds = ms.coerceAtLeast(0L) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        val mm = minutes.toString().padStart(2, '0')
        val ss = seconds.toString().padStart(2, '0')
        return if (hours > 0L) hours.toString().padStart(2, '0') + ":" + mm + ":" + ss else mm + ":" + ss
    }
}
