package id.andreasmlbngaol.islanddesk.ui.modules

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes a small preview for image files. Returns null for anything that is
 * not a supported image so the caller can fall back to a type icon.
 */
expect fun loadFileThumbnail(path: String): ImageBitmap?
