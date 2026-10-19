package id.andreasmlbngaol.islanddesk.ui.island

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.islanddesk.core.IslandModule
import id.andreasmlbngaol.islanddesk.ui.services.IslandServices

@Composable
fun ExpandedContent(
    modules: List<IslandModule>,
    activeModule: IslandModule?,
    onSelectModule: (String) -> Unit,
    services: IslandServices,
    modifier: Modifier = Modifier,
) {
    val clock = rememberClockText()
    val notification = services.notifications.current
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = clock.time,
                style = MaterialTheme.typography.titleMediumEmphasized.copy(fontFeatureSettings = "tnum"),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "IslandDesk",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onSelectModule("settings") }) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Pengaturan",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ModuleTabRow(
            modules = modules,
            activeModuleId = activeModule?.id,
            onSelectModule = onSelectModule,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (notification != null) {
                    NotificationBanner(
                        notification = notification,
                        onDismiss = { services.notifications.dismiss() },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (activeModule != null) {
                        activeModule.ExpandedContent()
                    } else {
                        Text(
                            text = "Belum ada modul.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Esc untuk menutup",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Tab • panah • Enter",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModuleTabRow(
    modules: List<IslandModule>,
    activeModuleId: String?,
    onSelectModule: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        modules.forEach { module ->
            val selected = module.id == activeModuleId
            val shape = RoundedCornerShape(50)
            Surface(
                modifier = Modifier
                    .widthIn(min = 84.dp)
                    .height(38.dp)
                    .clip(shape)
                    .clickable { onSelectModule(module.id) },
                shape = shape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = module.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = module.title,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
