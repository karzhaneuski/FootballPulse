package com.artsiom.footballpulse.ui.matchdetails

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.google.android.material.color.MaterialColors
import com.google.android.material.elevation.ElevationOverlayProvider

internal data class MatchDetailsAppearance(
    val colors: ColorScheme,
    val cardColor: Color,
    val controlColor: Color
)

@Composable
internal fun matchDetailsAppearance(): MatchDetailsAppearance {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val baseColors = MaterialTheme.colorScheme
    val density = LocalDensity.current.density
    // Preserve the XML Material theme during fragment interop, including its tonal elevation overlay.
    return remember(context, configuration, baseColors, density) {
        fun color(attribute: Int) = MaterialColors.getColor(context, attribute, android.graphics.Color.BLACK)
        val surface = color(com.google.android.material.R.attr.colorSurface)
        MatchDetailsAppearance(
            colors = baseColors.copy(
                background = Color(color(android.R.attr.colorBackground)),
                surface = Color(surface),
                onSurface = Color(color(com.google.android.material.R.attr.colorOnSurfaceVariant)),
                onSurfaceVariant = Color(color(com.google.android.material.R.attr.colorOnSurfaceVariant)),
                primary = Color(color(androidx.appcompat.R.attr.colorPrimary)),
                onPrimary = Color(color(com.google.android.material.R.attr.colorOnPrimary)),
                outlineVariant = Color(color(com.google.android.material.R.attr.colorOutlineVariant))
            ),
            cardColor = Color(ElevationOverlayProvider(context).compositeOverlayIfNeeded(surface, 2 * density)),
            controlColor = Color(color(androidx.appcompat.R.attr.colorControlActivated))
        )
    }
}

internal fun matchTextStyle(
    size: Int,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    alignment: TextAlign = TextAlign.Start
) = TextStyle(
    color = color,
    fontSize = size.sp,
    fontWeight = weight,
    fontFamily = FontFamily.Default,
    textAlign = alignment,
    platformStyle = PlatformTextStyle(includeFontPadding = true)
)
