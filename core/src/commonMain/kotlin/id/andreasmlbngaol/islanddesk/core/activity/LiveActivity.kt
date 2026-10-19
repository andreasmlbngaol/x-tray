package id.andreasmlbngaol.islanddesk.core.activity

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
    var current: LiveActivity? = null
        private set

    fun start(title: String, minutes: Int, kind: ActivityKind = ActivityKind.Focus): LiveActivity {
        val duration = minutes.coerceIn(1, 180) * 60_000L
        val activity = LiveActivity(
            id = "activity-" + clock(),
            title = title.ifBlank { "Fokus" },
            subtitle = if (kind == ActivityKind.Break) "Istirahat" else "Sesi fokus",
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
        fun format(ms: Long): String {
            val totalSeconds = (ms.coerceAtLeast(0L) + 999L) / 1_000L
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
        }
    }
}
