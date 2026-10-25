package id.andreasmlbngaol.xtray.platform.notifications

import id.andreasmlbngaol.xtray.platform.internal.Log
import id.andreasmlbngaol.xtray.platform.internal.Shell
import org.freedesktop.dbus.Struct
import org.freedesktop.dbus.types.Variant
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

/**
 * Finds a small PNG for the application that sent a notification.
 *
 * Senders are inconsistent: some put a file path in app_icon, others only send
 * a themed icon name or a desktop-entry hint, and many send raw ARGB pixels in
 * the image-data hint. The resolver tries those in order and falls back to the
 * icon named by the sender's .desktop file.
 */
internal object NotificationIconResolver {
    private const val MAX_ICON_BYTES = 4L * 1024L * 1024L

    private val cache = HashMap<String, ByteArray?>()
    private val missing = HashSet<String>()

    fun resolve(appIcon: String, hints: Map<String, Variant<*>>): ByteArray? {
        imageData(hints)?.let { return it }

        (hintString(hints, "image-path") ?: hintString(hints, "image_path"))?.let { path ->
            readImageFile(path)?.let { return it }
        }

        val desktopEntry = hintString(hints, "desktop-entry")
            ?: hintString(hints, "desktop_entry")
        val keys = buildList {
            if (appIcon.isNotBlank()) add(appIcon.trim())
            if (!desktopEntry.isNullOrBlank()) add(desktopEntry.trim())
        }

        for (key in keys) {
            readImageFile(key)?.let { return it }
            if (key.startsWith("/")) continue
            findIconFile(key)?.let { return it }
        }

        val themed = desktopEntry?.let { desktopIconName(it) }
        if (themed != null) {
            findIconFile(themed)?.let { return it }
        }
        return null
    }

    private fun hintString(hints: Map<String, Variant<*>>, key: String): String? =
        (hints[key]?.value as? String)?.trim()?.ifBlank { null }

    /**
     * D-Bus image-data is (width, height, rowstride, has_alpha, bits_per_sample,
     * channels, data) with ARGB32 bytes in network order.
     */
    private fun imageData(hints: Map<String, Variant<*>>): ByteArray? {
        val raw = hints["image-data"]?.value
            ?: hints["icon_data"]?.value
            ?: return null
        val parts: List<Any?> = when (raw) {
            is Array<*> -> raw.toList()
            is List<*> -> raw
            is Struct -> raw.parameters.toList()
            else -> return null
        }
        if (parts.size < 7) return null

        val width = (parts[0] as? Number)?.toInt() ?: return null
        val height = (parts[1] as? Number)?.toInt() ?: return null
        val rowStride = (parts[2] as? Number)?.toInt() ?: return null
        val hasAlpha = parts[3] as? Boolean ?: true
        val bitsPerSample = (parts[4] as? Number)?.toInt() ?: 8
        val channels = (parts[5] as? Number)?.toInt() ?: 4
        val data = when (val value = parts[6]) {
            is ByteArray -> value
            is List<*> -> value.mapNotNull { (it as? Number)?.toByte() }.toByteArray()
            else -> return null
        }
        if (width <= 0 || height <= 0 || width > 1024 || height > 1024) return null
        if (bitsPerSample != 8 || channels != 4) return null
        if (rowStride < width * 4 || data.size < rowStride.toLong() * height) return null

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            var offset = y * rowStride
            for (x in 0 until width) {
                val alpha = if (hasAlpha) data[offset].toInt() and 0xFF else 0xFF
                val red = data[offset + 1].toInt() and 0xFF
                val green = data[offset + 2].toInt() and 0xFF
                val blue = data[offset + 3].toInt() and 0xFF
                image.setRGB(x, y, (alpha shl 24) or (red shl 16) or (green shl 8) or blue)
                offset += 4
            }
        }
        return runCatching {
            ByteArrayOutputStream().use { output ->
                ImageIO.write(image, "png", output)
                output.toByteArray()
            }
        }.onFailure { Log.w("notifications: cannot encode icon", it) }.getOrNull()
    }

    private fun readImageFile(path: String): ByteArray? {
        val file = File(path.removePrefix("file://"))
        if (!file.isFile || file.length() > MAX_ICON_BYTES) return null
        val extension = file.extension.lowercase()
        if (extension == "svg") return rasterizeSvg(file)
        if (extension.isNotEmpty() && extension !in setOf("png", "jpg", "jpeg", "webp", "bmp")) {
            return null
        }
        return runCatching { file.readBytes() }.getOrNull()
    }

    /**
     * Compose can decode PNG/JPEG/WebP but not SVG, and many Flatpak apps ship
     * only a scalable icon. Rasterize it with whichever desktop tool exists.
     */
    private fun rasterizeSvg(file: File): ByteArray? {
        val output = runCatching { Files.createTempFile("x-tray-icon", ".png") }.getOrNull() ?: return null
        try {
            val target = output.toAbsolutePath().toString()
            val commands = buildList {
                if (Shell.exists("gdk-pixbuf-thumbnailer")) {
                    add(arrayOf("gdk-pixbuf-thumbnailer", "-s", "128", file.absolutePath, target))
                }
                if (Shell.exists("rsvg-convert")) {
                    add(arrayOf("rsvg-convert", "-w", "128", "-h", "128", "-o", target, file.absolutePath))
                }
                if (Shell.exists("convert")) {
                    add(
                        arrayOf(
                            "convert", "-background", "none", "-density", "192",
                            file.absolutePath, "-resize", "128x128", target,
                        ),
                    )
                }
            }
            for (command in commands) {
                Shell.run(*command, timeoutMs = 5_000L)
                if (Files.size(output) > 0L) return Files.readAllBytes(output)
            }
            return null
        } finally {
            runCatching { Files.deleteIfExists(output) }
        }
    }

    private fun findIconFile(name: String): ByteArray? {
        val trimmed = name.trim()
            .removeSuffix(".png")
            .removeSuffix(".svg")
            .removeSuffix(".xpm")
        if (trimmed.isBlank() || trimmed.contains('/')) return null
        synchronized(cache) {
            cache[trimmed]?.let { return it }
            if (missing.contains(trimmed)) return null
        }

        val file = iconCandidates(trimmed).firstOrNull { it.isFile }
        val bytes = file?.let { readImageFile(it.absolutePath) }
        synchronized(cache) {
            if (bytes != null) cache[trimmed] = bytes else missing.add(trimmed)
        }
        return bytes
    }

    private fun iconCandidates(name: String): Sequence<File> = sequence {
        val sizes = listOf(
            "512x512", "256x256", "128x128", "96x96", "64x64",
            "48x48", "32x32", "24x24", "16x16", "scalable",
        )
        for (root in iconRoots()) {
            for (theme in themeNames(root)) {
                for (size in sizes) {
                    val apps = File(File(root, theme), size).resolve("apps")
                    yield(apps.resolve("$name.png"))
                    yield(apps.resolve("$name.svg"))
                }
            }
            yield(File(root, "pixmaps/$name.png"))
            yield(File(root, "pixmaps/$name.svg"))
            yield(File(root, "$name.png"))
            yield(File(root, "$name.svg"))
        }
        for (root in iconRoots()) {
            if (!root.isDirectory) continue
            val found = runCatching {
                val stream = Files.walk(root.toPath(), 4)
                try {
                    stream
                        .filter {
                            val fileName = it.fileName.toString()
                            fileName == "$name.png" || fileName == "$name.svg"
                        }
                        .findFirst()
                        .orElse(null)
                        ?.toFile()
                } finally {
                    stream.close()
                }
            }.getOrNull()
            if (found != null) yield(found)
        }
    }

    private fun themeNames(root: File): List<String> {
        val preferred = listOf("hicolor", "Adwaita", "Yaru", "breeze", "breeze-dark", "Papirus")
        val installed = runCatching {
            root.listFiles { file -> file.isDirectory }
                ?.map { it.name }
                .orEmpty()
        }.getOrDefault(emptyList())
        return (preferred + installed).distinct()
    }

    private fun iconRoots(): List<File> {
        val dataDirs = buildList {
            val home = System.getProperty("user.home").orEmpty()
            add(File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share"))
            val system = System.getenv("XDG_DATA_DIRS") ?: "/usr/local/share:/usr/share"
            system.split(':').filter { it.isNotBlank() }.forEach { add(File(it)) }
            add(File("$home/.local/share/flatpak/exports/share"))
            add(File("/var/lib/flatpak/exports/share"))
            add(File("/var/lib/snapd/desktop"))
            add(File("$home/.icons"))
        }
        return dataDirs.map { File(it, "icons") } + dataDirs.map { File(it, "pixmaps") }
    }

    /** Human-readable application name from its desktop entry, when present. */
    fun appName(entry: String): String? = desktopEntryValue(entry, "Name")

    private fun desktopIconName(entry: String): String? = desktopEntryValue(entry, "Icon")

    private fun desktopEntryValue(entry: String, key: String): String? {
        val id = if (entry.endsWith(".desktop")) entry else "$entry.desktop"
        for (dir in applicationDirs()) {
            val file = File(dir, id)
            if (!file.isFile) continue
            val value = runCatching {
                file.useLines { lines ->
                    lines.firstOrNull { it.startsWith("$key=") }?.removePrefix("$key=")?.trim()
                }
            }.getOrNull()
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    private fun applicationDirs(): List<File> {
        val home = System.getProperty("user.home").orEmpty()
        val dataDirs = buildList {
            add(File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share"))
            val system = System.getenv("XDG_DATA_DIRS") ?: "/usr/local/share:/usr/share"
            system.split(':').filter { it.isNotBlank() }.forEach { add(File(it)) }
        }
        return dataDirs.map { File(it, "applications") } + listOf(
            File("$home/.local/share/flatpak/exports/share/applications"),
            File("/var/lib/flatpak/exports/share/applications"),
            File("/var/lib/snapd/desktop/applications"),
        )
    }
}
