package id.andreasmlbngaol.xtray.platform.window

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import id.andreasmlbngaol.xtray.core.geometry.IslandGeometry
import id.andreasmlbngaol.xtray.platform.internal.Log

/**
 * Windows counterpart of [id.andreasmlbngaol.xtray.platform.x11.X11Window].
 *
 * AWT cannot shape a window, so the island installs a `WM_NCHITTEST` handler
 * that reports [HTTRANSPARENT] for every point outside the visible island.
 * Clicks in the transparent margin then reach whatever is underneath while the
 * shadow still renders normally.
 */
object Win32Window {
    private const val WM_NCHITTEST = 0x0084
    private const val HTTRANSPARENT = -1

    private const val GWL_EXSTYLE = -20
    private const val GWLP_WNDPROC = -4
    private const val WS_EX_TOOLWINDOW = 0x00000080
    private const val WS_EX_APPWINDOW = 0x00040000

    private const val SWP_NOSIZE = 0x0001
    private const val SWP_NOMOVE = 0x0002
    private const val SWP_NOACTIVATE = 0x0010
    private const val SWP_FRAMECHANGED = 0x0020
    private const val MONITOR_DEFAULTTONEAREST = 2

    private val user32: User32? = runCatching {
        Native.load("user32", User32::class.java)
    }.onFailure { Log.w("cannot load user32", it) }.getOrNull()

    val isAvailable: Boolean get() = user32 != null

    /** Island bounds in client pixels: left, top, right, bottom. */
    @Volatile private var island: IntArray? = null

    // Both references must outlive the hook: JNA callbacks are freed when the
    // Java object is collected, and a freed window procedure crashes the app.
    private var hook: WNDPROC? = null
    private var originalProc: Pointer? = null
    private val lock = Any()

    fun applyInputRegion(geometry: IslandGeometry): Boolean {
        val u = user32 ?: return false
        if (geometry.islandWidthPx <= 0 || geometry.islandHeightPx <= 0) return false

        val left = geometry.paddingPx
        island = intArrayOf(left, 0, left + geometry.islandWidthPx, geometry.islandHeightPx)

        val hwnd = findWindow(u) ?: return false
        synchronized(lock) {
            if (hook != null) return true
            return runCatching {
                val callback = object : WNDPROC {
                    override fun callback(hwnd: Pointer, msg: Int, wParam: Pointer, lParam: Pointer): Pointer {
                        // Never let an exception escape: JNA would swallow it and
                        // report 0 (HTNOWHERE), which would kill all mouse input.
                        return try {
                            if (msg == WM_NCHITTEST && isOutside(u, hwnd, lParam)) {
                                Pointer(HTTRANSPARENT.toLong())
                            } else {
                                forward(u, hwnd, msg, wParam, lParam)
                            }
                        } catch (error: Throwable) {
                            forward(u, hwnd, msg, wParam, lParam)
                        }
                    }
                }
                val previous = u.SetWindowLongPtrW(hwnd, GWLP_WNDPROC, callback)
                if (previous == null || previous == Pointer.NULL) {
                    Log.w("cannot subclass the window procedure")
                    return false
                }
                originalProc = previous
                hook = callback
                Log.i("input region installed")
                true
            }.onFailure { Log.w("cannot install the input region", it) }.getOrDefault(false)
        }
    }

    /**
     * Keeps the island out of the taskbar and Alt+Tab, and re-asserts topmost
     * after the window is mapped (some shells drop it on the first show).
     */
    fun applyWindowHints(): Boolean {
        val u = user32 ?: return false
        val hwnd = findWindow(u) ?: return false
        return runCatching {
            val style = u.GetWindowLongW(hwnd, GWL_EXSTYLE)
            val updated = (style or WS_EX_TOOLWINDOW) and WS_EX_APPWINDOW.inv()
            if (updated != style) u.SetWindowLongW(hwnd, GWL_EXSTYLE, updated)
            u.SetWindowPos(
                hwnd,
                Pointer(-1L), // HWND_TOPMOST
                0, 0, 0, 0,
                SWP_NOMOVE or SWP_NOSIZE or SWP_NOACTIVATE or SWP_FRAMECHANGED,
            )
            true
        }.onFailure { Log.w("window hints failed", it) }.getOrDefault(false)
    }

    fun isActiveWindowFullscreen(): Boolean {
        val u = user32 ?: return false
        val foreground = u.GetForegroundWindow() ?: return false
        if (foreground == Pointer.NULL || belongsToUs(u, foreground)) return false
        // Windows 10+ maximized windows overhang the monitor by a few pixels,
        // so a plain rect comparison would call every maximized window
        // fullscreen. Zoomed windows are the maximized case, not this one.
        if (u.IsZoomed(foreground) != 0) return false
        return runCatching {
            val rect = RECT()
            rect.write()
            if (u.GetWindowRect(foreground, rect.pointer) == 0) return false
            rect.read()

            val monitor = u.MonitorFromWindow(foreground, MONITOR_DEFAULTTONEAREST) ?: return false
            val info = MONITORINFO()
            info.cbSize = info.size()
            info.write()
            if (u.GetMonitorInfoW(monitor, info.pointer) == 0) return false
            info.read()

            rect.left <= info.monitorLeft && rect.top <= info.monitorTop &&
                rect.right >= info.monitorRight && rect.bottom >= info.monitorBottom
        }.getOrDefault(false)
    }

    fun isActiveWindowMaximized(): Boolean {
        val u = user32 ?: return false
        val foreground = u.GetForegroundWindow() ?: return false
        if (foreground == Pointer.NULL || belongsToUs(u, foreground)) return false
        return u.IsZoomed(foreground) != 0
    }

    /** Top-level window handle, used by the brightness lookup. */
    fun hwnd(): Pointer? {
        val u = user32 ?: return null
        return findWindow(u)
    }

    private fun forward(u: User32, hwnd: Pointer, msg: Int, wParam: Pointer, lParam: Pointer): Pointer {
        val previous = originalProc
        if (previous == null || previous == Pointer.NULL) return Pointer(0L)
        return u.CallWindowProcW(previous, hwnd, msg, wParam, lParam)
    }

    private fun isOutside(u: User32, hwnd: Pointer, lParam: Pointer): Boolean {
        val bounds = island ?: return false
        val packed = Pointer.nativeValue(lParam)
        val point = POINT()
        point.x = (packed and 0xFFFFL).toInt().toShort().toInt()
        point.y = ((packed ushr 16) and 0xFFFFL).toInt().toShort().toInt()
        point.write()
        u.ScreenToClient(hwnd, point.pointer)
        point.read()
        return point.x < bounds[0] || point.x >= bounds[2] ||
            point.y < bounds[1] || point.y >= bounds[3]
    }

    private fun belongsToUs(u: User32, hwnd: Pointer): Boolean {
        val owner = IntByReference()
        u.GetWindowThreadProcessId(hwnd, owner)
        return owner.value == ProcessHandle.current().pid().toInt()
    }

    private fun findWindow(u: User32): Pointer? {
        val pid = ProcessHandle.current().pid().toInt()
        var first: Pointer? = null
        var titled: Pointer? = null
        val callback = object : WNDENUMPROC {
            override fun callback(hwnd: Pointer, lParam: Pointer?): Int {
                val owner = IntByReference()
                u.GetWindowThreadProcessId(hwnd, owner)
                if (owner.value != pid || u.IsWindowVisible(hwnd) == 0) return 1

                val buffer = Memory(512L)
                val length = u.GetWindowTextW(hwnd, buffer, 256)
                val title = if (length > 0) buffer.getWideString(0L) else ""
                if (title == "X-Tray") {
                    titled = hwnd
                    return 0
                }
                if (first == null) first = hwnd
                return 1
            }
        }
        u.EnumWindows(callback, null)
        return titled ?: first
    }
}
