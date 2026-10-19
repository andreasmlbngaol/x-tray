package id.andreasmlbngaol.islanddesk.core

import id.andreasmlbngaol.islanddesk.core.settings.IslandSettings

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
