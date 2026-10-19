package id.andreasmlbngaol.islanddesk.ui.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import id.andreasmlbngaol.islanddesk.core.activity.LiveActivityController
import id.andreasmlbngaol.islanddesk.core.clipboard.ClipboardHistory
import id.andreasmlbngaol.islanddesk.core.notification.NotificationCenter
import id.andreasmlbngaol.islanddesk.core.settings.SettingsStore
import id.andreasmlbngaol.islanddesk.core.shelf.ShelfStore
import id.andreasmlbngaol.islanddesk.platform.autostart.AutostartManager
import id.andreasmlbngaol.islanddesk.platform.battery.BatterySource
import id.andreasmlbngaol.islanddesk.platform.battery.BatteryState
import id.andreasmlbngaol.islanddesk.platform.clipboard.ClipboardWatcher
import id.andreasmlbngaol.islanddesk.platform.files.FileOpener
import id.andreasmlbngaol.islanddesk.platform.media.MediaSource
import id.andreasmlbngaol.islanddesk.platform.media.MediaTrack
import id.andreasmlbngaol.islanddesk.platform.system.SystemControls
import id.andreasmlbngaol.islanddesk.platform.system.SystemState

/**
 * Runtime wiring shared by every module. Compose state lives here so modules
 * stay small and the app owns the lifecycle of system sources.
 */
class IslandServices(
    val settings: SettingsStore,
    val media: MediaSource,
    val clipboard: ClipboardWatcher,
    val clipboardHistory: ClipboardHistory,
    val shelf: ShelfStore,
    val battery: BatterySource,
    val controls: SystemControls,
    val activity: LiveActivityController,
    val notifications: NotificationCenter,
    val autostart: AutostartManager,
    val fileOpener: FileOpener,
) {
    var batteryState: BatteryState by mutableStateOf(BatteryState())
    var mediaTrack: MediaTrack? by mutableStateOf(null)
    var systemState: SystemState by mutableStateOf(SystemState())

    /** Bumped whenever clipboard or shelf contents change. */
    var revision: Int by mutableIntStateOf(0)
        private set

    var clipboardPaused: Boolean by mutableStateOf(false)

    fun bump() {
        revision++
    }

    fun refreshBattery() {
        batteryState = runCatching { battery.read() }.getOrDefault(BatteryState())
    }

    fun refreshSystem() {
        systemState = runCatching { controls.snapshot() }.getOrDefault(SystemState())
    }
}
