package id.andreasmlbngaol.islanddesk.ui.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon

/**
 * A decorative shape from [MaterialShapes] used as a backdrop for empty states.
 * Material 3 Expressive leans on these organic silhouettes instead of plain
 * rounded rectangles, which is what keeps the modules from reading as vanilla
 * Material 3.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShapeAccent(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    shape: RoundedPolygon = MaterialShapes.Cookie9Sided,
    color: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color = color, shape = shape.toShape()),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
