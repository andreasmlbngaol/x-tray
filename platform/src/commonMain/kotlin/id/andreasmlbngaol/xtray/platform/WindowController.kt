package id.andreasmlbngaol.xtray.platform

/** Platform-neutral operations the island needs from its native window. */
interface WindowController {
    fun setAlwaysOnTop(enabled: Boolean)
    fun setPosition(x: Int, y: Int)
    fun setSize(width: Int, height: Int)
    fun requestFocus()
}
