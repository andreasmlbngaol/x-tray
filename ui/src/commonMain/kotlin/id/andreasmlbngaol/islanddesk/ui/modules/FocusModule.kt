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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.andreasmlbngaol.islanddesk.core.IslandModule
import id.andreasmlbngaol.islanddesk.core.activity.LiveActivityController
import id.andreasmlbngaol.islanddesk.platform.formatClockTime
import id.andreasmlbngaol.islanddesk.ui.services.IslandServices
import kotlinx.coroutines.delay

private const val MAX_FOCUS_SECONDS = 99 * 3_600 + 59 * 60 + 59

class FocusModule(private val services: IslandServices) : IslandModule {
    override val id = "focus"
    override val title = "Focus"
    override val icon: ImageVector = Icons.Rounded.Timer
    override val priority = 70

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun PeekContent() {
        val activity = services.activity.current
        var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(activity?.id) {
            if (activity == null) return@LaunchedEffect
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(500L)
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

        val remaining = (activity.startedAtMs + activity.durationMs - nowMs).coerceAtLeast(0L)
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
                    text = LiveActivityController.format(remaining) + " left",
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
        var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var notified by remember(activity?.id) { mutableStateOf(false) }
        LaunchedEffect(activity?.id) {
            if (activity == null) return@LaunchedEffect
            while (true) {
                nowMs = System.currentTimeMillis()
                if (!notified && nowMs >= activity.startedAtMs + activity.durationMs) {
                    notified = true
                    services.notifications.push("Focus session complete")
                }
                delay(250L)
            }
        }

        if (activity != null) {
            RunningSession(
                title = activity.title,
                remainingMs = (activity.startedAtMs + activity.durationMs - nowMs).coerceAtLeast(0L),
                progress = if (activity.durationMs <= 0L) {
                    0f
                } else {
                    ((nowMs - activity.startedAtMs).coerceIn(0L, activity.durationMs).toFloat() /
                        activity.durationMs.toFloat())
                },
                endsAt = formatClockTime(activity.startedAtMs + activity.durationMs),
            )
            return
        }

        val defaultSeconds = services.settings.current.focusSeconds.coerceIn(1, MAX_FOCUS_SECONDS)
        var hoursText by remember(defaultSeconds) { mutableStateOf((defaultSeconds / 3_600).toString()) }
        var minutesText by remember(defaultSeconds) { mutableStateOf(((defaultSeconds % 3_600) / 60).toString()) }
        var secondsText by remember(defaultSeconds) { mutableStateOf((defaultSeconds % 60).toString()) }

        fun durationSeconds(): Int {
            val hours = hoursText.toIntOrNull()?.coerceIn(0, 99) ?: 0
            val minutes = minutesText.toIntOrNull()?.coerceIn(0, 59) ?: 0
            val seconds = secondsText.toIntOrNull()?.coerceIn(0, 59) ?: 0
            return (hours * 3_600 + minutes * 60 + seconds).coerceIn(0, MAX_FOCUS_SECONDS)
        }

        fun applyPreset(totalSeconds: Int) {
            hoursText = (totalSeconds / 3_600).toString()
            minutesText = ((totalSeconds % 3_600) / 60).toString()
            secondsText = (totalSeconds % 60).toString()
        }

        val total = durationSeconds()

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShapeAccent(size = 58.dp) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("Focus session", style = MaterialTheme.typography.titleLargeEmphasized)
            Text(
                text = "Type any duration — hours are not capped at 12",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DurationField(
                    label = "Hours",
                    value = hoursText,
                    onValueChange = { hoursText = it.filter { char -> char.isDigit() }.take(3) },
                )
                DurationField(
                    label = "Minutes",
                    value = minutesText,
                    onValueChange = { minutesText = it.filter { char -> char.isDigit() }.take(2) },
                )
                DurationField(
                    label = "Seconds",
                    value = secondsText,
                    onValueChange = { secondsText = it.filter { char -> char.isDigit() }.take(2) },
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(5, 15, 25, 45, 60).forEach { preset ->
                    FilterChip(
                        selected = total == preset * 60,
                        onClick = { applyPreset(preset * 60) },
                        label = {
                            Text(
                                text = if (preset < 60) preset.toString() + "m" else (preset / 60).toString() + "h",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = if (total > 0) "Ends at " + formatClockTime(System.currentTimeMillis() + total * 1_000L) else "Set a duration",
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val duration = total.coerceAtLeast(1)
                    services.settings.update { it.copy(focusSeconds = duration) }
                    services.activity.startSeconds("Focus", duration.toLong())
                },
                enabled = total > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start focus")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "The countdown stays visible in peek, with seconds.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun RunningSession(
        title: String,
        remainingMs: Long,
        progress: Float,
        endsAt: String,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = LiveActivityController.format(remainingMs),
                style = MaterialTheme.typography.displayMediumEmphasized.copy(
                    fontFeatureSettings = "tnum",
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Ends at " + endsAt,
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            LinearWavyProgressIndicator(
                progress = { progress },
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
                text = if (remainingMs == 0L) "Session complete" else "Stay focused.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DurationField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.width(84.dp),
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        textStyle = MaterialTheme.typography.titleMediumEmphasized.copy(
            fontSize = 20.sp,
            fontFeatureSettings = "tnum",
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
