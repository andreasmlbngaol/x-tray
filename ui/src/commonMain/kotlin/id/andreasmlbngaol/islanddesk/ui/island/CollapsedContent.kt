package id.andreasmlbngaol.islanddesk.ui.island

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.islanddesk.ui.services.IslandServices

@Composable
fun CollapsedContent(
    services: IslandServices,
    modifier: Modifier = Modifier,
) {
    val clock = rememberClockText()
    val battery = services.batteryState
    val dnd = services.settings.current.doNotDisturb
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = clock.time,
            style = MaterialTheme.typography.titleMediumEmphasized.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (services.notifications.current != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
            if (dnd) {
                Icon(
                    imageVector = Icons.Rounded.DoNotDisturbOn,
                    contentDescription = "Jangan ganggu",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = if (battery.charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                contentDescription = "Baterai",
                modifier = Modifier.size(15.dp),
                tint = if (battery.charging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = battery.percent?.let { it.toString() + "%" } ?: "—",
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
