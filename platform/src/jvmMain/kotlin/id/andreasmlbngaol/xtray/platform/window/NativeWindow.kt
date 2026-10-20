package id.andreasmlbngaol.xtray.platform.window

import id.andreasmlbngaol.xtray.core.geometry.IslandGeometry
import id.andreasmlbngaol.xtray.platform.internal.Os
import id.andreasmlbngaol.xtray.platform.x11.X11Window

/**
 * Native window shaping for the two things AWT does not expose: pointer
 * transparency outside the island and window hints that keep it out of the
 * taskbar. Linux goes through X11, Windows through user32.
 */
object NativeWindow {
    val isSupported: Boolean
        get() = if (Os.isWindows) Win32Window.isAvailable else X11Window.isAvailable

    fun applyInputRegion(geometry: IslandGeometry): Boolean =
        if (Os.isWindows) Win32Window.applyInputRegion(geometry) else X11Window.applyInputRegion(geometry)

    fun applyWindowHints(): Boolean =
        if (Os.isWindows) Win32Window.applyWindowHints() else X11Window.applyEwmhHints()

    fun isActiveWindowFullscreen(): Boolean =
        if (Os.isWindows) Win32Window.isActiveWindowFullscreen() else X11Window.isActiveWindowFullscreen()

    fun isActiveWindowMaximized(): Boolean =
        if (Os.isWindows) Win32Window.isActiveWindowMaximized() else X11Window.isActiveWindowMaximized()
}
