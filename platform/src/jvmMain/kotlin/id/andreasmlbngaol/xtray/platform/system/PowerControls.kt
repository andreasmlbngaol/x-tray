package id.andreasmlbngaol.xtray.platform.system

import id.andreasmlbngaol.xtray.platform.internal.Shell

/** Session power actions, delegated to systemd/logind when available. */
interface PowerControls {
    val available: Boolean
    fun shutdown()
    fun reboot()
    fun suspend()
}

class ShellPowerControls : PowerControls {
    override val available: Boolean get() = Shell.exists("systemctl")

    override fun shutdown() {
        Shell.run("systemctl", "poweroff")
    }

    override fun reboot() {
        Shell.run("systemctl", "reboot")
    }

    override fun suspend() {
        Shell.run("systemctl", "suspend")
    }
}
