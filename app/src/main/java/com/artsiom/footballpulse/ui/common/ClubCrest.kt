package com.artsiom.footballpulse.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.ui.theme.FootballPulseTheme

@Composable
fun ClubCrest(crestUrl: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    // Use the application's ImageLoader so SVG decoding and caching match the existing Views.
    SubcomposeAsyncImage(
        model = crestUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        loading = { CrestPlaceholder() },
        error = { CrestPlaceholder() }
    )
}

@Composable
private fun CrestPlaceholder() {
    Box(Modifier.fillMaxSize().background(colorResource(R.color.color_divider), CircleShape))
}

@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun ClubCrestPreview() {
    FootballPulseTheme {
        Surface {
            ClubCrest(
                crestUrl = null,
                contentDescription = "Club crest",
                modifier = Modifier.padding(16.dp).size(48.dp)
            )
        }
    }
}
