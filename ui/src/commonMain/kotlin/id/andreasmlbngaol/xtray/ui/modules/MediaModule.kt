package id.andreasmlbngaol.xtray.ui.modules

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.xtray.ui.services.IslandServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The full player: artwork, title, artist, album, wavy progress and the
 * transport group. It sits at the bottom of the Controls tab, so media is one
 * click away instead of behind a separate rail entry.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MediaPlayerCard(services: IslandServices, modifier: Modifier = Modifier) {
    val track = services.mediaTrack ?: return
    val scope = rememberCoroutineScope()

    fun command(block: () -> Unit) {
        scope.launch {
            withContext(Dispatchers.Default) { runCatching(block) }
            services.mediaTrack = withContext(Dispatchers.Default) {
                runCatching { services.media.snapshot() }.getOrNull()
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MediaArtwork(player = track.player, size = 54.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist.ifBlank { track.player },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (track.album.isNotBlank()) {
                        Text(
                            text = track.album,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                FilledIconButton(
                    onClick = { command { services.media.playPause() } },
                    shapes = IconButtonShapes(
                        shape = RoundedCornerShape(50),
                        pressedShape = RoundedCornerShape(14.dp),
                    ),
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = if (track.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (track.playing) "Pause" else "Play",
                    )
                }
            }

            if (track.hasProgress) {
                LinearWavyProgressIndicator(
                    progress = { track.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatMediaTime(track.positionMs),
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatMediaTime(track.lengthMs),
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val previousSource = remember { MutableInteractionSource() }
                val nextSource = remember { MutableInteractionSource() }
                ButtonGroup(
                    overflowIndicator = {},
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    customItem(
                        buttonGroupContent = {
                            TransportButton(
                                icon = Icons.Rounded.SkipPrevious,
                                description = "Previous",
                                modifier = Modifier
                                    .weight(1f)
                                    .animateWidth(previousSource),
                                interactionSource = previousSource,
                                onClick = { command { services.media.previous() } },
                            )
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            TransportButton(
                                icon = Icons.Rounded.SkipNext,
                                description = "Next",
                                modifier = Modifier
                                    .weight(1f)
                                    .animateWidth(nextSource),
                                interactionSource = nextSource,
                                onClick = { command { services.media.next() } },
                            )
                        },
                        menuContent = {},
                    )
                }
                Text(
                    text = track.player,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(16.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        interactionSource = interactionSource,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(20.dp),
        )
    }
}
