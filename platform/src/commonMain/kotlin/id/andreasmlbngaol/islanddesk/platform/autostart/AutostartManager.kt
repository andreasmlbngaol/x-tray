package id.andreasmlbngaol.islanddesk.platform.autostart

interface AutostartManager {
    val supported: Boolean
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
}
