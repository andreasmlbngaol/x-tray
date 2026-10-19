package id.andreasmlbngaol.islanddesk.ui.island

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import id.andreasmlbngaol.islanddesk.platform.ClockText
import id.andreasmlbngaol.islanddesk.platform.currentClockText
import kotlinx.coroutines.delay

/**
 * Ticks once per second, aligned to the wall clock. This is the only recurring
 * timer in the collapsed state; everything else is event-driven.
 */
@Composable
fun rememberClockText(): ClockText {
    var value by remember { mutableStateOf(currentClockText()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            delay(1_000L - (now % 1_000L))
            value = currentClockText()
        }
    }
    return value
}
