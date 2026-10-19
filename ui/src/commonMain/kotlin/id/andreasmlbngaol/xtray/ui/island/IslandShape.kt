package id.andreasmlbngaol.xtray.ui.island

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * An island welded to the top edge of the screen.
 *
 * The silhouette is widest at y = 0 and narrows into the body through two
 * concave shoulders, so the island reads as growing out of the screen edge
 * instead of floating as a separate squircle. When [filletRadius] is zero it
 * degrades to a rounded rect.
 *
 * Keeping one Shape for every state is what makes collapsed -> peek ->
 * expanded feel like a single morph.
 */
class IslandShape(
    private val bottomRadius: Dp,
    private val filletRadius: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val bottom = with(density) { bottomRadius.toPx() }
            .coerceIn(0f, min(size.width / 2f, size.height / 2f))
        val fillet = with(density) { filletRadius.toPx() }
            .coerceIn(0f, min(size.width / 2f - bottom, size.height / 2f))

        if (fillet < 0.5f) {
            return RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = bottomRadius,
                bottomEnd = bottomRadius,
            ).createOutline(size, layoutDirection, density)
        }

        val path = Path().apply {
            moveTo(0f, 0f)
            // Concave shoulder: the top edge stays at y = 0 and the body
            // starts one fillet further in on both sides.
            quadraticTo(fillet, 0f, fillet, fillet)
            lineTo(fillet, size.height - bottom)
            quadraticTo(fillet, size.height, fillet + bottom, size.height)
            lineTo(size.width - fillet - bottom, size.height)
            quadraticTo(size.width - fillet, size.height, size.width - fillet, size.height - bottom)
            lineTo(size.width - fillet, fillet)
            quadraticTo(size.width - fillet, 0f, size.width, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}
