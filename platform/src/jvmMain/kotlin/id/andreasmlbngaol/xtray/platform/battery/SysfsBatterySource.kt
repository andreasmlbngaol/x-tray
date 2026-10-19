package id.andreasmlbngaol.xtray.platform.battery

import java.io.File

/** Reads /sys/class/power_supply, avoiding a UPower/D-Bus dependency. */
class SysfsBatterySource : BatterySource {
    override fun read(): BatteryState {
        val batteries = File("/sys/class/power_supply")
            .listFiles()
            ?.filter { it.name.startsWith("BAT") }
            .orEmpty()
        if (batteries.isEmpty()) return BatteryState()

        val percents = batteries.mapNotNull { readInt(File(it, "capacity")) }
        val percent = if (percents.isEmpty()) null else percents.average().toInt()
        val charging = batteries.any { battery ->
            val status = runCatching { File(battery, "status").readText().trim() }.getOrNull()
            status == "Charging" || status == "Full"
        }
        return BatteryState(percent, charging)
    }

    private fun readInt(file: File): Int? =
        runCatching { file.readText().trim().toInt() }.getOrNull()
}
