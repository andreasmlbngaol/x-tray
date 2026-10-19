package id.andreasmlbngaol.islanddesk.core.settings

import kotlinx.serialization.Serializable

/**
 * User-facing preferences. Stored as JSON because the set is small, the file
 * stays human editable, and there is no relational data to query.
 */
@Serializable
data class IslandSettings(
    val version: Int = 1,
    val reduceMotion: Boolean = false,
    val doNotDisturb: Boolean = false,
    val startAtLogin: Boolean = false,
    val monitorIndex: Int = 0,
    val hoverIntentMs: Long = 180L,
    val peekToExpandedMs: Long = 600L,
    val collapseDelayMs: Long = 350L,
    val clipboardEnabled: Boolean = true,
    val clipboardMaxItems: Int = 10,
    val shelfEnabled: Boolean = true,
    val mediaEnabled: Boolean = true,
    val controlsEnabled: Boolean = true,
    val focusMinutes: Int = 25,
) {
    /** Keeps values inside sane bounds even if the file was edited by hand. */
    fun normalized(): IslandSettings = copy(
        monitorIndex = monitorIndex.coerceAtLeast(0),
        hoverIntentMs = hoverIntentMs.coerceIn(50L, 2_000L),
        peekToExpandedMs = peekToExpandedMs.coerceIn(100L, 5_000L),
        collapseDelayMs = collapseDelayMs.coerceIn(0L, 5_000L),
        clipboardMaxItems = clipboardMaxItems.coerceIn(1, 50),
        focusMinutes = focusMinutes.coerceIn(1, 180),
    )
}
