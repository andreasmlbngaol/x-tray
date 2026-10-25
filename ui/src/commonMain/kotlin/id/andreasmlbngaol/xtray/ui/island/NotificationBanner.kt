package id.andreasmlbngaol.xtray.ui.island

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.xtray.core.notification.IslandNotification
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Shared geometry for the collapsed notification card. */
internal object NotificationCardMetrics {
    val IconSize = 40.dp
    val StartPadding = 26.dp
    val EndPadding = 22.dp
    val Gap = 14.dp
    val CloseSize = 32.dp
    val FixedWidth = StartPadding + IconSize + Gap * 2 + CloseSize + EndPadding
}

/**
 * The sender's real application icon when the bus provided one, otherwise a
 * lettered tile so the card still has a recognisable source.
 */
@Composable
fun NotificationAppIcon(
    notification: IslandNotification,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(notification.iconBytes) {
        notification.iconBytes?.let { bytes ->
            runCatching { bytes.decodeToImageBitmap() }.getOrNull()
        }
    }
    val shape = RoundedCornerShape(size * 0.3f)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = notification.appName.firstOrNull()?.uppercaseChar()?.toString() ?: "•",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
fun NotificationBanner(
    notification: IslandNotification,
    onDismiss: () -> Unit,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onActivate,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(
                start = NotificationCardMetrics.StartPadding,
                end = NotificationCardMetrics.EndPadding,
                top = 12.dp,
                bottom = 12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NotificationAppIcon(notification = notification, size = 40.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = NotificationCardMetrics.Gap),
            ) {
                Text(
                    text = notification.appName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = notification.headline,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (notification.detail.isNotBlank()) {
                    Text(
                        text = notification.detail,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FilledIconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp),
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
}
