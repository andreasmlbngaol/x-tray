package id.andreasmlbngaol.xtray.core

import id.andreasmlbngaol.xtray.core.settings.IslandSettings

data class IslandConfig(
    val hoverIntentMs: Long = 180L,
    val collapseDelayMs: Long = 180L,
) {
    companion object {
        fun from(settings: IslandSettings): IslandConfig = IslandConfig(
            hoverIntentMs = settings.hoverIntentMs,
            collapseDelayMs = settings.collapseDelayMs,
        )
    }
}
