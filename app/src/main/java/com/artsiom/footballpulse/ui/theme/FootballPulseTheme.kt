package com.artsiom.footballpulse.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artsiom.footballpulse.R

// Keep these palettes aligned with values/colors.xml and values-night/colors.xml during migration.
private val LIGHT_COLORS = lightColorScheme(
    primary = Color(color = 0xFF1565C0),
    onPrimary = Color(color = 0xFFFFFFFF),
    primaryContainer = Color(color = 0xFFEEEEEE),
    onPrimaryContainer = Color(color = 0xFF1565C0),
    secondary = Color(color = 0xFF4CAF50),
    onSecondary = Color(color = 0xFF000000),
    secondaryContainer = Color(color = 0xFFEEEEEE),
    onSecondaryContainer = Color(color = 0xFF212121),
    tertiary = Color(color = 0xFFFF9800),
    onTertiary = Color(color = 0xFF000000),
    tertiaryContainer = Color(color = 0xFFEEEEEE),
    onTertiaryContainer = Color(color = 0xFF212121),
    background = Color(color = 0xFFF5F5F5),
    onBackground = Color(color = 0xFF212121),
    surface = Color(color = 0xFFFFFFFF),
    onSurface = Color(color = 0xFF212121),
    surfaceVariant = Color(color = 0xFFEEEEEE),
    onSurfaceVariant = Color(color = 0xFF757575),
    surfaceTint = Color(color = 0xFF1565C0),
    surfaceContainerLowest = Color(color = 0xFFFFFFFF),
    surfaceContainerLow = Color(color = 0xFFFAFAFA),
    surfaceContainer = Color(color = 0xFFF5F5F5),
    surfaceContainerHigh = Color(color = 0xFFEEEEEE),
    surfaceContainerHighest = Color(color = 0xFFE0E0E0),
    inverseSurface = Color(color = 0xFF212121),
    inverseOnSurface = Color(color = 0xFFFFFFFF),
    inversePrimary = Color(color = 0xFF2196F3),
    error = Color(color = 0xFFF44336),
    onError = Color(color = 0xFF000000),
    errorContainer = Color(color = 0xFFEEEEEE),
    onErrorContainer = Color(color = 0xFF212121),
    outline = Color(color = 0xFF757575),
    outlineVariant = Color(color = 0xFFE0E0E0),
    scrim = Color(color = 0xFF000000)
)

private val DARK_COLORS = darkColorScheme(
    primary = Color(color = 0xFF2196F3),
    onPrimary = Color(color = 0xFF000000),
    primaryContainer = Color(color = 0xFF1565C0),
    onPrimaryContainer = Color(color = 0xFFFFFFFF),
    secondary = Color(color = 0xFF4CAF50),
    onSecondary = Color(color = 0xFF000000),
    secondaryContainer = Color(color = 0xFF2C2C2C),
    onSecondaryContainer = Color(color = 0xFF4CAF50),
    tertiary = Color(color = 0xFFFF9800),
    onTertiary = Color(color = 0xFF000000),
    tertiaryContainer = Color(color = 0xFF2C2C2C),
    onTertiaryContainer = Color(color = 0xFFFF9800),
    background = Color(color = 0xFF121212),
    onBackground = Color(color = 0xFFFFFFFF),
    surface = Color(color = 0xFF1E1E1E),
    onSurface = Color(color = 0xFFFFFFFF),
    surfaceVariant = Color(color = 0xFF2C2C2C),
    onSurfaceVariant = Color(color = 0xFFB0B0B0),
    surfaceTint = Color(color = 0xFF2196F3),
    surfaceContainerLowest = Color(color = 0xFF121212),
    surfaceContainerLow = Color(color = 0xFF1A1A1A),
    surfaceContainer = Color(color = 0xFF1E1E1E),
    surfaceContainerHigh = Color(color = 0xFF2C2C2C),
    surfaceContainerHighest = Color(color = 0xFF2C2C2C),
    inverseSurface = Color(color = 0xFFF5F5F5),
    inverseOnSurface = Color(color = 0xFF212121),
    inversePrimary = Color(color = 0xFF1565C0),
    error = Color(color = 0xFFF44336),
    onError = Color(color = 0xFF000000),
    errorContainer = Color(color = 0xFF2C2C2C),
    onErrorContainer = Color(color = 0xFFFFFFFF),
    outline = Color(color = 0xFFB0B0B0),
    outlineVariant = Color(color = 0xFF2C2C2C),
    scrim = Color(color = 0xFF000000)
)

@Composable
fun FootballPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = if (darkTheme) DARK_COLORS else LIGHT_COLORS, content = content)
}

@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun FootballPulseThemePreview() {
    FootballPulseTheme {
        Surface {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.retry), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
