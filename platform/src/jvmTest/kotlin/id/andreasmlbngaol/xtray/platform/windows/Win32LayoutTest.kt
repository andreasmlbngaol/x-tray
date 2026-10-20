package id.andreasmlbngaol.xtray.platform.windows

import id.andreasmlbngaol.xtray.platform.system.GUID
import id.andreasmlbngaol.xtray.platform.system.MIB_IFROW
import id.andreasmlbngaol.xtray.platform.system.PHYSICAL_MONITOR
import id.andreasmlbngaol.xtray.platform.system.SYSTEM_POWER_STATUS
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Win32 structures are read through raw memory, so their layout has to
 * match the Windows headers byte for byte. These values come from the SDK
 * definitions and are checked on every build.
 */
class Win32LayoutTest {
    @Test
    fun mibIfRowMatchesTheWindowsLayout() {
        val row = MIB_IFROW()
        assertEquals(860, row.size())
        assertEquals(512, row.offsetOf("index"))
        assertEquals(516, row.offsetOf("type"))
        assertEquals(552, row.offsetOf("inOctets"))
        assertEquals(576, row.offsetOf("outOctets"))
    }

    @Test
    fun systemPowerStatusIsTwelveBytes() {
        assertEquals(12, SYSTEM_POWER_STATUS().size())
    }

    @Test
    fun guidIsSixteenBytes() {
        assertEquals(16, GUID().size())
    }

    @Test
    fun physicalMonitorIs264Bytes() {
        assertEquals(264, PHYSICAL_MONITOR().size())
    }
}
