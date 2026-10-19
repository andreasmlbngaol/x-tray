package id.andreasmlbngaol.xtray.ui.modules

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import id.andreasmlbngaol.xtray.platform.internal.Log
import org.jetbrains.skia.Image
import java.io.File
import java.util.concurrent.TimeUnit

private val imageExtensions = setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")
private val videoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov", "m4v")
private const val MAX_THUMBNAIL_BYTES = 16L * 1024L * 1024L

private val ffmpegAvailable: Boolean by lazy {
    val path = System.getenv("PATH").orEmpty()
    path.split(File.pathSeparatorChar).any { File(it, "ffmpeg").canExecute() }
}

/** Small LRU so scrolling the shelf does not re-decode the same files. */
private val cache = object : LinkedHashMap<String, ImageBitmap?>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap?>?): Boolean =
        size > 24
}

actual fun loadFileThumbnail(path: String): ImageBitmap? {
    synchronized(cache) {
        if (cache.containsKey(path)) return cache[path]
    }
    val result = runCatching { decode(path) }
        .onFailure { Log.w("thumbnail failed for " + path, it) }
        .getOrNull()
    synchronized(cache) { cache[path] = result }
    return result
}

private fun decode(path: String): ImageBitmap? {
    val file = File(path)
    if (!file.isFile) return null
    val extension = file.extension.lowercase()
    val bytes = when (extension) {
        in imageExtensions -> {
            if (file.length() > MAX_THUMBNAIL_BYTES) return null
            file.readBytes()
        }
        in videoExtensions -> extractVideoFrame(file) ?: return null
        else -> return null
    }
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}

/** First frame at ~1 s, scaled down so decoding stays cheap. */
private fun extractVideoFrame(file: File): ByteArray? {
    if (!ffmpegAvailable) return null
    fun run(seek: String): ByteArray? = runCatching {
        val process = ProcessBuilder(
            "ffmpeg", "-v", "error", "-ss", seek, "-i", file.absolutePath,
            "-frames:v", "1", "-vf", "scale=360:-2",
            "-f", "image2pipe", "-vcodec", "png", "-",
        ).redirectErrorStream(false).start()
        val bytes = process.inputStream.use { it.readBytes() }
        if (!process.waitFor(6, TimeUnit.SECONDS)) {
            process.destroy()
            return null
        }
        bytes.takeIf { it.isNotEmpty() }
    }.getOrNull()

    return run("00:00:01") ?: run("00:00:00")
}
