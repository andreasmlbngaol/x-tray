package id.andreasmlbngaol.islanddesk.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import id.andreasmlbngaol.islanddesk.core.IslandState
import id.andreasmlbngaol.islanddesk.core.IslandStateRequest
import id.andreasmlbngaol.islanddesk.core.activity.LiveActivityController
import id.andreasmlbngaol.islanddesk.core.clipboard.ClipboardHistory
import id.andreasmlbngaol.islanddesk.core.ipc.IPC_USAGE
import id.andreasmlbngaol.islanddesk.core.ipc.IpcCommand
import id.andreasmlbngaol.islanddesk.core.ipc.parseIpcCommand
import id.andreasmlbngaol.islanddesk.core.notification.NotificationCenter
import id.andreasmlbngaol.islanddesk.core.settings.SettingsStore
import id.andreasmlbngaol.islanddesk.core.settings.ThemeMode
import id.andreasmlbngaol.islanddesk.core.shelf.ShelfStore
import id.andreasmlbngaol.islanddesk.platform.DisplayInfo
import id.andreasmlbngaol.islanddesk.platform.SessionInfo
import id.andreasmlbngaol.islanddesk.platform.autostart.XdgAutostartManager
import id.andreasmlbngaol.islanddesk.platform.battery.SysfsBatterySource
import id.andreasmlbngaol.islanddesk.platform.clipboard.AwtClipboardWatcher
import id.andreasmlbngaol.islanddesk.platform.files.DesktopFileOpener
import id.andreasmlbngaol.islanddesk.platform.instance.SingleInstance
import id.andreasmlbngaol.islanddesk.platform.internal.AppPaths
import id.andreasmlbngaol.islanddesk.platform.internal.Log
import id.andreasmlbngaol.islanddesk.platform.ipc.IpcServer
import id.andreasmlbngaol.islanddesk.platform.ipc.sendIpc
import id.andreasmlbngaol.islanddesk.platform.media.BusctlMediaSource
import id.andreasmlbngaol.islanddesk.platform.network.SysfsNetworkSpeedSource
import id.andreasmlbngaol.islanddesk.platform.settings.SettingsFile
import id.andreasmlbngaol.islanddesk.platform.system.ShellPowerControls
import id.andreasmlbngaol.islanddesk.platform.system.ShellSystemControls
import id.andreasmlbngaol.islanddesk.platform.x11.X11Window
import id.andreasmlbngaol.islanddesk.ui.island.IslandDesk
import id.andreasmlbngaol.islanddesk.ui.island.IslandDimens
import id.andreasmlbngaol.islanddesk.ui.services.IslandServices
import id.andreasmlbngaol.islanddesk.ui.theme.IslandDeskTheme
import kotlinx.coroutines.delay
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val session = currentSession()
    configureRendering(session)

    if (args.isNotEmpty()) {
        val command = parseIpcCommand(args.toList())
        if (command == null) {
            System.err.println(IPC_USAGE)
            exitProcess(2)
        }
        val reply = sendIpc(AppPaths.socket, command)
        if (reply == null) {
            System.err.println("IslandDesk: no running instance")
            exitProcess(1)
        }
        println(reply)
        return
    }

    val instance = SingleInstance(AppPaths.lock)
    if (!instance.tryAcquire()) {
        val reply = sendIpc(AppPaths.socket, IpcCommand.Show)
        Log.i("instance already running, forwarded show: " + reply)
        return
    }

    try {
        application { IslandDeskApp() }
    } finally {
        instance.release()
    }
}

private fun currentSession(): SessionInfo {
    val session = id.andreasmlbngaol.islanddesk.platform.currentSessionInfo()
    Log.i(
        "session=" + session.sessionType + " desktop=" + session.desktop +
            " displayServer=" + session.displayServer + " wayland=" + session.isWayland
    )
    return session
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
    Log.i("renderApi=" + api + " requested=" + requested)
}

@Composable
private fun ApplicationScope.IslandDeskApp() {
    val settings = remember { SettingsStore(SettingsFile()) }
    val services = remember { createServices(settings) }

    var islandState by remember { mutableStateOf(IslandState.Collapsed) }
    var stateRequest by remember { mutableStateOf<IslandStateRequest?>(null) }
    var requestCounter by remember { mutableStateOf(0L) }
    var visible by remember { mutableStateOf(true) }
    var fullscreen by remember { mutableStateOf(false) }
    var maximized by remember { mutableStateOf(false) }

    fun requestState(target: IslandState) {
        requestCounter += 1L
        stateRequest = IslandStateRequest(requestCounter, target)
    }

    LaunchedEffect(Unit) {
        services.refreshBattery()
        services.refreshSystem()
        if (settings.current.startAtLogin && !services.autostart.isEnabled()) {
            services.autostart.setEnabled(true)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            services.refreshBattery()
            delay(30_000L)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            services.refreshNetwork()
            delay(1_000L)
        }
    }

    DisposableEffect(Unit) {
        services.clipboard.start { text ->
            SwingUtilities.invokeLater {
                if (services.settings.current.clipboardEnabled && !services.clipboardPaused) {
                    services.clipboardHistory.add(text, System.currentTimeMillis())
                    services.bump()
                }
            }
        }
        onDispose { services.clipboard.stop() }
    }

    LaunchedEffect(Unit) {
        while (true) {
            fullscreen = runCatching { X11Window.isActiveWindowFullscreen() }.getOrDefault(false)
            maximized = runCatching { X11Window.isActiveWindowMaximized() }.getOrDefault(false)
            delay(1_000L)
        }
    }

    val notification = services.notifications.current
    LaunchedEffect(notification?.id) {
        if (notification != null) {
            delay(6_000L)
            if (services.notifications.current?.id == notification.id) {
                services.notifications.dismiss()
            }
        }
    }

    DisposableEffect(Unit) {
        val server = IpcServer(AppPaths.socket) { command ->
            handleCommand(
                command = command,
                services = services,
                currentState = { islandState },
                requestState = { target -> SwingUtilities.invokeLater { requestState(target) } },
                setVisible = { value -> SwingUtilities.invokeLater { visible = value } },
                quit = { SwingUtilities.invokeLater { exitApplication() } },
            )
        }
        server.start()
        onDispose { server.stop() }
    }

    DisposableEffect(Unit) {
        val tray = installTray(
            onToggle = { SwingUtilities.invokeLater { requestState(IslandState.Expanded) } },
            onCollapse = { SwingUtilities.invokeLater { requestState(IslandState.Collapsed) } },
            onHide = { SwingUtilities.invokeLater { visible = false } },
            onShow = { SwingUtilities.invokeLater { visible = true } },
            onQuit = { SwingUtilities.invokeLater { exitApplication() } },
            isDnd = { settings.current.doNotDisturb },
            setDnd = { value -> SwingUtilities.invokeLater { settings.update { it.copy(doNotDisturb = value) } } },
        )
        onDispose { tray?.let { runCatching { SystemTray.getSystemTray().remove(it) } } }
    }

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
                requestState(IslandState.Collapsed)
                true
            } else {
                false
            }
        },
    ) {
        LaunchedEffect(Unit) {
            window.isAutoRequestFocus = false
            window.addWindowFocusListener(object : java.awt.event.WindowFocusListener {
                override fun windowGainedFocus(event: java.awt.event.WindowEvent?) = Unit
                override fun windowLostFocus(event: java.awt.event.WindowEvent?) {
                    if (islandState == IslandState.Expanded) {
                        requestState(IslandState.Collapsed)
                    }
                }
            })
            repeat(6) {
                delay(250L)
                X11Window.applyEwmhHints()
            }
            startDemoDriver(
                demo = System.getenv("ISLANDDESK_DEMO"),
                expand = { SwingUtilities.invokeLater { requestState(IslandState.Expanded) } },
                collapse = { SwingUtilities.invokeLater { requestState(IslandState.Collapsed) } },
            )
        }

        fun placeWindow() {
            val position = DisplayInfo.topCenterPosition(
                width = window.width,
                height = window.height,
                topMargin = 0,
                monitorIndex = settings.current.monitorIndex,
            )
            window.setLocation(position.x, position.y)
        }

        val monitorIndex = settings.current.monitorIndex
        LaunchedEffect(monitorIndex) {
            placeWindow()
        }

        // Visibility is applied from a plain polling loop rather than a
        // LaunchedEffect keyed on state: while the window is hidden Compose may
        // stop requesting frames, so a keyed effect would never run again and
        // `islanddesk show` could not bring the island back.
        LaunchedEffect(Unit) {
            while (true) {
                val covered = fullscreen ||
                    (settings.current.hideOverMaximized && maximized)
                val shouldShow = visible && !covered
                if (window.isVisible != shouldShow) {
                    window.isVisible = shouldShow
                    Log.i("window shown=" + shouldShow + " covered=" + covered)
                }
                delay(300L)
            }
        }

        val systemDark = isSystemInDarkTheme()
        val darkTheme = when (settings.current.themeMode) {
            ThemeMode.System -> systemDark
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }

        IslandDeskTheme(
            darkTheme = darkTheme,
            accent = settings.current.accentColor,
            reduceMotion = settings.current.reduceMotion,
        ) {
            IslandDesk(
                services = services,
                stateRequest = stateRequest,
                initialModuleId = System.getenv("ISLANDDESK_MODULE"),
                onStateChange = {
                    islandState = it
                    Log.i("state=" + it)
                },
                onGeometryChanged = { geometry ->
                    window.setSize(geometry.windowWidthPx, geometry.windowHeightPx)
                    val position = DisplayInfo.topCenterPosition(
                        width = geometry.windowWidthPx,
                        height = geometry.windowHeightPx,
                        topMargin = 0,
                        monitorIndex = settings.current.monitorIndex,
                    )
                    window.setLocation(position.x, position.y)
                    Log.d(
                        "geometry window=" + geometry.windowWidthPx + "x" + geometry.windowHeightPx +
                            " island=" + geometry.islandWidthPx + "x" + geometry.islandHeightPx +
                            " at " + position.x + "," + position.y
                    )
                    SwingUtilities.invokeLater {
                        if (!X11Window.applyInputRegion(geometry)) {
                            Log.d("input region not applied")
                        }
                    }
                },
            )
        }
    }
}

private fun createServices(settings: SettingsStore): IslandServices = IslandServices(
    settings = settings,
    media = BusctlMediaSource(),
    clipboard = AwtClipboardWatcher(),
    clipboardHistory = ClipboardHistory(maxItems = 50),
    shelf = ShelfStore(),
    battery = SysfsBatterySource(),
    controls = ShellSystemControls(),
    activity = LiveActivityController { System.currentTimeMillis() },
    notifications = NotificationCenter(),
    autostart = XdgAutostartManager(),
    fileOpener = DesktopFileOpener(),
    network = SysfsNetworkSpeedSource(),
    power = ShellPowerControls(),
)

private fun handleCommand(
    command: IpcCommand,
    services: IslandServices,
    currentState: () -> IslandState,
    requestState: (IslandState) -> Unit,
    setVisible: (Boolean) -> Unit,
    quit: () -> Unit,
): String = when (command) {
    IpcCommand.Toggle -> {
        requestState(if (currentState() == IslandState.Expanded) IslandState.Collapsed else IslandState.Expanded)
        "ok"
    }
    IpcCommand.Expand -> {
        requestState(IslandState.Expanded)
        "ok"
    }
    IpcCommand.Collapse -> {
        requestState(IslandState.Collapsed)
        "ok"
    }
    IpcCommand.Show -> {
        setVisible(true)
        "ok"
    }
    IpcCommand.Hide -> {
        setVisible(false)
        "ok"
    }
    IpcCommand.Quit -> {
        quit()
        "ok"
    }
    IpcCommand.State -> currentState().name
    is IpcCommand.Notify -> {
        if (services.settings.current.doNotDisturb) {
            "suppressed"
        } else {
            SwingUtilities.invokeLater {
                services.notifications.push(command.message)
                setVisible(true)
            }
            "ok"
        }
    }
    is IpcCommand.StartActivity -> {
        SwingUtilities.invokeLater {
            services.activity.start(command.title, command.minutes)
            requestState(IslandState.Expanded)
        }
        "ok"
    }
    IpcCommand.StopActivity -> {
        SwingUtilities.invokeLater { services.activity.stop() }
        "ok"
    }
    is IpcCommand.SetDnd -> {
        SwingUtilities.invokeLater {
            services.settings.update { it.copy(doNotDisturb = command.enabled) }
        }
        "ok"
    }
}

private fun installTray(
    onToggle: () -> Unit,
    onCollapse: () -> Unit,
    onHide: () -> Unit,
    onShow: () -> Unit,
    onQuit: () -> Unit,
    isDnd: () -> Boolean,
    setDnd: (Boolean) -> Unit,
): TrayIcon? {
    if (!SystemTray.isSupported()) {
        Log.w("system tray not supported")
        return null
    }
    return runCatching {
        val popup = PopupMenu()
        popup.add(MenuItem("Open panel").apply { addActionListener { onToggle() } })
        popup.add(MenuItem("Collapse").apply { addActionListener { onCollapse() } })
        popup.add(MenuItem("Hide island").apply { addActionListener { onHide() } })
        popup.add(MenuItem("Show island").apply { addActionListener { onShow() } })
        popup.addSeparator()
        popup.add(
            java.awt.CheckboxMenuItem("Do not disturb", isDnd()).apply {
                addItemListener { event ->
                    setDnd((event.source as java.awt.CheckboxMenuItem).state)
                }
            }
        )
        popup.addSeparator()
        popup.add(MenuItem("Quit").apply { addActionListener { onQuit() } })

        val icon = TrayIcon(trayImage(), "IslandDesk", popup)
        icon.isImageAutoSize = true
        SystemTray.getSystemTray().add(icon)
        icon
    }.onFailure { Log.w("cannot install tray icon", it) }.getOrNull()
}

private fun trayImage(): BufferedImage {
    val size = 32
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    graphics.color = Color(0x08, 0x07, 0x0A)
    graphics.fillRoundRect(2, 8, 28, 16, 16, 16)
    graphics.color = Color(0xA9, 0xC7, 0xFF)
    graphics.fillOval(13, 14, 6, 6)
    graphics.dispose()
    return image
}

/**
 * Temporary verification aid for sessions where synthetic pointer events do
 * not reach the window (XWayland). Set ISLANDDESK_DEMO=cycle to alternate
 * between expanded and collapsed.
 */
private fun startDemoDriver(
    demo: String?,
    expand: () -> Unit,
    collapse: () -> Unit,
) {
    Log.i("demo=" + demo)
    if (demo != "cycle") return
    Thread {
        while (true) {
            Thread.sleep(1_500L)
            expand()
            Thread.sleep(3_000L)
            collapse()
        }
    }.apply { isDaemon = true }.start()
}
