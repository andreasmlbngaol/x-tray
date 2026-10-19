package id.andreasmlbngaol.islanddesk.core.geometry

/**
 * Pixel geometry of the visible island inside the native window. The app
 * layer uses this to shape the X11 input region.
 */
data class IslandGeometry(
    val windowWidthPx: Int,
    val windowHeightPx: Int,
    val islandWidthPx: Int,
    val islandHeightPx: Int,
    val bottomRadiusPx: Float,
    val paddingPx: Int,
)
