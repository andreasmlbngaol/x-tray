package id.andreasmlbngaol.islanddesk.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalReduceMotion = staticCompositionLocalOf { false }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IslandDeskTheme(
    darkTheme: Boolean = true,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialExpressiveTheme(
            colorScheme = if (darkTheme) IslandDarkColors else IslandLightColors,
            motionScheme = MotionScheme.expressive(),
            typography = islandTypography(),
            shapes = IslandShapes,
            content = content,
        )
    }
}
