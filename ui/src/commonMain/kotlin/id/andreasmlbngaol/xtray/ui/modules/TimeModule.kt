package id.andreasmlbngaol.xtray.ui.modules

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.andreasmlbngaol.xtray.core.IslandModule
import id.andreasmlbngaol.xtray.core.activity.LiveActivityController
import id.andreasmlbngaol.xtray.core.time.CountdownTimer
import id.andreasmlbngaol.xtray.core.time.Stopwatch
import id.andreasmlbngaol.xtray.core.time.TimeFormat
import id.andreasmlbngaol.xtray.platform.formatClockTime
import id.andreasmlbngaol.xtray.ui.services.IslandServices
import kotlinx.coroutines.delay

private const val MAX_FOCUS_SECONDS = 99 * 3_600 + 59 * 60 + 59

/** The three tools inside the Time tab. */
private enum class TimeTab(val label: String) {
    Stopwatch("Stopwatch"),
    Timer("Timer"),
    Focus("Focus"),
}

/**
 * Timekeeping: a stopwatch, a plain countdown, and the focus session that also
 * shows up in peek. The stopwatch and countdown state live in services so they
 * keep running when the panel closes.
 */
class TimeModule(private val services: IslandServices) : IslandModule {
    override val id = "time"
    override val title = "Time"
    override val icon: ImageVector = Icons.Rounded.Schedule
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
                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
                Column {
                    Text("Time", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Open the panel to start a stopwatch, timer or focus session",
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

    @Composable
    override fun ExpandedContent() {
        var tab by remember {
            mutableStateOf(
                if (services.activity.current != null) TimeTab.Focus else TimeTab.Stopwatch,
            )
        }
        Column(modifier = Modifier.fillMaxSize()) {
            TimeTabRow(selected = tab, onSelect = { tab = it })
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    TimeTab.Stopwatch -> StopwatchContent(services.stopwatch)
                    TimeTab.Timer -> TimerContent(services.timer)
                    TimeTab.Focus -> FocusContent(services)
                }
            }
        }
    }
}

@Composable
private fun TimeTabRow(selected: TimeTab, onSelect: (TimeTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TimeTab.entries.forEach { tab ->
            val active = tab == selected
            Surface(
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (active) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                contentColor = if (active) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StopwatchContent(stopwatch: Stopwatch) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(stopwatch.isRunning) {
        while (stopwatch.isRunning) {
            nowMs = System.currentTimeMillis()
            delay(100L)
        }
        nowMs = System.currentTimeMillis()
    }
    val elapsed = stopwatch.elapsedMs(nowMs)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShapeAccent(size = 58.dp) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = TimeFormat.elapsed(elapsed),
            style = MaterialTheme.typography.displayMediumEmphasized.copy(
                fontFeatureSettings = "tnum",
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { stopwatch.toggle() },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    when {
                        stopwatch.isRunning -> "Pause"
                        elapsed > 0L -> "Resume"
                        else -> "Start"
                    },
                )
            }
            if (elapsed > 0L) {
                FilledTonalButton(
                    onClick = { stopwatch.reset() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Reset")
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Keeps counting while the panel is closed.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimerContent(timer: CountdownTimer) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer.isRunning) {
        while (timer.isRunning) {
            nowMs = System.currentTimeMillis()
            delay(200L)
        }
        nowMs = System.currentTimeMillis()
    }

    when {
        timer.isRunning || timer.isPaused -> RunningTimer(timer = timer, nowMs = nowMs)
        timer.isFinished -> FinishedTimer(timer)
        else -> TimerSetup(timer)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RunningTimer(timer: CountdownTimer, nowMs: Long) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = TimeFormat.elapsed(timer.remainingMs(nowMs)),
            style = MaterialTheme.typography.displayMediumEmphasized.copy(
                fontFeatureSettings = "tnum",
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "of " + TimeFormat.elapsed(timer.durationMs),
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        LinearWavyProgressIndicator(
            progress = { timer.progress(nowMs) },
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (timer.isRunning) {
                FilledTonalButton(
                    onClick = { timer.pause() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Pause")
                }
            } else {
                Button(
                    onClick = { timer.resume() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Resume")
                }
            }
            FilledTonalButton(
                onClick = { timer.reset() },
                modifier = Modifier.weight(1f),
            ) {
                Text("Reset")
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "You will get a notification when it ends.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FinishedTimer(timer: CountdownTimer) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "00:00",
            style = MaterialTheme.typography.displayMediumEmphasized.copy(
                fontFeatureSettings = "tnum",
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Time's up",
            style = MaterialTheme.typography.titleMediumEmphasized,
        )
        Spacer(Modifier.height(14.dp))
        FilledTonalButton(onClick = { timer.reset() }) {
            Text("Done")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimerSetup(timer: CountdownTimer) {
    var minutesText by remember { mutableStateOf("5") }
    var secondsText by remember { mutableStateOf("0") }

    fun totalSeconds(): Int {
        val minutes = minutesText.toIntOrNull()?.coerceIn(0, 99) ?: 0
        val seconds = secondsText.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return minutes * 60 + seconds
    }

    fun applyPreset(total: Int) {
        minutesText = (total / 60).toString()
        secondsText = (total % 60).toString()
    }

    val total = totalSeconds()

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
        Text("Timer", style = MaterialTheme.typography.titleLargeEmphasized)
        Text(
            text = "Pick a duration, get a notification when it ends",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            listOf(1, 5, 10, 15, 25).forEach { preset ->
                FilterChip(
                    selected = total == preset * 60,
                    onClick = { applyPreset(preset * 60) },
                    label = {
                        Text(
                            text = preset.toString() + "m",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = if (total > 0) {
                "Ends at " + formatClockTime(System.currentTimeMillis() + total * 1_000L)
            } else {
                "Set a duration"
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { timer.start(total.coerceAtLeast(1) * 1_000L) },
            enabled = total > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start timer")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FocusContent(services: IslandServices) {
    val activity = services.activity.current
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activity?.id) {
        if (activity == null) return@LaunchedEffect
        while (true) {
            nowMs = System.currentTimeMillis()
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
            onStop = { services.activity.stop() },
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
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RunningSession(
    title: String,
    remainingMs: Long,
    progress: Float,
    endsAt: String,
    onStop: () -> Unit,
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
        FilledTonalButton(onClick = onStop) {
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
