package id.andreasmlbngaol.islanddesk.ui.island

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.islanddesk.core.IslandState

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
        width = 220.dp,
        height = 38.dp,
        bottomRadius = 19.dp,
        filletRadius = 8.dp,
    )

    val Peek = IslandSize(
        width = 420.dp,
        height = 140.dp,
        bottomRadius = 34.dp,
        filletRadius = 12.dp,
    )

    val Expanded = IslandSize(
        width = 460.dp,
        height = 620.dp,
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
