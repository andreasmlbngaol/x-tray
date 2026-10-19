package id.andreasmlbngaol.xtray.platform

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

    actual fun screens(): List<ScreenBounds> =
        GraphicsEnvironment.getLocalGraphicsEnvironment()
            .screenDevices
            .map { device ->
                val bounds = device.defaultConfiguration.bounds
                ScreenBounds(bounds.x, bounds.y, bounds.width, bounds.height)
            }

    actual fun usableBounds(monitorIndex: Int): ScreenBounds {
        val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val devices = environment.screenDevices
        if (devices.isEmpty()) return ScreenBounds(0, 0, 0, 0)
        val device = devices[monitorIndex.coerceIn(0, devices.lastIndex)]
        val configuration = device.defaultConfiguration
        val bounds = configuration.bounds
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
        return ScreenBounds(
            x = bounds.x + insets.left,
            y = bounds.y + insets.top,
            width = bounds.width - insets.left - insets.right,
            height = bounds.height - insets.top - insets.bottom,
        )
    }

    actual fun topCenterPosition(
        width: Int,
        height: Int,
        topMargin: Int,
        monitorIndex: Int,
    ): ScreenPoint {
        val usable = usableBounds(monitorIndex)
        return ScreenPoint(
            x = usable.x + (usable.width - width) / 2,
            y = usable.y + topMargin,
        )
    }
}
