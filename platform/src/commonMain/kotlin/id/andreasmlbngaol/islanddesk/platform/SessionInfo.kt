package id.andreasmlbngaol.islanddesk.platform

data class SessionInfo(
    val sessionType: String?,
    val desktop: String?,
    val displayServer: String?,
) {
    val isWayland: Boolean
        get() = sessionType.equals("wayland", ignoreCase = true) ||
            displayServer.equals("wayland", ignoreCase = true)
}

expect fun currentSessionInfo(): SessionInfo
