package id.andreasmlbngaol.islanddesk.core

/**
 * Timing for hover intent and collapse. All values are in milliseconds and can
 * be replaced by user settings later.
 */
data class IslandConfig(
    val hoverIntentMs: Long = 180L,
    val peekToExpandedMs: Long = 600L,
    val collapseDelayMs: Long = 350L,
)
