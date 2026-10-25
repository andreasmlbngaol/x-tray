package id.andreasmlbngaol.xtray.ui.modules

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.xtray.core.IslandModule
import id.andreasmlbngaol.xtray.core.settings.ThemeMode
import id.andreasmlbngaol.xtray.platform.system.SystemSettingsPage
import id.andreasmlbngaol.xtray.ui.services.IslandServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    /** First in the rail and the default tab: controls are what people reach for. */
    override val priority = 110

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
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                onPreview = { percent ->
                    if (state.muted == true) services.controls.toggleMute()
                    services.controls.setVolume(percent)
                },
                onCommit = { percent ->
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
                    onPreview = { percent -> services.controls.setBrightness(percent) },
                    onCommit = { percent ->
                        services.controls.setBrightness(percent)
                        refreshAsync()
                    },
                )
            }

            val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
            val airplane = state.wifiEnabled == false && state.bluetoothEnabled == false
            val tiles = buildList {
                if (services.controls.hasWifi) {
                    add(
                        ControlSpec(
                            label = "Wi-Fi",
                            icon = if (state.wifiEnabled == true) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                            checked = state.wifiEnabled == true,
                            onCheckedChange = {
                                services.controls.toggleWifi()
                                refreshAsync()
                            },
                            onLongPress = { services.controls.openSettings(SystemSettingsPage.Wifi) },
                        ),
                    )
                }
                if (services.controls.hasBluetooth) {
                    add(
                        ControlSpec(
                            label = "Bluetooth",
                            icon = if (state.bluetoothEnabled == true) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                            checked = state.bluetoothEnabled == true,
                            onCheckedChange = {
                                services.controls.toggleBluetooth()
                                refreshAsync()
                            },
                            onLongPress = { services.controls.openSettings(SystemSettingsPage.Bluetooth) },
                        ),
                    )
                }
                if (services.controls.hasWifi || services.controls.hasBluetooth) {
                    add(
                        ControlSpec(
                            label = "Airplane",
                            icon = Icons.Rounded.AirplanemodeActive,
                            checked = airplane,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (state.wifiEnabled == true) services.controls.toggleWifi()
                                    if (state.bluetoothEnabled == true) services.controls.toggleBluetooth()
                                } else {
                                    if (state.wifiEnabled == false) services.controls.toggleWifi()
                                    if (state.bluetoothEnabled == false) services.controls.toggleBluetooth()
                                }
                                refreshAsync()
                            },
                            onLongPress = { services.controls.openSettings(SystemSettingsPage.Network) },
                        ),
                    )
                }
                add(
                    ControlSpec(
                        label = "Mic",
                        icon = if (state.micMuted == true) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                        checked = state.micMuted != true,
                        onCheckedChange = {
                            services.controls.toggleMic()
                            refreshAsync()
                        },
                        onLongPress = { services.controls.openSettings(SystemSettingsPage.Sound) },
                    ),
                )
                add(
                    ControlSpec(
                        label = "DND",
                        icon = Icons.Rounded.DoNotDisturbOn,
                        checked = services.settings.current.doNotDisturb,
                        onCheckedChange = {
                            services.settings.update { it.copy(doNotDisturb = !it.doNotDisturb) }
                        },
                        onLongPress = { services.controls.openSettings(SystemSettingsPage.Notifications) },
                    ),
                )
                add(
                    ControlSpec(
                        label = "Dark",
                        icon = Icons.Rounded.DarkMode,
                        checked = darkSurface,
                        onCheckedChange = { dark ->
                            services.settings.update {
                                it.copy(themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light)
                            }
                        },
                        onLongPress = { services.controls.openSettings(SystemSettingsPage.Appearance) },
                    ),
                )
                add(
                    ControlSpec(
                        label = "Mute",
                        icon = if (state.muted == true) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                        checked = state.muted == true,
                        onCheckedChange = {
                            services.controls.toggleMute()
                            refreshAsync()
                        },
                        onLongPress = { services.controls.openSettings(SystemSettingsPage.Sound) },
                    ),
                )
                add(
                    ControlSpec(
                        label = "Settings",
                        icon = Icons.Rounded.Settings,
                        checked = false,
                        accent = true,
                        onCheckedChange = { services.openModule("settings") },
                    ),
                )
            }

            // Quick settings are a 4x2 grid; long-pressing a tile opens the
            // matching desktop settings page.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.chunked(4).forEach { rowTiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowTiles.forEach { tile ->
                            ControlTile(
                                label = tile.label,
                                icon = tile.icon,
                                checked = tile.checked,
                                accent = tile.accent,
                                modifier = Modifier.weight(1f),
                                onCheckedChange = tile.onCheckedChange,
                                onLongPress = tile.onLongPress,
                            )
                        }
                        repeat(4 - rowTiles.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            if (services.settings.current.mediaEnabled && services.mediaTrack != null) {
                Spacer(modifier = Modifier.height(4.dp))
                MediaPlayerCard(services)
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
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
) {
    var draft by remember { mutableFloatStateOf(value.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    var lastSentAt by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
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
                    // Apply while dragging, throttled so we do not spawn a
                    // process on every frame.
                    val now = System.currentTimeMillis()
                    if (now - lastSentAt >= 90L) {
                        lastSentAt = now
                        val percent = next.toInt()
                        scope.launch(Dispatchers.IO) { onPreview(percent) }
                    }
                },
                onValueChangeFinished = {
                    dragging = false
                    val percent = draft.toInt()
                    scope.launch(Dispatchers.IO) { onCommit(percent) }
                },
                valueRange = 0f..if (title == "Volume") 150f else 100f,
            )
        }
    }
}

private class ControlSpec(
    val label: String,
    val icon: ImageVector,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
    val onLongPress: (() -> Unit)? = null,
    val accent: Boolean = false,
)

@Composable
private fun ControlTile(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    val active = checked || accent
    val shape = if (active) RoundedCornerShape(18.dp) else CircleShape
    val container = when {
        accent -> MaterialTheme.colorScheme.secondaryContainer
        checked -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = when {
        accent -> MaterialTheme.colorScheme.onSecondaryContainer
        checked -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(shape)
                .background(container)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = { onCheckedChange(!checked) },
                    onLongClick = onLongPress,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = content,
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
