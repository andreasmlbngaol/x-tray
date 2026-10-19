package id.andreasmlbngaol.xtray.platform

actual fun currentSessionInfo(): SessionInfo = SessionInfo(
    sessionType = System.getenv("XDG_SESSION_TYPE"),
    desktop = System.getenv("XDG_CURRENT_DESKTOP"),
    displayServer = System.getenv("WAYLAND_DISPLAY"),
)
