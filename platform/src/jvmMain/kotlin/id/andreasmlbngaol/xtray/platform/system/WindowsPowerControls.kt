package id.andreasmlbngaol.xtray.platform.system

import id.andreasmlbngaol.xtray.platform.internal.Shell

/**
 * Session power actions. `shutdown.exe` works for the interactive user
 * without elevation, and `SetSuspendState` sleeps without extra privileges.
 */
class WindowsPowerControls : PowerControls {
    override val available: Boolean get() = true

    override fun shutdown() {
        Shell.run("shutdown.exe", "/s", "/t", "0")
    }

    override fun reboot() {
        Shell.run("shutdown.exe", "/r", "/t", "0")
    }

    override fun suspend() {
        val result = Win32Api.powrProf?.SetSuspendState(0, 0, 0)
        if (result == null || result == 0) {
            Shell.run("rundll32.exe", "powrprof.dll,SetSuspendState", "0,1,0")
        }
    }
}
