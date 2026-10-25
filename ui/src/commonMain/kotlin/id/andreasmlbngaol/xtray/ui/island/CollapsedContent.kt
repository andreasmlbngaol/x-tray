package id.andreasmlbngaol.xtray.ui.island

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.andreasmlbngaol.xtray.core.notification.IslandNotification
import id.andreasmlbngaol.xtray.platform.network.NetworkSpeed
import id.andreasmlbngaol.xtray.ui.services.IslandServices
import kotlin.math.roundToInt

@Composable
fun CollapsedContent(
    services: IslandServices,
    modifier: Modifier = Modifier,
) {
    val clock = rememberClockText()
    val battery = services.batteryState
    val dnd = services.settings.current.doNotDisturb
    val speed = services.networkSpeed
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = clock.time,
                style = MaterialTheme.typography.titleMediumEmphasized.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = clock.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(min = 54.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        // Always present, even at 0 B/s: a readout that comes and goes was
        // shifting the battery group on every sample. Two rows keep the
        // numbers legible; a single row was too small to read at a glance.
        SpeedReadout(speed = speed)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (dnd) {
                Icon(
                    imageVector = Icons.Rounded.DoNotDisturbOn,
                    contentDescription = "Do not disturb",
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BatteryIndicator(
                percent = battery.percent,
                charging = battery.charging,
                iconSize = 17.dp,
            )
        }
    }
}

/**
 * The card shown in collapsed state while a notification is current. It gets
 * its own, larger island size (see [IslandDimens.Notification]) so the sender,
 * the title and a couple of body lines all fit at once.
 */
@Composable
fun NotificationContent(
    notification: IslandNotification,
    onDismiss: () -> Unit,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onActivate)
            .padding(
                start = NotificationCardMetrics.StartPadding,
                end = NotificationCardMetrics.EndPadding,
                top = 12.dp,
                bottom = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NotificationCardMetrics.Gap),
    ) {
        NotificationAppIcon(notification = notification, size = NotificationCardMetrics.IconSize)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = notification.appName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = notification.headline,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (notification.detail.isNotBlank()) {
                Text(
                    text = notification.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        FilledIconButton(
            onClick = onDismiss,
            modifier = Modifier.size(NotificationCardMetrics.CloseSize),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Dismiss",
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Upload above download, one number per row. A single row with both figures was
 * unreadable at collapsed size; the arrows are real icons now so they read at
 * 11 dp instead of depending on a tiny glyph.
 */
@Composable
fun SpeedReadout(
    speed: NetworkSpeed,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 10.sp,
) {
    Column(
        modifier = modifier.widthIn(min = 46.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        SpeedRow(
            icon = Icons.Rounded.ArrowUpward,
            text = formatRate(speed.upBytesPerSecond),
            fontSize = fontSize,
        )
        SpeedRow(
            icon = Icons.Rounded.ArrowDownward,
            text = formatRate(speed.downBytesPerSecond),
            fontSize = fontSize,
        )
    }
}

@Composable
private fun SpeedRow(
    icon: ImageVector,
    text: String,
    fontSize: TextUnit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size((fontSize.value + 1).dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = fontSize,
                lineHeight = fontSize * 1.1f,
                fontFeatureSettings = "tnum",
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
    }
}

internal fun formatRate(bytesPerSecond: Long): String {
    val kilobytes = bytesPerSecond / 1024.0
    if (kilobytes < 1.0) return "0K"
    if (kilobytes < 1_000.0) return kilobytes.roundToInt().toString() + "K"
    val megabytes = kilobytes / 1024.0
    val rounded = (megabytes * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString() + "M"
    } else {
        rounded.toString() + "M"
    }
}
