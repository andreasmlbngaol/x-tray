package id.andreasmlbngaol.islanddesk.platform.clipboard

/**
 * Watches the system clipboard. Implementations may poll; the island only
 * keeps entries in memory by default.
 */
interface ClipboardWatcher {
    fun start(onText: (String) -> Unit)
    fun stop()
    fun copy(text: String)
}
