package com.artsiom.footballpulse.ui.matchdetails

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.MatchDetail
import com.artsiom.footballpulse.ui.common.LoadingState
import com.artsiom.footballpulse.ui.presentation.MatchStatusStyle
import com.artsiom.footballpulse.ui.theme.FootballPulseTheme

@Composable
fun MatchDetailsScreen(
    state: MatchDetailsUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appearance = matchDetailsAppearance()
    val typography = MaterialTheme.typography.copy(
        bodyLarge = matchTextStyle(14, appearance.colors.onSurfaceVariant, alignment = TextAlign.Center),
        labelLarge = matchTextStyle(14, appearance.colors.onPrimary, FontWeight.Medium, TextAlign.Center)
            .copy(letterSpacing = 0.1.sp, lineHeight = 20.sp)
    )
    MaterialTheme(colorScheme = appearance.colors, typography = typography) {
        Surface(modifier.fillMaxSize(), color = appearance.colors.background) {
            when (state) {
                MatchDetailsUiState.Loading -> MaterialTheme(
                    colorScheme = appearance.colors.copy(primary = appearance.controlColor)
                ) { LoadingState() }
                is MatchDetailsUiState.Error -> MatchDetailsError(state.message, onRetry)
                is MatchDetailsUiState.Success -> MatchDetailsContent(state.content, onBack, appearance)
            }
        }
    }
}

@Composable
private fun MatchDetailsContent(model: MatchDetailsUiModel, onBack: () -> Unit, appearance: MatchDetailsAppearance) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        BasicText(
            stringResource(R.string.match_details_back),
            modifier = Modifier.clickable(onClick = onBack).padding(16.dp),
            style = matchTextStyle(14, colorResource(R.color.color_tab_selected_bg))
        )
        MatchHeader(model, appearance)
        MatchScoreCard(model, appearance)
    }
}

@Composable
private fun MatchHeader(model: MatchDetailsUiModel, appearance: MatchDetailsAppearance) {
    Surface(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        shape = RoundedCornerShape(8.dp),
        color = appearance.cardColor,
        border = BorderStroke(
            with(LocalDensity.current) { 1.dp.roundToPx().toDp() }, appearance.colors.outlineVariant
        ),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MatchTeam(model.match.homeTeam, model.match.homeTeamCrest, true, appearance, Modifier.weight(1f))
                BasicText(
                    model.scoreOrTime,
                    modifier = Modifier.widthIn(min = 80.dp).padding(horizontal = 8.dp),
                    style = matchTextStyle(22, appearance.colors.onSurface, FontWeight.Bold, TextAlign.Center)
                )
                MatchTeam(model.match.awayTeam, model.match.awayTeamCrest, false, appearance, Modifier.weight(1f))
            }
            BasicText(
                model.dateLabel,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                style = matchTextStyle(12, appearance.colors.onSurfaceVariant, alignment = TextAlign.Center)
            )
            val background = when (model.statusStyle) {
                MatchStatusStyle.FINISHED -> colorResource(R.color.color_status_finished_bg)
                MatchStatusStyle.LIVE -> colorResource(R.color.color_status_live_bg)
                MatchStatusStyle.SCHEDULED -> colorResource(R.color.color_status_scheduled_bg)
            }
            val foreground = if (model.statusStyle == MatchStatusStyle.FINISHED) {
                colorResource(R.color.color_status_finished_text)
            } else Color.White
            Spacer(Modifier.height(6.dp))
            BasicText(
                model.match.status,
                modifier = Modifier.background(background, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                style = matchTextStyle(10, foreground, FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun MatchTeam(
    name: String,
    crest: String?,
    home: Boolean,
    appearance: MatchDetailsAppearance,
    modifier: Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (home) {
            MatchCrest(crest)
            Spacer(Modifier.width(8.dp))
        }
        BasicText(
            name,
            modifier = Modifier.weight(1f),
            style = matchTextStyle(15, appearance.colors.onSurface, FontWeight.Bold,
                if (home) TextAlign.Start else TextAlign.End),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (!home) {
            Spacer(Modifier.width(8.dp))
            MatchCrest(crest)
        }
    }
}

@Composable
private fun MatchScoreCard(model: MatchDetailsUiModel, appearance: MatchDetailsAppearance) {
    Surface(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        shape = RoundedCornerShape(8.dp),
        color = appearance.cardColor,
        border = BorderStroke(
            with(LocalDensity.current) { 1.dp.roundToPx().toDp() }, appearance.colors.outlineVariant
        ),
        shadowElevation = 2.dp
    ) {
        Column {
            BasicText(
                stringResource(R.string.match_details_score),
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
                style = matchTextStyle(12, appearance.colors.onSurfaceVariant, FontWeight.Bold)
            )
            HorizontalDivider(color = colorResource(R.color.color_divider), thickness = 1.dp)
            if (model.notStarted) {
                BasicText(
                    stringResource(R.string.match_not_started),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    style = matchTextStyle(
                        13, colorResource(R.color.color_text_secondary), alignment = TextAlign.Center
                    )
                )
            } else {
                val divider = with(LocalDensity.current) { (0.5f * density).toInt().coerceAtLeast(1).toDp() }
                model.scores.forEachIndexed { index, score ->
                    if (index > 0) HorizontalDivider(color = colorResource(R.color.color_divider), thickness = divider)
                    MatchScoreRow(score, appearance)
                }
            }
        }
    }
}

@Composable
private fun MatchScoreRow(score: ScoreRowUiModel, appearance: MatchDetailsAppearance) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        BasicText(stringResource(score.label), Modifier.weight(1f),
            style = matchTextStyle(13, colorResource(R.color.color_text_hint)))
        BasicText(score.home, Modifier.widthIn(min = 28.dp),
            style = matchTextStyle(14, appearance.colors.onSurface, FontWeight.Bold, TextAlign.End))
        BasicText(stringResource(R.string.match_details_score_separator),
            style = matchTextStyle(14, appearance.colors.onSurface, alignment = TextAlign.Center))
        BasicText(score.away, Modifier.widthIn(min = 28.dp),
            style = matchTextStyle(14, appearance.colors.onSurface, FontWeight.Bold))
    }
}

@Composable
private fun MatchCrest(url: String?) {
    val context = LocalContext.current
    val request = remember(context, url) { ImageRequest.Builder(context).data(url).crossfade(true).build() }
    val placeholder = colorResource(R.color.color_status_finished_bg)
    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        modifier = Modifier.size(32.dp),
        contentScale = ContentScale.Fit,
        loading = { Box(Modifier.fillMaxSize().background(placeholder, CircleShape)) },
        error = { Box(Modifier.fillMaxSize().background(placeholder, CircleShape)) }
    )
}

@Preview(name = "Loading light", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 360, heightDp = 640)
@Preview(name = "Loading dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 640)
@Composable
internal fun MatchDetailsLoadingPreview() {
    FootballPulseTheme { MatchDetailsScreen(MatchDetailsUiState.Loading, {}, {}) }
}

@Preview(name = "Error light", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 360, heightDp = 640)
@Preview(name = "Error dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 640)
@Composable
internal fun MatchDetailsErrorPreview() {
    FootballPulseTheme {
        MatchDetailsScreen(MatchDetailsUiState.Error(stringResource(R.string.match_details_preview_error)), {}, {})
    }
}

@Preview(name = "Success light", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 360, heightDp = 640)
@Preview(name = "Success dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 640)
@Composable
internal fun MatchDetailsSuccessPreview() {
    val match = MatchDetail(42, "Home", "Away", 2, 1, "2025-08-10T12:00:00Z", "FINISHED",
        1, 0, 2, 1, null, null)
    FootballPulseTheme { MatchDetailsScreen(MatchDetailsUiState.Success(match), {}, {}) }
}
