package id.andreasmlbngaol.islanddesk.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import id.andreasmlbngaol.islanddesk.core.settings.AccentColor

/**
 * The island ships a small set of accent families instead of a full dynamic
 * color engine. Each family is described by a hue plus a saturation weight,
 * and the tonal roles are generated from it so every accent keeps the same
 * contrast relationships as the hand-tuned navy default.
 */
data class AccentSpec(
    val hue: Float,
    val saturation: Float,
)

/** Parses #RRGGBB or RRGGBB; returns null for anything else. */
fun parseHexColor(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6 || clean.any { it.digitToIntOrNull(16) == null }) return null
    val value = clean.toLongOrNull(16) ?: return null
    return Color(0xFF000000L or value)
}

/** Derives hue and saturation from an arbitrary colour so custom accents reuse the same generator. */
fun accentSpecFromColor(color: Color): AccentSpec {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val lightness = (max + min) / 2f
    val delta = max - min
    val saturation = if (delta == 0f) 0f else delta / (1f - kotlin.math.abs(2f * lightness - 1f))
    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    return AccentSpec(
        hue = hue.mod(360f),
        saturation = saturation.coerceIn(0.25f, 0.90f),
    )
}

fun accentSpec(accent: AccentColor): AccentSpec = when (accent) {
    AccentColor.Navy -> AccentSpec(hue = 215f, saturation = 0.62f)
    AccentColor.Violet -> AccentSpec(hue = 265f, saturation = 0.58f)
    AccentColor.Emerald -> AccentSpec(hue = 158f, saturation = 0.52f)
    AccentColor.Amber -> AccentSpec(hue = 35f, saturation = 0.85f)
    AccentColor.Rose -> AccentSpec(hue = 340f, saturation = 0.60f)
}

/** Pure accent used for swatches in Settings. */
fun accentSwatch(accent: AccentColor, dark: Boolean): Color {
    val spec = accentSpec(accent)
    return if (dark) {
        Color.hsl(spec.hue, spec.saturation * 0.55f, 0.70f)
    } else {
        Color.hsl(spec.hue, spec.saturation * 0.75f, 0.38f)
    }
}

fun islandColorScheme(accent: AccentColor, dark: Boolean, customHex: String = ""): ColorScheme {
    val custom = parseHexColor(customHex)?.let { accentSpecFromColor(it) }
    val spec = custom ?: accentSpec(accent)
    val hue = spec.hue
    val saturation = spec.saturation
    val complement = (hue + 150f) % 360f
    return if (dark) darkScheme(hue, saturation, complement) else lightScheme(hue, saturation, complement)
}

private fun darkScheme(hue: Float, saturation: Float, complement: Float): ColorScheme {
    val surface = hsl(hue, 0.16f, 0.05f)
    val onSurface = hsl(hue, 0.10f, 0.91f)
    return darkColorScheme(
        primary = hsl(hue, saturation * 0.55f, 0.78f),
        onPrimary = hsl(hue, saturation * 0.75f, 0.18f),
        primaryContainer = hsl(hue, saturation * 0.70f, 0.32f),
        onPrimaryContainer = hsl(hue, saturation * 0.55f, 0.91f),
        secondary = hsl(hue, 0.10f, 0.78f),
        onSecondary = hsl(hue, 0.12f, 0.20f),
        secondaryContainer = hsl(hue, 0.14f, 0.32f),
        onSecondaryContainer = hsl(hue, 0.12f, 0.90f),
        tertiary = hsl(complement, 0.45f, 0.75f),
        onTertiary = hsl(complement, 0.60f, 0.18f),
        tertiaryContainer = hsl(complement, 0.50f, 0.30f),
        onTertiaryContainer = hsl(complement, 0.50f, 0.90f),
        background = surface,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = hsl(hue, 0.12f, 0.20f),
        onSurfaceVariant = hsl(hue, 0.08f, 0.78f),
        surfaceTint = hsl(hue, saturation * 0.55f, 0.78f),
        inverseSurface = onSurface,
        inverseOnSurface = hsl(hue, 0.12f, 0.20f),
        inversePrimary = hsl(hue, saturation * 0.65f, 0.42f),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = hsl(hue, 0.06f, 0.58f),
        outlineVariant = hsl(hue, 0.08f, 0.28f),
        scrim = Color.Black,
        surfaceBright = hsl(hue, 0.10f, 0.22f),
        surfaceDim = surface,
        surfaceContainerLowest = hsl(hue, 0.18f, 0.035f),
        surfaceContainerLow = hsl(hue, 0.16f, 0.075f),
        surfaceContainer = hsl(hue, 0.14f, 0.10f),
        surfaceContainerHigh = hsl(hue, 0.13f, 0.14f),
        surfaceContainerHighest = hsl(hue, 0.12f, 0.18f),
    )
}

private fun lightScheme(hue: Float, saturation: Float, complement: Float): ColorScheme {
    val surface = hsl(hue, 0.24f, 0.985f)
    return lightColorScheme(
        primary = hsl(hue, saturation * 0.75f, 0.34f),
        onPrimary = Color.White,
        primaryContainer = hsl(hue, saturation * 0.70f, 0.90f),
        onPrimaryContainer = hsl(hue, saturation * 0.75f, 0.14f),
        secondary = hsl(hue, 0.18f, 0.38f),
        onSecondary = Color.White,
        secondaryContainer = hsl(hue, 0.20f, 0.90f),
        onSecondaryContainer = hsl(hue, 0.22f, 0.16f),
        tertiary = hsl(complement, 0.55f, 0.32f),
        onTertiary = Color.White,
        tertiaryContainer = hsl(complement, 0.45f, 0.88f),
        onTertiaryContainer = hsl(complement, 0.55f, 0.14f),
        background = surface,
        onBackground = hsl(hue, 0.15f, 0.11f),
        surface = surface,
        onSurface = hsl(hue, 0.15f, 0.11f),
        surfaceVariant = hsl(hue, 0.15f, 0.90f),
        onSurfaceVariant = hsl(hue, 0.10f, 0.28f),
        surfaceTint = hsl(hue, saturation * 0.75f, 0.34f),
        inverseSurface = hsl(hue, 0.12f, 0.19f),
        inverseOnSurface = hsl(hue, 0.15f, 0.95f),
        inversePrimary = hsl(hue, saturation * 0.55f, 0.78f),
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        outline = hsl(hue, 0.08f, 0.46f),
        outlineVariant = hsl(hue, 0.12f, 0.80f),
        scrim = Color.Black,
        surfaceBright = Color.White,
        surfaceDim = hsl(hue, 0.15f, 0.86f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = hsl(hue, 0.20f, 0.965f),
        surfaceContainer = hsl(hue, 0.18f, 0.94f),
        surfaceContainerHigh = hsl(hue, 0.16f, 0.915f),
        surfaceContainerHighest = hsl(hue, 0.15f, 0.89f),
    )
}

private fun hsl(hue: Float, saturation: Float, lightness: Float): Color =
    Color.hsl(hue = hue.mod(360f), saturation = saturation.coerceIn(0f, 1f), lightness = lightness.coerceIn(0f, 1f))
