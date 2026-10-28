package id.andreasmlbngaol.xtray.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow

/**
 * Liquid Glass surface for the island.
 *
 * The island is a transparent window, so the backdrop stays transparent: the
 * compositor composites whatever is actually behind the island (wallpaper, a
 * window, the panel) through the tint. Only the accent glow is drawn in-app,
 * plus a rim highlight and inner shadow for the glass edge.
 */
@Composable
fun Modifier.glassSurface(shape: Shape): Modifier {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val backdrop = remember(dark, scheme.primary, scheme.tertiary) {
        GlassBackdrop(
            dark = dark,
            primary = scheme.primary,
            tertiary = scheme.tertiary,
        )
    }

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {},
        highlight = {
            Highlight(
                width = 1.dp,
                blurRadius = 1.5.dp,
                alpha = if (dark) 0.85f else 0.70f,
                style = HighlightStyle.Default(angle = 62f, falloff = 0.85f),
            )
        },
        innerShadow = {
            InnerShadow(
                radius = 16.dp,
                offset = DpOffset(0.dp, 6.dp),
                color = Color.Black.copy(alpha = if (dark) 0.28f else 0.10f),
            )
        },
        onDrawSurface = {
            // Tint only; the pixels behind the window are supplied by the
            // compositor, so no backdrop image is drawn here.
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        if (dark) Color.Black.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.42f),
                        if (dark) Color.Black.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.60f),
                    )
                )
            )
            drawRect(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (dark) 0.08f else 0.16f),
                        Color.Transparent,
                        Color.Transparent,
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height * 0.85f),
                )
            )
        },
    )
}

private class GlassBackdrop(
    private val dark: Boolean,
    private val primary: Color,
    private val tertiary: Color,
) : Backdrop {

    override val isCoordinatesDependent: Boolean = false

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        // A soft accent wash keeps the glass tied to the palette without
        // hiding what is behind the window.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = if (dark) 0.16f else 0.10f), Color.Transparent),
                center = Offset(size.width * 0.18f, -size.height * 0.25f),
                radius = size.width * 0.62f,
            ),
            radius = size.width * 0.62f,
            center = Offset(size.width * 0.18f, -size.height * 0.25f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = if (dark) 0.10f else 0.07f), Color.Transparent),
                center = Offset(size.width * 0.92f, size.height * 1.05f),
                radius = size.width * 0.55f,
            ),
            radius = size.width * 0.55f,
            center = Offset(size.width * 0.92f, size.height * 1.05f),
        )
    }
}
