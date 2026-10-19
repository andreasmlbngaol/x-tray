package id.andreasmlbngaol.xtray.platform.notifications

import id.andreasmlbngaol.xtray.platform.internal.Log
import id.andreasmlbngaol.xtray.platform.internal.Shell
import id.andreasmlbngaol.xtray.core.AppInfo
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.exceptions.DBusException
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.DBusSerializable
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant
import java.io.File

/**
 * Implements `org.freedesktop.Notifications` on the session bus.
 *
 * GNOME keeps that name on a small helper process
 * (`/usr/share/gnome-shell/org.gnome.Shell.Notifications`) which does not
 * allow replacement. Taking the name therefore means stopping that helper
 * first; on [stop] the helper is started again by pinging its own bus name,
 * which is exactly how D-Bus activation would start it.
 */
class DbusNotificationServer : SystemNotificationServer {
    override val available: Boolean = true
    override val running: Boolean get() = connection != null

    private var connection: DBusConnection? = null
    private var stoppedGnomeHelper = false
    private val shutdownHook = Thread { stop() }

    override fun start(
        onNotification: (IncomingNotification) -> Unit,
        onClosed: () -> Unit,
    ): Boolean {
        if (connection != null) return true
        val conn = try {
            DBusConnectionBuilder.forSessionBus().build()
        } catch (error: Throwable) {
            Log.w("notifications: cannot open the session bus", error)
            return false
        }
        val exported = NotificationsObject(conn, onNotification, onClosed)
        try {
            conn.exportObject(OBJECT_PATH, exported)
        } catch (error: Throwable) {
            Log.w("notifications: cannot export " + OBJECT_PATH, error)
            runCatching { conn.close() }
            return false
        }
        if (!acquireName(conn)) {
            runCatching { conn.close() }
            return false
        }
        connection = conn
        runCatching { Runtime.getRuntime().addShutdownHook(shutdownHook) }
        Log.i("notifications: serving " + BUS_NAME)
        return true
    }

    override fun stop() {
        val conn = connection ?: return
        connection = null
        runCatching { conn.releaseBusName(BUS_NAME) }
        runCatching { conn.close() }
        runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
        if (stoppedGnomeHelper) {
            stoppedGnomeHelper = false
            restoreGnomeHelper()
        }
        Log.i("notifications: released " + BUS_NAME)
    }

    private fun acquireName(conn: DBusConnection): Boolean {
        if (request(conn)) return true
        val pid = ownerPid()
        val command = pid?.let { commandLine(it) }.orEmpty()
        if (pid == null || !command.contains(GNOME_HELPER)) {
            Log.w("notifications: keeping the existing daemon (" + command.ifBlank { "unknown" } + ")")
            return false
        }
        Log.i("notifications: stopping the GNOME helper (pid " + pid + ")")
        ProcessHandle.of(pid).ifPresent { it.destroy() }
        stoppedGnomeHelper = true
        repeat(30) {
            Thread.sleep(100L)
            if (request(conn)) return true
        }
        Log.w("notifications: the GNOME helper did not release " + BUS_NAME)
        return false
    }

    private fun request(conn: DBusConnection): Boolean = try {
        conn.requestBusName(BUS_NAME)
        true
    } catch (_: DBusException) {
        false
    }

    private fun ownerPid(): Long? {
        val output = Shell.run(
            "busctl", "--user", "call",
            "org.freedesktop.DBus", "/org/freedesktop/DBus",
            "org.freedesktop.DBus", "GetConnectionUnixProcessID", "s", BUS_NAME,
        ) ?: return null
        return Regex("u\\s+(\\d+)").find(output)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun commandLine(pid: Long): String = runCatching {
        File("/proc/" + pid + "/cmdline").readText().replace('\u0000', ' ')
    }.getOrDefault("")

    /**
     * Poking the helper's own name starts it again through D-Bus activation;
     * the fresh process then re-requests `org.freedesktop.Notifications`.
     */
    private fun restoreGnomeHelper() {
        val reply = Shell.run(
            "busctl", "--user", "call",
            "org.gnome.Shell.Notifications", "/org/gnome/Shell/Notifications",
            "org.freedesktop.DBus.Peer", "Ping",
        )
        Log.i("notifications: GNOME helper restart " + if (reply == null) "failed" else "ok")
    }

    private companion object {
        const val BUS_NAME = "org.freedesktop.Notifications"
        const val OBJECT_PATH = "/org/freedesktop/Notifications"
        const val GNOME_HELPER = "org.gnome.Shell.Notifications"
    }
}

/** The four values `GetServerInformation` returns as separate out arguments. */
class ServerInformation(
    private val name: String = "",
    private val vendor: String = "",
    private val version: String = "",
    private val specVersion: String = "",
) : DBusSerializable {
    override fun serialize(): Array<Any> = arrayOf(name, vendor, version, specVersion)

    companion object {
        @JvmStatic
        fun deserialize(name: String, vendor: String, version: String, specVersion: String): ServerInformation =
            ServerInformation(name, vendor, version, specVersion)
    }
}

@DBusInterfaceName("org.freedesktop.Notifications")
@Suppress("FunctionName", "ParameterName")
interface NotificationsBus : DBusInterface {
    fun Notify(
        appName: String,
        replacesId: UInt32,
        appIcon: String,
        summary: String,
        body: String,
        actions: List<String>,
        hints: Map<String, Variant<*>>,
        expireTimeout: Int,
    ): UInt32

    fun CloseNotification(id: UInt32)

    fun GetCapabilities(): List<String>

    fun GetServerInformation(): ServerInformation

    class NotificationClosed(path: String, id: UInt32, reason: UInt32) : DBusSignal(path, id, reason)

    class ActionInvoked(path: String, id: UInt32, action: String) : DBusSignal(path, id, action)
}

private class NotificationsObject(
    private val connection: DBusConnection,
    private val onNotification: (IncomingNotification) -> Unit,
    private val onClosed: () -> Unit,
) : NotificationsBus {
    private var nextId = 1L

    override fun getObjectPath(): String = "/org/freedesktop/Notifications"

    override fun Notify(
        appName: String,
        replacesId: UInt32,
        appIcon: String,
        summary: String,
        body: String,
        actions: List<String>,
        hints: Map<String, Variant<*>>,
        expireTimeout: Int,
    ): UInt32 {
        val id = nextId++
        if (summary.isNotBlank() || body.isNotBlank()) {
            onNotification(
                IncomingNotification(
                    appName = appName.ifBlank { "Notification" },
                    title = summary,
                    body = body,
                    appIcon = appIcon,
                ),
            )
        }
        return UInt32(id)
    }

    override fun CloseNotification(id: UInt32) {
        onClosed()
        emitClosed(id)
    }

    override fun GetCapabilities(): List<String> = listOf("body", "body-markup")

    override fun GetServerInformation(): ServerInformation =
        ServerInformation(AppInfo.NAME, AppInfo.NAME, AppInfo.VERSION, "1.2")

    private fun emitClosed(id: UInt32) {
        runCatching {
            connection.sendMessage(
                NotificationsBus.NotificationClosed("/org/freedesktop/Notifications", id, UInt32(2)),
            )
        }
    }
}
