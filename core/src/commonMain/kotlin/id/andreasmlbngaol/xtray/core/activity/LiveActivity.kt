package id.andreasmlbngaol.xtray.core.activity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ActivityKind { Focus, Break, Generic }

data class LiveActivity(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: ActivityKind,
    val startedAtMs: Long,
    val durationMs: Long,
)

/**
 * Drives the single live activity shown by the island. Time is injected so
 * countdown behaviour can be tested without sleeping.
 */
class LiveActivityController(private val clock: () -> Long) {
    var current: LiveActivity? by mutableStateOf(null)
        private set

    fun start(title: String, minutes: Int, kind: ActivityKind = ActivityKind.Focus): LiveActivity =
        startSeconds(title, minutes.coerceIn(1, MAX_DURATION_SECONDS / 60) * 60L, kind)

    fun startSeconds(
        title: String,
        seconds: Long,
        kind: ActivityKind = ActivityKind.Focus,
    ): LiveActivity {
        val duration = seconds.coerceIn(1L, MAX_DURATION_SECONDS.toLong()) * 1_000L
        val activity = LiveActivity(
            id = "activity-" + clock(),
            title = title.ifBlank { "Focus" },
            subtitle = if (kind == ActivityKind.Break) "Break" else "Focus session",
            kind = kind,
            startedAtMs = clock(),
            durationMs = duration,
        )
        current = activity
        return activity
    }

    fun stop() {
        current = null
    }

    fun remainingMs(): Long {
        val activity = current ?: return 0L
        return (activity.startedAtMs + activity.durationMs - clock()).coerceAtLeast(0L)
    }

    fun isFinished(): Boolean = current != null && remainingMs() == 0L

    fun progress(): Float {
        val activity = current ?: return 0f
        if (activity.durationMs <= 0L) return 0f
        val elapsed = (clock() - activity.startedAtMs).coerceIn(0L, activity.durationMs)
        return elapsed.toFloat() / activity.durationMs.toFloat()
    }

    companion object {
        /** 99:59:59 — long enough for any focus session without overflow. */
        const val MAX_DURATION_SECONDS: Int = 99 * 3_600 + 59 * 60 + 59

        fun format(ms: Long): String {
            val totalSeconds = (ms.coerceAtLeast(0L) + 999L) / 1_000L
            val hours = totalSeconds / 3_600L
            val minutes = (totalSeconds % 3_600L) / 60L
            val seconds = totalSeconds % 60L
            val mm = minutes.toString().padStart(2, '0')
            val ss = seconds.toString().padStart(2, '0')
            return if (hours > 0L) hours.toString().padStart(2, '0') + ":" + mm + ":" + ss else mm + ":" + ss
        }
    }
}
