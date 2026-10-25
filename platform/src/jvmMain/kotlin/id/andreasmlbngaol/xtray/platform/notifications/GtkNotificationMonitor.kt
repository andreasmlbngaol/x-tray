package id.andreasmlbngaol.xtray.platform.notifications

import id.andreasmlbngaol.xtray.platform.internal.Log
import id.andreasmlbngaol.xtray.platform.internal.Shell
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.freedesktop.dbus.types.Variant

/**
 * GIO-based apps (Ghostty, many GTK applications) do not call
 * org.freedesktop.Notifications. On GNOME they send through the private
 * org.gtk.Notifications interface owned by gnome-shell, which bypasses the
 * island entirely.
 *
 * This monitor watches those calls on a separate bus connection and mirrors
 * them into the island. The original shell notification is removed afterwards
 * so the user does not see two copies.
 */
internal class GtkNotificationMonitor(
    private val onNotification: (IncomingNotification) -> Unit,
    private val onRemove: (appId: String, id: String) -> Unit,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var process: Process? = null
    private var thread: Thread? = null

    fun start(): Boolean {
        if (!Shell.exists("busctl")) return false
        val started = runCatching {
            ProcessBuilder(
                "busctl", "--user", "monitor", "--json=short",
                "--match=type='method_call',interface='org.gtk.Notifications',member='AddNotification'",
            )
                .redirectErrorStream(true)
                .start()
        }.onFailure { Log.w("notifications: cannot monitor org.gtk.Notifications", it) }
            .getOrNull() ?: return false

        process = started
        thread = Thread({ readLoop(started) }, "x-tray-gtk-notifications").apply {
            isDaemon = true
            start()
        }
        Log.i("notifications: monitoring org.gtk.Notifications")
        return true
    }

    fun stop() {
        runCatching { process?.destroy() }
        process = null
        thread?.interrupt()
        thread = null
    }

    private fun readLoop(process: Process) {
        runCatching {
            process.inputStream.bufferedReader().forEachLine { line -> handle(line) }
        }.onFailure { Log.w("notifications: gtk monitor stopped", it) }
    }

    private fun handle(line: String) {
        if (!line.startsWith("{")) return
        val root = runCatching { json.parseToJsonElement(line).jsonObject }.getOrNull() ?: return
        if (root["interface"]?.jsonPrimitive?.contentOrNull != "org.gtk.Notifications") return
        if (root["member"]?.jsonPrimitive?.contentOrNull != "AddNotification") return

        val data = root["payload"]?.jsonObject?.get("data")?.jsonArray ?: return
        val appId = data.getOrNull(0)?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val id = data.getOrNull(1)?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val payload = data.getOrNull(2)?.jsonObject ?: return
        if (appId.isBlank() || id.isBlank()) return

        fun value(key: String): String =
            payload[key]?.jsonObject?.get("data")?.jsonPrimitive?.contentOrNull.orEmpty()

        val title = value("title")
        val body = value("body")
        if (title.isBlank() && body.isBlank()) return

        val appName = NotificationIconResolver.appName(appId) ?: prettifyAppId(appId)
        Log.i("notif[gtk] " + appId + " -> " + appName + " | " + title.ifBlank { body })
        onNotification(
            IncomingNotification(
                appName = appName,
                title = title,
                body = body,
                appIcon = appId,
                iconBytes = NotificationIconResolver.resolve(
                    appId,
                    mapOf("desktop-entry" to Variant(appId)),
                ),
                notificationId = 0L,
            ),
        )

        // Let gnome-shell finish processing AddNotification first, then drop
        // its copy so only the island keeps showing it. The wait runs off the
        // monitor thread so a burst of notifications is not serialised.
        Thread {
            runCatching { Thread.sleep(150L) }
            onRemove(appId, id)
        }.apply { isDaemon = true }.start()
    }

    private fun prettifyAppId(appId: String): String =
        appId.substringAfterLast('.')
            .replace('-', ' ')
            .replace('_', ' ')
            .replaceFirstChar { it.uppercase() }
            .ifBlank { "Notification" }
}
