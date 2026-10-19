package id.andreasmlbngaol.islanddesk.ui.island

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                .height(50.dp)
                .padding(start = 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = clock.time + ":" + clock.seconds,
                style = MaterialTheme.typography.titleMediumEmphasized.copy(fontFeatureSettings = "tnum"),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = clock.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            SpeedReadout(speed = services.networkSpeed, fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            PowerMenu(services = services)
        }

        Row(modifier = Modifier.fillMaxSize()) {
            ModuleRail(
                modules = modules,
                activeModuleId = activeModule?.id,
                onSelectModule = onSelectModule,
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (notification != null) {
                    NotificationBanner(
                        notification = notification,
                        onDismiss = { services.notifications.dismiss() },
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (activeModule != null) {
                        activeModule.ExpandedContent()
                    } else {
                        Text(
                            text = "No modules available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun PowerMenu(services: IslandServices) {
    var expanded by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<PowerAction?>(null) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Rounded.PowerSettingsNew,
                contentDescription = "Power",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PowerAction.entries.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    enabled = services.power.available,
                    onClick = {
                        expanded = false
                        pending = action
                    },
                )
            }
        }
    }

    val action = pending
    if (action != null) {
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(action.label + "?") },
            text = { Text(action.question) },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (action) {
                            PowerAction.Suspend -> services.power.suspend()
                            PowerAction.Reboot -> services.power.reboot()
                            PowerAction.Shutdown -> services.power.shutdown()
                        }
                        pending = null
                    },
                    colors = if (action == PowerAction.Suspend) {
                        androidx.compose.material3.ButtonDefaults.textButtonColors()
                    } else {
                        androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        )
                    },
                ) {
                    Text(action.label)
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Cancel") }
            },
        )
    }
}

private enum class PowerAction(
    val label: String,
    val question: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Suspend(
        label = "Suspend",
        question = "Suspend the session? The island resumes when you unlock.",
        icon = Icons.Rounded.Bedtime,
    ),
    Reboot(
        label = "Reboot",
        question = "Restart the computer? Unsaved work will be lost.",
        icon = Icons.Rounded.RestartAlt,
    ),
    Shutdown(
        label = "Shut down",
        question = "Power off the computer? Unsaved work will be lost.",
        icon = Icons.Rounded.PowerSettingsNew,
    ),
}

@Composable
private fun ModuleRail(
    modules: List<IslandModule>,
    activeModuleId: String?,
    onSelectModule: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(66.dp)
            .fillMaxHeight()
            .padding(horizontal = 7.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        modules.forEach { module ->
            val selected = module.id == activeModuleId
            val shape = RoundedCornerShape(18.dp)
            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1.08f else 0.92f,
                animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                label = "railIconScale",
            )
            val container by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0f)
                },
                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                label = "railSelection",
            )
            Surface(
                onClick = { onSelectModule(module.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = shape,
                color = container,
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                tonalElevation = if (selected) 1.dp else 0.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = module.icon,
                        contentDescription = module.title,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = module.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
