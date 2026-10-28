package id.andreasmlbngaol.xtray.ui.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.xtray.core.IslandModule
import id.andreasmlbngaol.xtray.core.notification.IslandNotification
import id.andreasmlbngaol.xtray.core.notification.NotificationGroup
import id.andreasmlbngaol.xtray.platform.formatClockTime
import id.andreasmlbngaol.xtray.ui.island.NotificationAppIcon
import id.andreasmlbngaol.xtray.ui.services.IslandServices

/**
 * Everything the notification daemon received this session, grouped per app so
 * a chatty sender cannot fill the whole list. Groups expand on demand, a
 * horizontal swipe removes one message, and the whole history can be dropped
 * at once.
 */
class NotificationsModule(private val services: IslandServices) : IslandModule {
    override val id = "notifications"
    override val title = "Notifications"
    override val shortTitle = "Notifs"
    override val icon: ImageVector = Icons.Rounded.Notifications
    override val priority = 90

    @Composable
    override fun ExpandedContent() {
        val history = services.notifications.history
        val groups = remember(history) { services.notifications.grouped() }
        var expandedGroups by remember { mutableStateOf(emptySet<String>()) }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "History (" + history.size + ")",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(
                    onClick = {
                        services.notifications.clearAll()
                        expandedGroups = emptySet()
                    },
                    enabled = history.isNotEmpty(),
                ) {
                    Text("Clear all", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (groups.isEmpty()) {
                EmptyNotifications()
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groups.forEach { group ->
                        val expanded = group.appName in expandedGroups
                        NotificationGroupCard(
                            group = group,
                            expanded = expanded,
                            onToggle = {
                                expandedGroups = if (expanded) {
                                    expandedGroups - group.appName
                                } else {
                                    expandedGroups + group.appName
                                }
                            },
                            onActivate = { notification ->
                                notification.sourceId?.let { services.notificationServer.activate(it) }
                            },
                            onRemove = { notification -> services.notifications.remove(notification.id) },
                            onRemoveGroup = { appName -> services.notifications.removeGroup(appName) },
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyNotifications() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShapeAccent(size = 78.dp) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "No notifications yet",
            style = MaterialTheme.typography.titleMediumEmphasized,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Notifications you receive will be kept here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Horizontal swipe that removes the row it wraps. Both directions work so the
 * gesture does not depend on where the drag started.
 */
@Composable
private fun SwipeToDelete(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        onDismiss = { onDelete() },
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    Arrangement.Start
                } else {
                    Arrangement.End
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete notification",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        content()
    }
}

@Composable
private fun NotificationGroupCard(
    group: NotificationGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onActivate: (IslandNotification) -> Unit,
    onRemove: (IslandNotification) -> Unit,
    onRemoveGroup: (String) -> Unit,
) {
    val expandable = group.notifications.size > 1
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            // The header stands for the whole sender, so swiping it clears the
            // group; a single message is swiped inside the dropdown.
            key(group.appName) {
                SwipeToDelete(onDelete = { onRemoveGroup(group.appName) }) {
                    // Opaque, so the delete backdrop only shows once the row
                    // has actually been dragged aside.
                    Surface(
                        onClick = {
                            if (expandable) onToggle() else onActivate(group.latest)
                        },
                        enabled = expandable || group.latest.sourceId != null,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        NotificationGroupHeader(
                            group = group,
                            expandable = expandable,
                            expanded = expanded,
                        )
                    }
                }
            }
            if (expanded) {
                group.notifications.forEach { notification ->
                    key(notification.id) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        SwipeToDelete(onDelete = { onRemove(notification) }) {
                            NotificationRow(notification = notification, onActivate = onActivate)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationGroupHeader(
    group: NotificationGroup,
    expandable: Boolean,
    expanded: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NotificationAppIcon(notification = group.latest, size = 30.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = group.appName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = group.latest.headline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // A lone message has no dropdown to open, so its body belongs here
            // instead of being hidden behind a click.
            if (!expandable && group.latest.detail.isNotBlank()) {
                Text(
                    text = group.latest.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (expandable) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(
                    text = group.notifications.size.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Show all",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotificationRow(
    notification: IslandNotification,
    onActivate: (IslandNotification) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(enabled = notification.sourceId != null) { onActivate(notification) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = formatClockTime(notification.createdAtMs),
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(38.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.headline,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (notification.detail.isNotBlank()) {
                Text(
                    text = notification.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
