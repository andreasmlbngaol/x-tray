package id.andreasmlbngaol.islanddesk.platform.battery

data class BatteryState(
    val percent: Int? = null,
    val charging: Boolean = false,
)

interface BatterySource {
    fun read(): BatteryState
}
