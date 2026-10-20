package id.andreasmlbngaol.xtray.app

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
import id.andreasmlbngaol.xtray.core.IslandState
import id.andreasmlbngaol.xtray.core.IslandStateRequest
import id.andreasmlbngaol.xtray.core.activity.LiveActivityController
import id.andreasmlbngaol.xtray.core.clipboard.ClipboardHistory
import id.andreasmlbngaol.xtray.core.ipc.IPC_USAGE
import id.andreasmlbngaol.xtray.core.ipc.IpcCommand
import id.andreasmlbngaol.xtray.core.ipc.parseIpcCommand
import id.andreasmlbngaol.xtray.core.notification.NotificationCenter
import id.andreasmlbngaol.xtray.core.settings.SettingsStore
import id.andreasmlbngaol.xtray.core.settings.ThemeMode
import id.andreasmlbngaol.xtray.core.shelf.ShelfStore
import id.andreasmlbngaol.xtray.platform.DisplayInfo
import id.andreasmlbngaol.xtray.platform.SessionInfo
import id.andreasmlbngaol.xtray.platform.autostart.XdgAutostartManager
import id.andreasmlbngaol.xtray.platform.battery.SysfsBatterySource
import id.andreasmlbngaol.xtray.platform.clipboard.AwtClipboardWatcher
import id.andreasmlbngaol.xtray.platform.files.DesktopFileOpener
import id.andreasmlbngaol.xtray.platform.instance.SingleInstance
import id.andreasmlbngaol.xtray.platform.internal.AppPaths
import id.andreasmlbngaol.xtray.platform.internal.Log
import id.andreasmlbngaol.xtray.platform.ipc.IpcServer
import id.andreasmlbngaol.xtray.platform.ipc.sendIpc
import id.andreasmlbngaol.xtray.platform.media.BusctlMediaSource
import id.andreasmlbngaol.xtray.platform.network.SysfsNetworkSpeedSource
import id.andreasmlbngaol.xtray.platform.notifications.DbusNotificationServer
import id.andreasmlbngaol.xtray.platform.settings.SettingsFile
import id.andreasmlbngaol.xtray.platform.system.ShellPowerControls
import id.andreasmlbngaol.xtray.platform.system.ShellSystemControls
import id.andreasmlbngaol.xtray.platform.x11.X11Window
import id.andreasmlbngaol.xtray.ui.island.XTray
import id.andreasmlbngaol.xtray.ui.island.IslandDimens
import id.andreasmlbngaol.xtray.ui.services.IslandServices
import id.andreasmlbngaol.xtray.ui.theme.XTrayTheme
import kotlinx.coroutines.delay
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.MenuItem
import java.awt.MouseInfo
import java.awt.Point
import java.awt.PopupMenu
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    installStderrNoiseFilter()
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
            System.err.println("X-Tray: no running instance")
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
        application { XTrayApplication() }
    } finally {
        instance.release()
    }
}

/**
 * AWT prints "Exception ... while constructing DataFlavor" whenever another
 * application (for example an IDE) puts Java-serialized objects on the
 * clipboard. The flavours are foreign to us and harmless, but the lines look
 * like crashes, so they are dropped from stderr.
 */
private fun installStderrNoiseFilter() {
    val original = System.err
    val filtered = object : java.io.OutputStream() {
        private val line = StringBuilder()

        override fun write(byte: Int) {
            if (byte == '\n'.code) {
                val text = line.toString()
                line.setLength(0)
                if (!text.contains("while constructing DataFlavor")) {
                    original.print(text + "\n")
                }
            } else {
                line.append(byte.toChar())
            }
        }

        override fun flush() {
            original.flush()
        }
    }
    System.setErr(java.io.PrintStream(filtered, true))
}

private fun currentSession(): SessionInfo {
    val session = id.andreasmlbngaol.xtray.platform.currentSessionInfo()
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
 * Wayland sessions. Set XTRAY_RENDER_API=OPENGL to override.
 */
private fun configureRendering(session: SessionInfo) {
    val requested = System.getenv("XTRAY_RENDER_API")?.uppercase()
    val api = requested ?: if (session.isWayland) "SOFTWARE" else "OPENGL"
    System.setProperty("skiko.renderApi", api)
    Log.i("renderApi=" + api + " requested=" + requested)
}

@Composable
private fun ApplicationScope.XTrayApplication() {
    val settings = remember { SettingsStore(SettingsFile()) }
    val services = remember { createServices(settings) }

    var islandState by remember { mutableStateOf(IslandState.Collapsed) }
    var stateRequest by remember { mutableStateOf<IslandStateRequest?>(null) }
    var requestCounter by remember { mutableStateOf(0L) }
    // Right click (or the tray / IPC hide) shrinks the island to the dormant
    // hairline instead of unmapping the window: it stays reachable at the top
    // edge and hovering it opens peek again.
    var hidden by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    var maximized by remember { mutableStateOf(false) }

    // The window is resized under the pointer (collapsed -> peek -> dormant)
    // and Compose loses its hover state on those resizes, so the app layer owns
    // hover: the compositor's enter/exit plus a poll of the X pointer. Both
    // share a [PointerGate] because XWayland freezes the X pointer over native
    // Wayland surfaces and the stale sample must not win over a real exit.
    var islandHovered by remember { mutableStateOf(false) }
    var islandBounds by remember { mutableStateOf(Rectangle(0, 0, 0, 0)) }
    val pointerGate = remember { PointerGate() }

    LaunchedEffect(Unit) {
        var tick = 0
        while (true) {
            tick++
            val pointer = runCatching { MouseInfo.getPointerInfo()?.location }.getOrNull()
            if (pointer != null && pointerGate.last == null) {
                // Baseline only: a sample taken at startup may be stale, so it
                // must not open peek on its own.
                pointerGate.last = pointer
            } else if (pointer != null && pointer != pointerGate.last) {
                pointerGate.last = pointer
                val bounds = islandBounds
                val inside = bounds.width > 0 && bounds.height > 0 && bounds.contains(pointer)
                if (inside != islandHovered) {
                    islandHovered = inside
                    Log.d("hover=" + inside + " pointer=" + pointer.x + "," + pointer.y)
                }
            }
            if (tick % 166 == 0) {
                Log.d(
                    "poll pointer=" + (pointer?.x ?: -1) + "," + (pointer?.y ?: -1) +
                        " bounds=" + islandBounds + " hovered=" + islandHovered,
                )
            }
            delay(60L)
        }
    }

    fun requestState(target: IslandState) {
        requestCounter += 1L
        stateRequest = IslandStateRequest(requestCounter, target)
    }

    LaunchedEffect(Unit) {
        // Test hook: seed the shelf so drag-out can be exercised without a
        // file manager. Comma separated absolute paths.
        System.getenv("XTRAY_SHELF_SEED")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.let { paths ->
                services.shelf.add(paths, System.currentTimeMillis())
                services.bump()
            }
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

    // The island can host org.freedesktop.Notifications itself. GNOME keeps
    // that name on a small helper process which does not allow replacement, so
    // taking over means stopping it; stop() pings the helper's own bus name to
    // start it again when X-Tray exits.
    val replaceNotifications = settings.current.replaceSystemNotifications
    DisposableEffect(replaceNotifications) {
        if (replaceNotifications) {
            val started = services.notificationServer.start(
                onNotification = { incoming ->
                    SwingUtilities.invokeLater {
                        if (!services.settings.current.doNotDisturb) {
                            services.notifications.push(incoming.appName, incoming.title, incoming.body)
                            hidden = false
                        }
                    }
                },
                onClosed = { SwingUtilities.invokeLater { services.notifications.dismiss() } },
            )
            Log.i("notification daemon started=" + started)
        }
        onDispose { services.notificationServer.stop() }
    }

    DisposableEffect(Unit) {
        val server = IpcServer(AppPaths.socket) { command ->
            handleCommand(
                command = command,
                services = services,
                currentState = { islandState },
                requestState = { target -> SwingUtilities.invokeLater { requestState(target) } },
                setHidden = { value -> SwingUtilities.invokeLater { hidden = value } },
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
            onHide = { SwingUtilities.invokeLater { hidden = true } },
            onShow = { SwingUtilities.invokeLater { hidden = false } },
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
        title = "X-Tray",
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
        // AWT enter/exit comes from the compositor, so it still fires when the
        // pointer moves onto a native Wayland surface (where XWayland freezes
        // the X pointer and the poll below stops seeing movement).
        DisposableEffect(window) {
            val listener = object : MouseAdapter() {
                override fun mouseEntered(event: MouseEvent) {
                    Log.d("awt enter " + event.x + "," + event.y + " bounds=" + islandBounds)
                    islandHovered = true
                }

                override fun mouseExited(event: MouseEvent) {
                    // XWayland freezes the X pointer the moment the cursor lands
                    // on a native Wayland surface, so the current sample still
                    // reads as "inside". Mark it as handled; otherwise the poll
                    // below would immediately undo this exit.
                    pointerGate.last =
                        runCatching { MouseInfo.getPointerInfo()?.location }.getOrNull()
                    Log.d("awt exit")
                    islandHovered = false
                }
            }
            window.addMouseListener(listener)
            onDispose { window.removeMouseListener(listener) }
        }

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
                demo = System.getenv("XTRAY_DEMO"),
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
        // `x-tray show` could not bring the island back.
        val systemDark = isSystemInDarkTheme()
        val darkTheme = when (settings.current.themeMode) {
            ThemeMode.System -> systemDark
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }

        XTrayTheme(
            darkTheme = darkTheme,
            accent = settings.current.accentColor,
            customAccent = settings.current.customAccent,
            reduceMotion = settings.current.reduceMotion,
        ) {
            XTray(
                services = services,
                stateRequest = stateRequest,
                initialModuleId = System.getenv("XTRAY_MODULE"),
                dormant = hidden || fullscreen || (settings.current.hideOverMaximized && maximized),
                hovered = islandHovered,
                onStateChange = {
                    islandState = it
                    // Opening peek or expanded means the island is in use again:
                    // dropping back to collapsed must show the normal bar, not
                    // the hairline.
                    if (it != IslandState.Collapsed) hidden = false
                    Log.i("state=" + it)
                },
                onRequestHide = {
                    requestState(IslandState.Collapsed)
                    hidden = true
                    Log.i("island hidden by right click")
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
                    islandBounds = Rectangle(
                        position.x + geometry.paddingPx,
                        position.y,
                        geometry.islandWidthPx,
                        geometry.islandHeightPx,
                    )
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
    notificationServer = DbusNotificationServer(),
)

private fun handleCommand(
    command: IpcCommand,
    services: IslandServices,
    currentState: () -> IslandState,
    requestState: (IslandState) -> Unit,
    setHidden: (Boolean) -> Unit,
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
        setHidden(false)
        "ok"
    }
    IpcCommand.Hide -> {
        setHidden(true)
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
                services.notifications.push("X-Tray", "", command.message)
                setHidden(false)
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

        val icon = TrayIcon(trayImage(), "X-Tray", popup)
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
 * Remembers the last X pointer sample the island already reacted to.
 *
 * XWayland stops updating the X pointer as soon as the cursor is over a native
 * Wayland surface, so the final sample can stay inside the island long after
 * the pointer left. Sharing the sample between the poll and the AWT listener
 * lets the compositor's exit mark it as handled instead of being undone.
 */
private class PointerGate {
    var last: Point? = null
}

/**
 * Temporary verification aid for sessions where synthetic pointer events do
 * not reach the window (XWayland). Set XTRAY_DEMO=cycle to alternate
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
