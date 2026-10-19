package id.andreasmlbngaol.islanddesk.ui.modules

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.io.File

private val imageExtensions = setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")
private const val MAX_THUMBNAIL_BYTES = 12L * 1024L * 1024L

actual fun loadFileThumbnail(path: String): ImageBitmap? {
    val file = File(path)
    if (!file.isFile || file.length() > MAX_THUMBNAIL_BYTES) return null
    if (file.extension.lowercase() !in imageExtensions) return null
    return runCatching {
        Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
    }.getOrNull()
}
