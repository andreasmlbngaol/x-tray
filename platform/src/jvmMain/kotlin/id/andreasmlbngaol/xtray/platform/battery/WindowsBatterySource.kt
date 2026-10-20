package id.andreasmlbngaol.xtray.platform.battery

import id.andreasmlbngaol.xtray.platform.system.SYSTEM_POWER_STATUS
import id.andreasmlbngaol.xtray.platform.system.Win32Api

/**
 * Battery state from `GetSystemPowerStatus`, which works for laptops and
 * reports "no battery" as 255 percent on desktops.
 */
class WindowsBatterySource : BatterySource {
    override fun read(): BatteryState {
        val kernel32 = Win32Api.kernel32 ?: return BatteryState()
        val status = SYSTEM_POWER_STATUS()
        status.write()
        if (kernel32.GetSystemPowerStatus(status.pointer) == 0) return BatteryState()
        status.read()

        val percent = status.batteryLifePercent.toInt() and 0xFF
        if (percent == 255) return BatteryState()
        return BatteryState(
            percent = percent,
            charging = (status.acLineStatus.toInt() and 0xFF) == 1,
        )
    }
}
