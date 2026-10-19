package id.andreasmlbngaol.xtray.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import id.andreasmlbngaol.xtray.ui.resources.Res
import id.andreasmlbngaol.xtray.ui.resources.google_sans_flex
import org.jetbrains.compose.resources.Font

/**
 * Google Sans Flex, pinned to the rounded axis (ROND 100). The font is
 * distributed under the SIL Open Font License 1.1; the license text lives in
 * docs/licenses/google_sans_flex_OFL.txt.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
private fun googleSansFamily(): FontFamily {
    @Composable
    fun font(weight: FontWeight) = Font(
        resource = Res.font.google_sans_flex,
        weight = weight,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight.weight),
            FontVariation.Setting("ROND", 100f),
        ),
    )

    return FontFamily(
        font(FontWeight.Light),
        font(FontWeight.Normal),
        font(FontWeight.Medium),
        font(FontWeight.SemiBold),
        font(FontWeight.Bold),
        font(FontWeight.ExtraBold),
        font(FontWeight.Black),
    )
}

@OptIn(ExperimentalTextApi::class)
private fun style(
    family: FontFamily,
    size: Int,
    weight: FontWeight,
    lineHeight: Int,
    tracking: Double,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.em,
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun islandTypography(): Typography {
    val family = googleSansFamily()
    return remember(family) {
        Typography(
            displayLarge = style(family, 56, FontWeight.Bold, 58, -0.03),
            displayMedium = style(family, 44, FontWeight.Bold, 48, -0.025),
            displaySmall = style(family, 36, FontWeight.Bold, 40, -0.02),
            headlineLarge = style(family, 30, FontWeight.SemiBold, 36, -0.015),
            headlineMedium = style(family, 26, FontWeight.SemiBold, 32, -0.01),
            headlineSmall = style(family, 22, FontWeight.SemiBold, 28, -0.005),
            titleLarge = style(family, 20, FontWeight.SemiBold, 26, 0.0),
            titleMedium = style(family, 16, FontWeight.SemiBold, 22, 0.0),
            titleSmall = style(family, 14, FontWeight.SemiBold, 19, 0.0),
            bodyLarge = style(family, 15, FontWeight.Normal, 22, 0.0),
            bodyMedium = style(family, 13, FontWeight.Normal, 18, 0.005),
            bodySmall = style(family, 11, FontWeight.Normal, 16, 0.01),
            labelLarge = style(family, 13, FontWeight.SemiBold, 18, 0.01),
            labelMedium = style(family, 11, FontWeight.SemiBold, 15, 0.02),
            labelSmall = style(family, 10, FontWeight.SemiBold, 14, 0.03),

            displayLargeEmphasized = style(family, 56, FontWeight.ExtraBold, 58, -0.03),
            displayMediumEmphasized = style(family, 44, FontWeight.ExtraBold, 48, -0.025),
            displaySmallEmphasized = style(family, 36, FontWeight.ExtraBold, 40, -0.02),
            headlineLargeEmphasized = style(family, 30, FontWeight.Bold, 36, -0.015),
            headlineMediumEmphasized = style(family, 26, FontWeight.Bold, 32, -0.01),
            headlineSmallEmphasized = style(family, 22, FontWeight.Bold, 28, -0.005),
            titleLargeEmphasized = style(family, 20, FontWeight.Bold, 26, 0.0),
            titleMediumEmphasized = style(family, 16, FontWeight.Bold, 22, 0.0),
            titleSmallEmphasized = style(family, 14, FontWeight.Bold, 19, 0.0),
            bodyLargeEmphasized = style(family, 15, FontWeight.Medium, 22, 0.0),
            bodyMediumEmphasized = style(family, 13, FontWeight.Medium, 18, 0.005),
            bodySmallEmphasized = style(family, 11, FontWeight.Medium, 16, 0.01),
            labelLargeEmphasized = style(family, 13, FontWeight.Bold, 18, 0.01),
            labelMediumEmphasized = style(family, 11, FontWeight.Bold, 15, 0.02),
            labelSmallEmphasized = style(family, 10, FontWeight.Bold, 14, 0.03),
        )
    }
}
