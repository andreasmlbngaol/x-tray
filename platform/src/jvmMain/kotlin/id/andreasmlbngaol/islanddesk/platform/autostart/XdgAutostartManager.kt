package id.andreasmlbngaol.islanddesk.platform.autostart

import id.andreasmlbngaol.islanddesk.platform.internal.AppPaths
import id.andreasmlbngaol.islanddesk.platform.internal.Log
import java.io.File
import java.nio.file.Files

class XdgAutostartManager(
    private val path: java.nio.file.Path = AppPaths.autostart,
) : AutostartManager {
    override val supported: Boolean = true

    override fun isEnabled(): Boolean = Files.exists(path)

    override fun setEnabled(enabled: Boolean) {
        runCatching {
            if (enabled) {
                Files.createDirectories(path.parent)
                Files.writeString(path, desktopEntry())
            } else {
                Files.deleteIfExists(path)
            }
        }.onFailure { Log.w("cannot update autostart", it) }
    }

    private fun desktopEntry(): String {
        val exec = executable()
        return buildString {
            appendLine("[Desktop Entry]")
            appendLine("Type=Application")
            appendLine("Name=IslandDesk")
            appendLine("Comment=Dynamic Island untuk desktop Linux")
            appendLine("Exec=" + exec)
            appendLine("Terminal=false")
            appendLine("X-GNOME-Autostart-enabled=true")
            appendLine("NoDisplay=false")
        }
    }

    private fun executable(): String {
        System.getenv("ISLANDDESK_EXEC")?.takeIf { it.isNotBlank() }?.let { return it }
        val packaged = "/opt/islanddesk/bin/islanddesk"
        if (File(packaged).canExecute()) return packaged
        val command = ProcessHandle.current().info().command().orElse("islanddesk")
        return command
    }
}
