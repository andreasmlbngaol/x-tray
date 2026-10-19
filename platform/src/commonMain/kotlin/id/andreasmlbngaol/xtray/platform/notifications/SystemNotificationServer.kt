package id.andreasmlbngaol.xtray.platform.notifications

/** One message as it arrives from the desktop notification bus. */
data class IncomingNotification(
    val appName: String,
    val title: String,
    val body: String,
    val appIcon: String = "",
)

/**
 * Hosts the desktop notification daemon so the island can be the only place
 * where notifications appear. Implementations must be idempotent: [start]
 * returns false when another daemon keeps the bus name.
 */
interface SystemNotificationServer {
    val available: Boolean
    val running: Boolean

    fun start(
        onNotification: (IncomingNotification) -> Unit,
        onClosed: () -> Unit,
    ): Boolean

    fun stop()
}
