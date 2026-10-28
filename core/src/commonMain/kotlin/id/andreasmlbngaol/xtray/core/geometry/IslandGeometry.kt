package id.andreasmlbngaol.xtray.core.geometry

/**
 * Pixel geometry of the visible island inside the native window. The app
 * layer uses this to shape the X11 input region.
 *
 * When [captureOutside] is true the island is modal: the window covers the
 * whole monitor and its input region is the full window, so a click next to
 * the panel dismisses it instead of reaching the application underneath.
 */
data class IslandGeometry(
    val windowWidthPx: Int,
    val windowHeightPx: Int,
    val islandWidthPx: Int,
    val islandHeightPx: Int,
    val bottomRadiusPx: Float,
    val paddingPx: Int,
    val captureOutside: Boolean = false,
)
