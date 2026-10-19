package id.andreasmlbngaol.islanddesk.platform.clipboard

import id.andreasmlbngaol.islanddesk.platform.internal.Log
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * AWT clipboard polling. A FlavorListener alone is not reliable on Wayland
 * (XWayland bridges selections), so a light 1.2 s poll keeps the history
 * fresh without measurable CPU cost.
 */
class AwtClipboardWatcher : ClipboardWatcher {
    private val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    private var executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "islanddesk-clipboard").apply { isDaemon = true }
    }
    private var lastSeen: String? = null

    override fun start(onText: (String) -> Unit) {
        lastSeen = readText()
        executor.scheduleWithFixedDelay({
            runCatching {
                val text = readText()
                if (text != null && text != lastSeen) {
                    lastSeen = text
                    onText(text)
                }
            }.onFailure { Log.d("clipboard poll failed: " + it.message) }
        }, 1_200L, 1_200L, TimeUnit.MILLISECONDS)
    }

    override fun stop() {
        executor.shutdownNow()
    }

    override fun copy(text: String) {
        runCatching {
            clipboard.setContents(StringSelection(text), null)
            lastSeen = text
        }.onFailure { Log.w("cannot write clipboard", it) }
    }

    /**
     * Requests only the string flavor. `isDataFlavorAvailable` enumerates every
     * flavor on the clipboard, which makes AWT print ClassNotFoundExceptions
     * for foreign MIME types (for example IntelliJ's serialized objects).
     */
    private fun readText(): String? = runCatching {
        val contents = clipboard.getContents(null) ?: return null
        contents.getTransferData(DataFlavor.stringFlavor) as? String
    }.getOrNull()
}
