package id.andreasmlbngaol.xtray.ui.island

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.xtray.core.IslandState

data class IslandSize(
    val width: Dp,
    val height: Dp,
    val bottomRadius: Dp,
    val filletRadius: Dp,
)

object IslandDimens {
    /** Transparent margin around the island so the shadow is not clipped. */
    val WindowHorizontalPadding = 12.dp
    val WindowBottomPadding = 12.dp
    val WindowTopPadding = 0.dp

    val Collapsed = IslandSize(
        width = 300.dp,
        height = 38.dp,
        bottomRadius = 19.dp,
        filletRadius = 12.dp,
    )

    /**
     * Notification card: wider and taller than the collapsed bar so a sender,
     * a title and a couple of body lines fit without truncating to one word.
     */
    val Notification = IslandSize(
        width = 384.dp,
        height = 100.dp,
        bottomRadius = 30.dp,
        filletRadius = 12.dp,
    )

    /**
     * Dormant form used while a maximized or fullscreen window is focused.
     * A 120x6 dp hairline keeps the island reachable (hover opens peek) while
     * covering practically nothing of the window underneath.
     */
    val Dormant = IslandSize(
        width = 120.dp,
        height = 6.dp,
        bottomRadius = 3.dp,
        filletRadius = 2.dp,
    )

    val Peek = IslandSize(
        width = 420.dp,
        height = 140.dp,
        bottomRadius = 34.dp,
        filletRadius = 12.dp,
    )

    val Expanded = IslandSize(
        width = 460.dp,
        // Tall enough for Controls with the player card and its progress rows.
        height = 660.dp,
        bottomRadius = 34.dp,
        filletRadius = 12.dp,
    )

    fun forState(state: IslandState): IslandSize = when (state) {
        IslandState.Collapsed -> Collapsed
        IslandState.Peek -> Peek
        IslandState.Expanded -> Expanded
    }

    fun windowWidth(size: IslandSize): Dp = size.width + WindowHorizontalPadding * 2
    fun windowHeight(size: IslandSize): Dp = size.height + WindowTopPadding + WindowBottomPadding
}
