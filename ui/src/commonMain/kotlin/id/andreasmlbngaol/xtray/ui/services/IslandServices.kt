package id.andreasmlbngaol.xtray.ui.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import id.andreasmlbngaol.xtray.core.activity.LiveActivityController
import id.andreasmlbngaol.xtray.core.clipboard.ClipboardHistory
import id.andreasmlbngaol.xtray.core.notification.NotificationCenter
import id.andreasmlbngaol.xtray.core.settings.SettingsStore
import id.andreasmlbngaol.xtray.core.shelf.ShelfStore
import id.andreasmlbngaol.xtray.platform.autostart.AutostartManager
import id.andreasmlbngaol.xtray.platform.battery.BatterySource
import id.andreasmlbngaol.xtray.platform.battery.BatteryState
import id.andreasmlbngaol.xtray.platform.clipboard.ClipboardWatcher
import id.andreasmlbngaol.xtray.platform.files.FileOpener
import id.andreasmlbngaol.xtray.platform.media.MediaSource
import id.andreasmlbngaol.xtray.platform.media.MediaTrack
import id.andreasmlbngaol.xtray.platform.network.NetworkSpeed
import id.andreasmlbngaol.xtray.platform.network.NetworkSpeedSource
import id.andreasmlbngaol.xtray.platform.notifications.SystemNotificationServer
import id.andreasmlbngaol.xtray.platform.system.PowerControls
import id.andreasmlbngaol.xtray.platform.system.SystemControls
import id.andreasmlbngaol.xtray.platform.system.SystemState

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
    val network: NetworkSpeedSource,
    val power: PowerControls,
    val notificationServer: SystemNotificationServer,
) {
    var batteryState: BatteryState by mutableStateOf(BatteryState())
    var mediaTrack: MediaTrack? by mutableStateOf(null)
    var systemState: SystemState by mutableStateOf(SystemState())
    var networkSpeed: NetworkSpeed by mutableStateOf(NetworkSpeed())

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

    fun refreshNetwork() {
        networkSpeed = runCatching { network.sample() }.getOrDefault(NetworkSpeed())
    }
}
