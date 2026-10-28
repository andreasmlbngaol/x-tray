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

/** Every notification from one application, newest first. */
data class NotificationGroup(
    val appName: String,
    val notifications: List<IslandNotification>,
) {
    val latest: IslandNotification get() = notifications.first()
}

/**
 * Transient in-island messages pushed by IPC or by the notification daemon,
 * plus the history the Notifications tab groups by sender.
 */
class NotificationCenter(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val maxHistory: Int = 100,
    /** Called for every accepted message; the app layer uses it for the chime. */
    private val onPush: (IslandNotification) -> Unit = {},
) {
    var current: IslandNotification? by mutableStateOf(null)
        private set

    /** Everything received this session, newest first. Dismissing the card keeps it. */
    var history: List<IslandNotification> by mutableStateOf(emptyList())
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
        history = (listOf(notification) + history).take(maxHistory.coerceAtLeast(1))
        onPush(notification)
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

    /** Drops a single message; the on-screen card goes too when it is that one. */
    fun remove(id: Long) {
        history = history.filterNot { it.id == id }
        if (current?.id == id) current = null
    }

    /** Drops every message from [appName]; the card goes too when it is one of them. */
    fun removeGroup(appName: String) {
        history = history.filterNot { it.appName == appName }
        if (current?.appName == appName) current = null
    }

    /** Drops the whole history and the card that is on screen. */
    fun clearAll() {
        current = null
        history = emptyList()
    }

    /** History bucketed per sender, most recently active sender first. */
    fun grouped(): List<NotificationGroup> =
        history
            .groupBy { it.appName }
            .map { (appName, items) -> NotificationGroup(appName, items) }
            .sortedByDescending { it.latest.createdAtMs }
}
