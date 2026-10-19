package id.andreasmlbngaol.islanddesk.platform

data class ScreenBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

data class ScreenPoint(val x: Int, val y: Int)

/**
 * Screen geometry in device-independent AWT coordinates. The usable bounds
 * exclude panels/taskbars, which is what the island should attach to.
 */
expect object DisplayInfo {
    fun primaryBounds(): ScreenBounds
    fun usableBounds(): ScreenBounds
    fun topCenterPosition(width: Int, height: Int, topMargin: Int = 0): ScreenPoint
}
