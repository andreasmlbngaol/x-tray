package id.andreasmlbngaol.xtray.ui.modules

import id.andreasmlbngaol.xtray.core.IslandModule
import id.andreasmlbngaol.xtray.core.settings.IslandSettings
import id.andreasmlbngaol.xtray.ui.services.IslandServices

object ModuleCatalog {
    fun create(services: IslandServices): List<IslandModule> = listOf(
        MediaModule(services),
        FocusModule(services),
        ClipboardModule(services),
        ShelfModule(services),
        ControlsModule(services),
        SettingsModule(services),
    )

    fun filter(modules: List<IslandModule>, settings: IslandSettings): List<IslandModule> =
        modules.filter { module ->
            when (module.id) {
                "media" -> settings.mediaEnabled
                "clipboard" -> settings.clipboardEnabled
                "shelf" -> settings.shelfEnabled
                "controls" -> settings.controlsEnabled
                else -> true
            }
        }
}
