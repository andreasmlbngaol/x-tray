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
    private var notificationsObject: NotificationsObject? = null
    private var gtkMonitor: GtkNotificationMonitor? = null
    private var gtkNotifications: GtkNotificationsBus? = null
    private var stoppedGnomeHelper = false
    private val shutdownHook = Thread { stop() }

    override fun start(
        onNotification: (IncomingNotification) -> Unit,
        onClosed: (Long) -> Unit,
    ): Boolean {
        if (connection != null) return true
        val conn = try {
            DBusConnectionBuilder.forSessionBus().build()
        } catch (error: Throwable) {
            Log.w("notifications: cannot open the session bus", error)
            return false
        }
        val exported = NotificationsObject(conn, onNotification, onClosed)
        notificationsObject = exported
        try {
            conn.exportObject(OBJECT_PATH, exported)
        } catch (error: Throwable) {
            Log.w("notifications: cannot export " + OBJECT_PATH, error)
            notificationsObject = null
            runCatching { conn.close() }
            return false
        }
        if (!acquireName(conn)) {
            notificationsObject = null
            runCatching { conn.close() }
            return false
        }
        connection = conn
        val gtk = runCatching {
            conn.getRemoteObject(GTK_BUS_NAME, GTK_OBJECT_PATH, GtkNotificationsBus::class.java)
        }.onFailure {
            Log.w("notifications: cannot open " + GTK_BUS_NAME, it)
        }.getOrNull()
        gtkNotifications = gtk
        gtkMonitor = GtkNotificationMonitor(
            onNotification = { incoming ->
                // GIO payloads carry no numeric id, so give each one a private
                // negative id and remember how to open its app on click.
                val id = exported.registerExternal(incoming.appName, incoming.appIcon)
                onNotification(incoming.copy(notificationId = id))
            },
            onRemove = { appId, id ->
                runCatching { gtk?.RemoveNotification(appId, id) }
                    .onFailure { Log.w("notifications: cannot dismiss GTK notification", it) }
            },
        ).also { it.start() }
        runCatching { Runtime.getRuntime().addShutdownHook(shutdownHook) }
        Log.i("notifications: serving " + BUS_NAME)
        return true
    }

    override fun activate(id: Long): Boolean = notificationsObject?.activate(id) == true

    override fun stop() {
        val conn = connection ?: return
        gtkMonitor?.stop()
        gtkMonitor = null
        gtkNotifications = null
        connection = null
        notificationsObject = null
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
        const val GTK_BUS_NAME = "org.gtk.Notifications"
        const val GTK_OBJECT_PATH = "/org/gtk/Notifications"
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

@DBusInterfaceName("org.gtk.Notifications")
@Suppress("FunctionName", "ParameterName")
interface GtkNotificationsBus : DBusInterface {
    fun AddNotification(appId: String, id: String, notification: Map<String, Variant<*>>)

    fun RemoveNotification(appId: String, id: String)
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
    private val onClosed: (Long) -> Unit,
) : NotificationsBus {
    private var nextId = 1L
    private var nextExternalId = -1L
    private val records = mutableMapOf<Long, Record>()

    private data class Record(
        val appName: String,
        val actions: List<String>,
        val desktopEntry: String?,
        val appIcon: String,
    )

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
        val desktopEntry = (hints["desktop-entry"]?.value as? String)?.trim()?.ifBlank { null }
        // Portal clients send an empty app name and identify themselves only
        // through the desktop-entry hint, so fall back to that entry's name.
        val resolvedName = appName.trim().ifBlank {
            desktopEntry?.let { NotificationIconResolver.appName(it) }.orEmpty()
        }.ifBlank { "Notification" }
        val resolvedIcon = appIcon.trim().ifBlank { desktopEntry.orEmpty() }
        Log.i("notif[freedesktop] " + resolvedName + " | " + summary.ifBlank { body })
        if (summary.isNotBlank() || body.isNotBlank()) {
            if (records.size >= 64) {
                records.keys.take(records.size - 63).forEach { records.remove(it) }
            }
            records[id] = Record(
                appName = resolvedName,
                actions = actions,
                desktopEntry = desktopEntry,
                appIcon = resolvedIcon,
            )
            onNotification(
                IncomingNotification(
                    appName = resolvedName,
                    title = summary,
                    body = body,
                    appIcon = resolvedIcon,
                    iconBytes = NotificationIconResolver.resolve(resolvedIcon, hints),
                    notificationId = id,
                ),
            )
        }
        return UInt32(id)
    }

    override fun CloseNotification(id: UInt32) {
        records.remove(id.toLong())
        onClosed(id.toLong())
        emitClosed(id)
    }

    /**
     * Notifications that arrived through GIO (org.gtk.Notifications) carry no
     * numeric id, so they get a private negative one. Remembering the sender
     * keeps click-to-open working through its desktop entry.
     */
    fun registerExternal(appName: String, appIcon: String): Long {
        val id = nextExternalId--
        if (records.size >= 64) {
            records.keys.firstOrNull { it > 0 }?.let { records.remove(it) }
        }
        records[id] = Record(
            appName = appName,
            actions = emptyList(),
            desktopEntry = appIcon.takeIf { it.isNotBlank() && !it.contains('/') },
            appIcon = appIcon,
        )
        return id
    }

    override fun GetCapabilities(): List<String> = listOf("body", "body-markup", "actions")

    /**
     * Invokes the sender's default action when it offered one; otherwise opens
     * the application through its desktop entry. Emitting ActionInvoked is what
     * lets clients such as ZapZap open the exact conversation behind a message.
     */
    fun activate(id: Long): Boolean {
        val record = records[id] ?: return false
        if (record.actions.contains("default")) {
            emitAction(id, "default")
            records.remove(id)
            emitClosed(UInt32(id), UInt32(2))
            return true
        }
        val entry = record.desktopEntry
            ?: record.appIcon.takeIf { it.isNotBlank() && !it.contains('/') }
        if (entry != null && launchDesktopEntry(entry)) {
            records.remove(id)
            emitClosed(UInt32(id), UInt32(2))
            return true
        }
        return false
    }

    private fun launchDesktopEntry(entry: String): Boolean {
        val id = entry.removeSuffix(".desktop")
        if (id.isBlank()) return false
        val path = desktopFile(id + ".desktop") ?: return false
        if (Shell.exists("gtk-launch") && Shell.launch("gtk-launch", id)) return true
        if (Shell.exists("gio") && Shell.launch("gio", "launch", path.absolutePath)) return true
        return false
    }

    private fun desktopFile(id: String): File? {
        val home = System.getProperty("user.home").orEmpty()
        val dataDirs = buildList {
            add(File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share"))
            val system = System.getenv("XDG_DATA_DIRS") ?: "/usr/local/share:/usr/share"
            system.split(':').filter { it.isNotBlank() }.forEach { add(File(it)) }
        }
        val applicationDirs = dataDirs.map { File(it, "applications") } + listOf(
            File("$home/.local/share/flatpak/exports/share/applications"),
            File("/var/lib/flatpak/exports/share/applications"),
            File("/var/lib/snapd/desktop/applications"),
        )
        return applicationDirs
            .map { File(it, id) }
            .firstOrNull { it.isFile }
    }

    override fun GetServerInformation(): ServerInformation =
        ServerInformation(AppInfo.NAME, AppInfo.NAME, AppInfo.VERSION, "1.2")

    private fun emitClosed(id: UInt32, reason: UInt32 = UInt32(2)) {
        runCatching {
            connection.sendMessage(
                NotificationsBus.NotificationClosed("/org/freedesktop/Notifications", id, reason),
            )
        }
    }

    private fun emitAction(id: Long, action: String) {
        runCatching {
            connection.sendMessage(
                NotificationsBus.ActionInvoked(
                    "/org/freedesktop/Notifications",
                    UInt32(id),
                    action,
                ),
            )
        }
    }
}
