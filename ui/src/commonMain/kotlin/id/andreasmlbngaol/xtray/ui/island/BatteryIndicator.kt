package id.andreasmlbngaol.xtray.ui.island

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Battery0Bar
import androidx.compose.material.icons.rounded.Battery1Bar
import androidx.compose.material.icons.rounded.Battery2Bar
import androidx.compose.material.icons.rounded.Battery3Bar
import androidx.compose.material.icons.rounded.Battery4Bar
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.Battery6Bar
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ChargingBlue = Color(0xFF1E88E5)
private val LowRed = Color(0xFFE53935)
private val MediumOrange = Color(0xFFFB8C00)
private val HealthyGreen = Color(0xFF43A047)

/**
 * Battery readout shared by every disclosure level. The bar count tracks the
 * charge, the tint follows it (red, orange, green) and charging turns the icon
 * blue with a separate bolt next to it.
 */
@Composable
fun BatteryIndicator(
    percent: Int?,
    charging: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 17.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    showPercent: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = batteryIcon(percent),
            contentDescription = "Battery",
            modifier = Modifier.size(iconSize),
            tint = batteryTint(percent, charging),
        )
        if (charging) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = "Charging",
                modifier = Modifier.size(iconSize * 0.8f),
                tint = ChargingBlue,
            )
        }
        if (showPercent) {
            Text(
                text = percent?.let { it.toString() + "%" } ?: "—",
                style = textStyle.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

private fun batteryIcon(percent: Int?): ImageVector = when {
    percent == null -> Icons.Rounded.BatteryFull
    percent < 10 -> Icons.Rounded.Battery0Bar
    percent < 25 -> Icons.Rounded.Battery1Bar
    percent < 40 -> Icons.Rounded.Battery2Bar
    percent < 55 -> Icons.Rounded.Battery3Bar
    percent < 70 -> Icons.Rounded.Battery4Bar
    percent < 85 -> Icons.Rounded.Battery5Bar
    percent < 95 -> Icons.Rounded.Battery6Bar
    else -> Icons.Rounded.BatteryFull
}

@Composable
private fun batteryTint(percent: Int?, charging: Boolean): Color = when {
    charging -> ChargingBlue
    percent == null -> MaterialTheme.colorScheme.onSurfaceVariant
    percent <= 15 -> LowRed
    percent <= 40 -> MediumOrange
    else -> HealthyGreen
}
