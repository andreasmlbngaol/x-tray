package id.andreasmlbngaol.islanddesk.ui.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.islanddesk.core.IslandModule
import id.andreasmlbngaol.islanddesk.platform.DisplayInfo
import id.andreasmlbngaol.islanddesk.ui.services.IslandServices

class SettingsModule(private val services: IslandServices) : IslandModule {
    override val id = "settings"
    override val title = "Setelan"
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
                Text("Pengaturan", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Tema, startup, dan monitor",
                    style = MaterialTheme.typography.bodySmall,
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsCard {
                SwitchRow(
                    title = "Kurangi animasi",
                    subtitle = "Transisi langsung tanpa pegas",
                    checked = settings.reduceMotion,
                    onCheckedChange = { value ->
                        services.settings.update { it.copy(reduceMotion = value) }
                    },
                )
                SwitchRow(
                    title = "Jangan ganggu",
                    subtitle = "Hover tidak membuka panel",
                    checked = settings.doNotDisturb,
                    onCheckedChange = { value ->
                        services.settings.update { it.copy(doNotDisturb = value) }
                    },
                )
                SwitchRow(
                    title = "Mulai saat login",
                    subtitle = "Menulis entri autostart XDG",
                    checked = services.autostart.isEnabled(),
                    onCheckedChange = { value ->
                        services.autostart.setEnabled(value)
                        services.settings.update { it.copy(startAtLogin = value) }
                    },
                )
            }

            SettingsCard {
                Text(
                    "Monitor",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Monitor " + (settings.monitorIndex + 1) + " dari " + screens,
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
                        Text("Ganti")
                    }
                }
            }

            SettingsCard {
                Text(
                    "Jeda hover: " + settings.hoverIntentMs + " ms",
                    style = MaterialTheme.typography.labelMedium,
                )
                Slider(
                    value = settings.hoverIntentMs.toFloat(),
                    onValueChange = { value ->
                        services.settings.update { it.copy(hoverIntentMs = value.toLong()) }
                    },
                    valueRange = 50f..600f,
                )
                Text(
                    "Jeda tutup: " + settings.collapseDelayMs + " ms",
                    style = MaterialTheme.typography.labelMedium,
                )
                Slider(
                    value = settings.collapseDelayMs.toFloat(),
                    onValueChange = { value ->
                        services.settings.update { it.copy(collapseDelayMs = value.toLong()) }
                    },
                    valueRange = 0f..1_500f,
                )
            }

            SettingsCard {
                Text("IslandDesk 0.1.0", style = MaterialTheme.typography.labelSmall)
                Text(
                    "Kotlin " + KotlinVersion.CURRENT + " • Compose Multiplatform",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
