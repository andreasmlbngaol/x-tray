package id.andreasmlbngaol.islanddesk.ui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.islanddesk.core.IslandModule
import id.andreasmlbngaol.islanddesk.core.ModuleRegistry

/**
 * Placeholder modules for the Phase 1 prototype. Their content is deliberately
 * static; real data sources arrive in Phase 3+.
 */
object MediaModule : IslandModule {
    override val id = "media"
    override val title = "Media"
    override val icon: ImageVector = Icons.Rounded.GraphicEq
    override val priority = 100

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun PeekContent() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Artwork(size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Midnight City",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "M83",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                LinearWavyProgressIndicator(
                    progress = { 0.42f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Putar",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun ExpandedContent() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Artwork(size = 64.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Midnight City",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "M83 — Hurry Up, We're Dreaming",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "Lirik",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LinearWavyProgressIndicator(
                progress = { 0.42f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("1:42", style = MaterialTheme.typography.labelSmall)
                Text("4:03", style = MaterialTheme.typography.labelSmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledIconButton(
                    onClick = {},
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Sebelumnya")
                }
                FilledIconButton(
                    onClick = {},
                    modifier = Modifier.size(52.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Rounded.Pause, contentDescription = "Jeda")
                }
                FilledIconButton(
                    onClick = {},
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Berikutnya")
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = "Sumber: MPRIS • pemutar aktif",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Artwork(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF5B4B8A),
                        Color(0xFFB06A8A),
                        Color(0xFFE0A86A),
                    )
                )
            ),
    )
}

object ClipboardModule : IslandModule {
    override val id = "clipboard"
    override val title = "Clipboard"
    override val icon: ImageVector = Icons.Rounded.ContentPaste
    override val priority = 80

    @Composable
    override fun PeekContent() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(20.dp))
            Column {
                Text("3 item terbaru", style = MaterialTheme.typography.titleSmall)
                Text(
                    "https://kotlinlang.org",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    override fun ExpandedContent() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        "Cari clipboard...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ClipboardRow(Icons.Rounded.Description, "Kode", "fun main() { println(\"hi\") }")
            ClipboardRow(Icons.Rounded.Image, "Gambar", "screenshot-2026-10-19.png")
            ClipboardRow(Icons.Rounded.ContentPaste, "Tautan", "https://kotlinlang.org")
            Text(
                "Semua data tetap lokal. Perekaman bisa dijeda.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ClipboardRow(icon: ImageVector, type: String, preview: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    preview,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.PushPin, contentDescription = "Pin", modifier = Modifier.size(16.dp))
            Icon(Icons.Rounded.Delete, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
        }
    }
}

object ShelfModule : IslandModule {
    override val id = "shelf"
    override val title = "Shelf"
    override val icon: ImageVector = Icons.Rounded.Folder
    override val priority = 60

    @Composable
    override fun PeekContent() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
            Column {
                Text("2 item di shelf", style = MaterialTheme.typography.titleSmall)
                Text(
                    "mockup.png • catatan.txt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    override fun ExpandedContent() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShelfTile("mockup.png", Icons.Rounded.Image, Modifier.weight(1f))
                ShelfTile("catatan.txt", Icons.Rounded.Description, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShelfTile("screenshot.png", Icons.Rounded.Image, Modifier.weight(1f))
                ShelfTile("kosong", Icons.Rounded.Folder, Modifier.weight(1f))
            }
            Text(
                "Tarik file ke sini. Item bertahan setelah restart.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ShelfTile(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(92.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

object ControlsModule : IslandModule {
    override val id = "controls"
    override val title = "Kontrol"
    override val icon: ImageVector = Icons.Rounded.Tune
    override val priority = 40

    @Composable
    override fun PeekContent() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
            Column {
                Text("Volume 45% • Mic mati", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Wi-Fi aktif • Bluetooth mati",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    override fun ExpandedContent() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ControlTile("Volume", "45%", Icons.AutoMirrored.Rounded.VolumeUp, Modifier.weight(1f))
                ControlTile("Kecerahan", "70%", Icons.Rounded.Brightness6, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ControlTile("Wi-Fi", "Aktif", Icons.Rounded.Wifi, Modifier.weight(1f))
                ControlTile("Bluetooth", "Mati", Icons.Rounded.Bluetooth, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ControlTile("Mic", "Mati", Icons.Rounded.Mic, Modifier.weight(1f))
                ControlTile("CPU", "12%", Icons.Rounded.Tune, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ControlTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(value, style = MaterialTheme.typography.titleMediumEmphasized)
        }
    }
}

val DemoModules = ModuleRegistry(
    listOf(
        MediaModule,
        ClipboardModule,
        ShelfModule,
        ControlsModule,
    )
)
