package id.andreasmlbngaol.xtray.platform.files

import id.andreasmlbngaol.xtray.platform.internal.Log
import java.awt.Desktop
import java.io.File
import java.net.URI

class DesktopFileOpener : FileOpener {
    private val desktop: Desktop? = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null

    override fun open(path: String): Boolean = runCatching {
        val file = File(path)
        if (!file.exists()) return@runCatching false
        if (desktop != null) {
            desktop.open(file)
        } else {
            ProcessBuilder("xdg-open", path).start()
        }
        true
    }.onFailure { Log.w("cannot open " + path, it) }.getOrDefault(false)

    override fun revealInFolder(path: String): Boolean = runCatching {
        val parent = File(path).parentFile ?: return@runCatching false
        if (desktop != null) {
            desktop.open(parent)
        } else {
            ProcessBuilder("xdg-open", parent.absolutePath).start()
        }
        true
    }.onFailure { Log.w("cannot reveal " + path, it) }.getOrDefault(false)

    override fun openUrl(url: String): Boolean = runCatching {
        if (desktop != null) {
            desktop.browse(URI(url))
        } else {
            ProcessBuilder("xdg-open", url).start()
        }
        true
    }.onFailure { Log.w("cannot open url " + url, it) }.getOrDefault(false)
}
