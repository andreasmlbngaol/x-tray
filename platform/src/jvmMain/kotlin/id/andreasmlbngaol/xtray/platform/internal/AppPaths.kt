package id.andreasmlbngaol.xtray.platform.internal

import java.nio.file.Path
import java.nio.file.Paths

object AppPaths {
    private val home: String = System.getProperty("user.home")

    val configDir: Path = Paths.get(System.getenv("XDG_CONFIG_HOME") ?: (home + "/.config"), "x-tray")
    val settings: Path = configDir.resolve("settings.json")
    val lock: Path = configDir.resolve("instance.lock")
    val autostart: Path = Paths.get(System.getenv("XDG_CONFIG_HOME") ?: (home + "/.config"), "autostart", "x-tray.desktop")

    val logFile: Path = Paths.get(
        System.getenv("XDG_STATE_HOME") ?: (home + "/.local/state"),
        "x-tray",
        "x-tray.log",
    )

    val socket: Path = run {
        val runtime = System.getenv("XDG_RUNTIME_DIR")
        if (runtime.isNullOrBlank()) {
            Paths.get(System.getProperty("java.io.tmpdir"), "x-tray-" + ProcessHandle.current().pid() + ".sock")
        } else {
            Paths.get(runtime, "x-tray.sock")
        }
    }
}
