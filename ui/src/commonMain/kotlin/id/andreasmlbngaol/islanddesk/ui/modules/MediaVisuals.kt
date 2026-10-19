package id.andreasmlbngaol.islanddesk.ui.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun playerIcon(player: String): ImageVector = when {
    player.contains("vlc", ignoreCase = true) ||
        player.contains("mpv", ignoreCase = true) ||
        player.contains("celluloid", ignoreCase = true) -> Icons.Rounded.Movie

    player.contains("spotify", ignoreCase = true) ||
        player.contains("music", ignoreCase = true) -> Icons.Rounded.MusicNote

    player.contains("brave", ignoreCase = true) ||
        player.contains("chrom", ignoreCase = true) ||
        player.contains("firefox", ignoreCase = true) -> Icons.Rounded.Public

    else -> Icons.Rounded.GraphicEq
}

@Composable
fun MediaArtwork(player: String, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.2f))
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
            imageVector = playerIcon(player),
            contentDescription = null,
            modifier = Modifier.size(size / 2.3f),
            tint = Color.White.copy(alpha = 0.9f),
        )
    }
}

internal fun formatMediaTime(ms: Long): String {
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
