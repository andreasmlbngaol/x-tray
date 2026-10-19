package id.andreasmlbngaol.xtray.ui.modules

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.xtray.core.IslandModule
import id.andreasmlbngaol.xtray.core.shelf.ShelfItem
import id.andreasmlbngaol.xtray.ui.services.IslandServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShelfModule(private val services: IslandServices) : IslandModule {
    override val id = "shelf"
    override val title = "Shelf"
    override val icon: ImageVector = Icons.Rounded.Folder
    override val priority = 60

    @Composable
    override fun PeekContent() {
        val items = services.shelf.items()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (items.isEmpty()) "Shelf is empty" else items.size.toString() + " items on the shelf",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = if (items.isEmpty()) "Drop files onto the island" else items.joinToString(" • ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    @Composable
    override fun ExpandedContent() {
        val items = services.shelf.items()
        // Hover lives here, not inside the lazy item: a tile that is
        // recomposed (clock tick, new thumbnail) would otherwise lose its
        // hover state and the action buttons would flicker away.
        var hoveredPath by remember { mutableStateOf<String?>(null) }
        Column(modifier = Modifier.fillMaxSize()) {
            if (items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ShapeAccent(size = 78.dp) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Shelf is empty",
                        style = MaterialTheme.typography.titleMediumEmphasized,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Drag files onto the island to keep them here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = items.size.toString() + " files",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )

                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = items, key = { it.path }) { item ->
                        ShelfTile(
                            item = item,
                            hovered = hoveredPath == item.path,
                            onHoverChange = { inside ->
                                hoveredPath = if (inside) item.path else hoveredPath.takeIf { it != item.path }
                            },
                            onOpen = { services.fileOpener.open(item.path) },
                            onRemove = {
                                services.shelf.remove(item.path)
                                services.bump()
                            },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                ShelfHint()
            }
        }
    }
}

/**
 * A shelf entry shown as a large thumbnail. Images and videos fill the tile;
 * anything else falls back to its icon plus the file name. Open and delete
 * appear on hover so the tile itself stays a clean preview and can still be
 * dragged out to another application.
 */
/** Three short lines instead of one cramped sentence. */
@Composable
private fun ShelfHint() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Hover a tile to open or delete it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Drag a tile out to copy the file",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "The shelf is kept until X-Tray closes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ShelfTile(
    item: ShelfItem,
    hovered: Boolean,
    onHoverChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = item.path) {
        value = withContext(Dispatchers.IO) { loadFileThumbnail(item.path) }
    }
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .onPointerEvent(PointerEventType.Enter) { onHoverChange(true) }
            .onPointerEvent(PointerEventType.Exit) { onHoverChange(false) }
            .fileDragSource(item.path),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val image = bitmap
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0f),
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f),
                                )
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = fileIcon(item.name),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (hovered) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(
                            onClick = onOpen,
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(13.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = "Open",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Remove",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun fileIcon(name: String): ImageVector {
    val extension = name.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "png", "jpg", "jpeg", "webp", "gif", "bmp", "svg" -> Icons.Rounded.Image
        "mp4", "mkv", "webm", "avi", "mov" -> Icons.Rounded.Movie
        "mp3", "flac", "ogg", "wav", "m4a" -> Icons.Rounded.AudioFile
        "pdf" -> Icons.Rounded.PictureAsPdf
        "zip", "tar", "gz", "xz", "7z", "rar" -> Icons.Rounded.FolderZip
        "txt", "md", "kt", "kts", "json", "yml", "yaml", "toml", "sh" -> Icons.Rounded.Description
        else -> Icons.AutoMirrored.Rounded.InsertDriveFile
    }
}
