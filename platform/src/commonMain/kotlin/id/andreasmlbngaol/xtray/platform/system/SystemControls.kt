package id.andreasmlbngaol.xtray.platform.system

data class SystemState(
    val volumePercent: Int? = null,
    val muted: Boolean? = null,
    val micMuted: Boolean? = null,
    val brightnessPercent: Int? = null,
    val wifiEnabled: Boolean? = null,
    val bluetoothEnabled: Boolean? = null,
)

/** Desktop settings pages a quick-control tile can deep-link to. */
enum class SystemSettingsPage {
    Sound,
    Wifi,
    Bluetooth,
    Network,
    Power,
    Display,
    Notifications,
    Appearance,
}

/**
 * Best-effort desktop controls. Each capability is nullable: when the backing
 * tool is missing the UI hides that control instead of failing.
 */
interface SystemControls {
    val hasBrightness: Boolean
    val hasWifi: Boolean
    val hasBluetooth: Boolean

    fun snapshot(): SystemState
    fun volumeStep(deltaPercent: Int)
    fun setVolume(percent: Int)
    fun toggleMute()
    fun toggleMic()
    fun brightnessStep(deltaPercent: Int)
    fun setBrightness(percent: Int)
    fun toggleWifi()
    fun toggleBluetooth()

    /** Opens the desktop settings page for [page]; false when no handler exists. */
    fun openSettings(page: SystemSettingsPage): Boolean = false
}
