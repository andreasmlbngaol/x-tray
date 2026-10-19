package id.andreasmlbngaol.islanddesk.ui.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.andreasmlbngaol.islanddesk.core.IslandModule
import id.andreasmlbngaol.islanddesk.core.activity.LiveActivityController
import id.andreasmlbngaol.islanddesk.ui.services.IslandServices
import kotlinx.coroutines.delay

class FocusModule(private val services: IslandServices) : IslandModule {
    override val id = "focus"
    override val title = "Focus"
    override val icon: ImageVector = Icons.Rounded.Timer
    override val priority = 70

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun PeekContent() {
        val activity = services.activity.current
        var tick by remember { mutableIntStateOf(0) }
        LaunchedEffect(activity?.id) {
            if (activity == null) return@LaunchedEffect
            while (true) {
                delay(1_000L)
                tick++
                if (services.activity.isFinished()) break
            }
        }
        if (activity == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                Column {
                    Text("Focus", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Open the panel to start a session",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = LiveActivityController.format(services.activity.remainingMs()) + " left",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearWavyProgressIndicator(
                    progress = { services.activity.progress() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun ExpandedContent() {
        val activity = services.activity.current
        var tick by remember { mutableIntStateOf(0) }
        LaunchedEffect(activity?.id) {
            if (activity == null) return@LaunchedEffect
            while (true) {
                delay(1_000L)
                tick++
                if (services.activity.isFinished()) {
                    services.notifications.push("Focus session complete")
                    break
                }
            }
        }

        if (activity != null) {
            RunningSession(activity.title, tick)
            return
        }

        val defaultSeconds = services.settings.current.focusSeconds
        var hours by remember(defaultSeconds) { mutableIntStateOf(defaultSeconds / 3_600) }
        var minutes by remember(defaultSeconds) { mutableIntStateOf((defaultSeconds % 3_600) / 60) }
        var seconds by remember(defaultSeconds) { mutableIntStateOf(defaultSeconds % 60) }
        val total = hours * 3_600 + minutes * 60 + seconds

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text("Focus session", style = MaterialTheme.typography.titleLargeEmphasized)
            Text(
                text = "Choose a duration",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TimeUnit(
                    value = hours,
                    range = 0..12,
                    onChange = { hours = it },
                )
                Text(":", style = MaterialTheme.typography.titleLargeEmphasized)
                TimeUnit(
                    value = minutes,
                    range = 0..59,
                    onChange = { minutes = it },
                )
                Text(":", style = MaterialTheme.typography.titleLargeEmphasized)
                TimeUnit(
                    value = seconds,
                    range = 0..59,
                    onChange = { seconds = it },
                )
            }
            Text(
                text = "hours      minutes      seconds",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val duration = total.coerceAtLeast(1)
                    services.settings.update { it.copy(focusSeconds = duration) }
                    services.activity.startSeconds("Focus", duration.toLong())
                },
                enabled = total > 0,
            ) {
                Text("Start focus")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "The timer stays visible in the peek.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun RunningSession(title: String, tick: Int) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = LiveActivityController.format(services.activity.remainingMs()),
                style = MaterialTheme.typography.displayMediumEmphasized.copy(
                    fontFeatureSettings = "tnum",
                ),
            )
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            LinearWavyProgressIndicator(
                progress = { services.activity.progress() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(onClick = { services.activity.stop() }) {
                Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Stop")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (services.activity.isFinished()) "Session complete" else "Stay focused.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TimeUnit(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = { onChange(if (value >= range.last) range.first else value + 1) },
            modifier = Modifier.size(26.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Increase",
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.titleLargeEmphasized.copy(
                fontSize = 28.sp,
                fontFeatureSettings = "tnum",
            ),
        )
        IconButton(
            onClick = { onChange(if (value <= range.first) range.last else value - 1) },
            modifier = Modifier.size(26.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Decrease",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
