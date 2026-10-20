package id.andreasmlbngaol.xtray.platform.internal

import java.nio.file.Path
import java.nio.file.Paths

object AppPaths {
    private val home: String = System.getProperty("user.home")

    private val xdgConfig: String = System.getenv("XDG_CONFIG_HOME") ?: (home + "/.config")
    private val xdgState: String = System.getenv("XDG_STATE_HOME") ?: (home + "/.local/state")

    val configDir: Path = if (Os.isWindows) {
        Paths.get(System.getenv("APPDATA") ?: (home + "/AppData/Roaming"), "x-tray")
    } else {
        Paths.get(xdgConfig, "x-tray")
    }

    val settings: Path = configDir.resolve("settings.json")
    val lock: Path = configDir.resolve("instance.lock")

    /** Only meaningful on Linux; the Windows build uses the registry instead. */
    val autostart: Path = Paths.get(xdgConfig, "autostart", "x-tray.desktop")

    val logFile: Path = if (Os.isWindows) {
        Paths.get(System.getenv("LOCALAPPDATA") ?: (home + "/AppData/Local"), "x-tray", "x-tray.log")
    } else {
        Paths.get(xdgState, "x-tray", "x-tray.log")
    }

    /**
     * Unix socket path. Windows has no useful AF_UNIX story in AWT-era JVMs, so
     * the IPC layer uses a loopback TCP port there and stores it in this file.
     */
    val socket: Path = run {
        val runtime = System.getenv("XDG_RUNTIME_DIR")
        if (runtime.isNullOrBlank()) {
            Paths.get(System.getProperty("java.io.tmpdir"), "x-tray-" + ProcessHandle.current().pid() + ".sock")
        } else {
            Paths.get(runtime, "x-tray.sock")
        }
    }

    /** Port of the loopback IPC listener, written by the running instance. */
    val ipcPort: Path = configDir.resolve("ipc.port")
}
