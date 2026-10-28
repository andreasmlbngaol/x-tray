package id.andreasmlbngaol.xtray.platform.notifications

import id.andreasmlbngaol.xtray.platform.internal.Log
import id.andreasmlbngaol.xtray.platform.internal.Shell
import java.io.File
import java.util.concurrent.Executors

/**
 * Plays the desktop's notification chime.
 *
 * Taking over org.freedesktop.Notifications also takes over the sound the stock
 * popups made, so the island has to make it itself. libcanberra follows the
 * user's sound theme, which keeps X-Tray in tune with the rest of the desktop
 * instead of shipping its own audio file.
 */
class NotificationSound {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "xtray-notification-sound").apply { isDaemon = true }
    }

    fun play(eventId: String = "message-new-instant") {
        runCatching {
            executor.execute {
                if (playWithCanberra(eventId)) return@execute
                playFallback(eventId)
            }
        }.onFailure { Log.w("notification sound failed", it) }
    }

    private fun playWithCanberra(eventId: String): Boolean {
        if (!Shell.exists("canberra-gtk-play")) return false
        return Shell.run("canberra-gtk-play", "-i", eventId, timeoutMs = 4_000L) != null
    }

    private fun playFallback(eventId: String) {
        val file = File("/usr/share/sounds/freedesktop/stereo", eventId + ".oga")
        if (file.exists()) {
            val player = when {
                Shell.exists("paplay") -> "paplay"
                Shell.exists("pw-play") -> "pw-play"
                else -> null
            }
            if (player != null && Shell.run(player, file.path, timeoutMs = 4_000L) != null) {
                return
            }
        }
        runCatching { java.awt.Toolkit.getDefaultToolkit().beep() }
    }
}
