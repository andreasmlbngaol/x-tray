package id.andreasmlbngaol.islanddesk.ui.modules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.islanddesk.core.IslandModule
import id.andreasmlbngaol.islanddesk.platform.system.SystemControls
import id.andreasmlbngaol.islanddesk.ui.services.IslandServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ControlsModule(private val services: IslandServices) : IslandModule {
    override val id = "controls"
    override val title = "Controls"
    override val icon: ImageVector = Icons.Rounded.Tune
    override val priority = 40

    @Composable
    override fun PeekContent() {
        LaunchedEffect(Unit) {
            while (true) {
                refresh()
                delay(2_500L)
            }
        }
        val state = services.systemState
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Volume " + (state.volumePercent?.toString()?.plus("%") ?: "—") +
                        if (state.muted == true) " • mute" else "",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = listOfNotNull(
                        state.brightnessPercent?.let { "Screen " + it + "%" },
                        state.wifiEnabled?.let { if (it) "Wi-Fi on" else "Wi-Fi off" },
                        state.bluetoothEnabled?.let { if (it) "Bluetooth on" else "Bluetooth off" },
                    ).joinToString(" • ").ifBlank { "System controls" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    @Composable
    override fun ExpandedContent() {
        LaunchedEffect(Unit) {
            while (true) {
                refresh()
                delay(2_500L)
            }
        }
        val state = services.systemState
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ControlTile(
                    label = "Volume",
                    value = state.volumePercent?.toString()?.plus("%") ?: "—",
                    icon = if (state.muted == true) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        services.controls.volumeStep(5)
                        refreshAsync()
                    },
                )
                ControlTile(
                    label = "Mute",
                    value = if (state.muted == true) "On" else "Off",
                    icon = Icons.AutoMirrored.Rounded.VolumeOff,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        services.controls.toggleMute()
                        refreshAsync()
                    },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (services.controls.hasBrightness) {
                    ControlTile(
                        label = "Brightness",
                        value = state.brightnessPercent?.toString()?.plus("%") ?: "—",
                        icon = Icons.Rounded.Brightness6,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            services.controls.brightnessStep(10)
                            refreshAsync()
                        },
                    )
                }
                ControlTile(
                    label = "Mic",
                    value = if (state.micMuted == true) "Off" else "On",
                    icon = if (state.micMuted == true) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        services.controls.toggleMic()
                        refreshAsync()
                    },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (services.controls.hasWifi) {
                    ControlTile(
                        label = "Wi-Fi",
                        value = if (state.wifiEnabled == true) "On" else "Off",
                        icon = if (state.wifiEnabled == true) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            services.controls.toggleWifi()
                            refreshAsync()
                        },
                    )
                }
                if (services.controls.hasBluetooth) {
                    ControlTile(
                        label = "Bluetooth",
                        value = if (state.bluetoothEnabled == true) "On" else "Off",
                        icon = if (state.bluetoothEnabled == true) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            services.controls.toggleBluetooth()
                            refreshAsync()
                        },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val dnd = services.settings.current.doNotDisturb
                ControlTile(
                    label = "Do not disturb",
                    value = if (dnd) "On" else "Off",
                    icon = Icons.Rounded.DoNotDisturbOn,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        services.settings.update { it.copy(doNotDisturb = !it.doNotDisturb) }
                    },
                )
            }
        }
    }

    private fun refreshAsync() {
        services.refreshSystem()
    }

    private suspend fun refresh() {
        services.systemState = withContext(Dispatchers.Default) {
            runCatching { services.controls.snapshot() }.getOrDefault(services.systemState)
        }
    }
}

@Composable
private fun ControlTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(value, style = MaterialTheme.typography.titleMediumEmphasized)
        }
    }
}
