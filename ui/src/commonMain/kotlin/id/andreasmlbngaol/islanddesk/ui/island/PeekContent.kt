package id.andreasmlbngaol.islanddesk.ui.island

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.andreasmlbngaol.islanddesk.core.IslandModule
import id.andreasmlbngaol.islanddesk.core.activity.LiveActivityController
import id.andreasmlbngaol.islanddesk.ui.services.IslandServices

@Composable
fun PeekContent(
    activeModule: IslandModule?,
    services: IslandServices,
    modifier: Modifier = Modifier,
) {
    val clock = rememberClockText()
    val battery = services.batteryState
    val notification = services.notifications.current
    val activity = services.activity.current

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.width(82.dp)) {
            Text(
                text = clock.time,
                style = MaterialTheme.typography.displaySmallEmphasized.copy(
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = clock.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(46.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                notification != null -> NotificationBanner(
                    notification = notification,
                    onDismiss = { services.notifications.dismiss() },
                )
                activeModule != null -> activeModule.PeekContent()
                else -> Text(
                    text = "Tidak ada modul aktif",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier.width(64.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (services.settings.current.doNotDisturb) {
                    Icon(
                        imageVector = Icons.Rounded.DoNotDisturbOn,
                        contentDescription = "Jangan ganggu",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (battery.charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (battery.charging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = battery.percent?.let { it.toString() + "%" } ?: "—",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                )
            }
            if (activity != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = LiveActivityController.format(services.activity.remainingMs()),
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
