package id.andreasmlbngaol.xtray.platform.notifications

/**
 * Windows has no replaceable notification daemon. Toasts are owned by the
 * shell, so the island keeps its own notifications (focus timer, `x-tray
 * notify`) without taking the system ones over.
 */
class WindowsNotificationServer : SystemNotificationServer {
    override val available: Boolean get() = false
    override val running: Boolean get() = false

    override fun start(
        onNotification: (IncomingNotification) -> Unit,
        onClosed: () -> Unit,
    ): Boolean = false

    override fun stop() = Unit
}
