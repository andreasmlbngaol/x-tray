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
        width = 200.dp,
        height = 36.dp,
        bottomRadius = 18.dp,
        filletRadius = 8.dp,
    )

    val Peek = IslandSize(
        width = 380.dp,
        height = 100.dp,
        bottomRadius = 28.dp,
        filletRadius = 10.dp,
    )

    val Expanded = IslandSize(
        width = 440.dp,
        height = 600.dp,
        bottomRadius = 32.dp,
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
