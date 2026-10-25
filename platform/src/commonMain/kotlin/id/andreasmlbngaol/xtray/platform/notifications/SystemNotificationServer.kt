package id.andreasmlbngaol.xtray.platform.notifications

/** One message as it arrives from the desktop notification bus. */
data class IncomingNotification(
    val appName: String,
    val title: String,
    val body: String,
    val appIcon: String = "",
    /** PNG bytes resolved from the sender's icon hints or desktop entry. */
    val iconBytes: ByteArray? = null,
    /** Id the sender used on the bus, so a click can activate the right app. */
    val notificationId: Long = 0L,
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
        onClosed: (Long) -> Unit,
    ): Boolean

    fun stop()

    /**
     * Opens the application that sent notification [id]: first through its
     * "default" action, otherwise by launching its desktop entry. Returns
     * false when no activation route is known.
     */
    fun activate(id: Long): Boolean = false
}
