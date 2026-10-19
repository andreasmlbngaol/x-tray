package id.andreasmlbngaol.islanddesk.ui.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.islanddesk.core.IslandModule
import id.andreasmlbngaol.islanddesk.ui.services.IslandServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Quick settings: volume and brightness are continuous sliders, while the
 * remaining toggles rely on the filled tonal state instead of repeating
 * "On"/"Off" labels.
 */
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
            LevelCard(
                title = "Volume",
                value = state.volumePercent ?: 0,
                icon = if (state.muted == true) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                muted = state.muted == true,
                onMuteToggle = {
                    services.controls.toggleMute()
                    refreshAsync()
                },
                onCommit = { percent ->
                    if (state.muted == true) services.controls.toggleMute()
                    services.controls.setVolume(percent)
                    refreshAsync()
                },
            )

            if (services.controls.hasBrightness) {
                LevelCard(
                    title = "Brightness",
                    value = state.brightnessPercent ?: 0,
                    icon = Icons.Rounded.Brightness6,
                    muted = false,
                    onMuteToggle = null,
                    onCommit = { percent ->
                        services.controls.setBrightness(percent)
                        refreshAsync()
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ToggleTile(
                    label = "Microphone",
                    icon = if (state.micMuted == true) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    checked = state.micMuted != true,
                    modifier = Modifier.weight(1f),
                    onCheckedChange = {
                        services.controls.toggleMic()
                        refreshAsync()
                    },
                )
                if (services.controls.hasWifi) {
                    ToggleTile(
                        label = "Wi-Fi",
                        icon = if (state.wifiEnabled == true) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                        checked = state.wifiEnabled == true,
                        modifier = Modifier.weight(1f),
                        onCheckedChange = {
                            services.controls.toggleWifi()
                            refreshAsync()
                        },
                    )
                }
                if (services.controls.hasBluetooth) {
                    ToggleTile(
                        label = "Bluetooth",
                        icon = if (state.bluetoothEnabled == true) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                        checked = state.bluetoothEnabled == true,
                        modifier = Modifier.weight(1f),
                        onCheckedChange = {
                            services.controls.toggleBluetooth()
                            refreshAsync()
                        },
                    )
                }
                ToggleTile(
                    label = "Do not disturb",
                    icon = Icons.Rounded.DoNotDisturbOn,
                    checked = services.settings.current.doNotDisturb,
                    modifier = Modifier.weight(1f),
                    onCheckedChange = {
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
private fun LevelCard(
    title: String,
    value: Int,
    icon: ImageVector,
    muted: Boolean,
    onMuteToggle: (() -> Unit)?,
    onCommit: (Int) -> Unit,
) {
    var draft by remember { mutableFloatStateOf(value.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (!dragging) draft = value.toFloat()
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = draft.toInt().toString() + "%",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (onMuteToggle != null) {
                    FilledTonalIconToggleButton(
                        checked = muted,
                        onCheckedChange = { onMuteToggle() },
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeOff,
                            contentDescription = if (muted) "Unmute" else "Mute",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Slider(
                value = draft,
                onValueChange = { next ->
                    dragging = true
                    draft = next
                },
                onValueChangeFinished = {
                    dragging = false
                    onCommit(draft.toInt())
                },
                valueRange = 0f..100f,
            )
        }
    }
}

@Composable
private fun ToggleTile(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilledTonalIconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
