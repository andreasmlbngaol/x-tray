package id.andreasmlbngaol.xtray.platform.autostart

import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import id.andreasmlbngaol.xtray.platform.internal.Log
import id.andreasmlbngaol.xtray.platform.system.Win32Api

/**
 * Autostart through the per-user Run key. No shortcut files and no elevation:
 * the value is the packaged executable, or XTRAY_EXEC when set.
 */
class WindowsAutostartManager : AutostartManager {
    override val supported: Boolean get() = Win32Api.advapi32 != null

    override fun isEnabled(): Boolean = read() != null

    override fun setEnabled(enabled: Boolean) {
        if (enabled) write(executable()) else delete()
    }

    private fun read(): String? {
        val advapi32 = Win32Api.advapi32 ?: return null
        val key = PointerByReference()
        if (advapi32.RegOpenKeyExW(HKEY_CURRENT_USER, wide(RUN_KEY), 0, KEY_READ, key) != 0) return null
        val handle = key.value ?: return null
        try {
            val buffer = Memory(2048L)
            val size = IntByReference(2048)
            val status = advapi32.RegQueryValueExW(handle, wide(VALUE_NAME), null, null, buffer, size)
            if (status != 0) return null
            return buffer.getWideString(0L).takeIf { it.isNotBlank() }
        } finally {
            advapi32.RegCloseKey(handle)
        }
    }

    private fun write(command: String) {
        val advapi32 = Win32Api.advapi32 ?: return
        val key = PointerByReference()
        if (advapi32.RegOpenKeyExW(HKEY_CURRENT_USER, wide(RUN_KEY), 0, KEY_SET_VALUE, key) != 0) {
            Log.w("cannot open the Run key")
            return
        }
        val handle = key.value ?: return
        try {
            val data = wide(command)
            val bytes = (command.length + 1) * 2
            val status = advapi32.RegSetValueExW(handle, wide(VALUE_NAME), 0, REG_SZ, data, bytes)
            if (status != 0) Log.w("cannot write the Run value: " + status)
        } finally {
            advapi32.RegCloseKey(handle)
        }
    }

    private fun delete() {
        val advapi32 = Win32Api.advapi32 ?: return
        val key = PointerByReference()
        if (advapi32.RegOpenKeyExW(HKEY_CURRENT_USER, wide(RUN_KEY), 0, KEY_SET_VALUE, key) != 0) return
        val handle = key.value ?: return
        try {
            advapi32.RegDeleteValueW(handle, wide(VALUE_NAME))
        } finally {
            advapi32.RegCloseKey(handle)
        }
    }

    private fun executable(): String {
        System.getenv("XTRAY_EXEC")?.takeIf { it.isNotBlank() }?.let { return it }
        val command = ProcessHandle.current().info().command().orElse(null) ?: return "x-tray.exe"
        return "\"" + command + "\""
    }

    private fun wide(text: String): Memory =
        Memory((text.length + 1) * 2L).also { it.setWideString(0L, text) }

    private companion object {
        // Predefined keys are sign-extended on 64-bit Windows:
        // HKEY_CURRENT_USER is (HKEY)(LONG)0x80000001.
        val HKEY_CURRENT_USER: Pointer = Pointer(-2147483647L)
        const val RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        const val VALUE_NAME = "X-Tray"
        const val KEY_READ = 0x20019
        const val KEY_SET_VALUE = 0x0002
        const val REG_SZ = 1
    }
}
