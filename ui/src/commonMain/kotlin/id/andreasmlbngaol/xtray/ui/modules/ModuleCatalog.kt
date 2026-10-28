package id.andreasmlbngaol.xtray.ui.modules

import id.andreasmlbngaol.xtray.core.IslandModule
import id.andreasmlbngaol.xtray.core.settings.IslandSettings
import id.andreasmlbngaol.xtray.ui.services.IslandServices

object ModuleCatalog {
    /**
     * Media has no rail entry of its own: the player card lives at the bottom
     * of Controls.
     */
    fun create(services: IslandServices): List<IslandModule> = listOf(
        ControlsModule(services),
        CalendarModule(),
        NotificationsModule(services),
        ClipboardModule(services),
        TimeModule(services),
        ShelfModule(services),
        SettingsModule(services),
    )

    fun filter(modules: List<IslandModule>, settings: IslandSettings): List<IslandModule> =
        modules.filter { module ->
            when (module.id) {
                "clipboard" -> settings.clipboardEnabled
                "shelf" -> settings.shelfEnabled
                "controls" -> settings.controlsEnabled
                else -> true
            }
        }
}
