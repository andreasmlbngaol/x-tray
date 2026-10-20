package id.andreasmlbngaol.xtray.platform.system

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import id.andreasmlbngaol.xtray.platform.internal.Log
import id.andreasmlbngaol.xtray.platform.window.User32

/** Win32 libraries the Windows sources talk to, loaded once and best-effort. */
internal object Win32Api {
    val user32: User32? = load("user32", User32::class.java)
    val kernel32: Kernel32? = load("kernel32", Kernel32::class.java)
    val dxva2: Dxva2? = load("dxva2", Dxva2::class.java)
    val advapi32: Advapi32? = load("advapi32", Advapi32::class.java)
    val ole32: Ole32? = load("ole32", Ole32::class.java)
    val powrProf: PowrProf? = load("powrprof", PowrProf::class.java)
    val iphlpapi: Iphlpapi? = load("iphlpapi", Iphlpapi::class.java)

    private fun <T : Library> load(name: String, type: Class<T>): T? =
        runCatching { Native.load(name, type) }
            .onFailure { Log.w("cannot load " + name, it) }
            .getOrNull()
}

internal interface Kernel32 : Library {
    fun GetSystemPowerStatus(status: Pointer): Int
}

internal interface PowrProf : Library {
    fun SetSuspendState(hibernate: Int, forceCritical: Int, disableWakeEvent: Int): Int
}

internal interface Iphlpapi : Library {
    fun GetIfTable(table: Pointer?, size: IntByReference, order: Int): Int
}

/**
 * MIB_IFROW, the row GetIfTable fills. The layout is fixed by Windows and the
 * field offsets are read back through JNA rather than hardcoded.
 */
@Structure.FieldOrder(
    "name", "index", "type", "mtu", "speed", "physAddrLen", "physAddr",
    "adminStatus", "operStatus", "lastChange",
    "inOctets", "inUcastPkts", "inNUcastPkts", "inDiscards", "inErrors", "inUnknownProtos",
    "outOctets", "outUcastPkts", "outNUcastPkts", "outDiscards", "outErrors", "outQLen",
    "descrLen", "descr",
)
internal class MIB_IFROW : Structure() {
    @JvmField var name: ByteArray = ByteArray(512)
    @JvmField var index: Int = 0
    @JvmField var type: Int = 0
    @JvmField var mtu: Int = 0
    @JvmField var speed: Int = 0
    @JvmField var physAddrLen: Int = 0
    @JvmField var physAddr: ByteArray = ByteArray(8)
    @JvmField var adminStatus: Int = 0
    @JvmField var operStatus: Int = 0
    @JvmField var lastChange: Int = 0
    @JvmField var inOctets: Int = 0
    @JvmField var inUcastPkts: Int = 0
    @JvmField var inNUcastPkts: Int = 0
    @JvmField var inDiscards: Int = 0
    @JvmField var inErrors: Int = 0
    @JvmField var inUnknownProtos: Int = 0
    @JvmField var outOctets: Int = 0
    @JvmField var outUcastPkts: Int = 0
    @JvmField var outNUcastPkts: Int = 0
    @JvmField var outDiscards: Int = 0
    @JvmField var outErrors: Int = 0
    @JvmField var outQLen: Int = 0
    @JvmField var descrLen: Int = 0
    @JvmField var descr: ByteArray = ByteArray(256)

    /** Structure.fieldOffset is protected; the reader needs it to walk rows. */
    fun offsetOf(field: String): Int = fieldOffset(field)
}

internal interface Dxva2 : Library {
    fun GetNumberOfPhysicalMonitorsFromHMONITOR(hMonitor: Pointer, count: IntByReference): Int
    fun GetPhysicalMonitorsFromHMONITOR(hMonitor: Pointer, count: Int, monitors: Pointer): Int
    fun GetMonitorBrightness(
        hMonitor: Pointer,
        minimum: IntByReference,
        current: IntByReference,
        maximum: IntByReference,
    ): Int
    fun SetMonitorBrightness(hMonitor: Pointer, value: Int): Int
    fun DestroyPhysicalMonitors(count: Int, monitors: Pointer): Int
}

internal interface Advapi32 : Library {
    fun RegOpenKeyExW(hKey: Pointer, subKey: Pointer, options: Int, access: Int, result: PointerByReference): Int
    fun RegQueryValueExW(
        hKey: Pointer,
        valueName: Pointer,
        reserved: Pointer?,
        type: Pointer?,
        data: Pointer?,
        size: IntByReference,
    ): Int
    fun RegSetValueExW(hKey: Pointer, valueName: Pointer, reserved: Int, type: Int, data: Pointer, size: Int): Int
    fun RegDeleteValueW(hKey: Pointer, valueName: Pointer): Int
    fun RegCloseKey(hKey: Pointer): Int
}

internal interface Ole32 : Library {
    fun CoInitializeEx(reserved: Pointer?, coInit: Int): Int
    fun CoUninitialize()
    fun CoCreateInstance(
        clsid: GUID,
        outer: Pointer?,
        context: Int,
        iid: GUID,
        out: PointerByReference,
    ): Int
}

@Structure.FieldOrder("data1", "data2", "data3", "data4")
internal class GUID : Structure() {
    @JvmField var data1: Int = 0
    @JvmField var data2: Short = 0
    @JvmField var data3: Short = 0
    @JvmField var data4: ByteArray = ByteArray(8)
}

@Structure.FieldOrder(
    "acLineStatus",
    "batteryFlag",
    "batteryLifePercent",
    "systemStatusFlag",
    "batteryLifeTime",
    "batteryFullLifeTime",
)
internal class SYSTEM_POWER_STATUS : Structure() {
    @JvmField var acLineStatus: Byte = 0
    @JvmField var batteryFlag: Byte = 0
    @JvmField var batteryLifePercent: Byte = 0
    @JvmField var systemStatusFlag: Byte = 0
    @JvmField var batteryLifeTime: Int = 0
    @JvmField var batteryFullLifeTime: Int = 0
}

@Structure.FieldOrder("hPhysicalMonitor", "description")
internal class PHYSICAL_MONITOR : Structure() {
    @JvmField var hPhysicalMonitor: Pointer? = null
    @JvmField var description: ByteArray = ByteArray(256)
}

/** Parses 8-4-4-16 hexadecimal into the 16-byte Windows GUID layout. */
internal fun guid(text: String): GUID {
    val parts = text.split("-")
    require(parts.size == 5) { "invalid GUID: " + text }
    val tail = parts[4]
    val bytes = ByteArray(8) { index -> tail.substring(index * 2, index * 2 + 2).toInt(16).toByte() }
    return GUID().apply {
        data1 = parts[0].toLong(16).toInt()
        data2 = parts[1].toInt(16).toShort()
        data3 = parts[2].toInt(16).toShort()
        data4 = bytes
    }
}

/** Copies a GUID into native memory, for COM calls that take a REFIID. */
internal fun GUID.toMemory(): com.sun.jna.Memory {
    write()
    val memory = com.sun.jna.Memory(16L)
    memory.write(0L, pointer.getByteArray(0L, 16), 0, 16)
    return memory
}
