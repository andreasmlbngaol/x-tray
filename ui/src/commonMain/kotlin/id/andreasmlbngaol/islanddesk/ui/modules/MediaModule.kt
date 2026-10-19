package id.andreasmlbngaol.islanddesk.ui.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.islanddesk.core.IslandModule
import id.andreasmlbngaol.islanddesk.platform.media.MediaSource
import id.andreasmlbngaol.islanddesk.platform.media.MediaTrack
import id.andreasmlbngaol.islanddesk.ui.services.IslandServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaModule(private val services: IslandServices) : IslandModule {
    override val id = "media"
    override val title = "Media"
    override val icon: ImageVector = Icons.Rounded.GraphicEq
    override val priority = 100

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun PeekContent() {
        val scope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            while (true) {
                refresh()
                delay(1_500L)
            }
        }
        val track = services.mediaTrack
        if (track == null) {
            EmptyPeek()
            return
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Artwork(size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist.ifBlank { track.player },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (track.hasProgress) {
                    LinearWavyProgressIndicator(
                        progress = { track.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.Default) { runCatching { services.media.playPause() } }
                        refresh()
                    }
                },
            ) {
                Icon(
                    imageVector = if (track.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (track.playing) "Jeda" else "Putar",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun ExpandedContent() {
        val scope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            while (true) {
                refresh()
                delay(1_500L)
            }
        }
        val track = services.mediaTrack
        if (track == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicOff,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tidak ada media yang diputar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "MPRIS • busctl",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

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
                        text = track.title,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOf(track.artist, track.album).filter { it.isNotBlank() }.joinToString(" — "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (track.hasProgress) {
                LinearWavyProgressIndicator(
                    progress = { track.progress },
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
                    Text(formatTime(track.positionMs), style = MaterialTheme.typography.labelSmall)
                    Text(formatTime(track.lengthMs), style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Spacer(Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledIconButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.Default) { runCatching { services.media.previous() } }
                            refresh()
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Sebelumnya")
                }
                FilledIconButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.Default) { runCatching { services.media.playPause() } }
                            refresh()
                        }
                    },
                    modifier = Modifier.size(52.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = if (track.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (track.playing) "Jeda" else "Putar",
                    )
                }
                FilledIconButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.Default) { runCatching { services.media.next() } }
                            refresh()
                        }
                    },
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
                text = "Sumber: " + track.player + " • MPRIS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    private suspend fun refresh() {
        services.mediaTrack = withContext(Dispatchers.Default) {
            runCatching { services.media.snapshot() }.getOrNull()
        }
    }
}

@Composable
private fun EmptyPeek() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicOff,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column {
            Text("Tidak ada media", style = MaterialTheme.typography.titleSmall)
            Text(
                "Buka pemutar musik apa pun",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Artwork(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF5B4B8A),
                        Color(0xFFB06A8A),
                        Color(0xFFE0A86A),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = null,
            modifier = Modifier.size(size / 2.4f),
            tint = Color.White.copy(alpha = 0.85f),
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    val paddedSeconds = seconds.toString().padStart(2, '0')
    return if (hours > 0L) {
        hours.toString() + ":" + minutes.toString().padStart(2, '0') + ":" + paddedSeconds
    } else {
        minutes.toString() + ":" + paddedSeconds
    }
}
