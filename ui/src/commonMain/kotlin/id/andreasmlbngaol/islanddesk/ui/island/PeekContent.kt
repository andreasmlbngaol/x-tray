package id.andreasmlbngaol.islanddesk.ui.island

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.andreasmlbngaol.islanddesk.core.activity.LiveActivity
import id.andreasmlbngaol.islanddesk.core.activity.LiveActivityController
import id.andreasmlbngaol.islanddesk.platform.formatClockTime
import id.andreasmlbngaol.islanddesk.platform.media.MediaTrack
import id.andreasmlbngaol.islanddesk.ui.modules.MediaArtwork
import id.andreasmlbngaol.islanddesk.ui.modules.formatMediaTime
import id.andreasmlbngaol.islanddesk.ui.services.IslandServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PeekContent(
    services: IslandServices,
    modifier: Modifier = Modifier,
) {
    val clock = rememberClockText()
    val track = services.mediaTrack
    val battery = services.batteryState
    val notification = services.notifications.current
    val activity = services.activity.current
    val scope = rememberCoroutineScope()

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activity?.id) {
        if (activity == null) return@LaunchedEffect
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(500L)
        }
    }

    fun command(block: () -> Unit) {
        scope.launch {
            withContext(Dispatchers.Default) { runCatching(block) }
            services.mediaTrack = withContext(Dispatchers.Default) {
                runCatching { services.media.snapshot() }.getOrNull()
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PeekClock(clock = clock, modifier = Modifier.width(74.dp))

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(100.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (activity != null) {
                FocusHero(
                    activity = activity,
                    remainingMs = (activity.startedAtMs + activity.durationMs - nowMs).coerceAtLeast(0L),
                    progress = if (activity.durationMs <= 0L) {
                        0f
                    } else {
                        ((nowMs - activity.startedAtMs).coerceIn(0L, activity.durationMs).toFloat() /
                            activity.durationMs.toFloat())
                    },
                )
            } else {
                MediaHeader(
                    track = track,
                    batteryPercent = battery.percent,
                    batteryCharging = battery.charging,
                    dnd = services.settings.current.doNotDisturb,
                )

                if (track != null && track.hasProgress) {
                    LinearWavyProgressIndicator(
                        progress = { track.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (track != null && track.hasProgress) {
                            formatMediaTime(track.positionMs) + " / " + formatMediaTime(track.lengthMs)
                        } else {
                            track?.album?.ifBlank { track.player }.orEmpty()
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontFeatureSettings = "tnum",
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    PeekTransport(
                        playing = track?.playing == true,
                        enabled = track != null,
                        onPrevious = { command { services.media.previous() } },
                        onPlayPause = { command { services.media.playPause() } },
                        onNext = { command { services.media.next() } },
                    )
                }
            }

            when {
                notification != null -> PeekNotification(
                    message = notification.message,
                    onDismiss = { services.notifications.dismiss() },
                )
                activity != null -> Text(
                    text = "Focus is running • tap the island for details",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FocusHero(
    activity: LiveActivity,
    remainingMs: Long,
    progress: Float,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Timer,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = activity.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Ends " + formatClockTime(activity.startedAtMs + activity.durationMs),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
    Text(
        text = LiveActivityController.format(remainingMs),
        style = MaterialTheme.typography.displaySmallEmphasized.copy(
            fontSize = 34.sp,
            lineHeight = 36.sp,
            fontFeatureSettings = "tnum",
        ),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
    )
    LinearWavyProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
private fun MediaHeader(
    track: MediaTrack?,
    batteryPercent: Int?,
    batteryCharging: Boolean,
    dnd: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MediaArtwork(player = track?.player.orEmpty(), size = 34.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track?.title ?: "No media playing",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track?.let { t -> t.artist.ifBlank { t.album }.ifBlank { t.player } }
                    ?: "Start a player to see it here",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (dnd) {
            Icon(
                imageVector = Icons.Rounded.DoNotDisturbOn,
                contentDescription = "Do not disturb",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = if (batteryCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                contentDescription = "Battery",
                modifier = Modifier.size(16.dp),
                tint = if (batteryCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = batteryPercent?.let { it.toString() + "%" } ?: "—",
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun PeekTransport(
    playing: Boolean,
    enabled: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = enabled,
            modifier = Modifier.size(26.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(15.dp),
            )
        }
        FilledIconButton(
            onClick = onPlayPause,
            enabled = enabled,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                modifier = Modifier.size(16.dp),
            )
        }
        IconButton(
            onClick = onNext,
            enabled = enabled,
            modifier = Modifier.size(26.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun PeekNotification(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDismiss),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Dismiss",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
