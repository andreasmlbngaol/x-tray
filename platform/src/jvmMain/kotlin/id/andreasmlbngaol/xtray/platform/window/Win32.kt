package id.andreasmlbngaol.xtray.platform.window

import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary.StdCallCallback

/**
 * The slice of user32 the island needs. Everything is pointer-sized so the
 * same bindings work on 32- and 64-bit Windows.
 */
internal interface User32 : Library {
    fun EnumWindows(callback: WNDENUMPROC, lParam: Pointer?): Int
    fun GetWindowThreadProcessId(hwnd: Pointer, pid: IntByReference): Int
    fun GetWindowTextW(hwnd: Pointer, text: Pointer, max: Int): Int
    fun IsWindowVisible(hwnd: Pointer): Int

    fun SetWindowLongPtrW(hwnd: Pointer, index: Int, newProc: WNDPROC): Pointer
    fun GetWindowLongW(hwnd: Pointer, index: Int): Int
    fun SetWindowLongW(hwnd: Pointer, index: Int, value: Int): Int
    fun CallWindowProcW(previous: Pointer, hwnd: Pointer, msg: Int, wParam: Pointer, lParam: Pointer): Pointer

    fun SetWindowPos(hwnd: Pointer, insertAfter: Pointer, x: Int, y: Int, cx: Int, cy: Int, flags: Int): Int
    fun ScreenToClient(hwnd: Pointer, point: Pointer): Int
    fun GetForegroundWindow(): Pointer
    fun GetDesktopWindow(): Pointer
    fun GetWindowRect(hwnd: Pointer, rect: Pointer): Int
    fun IsZoomed(hwnd: Pointer): Int
    fun MonitorFromWindow(hwnd: Pointer, flags: Int): Pointer
    fun GetMonitorInfoW(monitor: Pointer, info: Pointer): Int
}

/** BOOL CALLBACK EnumWindowsProc(HWND, LPARAM). */
internal interface WNDENUMPROC : StdCallCallback {
    fun callback(hwnd: Pointer, lParam: Pointer?): Int
}

/** LRESULT CALLBACK WindowProc(HWND, UINT, WPARAM, LPARAM). */
internal interface WNDPROC : StdCallCallback {
    fun callback(hwnd: Pointer, msg: Int, wParam: Pointer, lParam: Pointer): Pointer
}

@Structure.FieldOrder("x", "y")
internal class POINT : Structure() {
    @JvmField var x: Int = 0
    @JvmField var y: Int = 0
}

@Structure.FieldOrder("left", "top", "right", "bottom")
internal class RECT : Structure() {
    @JvmField var left: Int = 0
    @JvmField var top: Int = 0
    @JvmField var right: Int = 0
    @JvmField var bottom: Int = 0
}

/**
 * MONITORINFO with the nested RECTs flattened: JNA only guarantees reading
 * nested structures when they are written field by field.
 */
@Structure.FieldOrder(
    "cbSize",
    "monitorLeft", "monitorTop", "monitorRight", "monitorBottom",
    "workLeft", "workTop", "workRight", "workBottom",
    "flags",
)
internal class MONITORINFO : Structure() {
    @JvmField var cbSize: Int = 0
    @JvmField var monitorLeft: Int = 0
    @JvmField var monitorTop: Int = 0
    @JvmField var monitorRight: Int = 0
    @JvmField var monitorBottom: Int = 0
    @JvmField var workLeft: Int = 0
    @JvmField var workTop: Int = 0
    @JvmField var workRight: Int = 0
    @JvmField var workBottom: Int = 0
    @JvmField var flags: Int = 0
}
