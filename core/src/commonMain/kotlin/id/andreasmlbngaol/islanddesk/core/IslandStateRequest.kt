package id.andreasmlbngaol.islanddesk.core

/** External request (IPC, tray) to move the island to a specific state. */
data class IslandStateRequest(
    val id: Long,
    val target: IslandState,
)
