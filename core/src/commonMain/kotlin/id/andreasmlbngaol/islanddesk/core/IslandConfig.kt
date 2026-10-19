package id.andreasmlbngaol.islanddesk.core

import id.andreasmlbngaol.islanddesk.core.settings.IslandSettings

data class IslandConfig(
    val hoverIntentMs: Long = 180L,
    val peekToExpandedMs: Long = 600L,
    val collapseDelayMs: Long = 350L,
) {
    companion object {
        fun from(settings: IslandSettings): IslandConfig = IslandConfig(
            hoverIntentMs = settings.hoverIntentMs,
            peekToExpandedMs = settings.peekToExpandedMs,
            collapseDelayMs = settings.collapseDelayMs,
        )
    }
}
