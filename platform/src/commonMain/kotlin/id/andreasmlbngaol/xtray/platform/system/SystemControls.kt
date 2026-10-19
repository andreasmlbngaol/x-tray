package id.andreasmlbngaol.xtray.platform.system

data class SystemState(
    val volumePercent: Int? = null,
    val muted: Boolean? = null,
    val micMuted: Boolean? = null,
    val brightnessPercent: Int? = null,
    val wifiEnabled: Boolean? = null,
    val bluetoothEnabled: Boolean? = null,
)

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
}
