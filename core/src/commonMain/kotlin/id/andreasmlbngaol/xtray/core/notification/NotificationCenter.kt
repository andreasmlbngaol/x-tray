package id.andreasmlbngaol.xtray.core.notification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A message the island shows for a few seconds. It can come from the IPC
 * `notify` command or from the desktop notification bus; both end up here so
 * there is exactly one place that decides how a notification looks.
 */
data class IslandNotification(
    val id: Long,
    val appName: String,
    val title: String,
    val body: String,
    val createdAtMs: Long,
    /** PNG bytes for the sender's app icon, when the desktop bus provided one. */
    val iconBytes: ByteArray? = null,
    /** Notification id on the desktop bus; used to activate the sender on click. */
    val sourceId: Long? = null,
) {
    /** Bold first line: the title, or the body when the sender had no title. */
    val headline: String get() = title.ifBlank { body }

    /** Second line; empty when [headline] already carries the whole message. */
    val detail: String get() = if (title.isBlank()) "" else body
}

/** Transient in-island messages pushed by IPC or by the notification daemon. */
class NotificationCenter(private val clock: () -> Long = { System.currentTimeMillis() }) {
    var current: IslandNotification? by mutableStateOf(null)
        private set

    private var nextId = 1L

    fun push(
        appName: String,
        title: String,
        body: String,
        iconBytes: ByteArray? = null,
        sourceId: Long? = null,
    ): IslandNotification {
        val notification = IslandNotification(
            id = nextId++,
            appName = appName.trim().ifBlank { "Notification" },
            title = title.trim(),
            body = body.trim(),
            createdAtMs = clock(),
            iconBytes = iconBytes,
            sourceId = sourceId,
        )
        current = notification
        return notification
    }

    /** Dismisses the card, or only the card with [id] when one is given. */
    fun dismiss(id: Long? = null) {
        if (id == null || current?.id == id) {
            current = null
        }
    }

    /** Dismisses the card only when it came from desktop notification [sourceId]. */
    fun dismissSource(sourceId: Long) {
        if (current?.sourceId == sourceId) {
            current = null
        }
    }
}
