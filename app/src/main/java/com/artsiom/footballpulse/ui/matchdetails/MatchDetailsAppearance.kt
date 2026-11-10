package com.artsiom.footballpulse.ui.matchdetails

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

internal data class MatchDetailsAppearance(
    val colors: ColorScheme,
    val cardColor: Color,
    // The theme's real onSurface; `colors.onSurface` is remapped to onSurfaceVariant below.
    val titleColor: Color
)

@Composable
internal fun matchDetailsAppearance(): MatchDetailsAppearance {
    val base = MaterialTheme.colorScheme
    return remember(base) {
        MatchDetailsAppearance(
            // The reviewed look uses the muted on-surface tone for body text on every screen.
            colors = base.copy(onSurface = base.onSurfaceVariant),
            cardColor = base.surface,
            titleColor = base.onSurface
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
