package id.andreasmlbngaol.islanddesk.platform

import java.awt.GraphicsEnvironment
import java.awt.Toolkit

actual object DisplayInfo {
    actual fun primaryBounds(): ScreenBounds {
        val bounds = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .defaultScreenDevice
            .defaultConfiguration
            .bounds
        return ScreenBounds(bounds.x, bounds.y, bounds.width, bounds.height)
    }

    actual fun usableBounds(): ScreenBounds {
        val configuration = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .defaultScreenDevice
            .defaultConfiguration
        val bounds = configuration.bounds
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
        return ScreenBounds(
            x = bounds.x + insets.left,
            y = bounds.y + insets.top,
            width = bounds.width - insets.left - insets.right,
            height = bounds.height - insets.top - insets.bottom,
        )
    }

    actual fun topCenterPosition(width: Int, height: Int, topMargin: Int): ScreenPoint {
        val usable = usableBounds()
        return ScreenPoint(
            x = usable.x + (usable.width - width) / 2,
            y = usable.y + topMargin,
        )
    }
}
