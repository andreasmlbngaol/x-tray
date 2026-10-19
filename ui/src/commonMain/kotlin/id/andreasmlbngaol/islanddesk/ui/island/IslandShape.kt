package id.andreasmlbngaol.islanddesk.ui.island

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
 * A top-attached island: flat top edge, concave shoulders (fillets) and a
 * rounded bottom. When [filletRadius] is zero it degrades to a rounded rect.
 *
 * Keeping this as one Shape (rather than switching composables) is what makes
 * the collapsed -> peek -> expanded transition feel like a single morph.
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
        val fillet = with(density) { filletRadius.toPx() }
            .coerceIn(0f, min(size.width / 2f, size.height / 2f))
        val bottom = with(density) { bottomRadius.toPx() }
            .coerceIn(0f, min(size.width / 2f, size.height / 2f))

        if (fillet < 0.5f) {
            return RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = bottomRadius,
                bottomEnd = bottomRadius,
            ).createOutline(size, layoutDirection, density)
        }

        val path = Path().apply {
            moveTo(0f, fillet)
            quadraticTo(0f, 0f, fillet, 0f)
            lineTo(size.width - fillet, 0f)
            quadraticTo(size.width, 0f, size.width, fillet)
            lineTo(size.width, size.height - bottom)
            quadraticTo(size.width, size.height, size.width - bottom, size.height)
            lineTo(bottom, size.height)
            quadraticTo(0f, size.height, 0f, size.height - bottom)
            close()
        }
        return Outline.Generic(path)
    }
}
