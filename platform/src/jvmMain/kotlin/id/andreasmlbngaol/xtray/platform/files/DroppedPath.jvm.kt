package id.andreasmlbngaol.xtray.platform.files

import java.net.URI
import java.nio.file.Paths

actual fun normalizeDroppedPath(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return trimmed
    return if (trimmed.startsWith("file:", ignoreCase = true)) {
        runCatching { Paths.get(URI(trimmed)).toString() }.getOrDefault(trimmed)
    } else {
        trimmed
    }
}
