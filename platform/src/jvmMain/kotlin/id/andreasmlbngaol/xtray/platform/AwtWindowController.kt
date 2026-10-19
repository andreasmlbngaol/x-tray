package id.andreasmlbngaol.xtray.platform

import java.awt.Window

class AwtWindowController(
    private val window: Window,
) : WindowController {
    override fun setAlwaysOnTop(enabled: Boolean) {
        window.isAlwaysOnTop = enabled
    }

    override fun setPosition(x: Int, y: Int) {
        window.setLocation(x, y)
    }

    override fun setSize(width: Int, height: Int) {
        window.setSize(width, height)
    }

    override fun requestFocus() {
        window.requestFocus()
    }
}
