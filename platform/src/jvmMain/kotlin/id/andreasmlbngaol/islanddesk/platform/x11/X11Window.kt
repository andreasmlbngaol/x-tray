package id.andreasmlbngaol.islanddesk.platform.x11

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.NativeLongByReference
import com.sun.jna.ptr.PointerByReference
import id.andreasmlbngaol.islanddesk.core.geometry.IslandGeometry
import id.andreasmlbngaol.islanddesk.platform.internal.Log
import kotlin.math.max
import kotlin.math.sqrt

private interface X11Library : Library {
    fun XOpenDisplay(name: String?): Pointer?
    fun XCloseDisplay(display: Pointer): Int
    fun XDefaultRootWindow(display: Pointer): Long
    fun XInternAtom(display: Pointer, name: String, onlyIfExists: Int): Long
    fun XFetchName(display: Pointer, window: Long, name: PointerByReference): Int
    fun XFree(data: Pointer?): Int
    fun XFlush(display: Pointer): Int
    fun XChangeProperty(
        display: Pointer,
        window: Long,
        property: Long,
        type: Long,
        format: Int,
        mode: Int,
        data: Pointer?,
        elements: Int,
    ): Int

    fun XGetWindowProperty(
        display: Pointer,
        window: Long,
        property: Long,
        offset: Long,
        length: Long,
        delete: Int,
        requiredType: Long,
        actualType: PointerByReference,
        actualFormat: IntByReference,
        items: NativeLongByReference,
        bytesAfter: NativeLongByReference,
        data: PointerByReference,
    ): Int
}

private interface XextLibrary : Library {
    fun XShapeCombineRectangles(
        display: Pointer,
        window: Long,
        destination: Int,
        xOffset: Int,
        yOffset: Int,
        rectangles: Pointer,
        count: Int,
        operation: Int,
        ordering: Int,
    )
}

@Structure.FieldOrder("x", "y", "width", "height")
class XRectangle : Structure() {
    @JvmField var x: Short = 0
    @JvmField var y: Short = 0
    @JvmField var width: Short = 0
    @JvmField var height: Short = 0
}

/**
 * Minimal X11 helper for the two things AWT does not expose: an input shape
 * (click-through outside the visible island) and EWMH window hints.
 *
 * Every call opens and closes its own Display, which keeps the code safe to
 * use from the EDT and from background threads without global locking.
 */
object X11Window {
    private const val SHAPE_INPUT = 2
    private const val SHAPE_SET = 0
    private const val XA_ATOM = 4L
    private const val PROPERTY_REPLACE = 0
    private const val STRIPS = 24

    private val x11: X11Library? = runCatching {
        Native.load("X11", X11Library::class.java)
    }.onFailure { Log.w("cannot load libX11", it) }.getOrNull()

    private val xext: XextLibrary? = runCatching {
        Native.load("Xext", XextLibrary::class.java)
    }.onFailure { Log.w("cannot load libXext", it) }.getOrNull()

    private val lock = Any()

    val isAvailable: Boolean get() = x11 != null && xext != null

    /**
     * Restricts pointer input to the visible island shape. Areas outside the
     * shape fall through to the window below.
     */
    fun applyInputRegion(geometry: IslandGeometry): Boolean {
        val display = x11 ?: return false
        val ext = xext ?: return false
        synchronized(lock) {
            val d = display.XOpenDisplay(null) ?: return false
            try {
                val window = findWindow(display, d) ?: return false
                val rectangles = buildRectangles(geometry)
                if (rectangles.isEmpty()) return false

                val array = XRectangle().toArray(rectangles.size).map { it as XRectangle }
                rectangles.forEachIndexed { index, rect ->
                    array[index].x = rect[0].toShort()
                    array[index].y = rect[1].toShort()
                    array[index].width = rect[2].toShort()
                    array[index].height = rect[3].toShort()
                    array[index].write()
                }
                ext.XShapeCombineRectangles(
                    d, window, SHAPE_INPUT, 0, 0,
                    array[0].pointer, array.size, SHAPE_SET, 0,
                )
                display.XFlush(d)
                return true
            } catch (error: Throwable) {
                Log.w("XShape failed", error)
                return false
            } finally {
                display.XCloseDisplay(d)
            }
        }
    }

    /**
     * Marks the window as a utility window that stays out of the taskbar and
     * pager. Some window managers only apply these hints at map time.
     */
    fun applyEwmhHints(): Boolean {
        val display = x11 ?: return false
        synchronized(lock) {
            val d = display.XOpenDisplay(null) ?: return false
            try {
                val window = findWindow(display, d) ?: return false

                val typeAtom = display.XInternAtom(d, "_NET_WM_WINDOW_TYPE", 0)
                val utility = display.XInternAtom(d, "_NET_WM_WINDOW_TYPE_UTILITY", 0)
                changeAtoms(display, d, window, typeAtom, longArrayOf(utility))

                val stateAtom = display.XInternAtom(d, "_NET_WM_STATE", 0)
                val states = listOf(
                    "_NET_WM_STATE_ABOVE",
                    "_NET_WM_STATE_SKIP_TASKBAR",
                    "_NET_WM_STATE_SKIP_PAGER",
                    "_NET_WM_STATE_STICKY",
                ).map { display.XInternAtom(d, it, 0) }.toLongArray()
                changeAtoms(display, d, window, stateAtom, states)
                display.XFlush(d)
                return true
            } catch (error: Throwable) {
                Log.w("EWMH hints failed", error)
                return false
            } finally {
                display.XCloseDisplay(d)
            }
        }
    }

    /** True when the currently focused window is fullscreen. */
    fun isActiveWindowFullscreen(): Boolean =
        activeWindowStateFlags("_NET_WM_STATE_FULLSCREEN")

    /**
     * True when the focused window is maximized in both directions. A maximized
     * window puts its tab strip right under the panel, which is exactly where
     * the island sits.
     */
    fun isActiveWindowMaximized(): Boolean =
        activeWindowStateFlags("_NET_WM_STATE_MAXIMIZED_VERT") &&
            activeWindowStateFlags("_NET_WM_STATE_MAXIMIZED_HORZ")

    private fun activeWindowStateFlags(flag: String): Boolean {
        val display = x11 ?: return false
        synchronized(lock) {
            val d = display.XOpenDisplay(null) ?: return false
            try {
                val root = display.XDefaultRootWindow(d)
                val activeAtom = display.XInternAtom(d, "_NET_ACTIVE_WINDOW", 1)
                if (activeAtom == 0L) return false
                val active = readLongs(display, d, root, activeAtom)?.firstOrNull() ?: return false
                if (active == 0L) return false

                // Ignore ourselves: clicking the island must never hide it.
                val pidAtom = display.XInternAtom(d, "_NET_WM_PID", 1)
                if (pidAtom != 0L) {
                    val pid = readLongs(display, d, active, pidAtom)?.firstOrNull()
                    if (pid == ProcessHandle.current().pid()) return false
                }

                val stateAtom = display.XInternAtom(d, "_NET_WM_STATE", 1)
                if (stateAtom == 0L) return false
                val states = readLongs(display, d, active, stateAtom) ?: return false
                val target = display.XInternAtom(d, flag, 1)
                return states.contains(target)
            } catch (error: Throwable) {
                Log.d("window state query failed: " + error.message)
                return false
            } finally {
                display.XCloseDisplay(d)
            }
        }
    }

    private fun buildRectangles(geometry: IslandGeometry): List<IntArray> {
        val islandWidth = geometry.islandWidthPx
        val islandHeight = geometry.islandHeightPx
        val radius = geometry.bottomRadiusPx
        val padding = geometry.paddingPx
        if (islandWidth <= 0 || islandHeight <= 0) return emptyList()

        val strips = STRIPS.coerceAtMost(islandHeight)
        val stripHeight = max(1, islandHeight / strips)
        val result = ArrayList<IntArray>(strips)
        var y = 0
        while (y < islandHeight) {
            val height = minOf(stripHeight, islandHeight - y)
            val bottom = (y + height).toDouble()
            val inset = if (bottom <= islandHeight - radius) {
                0.0
            } else {
                val dy = bottom - (islandHeight - radius)
                radius - sqrt(max(0.0, radius * radius - dy * dy))
            }.toInt()
            val left = padding + inset
            val width = islandWidth - inset * 2
            if (width > 0) {
                result.add(intArrayOf(left, y, width, height))
            }
            y += height
        }
        return result
    }

    private fun changeAtoms(
        display: X11Library,
        d: Pointer,
        window: Long,
        property: Long,
        atoms: LongArray,
    ) {
        val memory = Memory(atoms.size.toLong() * 8L)
        atoms.forEachIndexed { index, atom -> memory.setLong(index.toLong() * 8L, atom) }
        display.XChangeProperty(
            display = d,
            window = window,
            property = property,
            type = XA_ATOM,
            format = 32,
            mode = PROPERTY_REPLACE,
            data = memory,
            elements = atoms.size,
        )
    }

    private fun findWindow(display: X11Library, d: Pointer): Long? {
        val root = display.XDefaultRootWindow(d)
        val pid = ProcessHandle.current().pid()
        val pidAtom = display.XInternAtom(d, "_NET_WM_PID", 1)
        val clientsAtom = display.XInternAtom(d, "_NET_CLIENT_LIST", 1)
        val clients: List<Long> = if (clientsAtom == 0L) {
            emptyList()
        } else {
            readLongs(display, d, root, clientsAtom)?.toList() ?: emptyList()
        }

        if (pidAtom != 0L) {
            for (client in clients) {
                val values = readLongs(display, d, client, pidAtom)
                if (values?.firstOrNull() == pid) return client
            }
        }

        // Fallback: match the WM_NAME we set on the AWT window.
        for (client in clients) {
            val nameRef = PointerByReference()
            if (display.XFetchName(d, client, nameRef) != 0) {
                val title = nameRef.value?.getString(0)
                display.XFree(nameRef.value)
                if (title == "IslandDesk") return client
            }
        }
        return null
    }

    private fun readLongs(
        display: X11Library,
        d: Pointer,
        window: Long,
        property: Long,
    ): LongArray? {
        val actualType = PointerByReference()
        val actualFormat = IntByReference()
        val items = NativeLongByReference()
        val bytesAfter = NativeLongByReference()
        val data = PointerByReference()
        val status = display.XGetWindowProperty(
            d, window, property, 0L, 4096L, 0, 0L,
            actualType, actualFormat, items, bytesAfter, data,
        )
        if (status != 0) return null
        val pointer = data.value ?: return null
        return try {
            val count = items.value.toInt()
            if (actualFormat.value != 32) return null
            LongArray(count) { index -> pointer.getLong(index.toLong() * 8L) }
        } finally {
            display.XFree(pointer)
        }
    }
}
