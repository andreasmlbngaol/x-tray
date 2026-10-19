package id.andreasmlbngaol.islanddesk.ui.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.islanddesk.core.IslandModule
import id.andreasmlbngaol.islanddesk.core.settings.AccentColor
import id.andreasmlbngaol.islanddesk.core.settings.IslandSettings
import id.andreasmlbngaol.islanddesk.core.settings.ThemeMode
import id.andreasmlbngaol.islanddesk.platform.DisplayInfo
import id.andreasmlbngaol.islanddesk.ui.services.IslandServices
import id.andreasmlbngaol.islanddesk.ui.theme.accentSwatch

class SettingsModule(private val services: IslandServices) : IslandModule {
    override val id = "settings"
    override val title = "Settings"
    override val icon: ImageVector = Icons.Rounded.Settings
    override val priority = 10

    @Composable
    override fun PeekContent() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
            Column {
                Text("Settings", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Theme, startup and display",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    override fun ExpandedContent() {
        val settings = services.settings.current
        val screens = DisplayInfo.screens().size.coerceAtLeast(1)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section(title = "Appearance") {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = {
                                services.settings.update { it.copy(themeMode = mode) }
                            },
                            label = {
                                Text(
                                    text = when (mode) {
                                        ThemeMode.System -> "System"
                                        ThemeMode.Light -> "Light"
                                        ThemeMode.Dark -> "Dark"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                        )
                    }
                }
                Text(
                    text = "Accent",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                    AccentColor.entries.forEach { accent ->
                        val selected = settings.accentColor == accent
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(accentSwatch(accent, darkSurface))
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable {
                                    services.settings.update { it.copy(accentColor = accent) }
                                },
                        )
                    }
                }
                SwitchRow(
                    title = "Reduce motion",
                    subtitle = "Snap between states instead of springing",
                    checked = settings.reduceMotion,
                    onCheckedChange = { value ->
                        services.settings.update { it.copy(reduceMotion = value) }
                    },
                )
            }

            Section(title = "Behaviour") {
                SwitchRow(
                    title = "Do not disturb",
                    subtitle = "Hover stays collapsed; notifications are muted",
                    checked = settings.doNotDisturb,
                    onCheckedChange = { value ->
                        services.settings.update { it.copy(doNotDisturb = value) }
                    },
                )
                SwitchRow(
                    title = "Hide over maximized windows",
                    subtitle = "Steps aside when a window fills the screen, so browser tabs stay reachable",
                    checked = settings.hideOverMaximized,
                    onCheckedChange = { value ->
                        services.settings.update { it.copy(hideOverMaximized = value) }
                    },
                )
                SwitchRow(
                    title = "Start at login",
                    subtitle = "Writes an XDG autostart entry",
                    checked = services.autostart.isEnabled(),
                    onCheckedChange = { value ->
                        services.autostart.setEnabled(value)
                        services.settings.update { it.copy(startAtLogin = value) }
                    },
                )
            }

            Section(title = "Display") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Monitor " + (settings.monitorIndex + 1) + " of " + screens,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            services.settings.update {
                                it.copy(monitorIndex = (it.monitorIndex + 1) % screens)
                            }
                        },
                    ) {
                        Text("Next monitor")
                    }
                }
            }

            Section(title = "Timing") {
                var hoverDraft by remember(settings.hoverIntentMs) {
                    mutableFloatStateOf(settings.hoverIntentMs.toFloat())
                }
                Text(
                    text = "Hover delay: " + hoverDraft.toInt() + " ms",
                    style = MaterialTheme.typography.labelMedium,
                )
                Slider(
                    value = hoverDraft,
                    onValueChange = { value -> hoverDraft = value },
                    onValueChangeFinished = {
                        services.settings.update { it.copy(hoverIntentMs = hoverDraft.toLong()) }
                    },
                    valueRange = 50f..600f,
                )

                var collapseDraft by remember(settings.collapseDelayMs) {
                    mutableFloatStateOf(settings.collapseDelayMs.toFloat())
                }
                Text(
                    text = "Peek close delay: " + collapseDraft.toInt() + " ms",
                    style = MaterialTheme.typography.labelMedium,
                )
                Slider(
                    value = collapseDraft,
                    onValueChange = { value -> collapseDraft = value },
                    onValueChangeFinished = {
                        services.settings.update { it.copy(collapseDelayMs = collapseDraft.toLong()) }
                    },
                    valueRange = 0f..1_000f,
                )
            }

            Section(title = "Notifications") {
                Text(
                    text = "Notifications appear as a banner above the active module " +
                        "when the island is expanded, and in the peek's bottom row.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        services.notifications.push("Test notification from IslandDesk")
                    },
                ) {
                    Text("Send a test notification")
                }
            }

            Section(title = "About") {
                Text("IslandDesk 0.1.0", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = "Kotlin " + KotlinVersion.CURRENT + " • Compose Multiplatform",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Media through MPRIS • controls through PipeWire and NetworkManager",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                var confirmReset by remember { mutableStateOf(false) }
                TextButton(onClick = { confirmReset = true }) {
                    Text("Reset all settings")
                }
                if (confirmReset) {
                    AlertDialog(
                        onDismissRequest = { confirmReset = false },
                        title = { Text("Reset settings?") },
                        text = {
                            Text(
                                "Theme, accent, delays, toggles and the focus duration " +
                                    "return to their defaults.",
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val defaults = IslandSettings()
                                    services.settings.update { defaults }
                                    services.autostart.setEnabled(defaults.startAtLogin)
                                    confirmReset = false
                                },
                            ) {
                                Text("Reset")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmReset = false }) {
                                Text("Cancel")
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
