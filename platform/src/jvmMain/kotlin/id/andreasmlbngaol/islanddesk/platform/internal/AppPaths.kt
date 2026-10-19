package id.andreasmlbngaol.islanddesk.platform.internal

import java.nio.file.Path
import java.nio.file.Paths

object AppPaths {
    private val home: String = System.getProperty("user.home")

    val configDir: Path = Paths.get(System.getenv("XDG_CONFIG_HOME") ?: (home + "/.config"), "islanddesk")
    val settings: Path = configDir.resolve("settings.json")
    val lock: Path = configDir.resolve("instance.lock")
    val autostart: Path = Paths.get(System.getenv("XDG_CONFIG_HOME") ?: (home + "/.config"), "autostart", "islanddesk.desktop")

    val logFile: Path = Paths.get(
        System.getenv("XDG_STATE_HOME") ?: (home + "/.local/state"),
        "islanddesk",
        "islanddesk.log",
    )

    val socket: Path = run {
        val runtime = System.getenv("XDG_RUNTIME_DIR")
        if (runtime.isNullOrBlank()) {
            Paths.get(System.getProperty("java.io.tmpdir"), "islanddesk-" + ProcessHandle.current().pid() + ".sock")
        } else {
            Paths.get(runtime, "islanddesk.sock")
        }
    }
}
