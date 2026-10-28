package id.andreasmlbngaol.xtray.platform.system

import id.andreasmlbngaol.xtray.platform.internal.Log
import id.andreasmlbngaol.xtray.platform.internal.Shell

/**
 * Desktop controls backed by the tools already present on most Linux
 * desktops: wpctl (PipeWire), nmcli (NetworkManager), bluetoothctl and
 * brightnessctl. Missing tools disable the corresponding control.
 */
class ShellSystemControls : SystemControls {
    private val hasWpctl = Shell.exists("wpctl")
    private val hasBrightnessctl = Shell.exists("brightnessctl")
    private val hasNmcli = Shell.exists("nmcli")
    private val hasBluetoothctl = Shell.exists("bluetoothctl")
    private val hasBusctl = Shell.exists("busctl")

    /**
     * GNOME exposes screen brightness on the session bus, which covers
     * machines without brightnessctl (and needs no extra privileges). The
     * probe is retried while it fails: at login the power daemon can come up
     * after X-Tray, and latching the first miss hid the slider all session.
     */
    private var gnomePowerAvailable = false
    private var gnomePowerCheckedAt = 0L

    private fun gnomePower(): Boolean {
        if (gnomePowerAvailable) return true
        if (!hasBusctl) return false
        val now = System.currentTimeMillis()
        if (now - gnomePowerCheckedAt < 5_000L) return false
        gnomePowerCheckedAt = now
        gnomePowerAvailable = runCatching { gnomeBrightness() != null }.getOrDefault(false)
        return gnomePowerAvailable
    }

    override val hasBrightness: Boolean get() = hasBrightnessctl || gnomePower()
    override val hasWifi: Boolean get() = hasNmcli
    override val hasBluetooth: Boolean get() = hasBluetoothctl

    override fun snapshot(): SystemState = SystemState(
        volumePercent = volumePercent(),
        muted = muted(),
        micMuted = micMuted(),
        brightnessPercent = brightnessPercent(),
        wifiEnabled = wifiEnabled(),
        bluetoothEnabled = bluetoothEnabled(),
    )

    override fun volumeStep(deltaPercent: Int) {
        if (!hasWpctl) return
        val sign = if (deltaPercent >= 0) "+" else "-"
        Shell.run("wpctl", "set-volume", "@DEFAULT_AUDIO_SINK@", Math.abs(deltaPercent).toString() + "%" + sign)
    }

    override fun setVolume(percent: Int) {
        if (!hasWpctl) return
        Shell.run("wpctl", "set-volume", "@DEFAULT_AUDIO_SINK@", percent.coerceIn(0, 150).toString() + "%")
    }

    override fun toggleMute() {
        if (hasWpctl) Shell.run("wpctl", "set-mute", "@DEFAULT_AUDIO_SINK@", "toggle")
    }

    override fun toggleMic() {
        if (hasWpctl) Shell.run("wpctl", "set-mute", "@DEFAULT_AUDIO_SOURCE@", "toggle")
    }

    override fun brightnessStep(deltaPercent: Int) {
        if (hasBrightnessctl) {
            val sign = if (deltaPercent >= 0) "+" else "-"
            Shell.run("brightnessctl", "set", Math.abs(deltaPercent).toString() + "%" + sign)
            return
        }
        val current = brightnessPercent() ?: return
        setBrightness(current + deltaPercent)
    }

    override fun setBrightness(percent: Int) {
        val value = percent.coerceIn(1, 100)
        if (hasBrightnessctl) {
            Shell.run("brightnessctl", "set", value.toString() + "%")
            return
        }
        if (gnomePower()) {
            Shell.run(
                "busctl", "--user", "set-property",
                "org.gnome.SettingsDaemon.Power",
                "/org/gnome/SettingsDaemon/Power",
                "org.gnome.SettingsDaemon.Power.Screen",
                "Brightness", "i", value.toString(),
            )
        }
    }

    override fun toggleWifi() {
        if (!hasNmcli) return
        val enabled = wifiEnabled() ?: return
        Shell.run("nmcli", "radio", "wifi", if (enabled) "off" else "on")
    }

    override fun toggleBluetooth() {
        if (!hasBluetoothctl) return
        val enabled = bluetoothEnabled() ?: return
        Shell.run("bluetoothctl", "power", if (enabled) "off" else "on")
    }

    /**
     * GNOME and KDE use different panel identifiers; try both before giving
     * up. Launchers return immediately, so openSettings does not block the UI.
     */
    override fun openSettings(page: SystemSettingsPage): Boolean {
        val gnome = when (page) {
            SystemSettingsPage.Sound -> "sound"
            SystemSettingsPage.Wifi -> "wifi"
            SystemSettingsPage.Bluetooth -> "bluetooth"
            SystemSettingsPage.Network -> "network"
            SystemSettingsPage.Power -> "power"
            SystemSettingsPage.Display -> "display"
            SystemSettingsPage.Notifications -> "notifications"
            SystemSettingsPage.Appearance -> "appearance"
        }
        val kde = when (page) {
            SystemSettingsPage.Sound -> "kcm_pulseaudio"
            SystemSettingsPage.Wifi -> "kcm_networkmanagement"
            SystemSettingsPage.Bluetooth -> "kcm_bluetooth"
            SystemSettingsPage.Network -> "kcm_networkmanagement"
            SystemSettingsPage.Power -> "kcm_powerdevil"
            SystemSettingsPage.Display -> "kcm_kscreen"
            SystemSettingsPage.Notifications -> "kcm_notifications"
            SystemSettingsPage.Appearance -> "kcm_lookandfeel"
        }
        if (Shell.exists("gnome-control-center") && Shell.launch("gnome-control-center", gnome)) {
            return true
        }
        if (Shell.exists("systemsettings") && Shell.launch("systemsettings", kde)) {
            return true
        }
        for (binary in listOf("kcmshell6", "kcmshell5")) {
            if (Shell.exists(binary) && Shell.launch(binary, kde)) return true
        }
        Log.w("no settings handler for " + page)
        return false
    }

    private fun volumePercent(): Int? {
        if (!hasWpctl) return null
        val output = Shell.run("wpctl", "get-volume", "@DEFAULT_AUDIO_SINK@") ?: return null
        val match = Regex("Volume:\\s*([0-9.]+)").find(output) ?: return null
        return (match.groupValues[1].toFloatOrNull()?.times(100))?.toInt()
    }

    private fun muted(): Boolean? {
        if (!hasWpctl) return null
        val output = Shell.run("wpctl", "get-volume", "@DEFAULT_AUDIO_SINK@") ?: return null
        return output.contains("[MUTED]")
    }

    private fun micMuted(): Boolean? {
        if (!hasWpctl) return null
        val output = Shell.run("wpctl", "get-volume", "@DEFAULT_AUDIO_SOURCE@") ?: return null
        return output.contains("[MUTED]")
    }

    private fun brightnessPercent(): Int? {
        if (hasBrightnessctl) {
            val output = Shell.run("brightnessctl", "-m") ?: return null
            val match = Regex("(\\d+)%").find(output) ?: return null
            return match.groupValues[1].toIntOrNull()
        }
        return gnomeBrightness()
    }

    private fun gnomeBrightness(): Int? {
        val output = Shell.run(
            "busctl", "--user", "get-property",
            "org.gnome.SettingsDaemon.Power",
            "/org/gnome/SettingsDaemon/Power",
            "org.gnome.SettingsDaemon.Power.Screen",
            "Brightness",
        ) ?: return null
        return Regex("i\\s+(\\d+)").find(output)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun wifiEnabled(): Boolean? {
        if (!hasNmcli) return null
        val output = Shell.run("nmcli", "radio", "wifi") ?: return null
        return when {
            output.contains("enabled", ignoreCase = true) -> true
            output.contains("disabled", ignoreCase = true) -> false
            else -> null
        }
    }

    private fun bluetoothEnabled(): Boolean? {
        if (!hasBluetoothctl) return null
        val output = Shell.run("bluetoothctl", "show") ?: return null
        val match = Regex("Powered:\\s*(yes|no)").find(output) ?: return null
        return match.groupValues[1] == "yes"
    }
}
