package id.andreasmlbngaol.xtray.platform.autostart

interface AutostartManager {
    val supported: Boolean
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
}
