package id.andreasmlbngaol.xtray.ui.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import id.andreasmlbngaol.xtray.core.AppInfo
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
import id.andreasmlbngaol.xtray.core.IslandModule
import id.andreasmlbngaol.xtray.core.settings.AccentColor
import id.andreasmlbngaol.xtray.core.settings.IslandSettings
import id.andreasmlbngaol.xtray.core.settings.ThemeMode
import id.andreasmlbngaol.xtray.platform.DisplayInfo
import id.andreasmlbngaol.xtray.ui.services.IslandServices
import id.andreasmlbngaol.xtray.ui.theme.accentSwatch
import id.andreasmlbngaol.xtray.ui.theme.parseHexColor

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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                    AccentColor.entries.forEach { accent ->
                        val selected = settings.accentColor == accent && settings.customAccent.isBlank()
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(accentSwatch(accent, darkSurface))
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable {
                                    services.settings.update {
                                        it.copy(accentColor = accent, customAccent = "")
                                    }
                                },
                        )
                    }
                }

                CustomAccentRow(services = services, current = settings.customAccent)

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
                    title = "Shrink over maximized windows",
                    subtitle = "Collapses to a 120x6 hairline so browser tabs stay reachable",
                    checked = settings.hideOverMaximized,
                    onCheckedChange = { value ->
                        services.settings.update { it.copy(hideOverMaximized = value) }
                    },
                )
                SwitchRow(
                    title = "Replace system notifications",
                    subtitle = "Stops GNOME's own popups; every notification lands here",
                    checked = settings.replaceSystemNotifications,
                    onCheckedChange = { value ->
                        services.settings.update { it.copy(replaceSystemNotifications = value) }
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

            Section(title = "About") {
                Text(AppInfo.NAME + " " + AppInfo.VERSION, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Google Sans Flex under SIL OFL 1.1 · Kotlin " + KotlinVersion.CURRENT,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { services.fileOpener.openUrl("https://github.com/andreasmlbngaol") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("github.com/andreasmlbngaol")
                }
            }

            DangerZone(services = services)
        }
    }
}

@Composable
private fun CustomAccentRow(services: IslandServices, current: String) {
    var draft by remember(current) { mutableStateOf(current) }
    val parsed = parseHexColor(draft)
    val dirty = draft.trim() != current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = { value -> draft = value.take(7) },
            modifier = Modifier.weight(1f),
            label = { Text("Custom hex", style = MaterialTheme.typography.labelSmall) },
            placeholder = { Text("#1B4B8F", style = MaterialTheme.typography.labelSmall) },
            singleLine = true,
            isError = draft.isNotBlank() && parsed == null,
            trailingIcon = {
                val preview = parsed
                if (preview != null) {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(preview),
                    )
                }
            },
        )
        Button(
            onClick = {
                services.settings.update { it.copy(customAccent = draft.trim()) }
            },
            enabled = parsed != null && dirty,
        ) {
            Text("Apply")
        }
        if (current.isNotBlank()) {
            TextButton(
                onClick = {
                    draft = ""
                    services.settings.update { it.copy(customAccent = "") }
                },
            ) {
                Text("Clear")
            }
        }
    }
}

@Composable
private fun DangerZone(services: IslandServices) {
    var confirmReset by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Danger zone",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Reset all settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "Theme, accent, delays, toggles and the focus duration return to " +
                        "their defaults. This cannot be undone.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { confirmReset = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Reset everything")
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset every setting?") },
            text = {
                Text(
                    "All preferences go back to defaults and the autostart entry is removed. " +
                        "There is no undo.",
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
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Reset everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
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
