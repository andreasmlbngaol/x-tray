package id.andreasmlbngaol.islanddesk.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import id.andreasmlbngaol.islanddesk.core.IslandState
import id.andreasmlbngaol.islanddesk.platform.DisplayInfo
import id.andreasmlbngaol.islanddesk.platform.SessionInfo
import id.andreasmlbngaol.islanddesk.platform.currentSessionInfo
import id.andreasmlbngaol.islanddesk.ui.demo.DemoModules
import id.andreasmlbngaol.islanddesk.ui.island.IslandDesk
import id.andreasmlbngaol.islanddesk.ui.island.IslandDimens
import id.andreasmlbngaol.islanddesk.ui.theme.IslandDeskTheme
import javax.swing.SwingUtilities

fun main() {
    val session = currentSessionInfo()
    println(
        "IslandDesk: session=${session.sessionType} desktop=${session.desktop} " +
            "displayServer=${session.displayServer} wayland=${session.isWayland}"
    )
    configureRendering(session)
    application { IslandDeskApp() }
}

/**
 * Skiko's OpenGL backend hangs on XWayland when the window is transparent
 * (swapBuffers never returns, freezing the AWT event queue). The software
 * backend renders this small surface fine, so it is the safe default on
 * Wayland sessions. Set ISLANDDESK_RENDER_API=OPENGL to override.
 */
private fun configureRendering(session: SessionInfo) {
    val requested = System.getenv("ISLANDDESK_RENDER_API")?.uppercase()
    val api = requested ?: if (session.isWayland) "SOFTWARE" else "OPENGL"
    System.setProperty("skiko.renderApi", api)
    println("IslandDesk: renderApi=$api requested=$requested")
}

@Composable
private fun ApplicationScope.IslandDeskApp() {
    var collapseSignal by remember { mutableIntStateOf(0) }
    var activateSignal by remember { mutableIntStateOf(0) }
    var islandState by remember { mutableStateOf(IslandState.Collapsed) }

    val collapsedWindow = IslandDimens.forState(IslandState.Collapsed)
    val windowState = rememberWindowState(
        size = DpSize(
            IslandDimens.windowWidth(collapsedWindow),
            IslandDimens.windowHeight(collapsedWindow),
        ),
        position = WindowPosition.Aligned(Alignment.TopCenter),
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "IslandDesk",
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = true,
        alwaysOnTop = true,
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                collapseSignal++
                true
            } else {
                false
            }
        },
    ) {
        LaunchedEffect(Unit) {
            window.isAutoRequestFocus = false
            startDemoDriver(System.getenv("ISLANDDESK_DEMO"), { activateSignal++ }, { collapseSignal++ })
        }

        val density = LocalDensity.current
        LaunchedEffect(islandState) {
            val size = IslandDimens.forState(islandState)
            val widthPx = with(density) { IslandDimens.windowWidth(size).roundToPx() }
            val heightPx = with(density) { IslandDimens.windowHeight(size).roundToPx() }

            window.setSize(widthPx, heightPx)
            val position = DisplayInfo.topCenterPosition(widthPx, heightPx, topMargin = 0)
            window.setLocation(position.x, position.y)

            println(
                "IslandDesk: state=$islandState window=${window.bounds} " +
                    "screen=${DisplayInfo.primaryBounds()} usable=${DisplayInfo.usableBounds()}"
            )
        }

        IslandDeskTheme(darkTheme = true) {
            IslandDesk(
                registry = DemoModules,
                collapseSignal = collapseSignal,
                activateSignal = activateSignal,
                onStateChange = { islandState = it },
            )
        }
    }
}

/**
 * Temporary verification aid for sessions where synthetic pointer events do
 * not reach the window (XWayland). Set ISLANDDESK_DEMO=cycle to alternate
 * between expanded and collapsed.
 */
private fun startDemoDriver(demo: String?, activate: () -> Unit, collapse: () -> Unit) {
    println("IslandDesk: demo=$demo")
    if (demo != "cycle") return
    println("IslandDesk: demo cycle started")
    Thread {
        while (true) {
            Thread.sleep(1_500)
            println("IslandDesk: demo activate")
            SwingUtilities.invokeLater(activate)
            Thread.sleep(2_500)
            println("IslandDesk: demo collapse")
            SwingUtilities.invokeLater(collapse)
        }
    }.start()
}
