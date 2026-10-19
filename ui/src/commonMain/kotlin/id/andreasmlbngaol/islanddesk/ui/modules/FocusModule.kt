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
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import id.andreasmlbngaol.islanddesk.core.IslandModule
import id.andreasmlbngaol.islanddesk.core.activity.LiveActivityController
import id.andreasmlbngaol.islanddesk.ui.services.IslandServices
import kotlinx.coroutines.delay

class FocusModule(private val services: IslandServices) : IslandModule {
    override val id = "focus"
    override val title = "Fokus"
    override val icon: ImageVector = Icons.Rounded.Timer
    override val priority = 70

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun PeekContent() {
        val activity = services.activity.current
        var tick by remember { mutableIntStateOf(0) }
        LaunchedEffect(activity?.id) {
            while (services.activity.current != null) {
                delay(1_000L)
                tick++
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
                    Text("Fokus", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Mulai sesi " + services.settings.current.focusMinutes + " menit",
                        style = MaterialTheme.typography.bodySmall,
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
                Text(activity.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = LiveActivityController.format(services.activity.remainingMs()) + " tersisa",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearWavyProgressIndicator(
                    progress = { services.activity.progress() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
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
            while (services.activity.current != null) {
                delay(1_000L)
                tick++
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (activity == null) {
                Spacer(Modifier.height(12.dp))
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text("Sesi fokus", style = MaterialTheme.typography.titleLargeEmphasized)
                Text(
                    "Timer tampil di island selama berjalan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = {
                    services.activity.start("Fokus", services.settings.current.focusMinutes)
                }) {
                    Text("Mulai " + services.settings.current.focusMinutes + " menit")
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = LiveActivityController.format(services.activity.remainingMs()),
                    style = MaterialTheme.typography.displayMediumEmphasized,
                )
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                LinearWavyProgressIndicator(
                    progress = { services.activity.progress() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { services.activity.stop() }) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Hentikan")
                    }
                }
                Text(
                    text = if (services.activity.isFinished()) "Sesi selesai" else "Tetap fokus.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
