package id.andreasmlbngaol.islanddesk.platform.files

interface FileOpener {
    fun open(path: String): Boolean
    fun revealInFolder(path: String): Boolean
    fun openUrl(url: String): Boolean
}

/**
 * Drag-and-drop on X11 hands us `file:///home/me/My%20Notes.pdf`. Decoding the
 * URI is required before the path can be opened or dragged back out.
 */
expect fun normalizeDroppedPath(raw: String): String
