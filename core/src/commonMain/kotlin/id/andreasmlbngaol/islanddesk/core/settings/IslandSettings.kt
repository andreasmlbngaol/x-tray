package id.andreasmlbngaol.islanddesk.core.settings

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { System, Light, Dark }

/**
 * User-facing preferences. Stored as JSON because the set is small, the file
 * stays human editable, and there is no relational data to query.
 */
@Serializable
data class IslandSettings(
    val version: Int = 1,
    val themeMode: ThemeMode = ThemeMode.System,
    val reduceMotion: Boolean = false,
    val doNotDisturb: Boolean = false,
    val startAtLogin: Boolean = false,
    val monitorIndex: Int = 0,
    val hoverIntentMs: Long = 180L,
    val collapseDelayMs: Long = 180L,
    val clipboardEnabled: Boolean = true,
    val clipboardMaxItems: Int = 10,
    val shelfEnabled: Boolean = true,
    val mediaEnabled: Boolean = true,
    val controlsEnabled: Boolean = true,
    val focusSeconds: Int = 1_500,
) {
    /** Keeps values inside sane bounds even if the file was edited by hand. */
    fun normalized(): IslandSettings = copy(
        monitorIndex = monitorIndex.coerceAtLeast(0),
        hoverIntentMs = hoverIntentMs.coerceIn(50L, 2_000L),
        collapseDelayMs = collapseDelayMs.coerceIn(0L, 5_000L),
        clipboardMaxItems = clipboardMaxItems.coerceIn(1, 50),
        focusSeconds = focusSeconds.coerceIn(1, 12 * 3_600),
    )
}
