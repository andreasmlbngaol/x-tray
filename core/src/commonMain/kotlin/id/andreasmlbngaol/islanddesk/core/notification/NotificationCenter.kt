package id.andreasmlbngaol.islanddesk.core.notification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class IslandNotification(
    val id: Long,
    val message: String,
    val createdAtMs: Long,
)

/** Transient in-island messages pushed by the IPC `notify` command. */
class NotificationCenter(private val clock: () -> Long = { System.currentTimeMillis() }) {
    var current: IslandNotification? by mutableStateOf(null)
        private set

    fun push(message: String): IslandNotification {
        val notification = IslandNotification(
            id = clock(),
            message = message.trim(),
            createdAtMs = clock(),
        )
        current = notification
        return notification
    }

    fun dismiss() {
        current = null
    }
}
