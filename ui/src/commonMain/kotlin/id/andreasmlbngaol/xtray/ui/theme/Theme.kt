package id.andreasmlbngaol.xtray.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import id.andreasmlbngaol.xtray.core.settings.AccentColor

val LocalReduceMotion = staticCompositionLocalOf { false }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun XTrayTheme(
    darkTheme: Boolean = true,
    accent: AccentColor = AccentColor.Navy,
    customAccent: String = "",
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(accent, customAccent, darkTheme) {
        islandColorScheme(accent, darkTheme, customAccent)
    }
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = islandTypography(),
            shapes = IslandShapes,
            content = content,
        )
    }
}
