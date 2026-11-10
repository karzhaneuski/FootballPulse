package com.artsiom.footballpulse.ui.teamdetails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.ui.common.ClubCrest
import com.artsiom.footballpulse.ui.common.ErrorState
import com.artsiom.footballpulse.ui.common.LoadingState
import com.artsiom.footballpulse.ui.matchdetails.MatchDetailsAppearance
import com.artsiom.footballpulse.ui.matchdetails.matchDetailsAppearance
import com.artsiom.footballpulse.ui.matchdetails.matchTextStyle

@Composable
fun TeamDetailsScreen(
    state: TeamDetailsUiState,
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
                TeamDetailsUiState.Loading -> LoadingState()
                is TeamDetailsUiState.Error -> ErrorState(state.message, onRetry, retryButtonMinWidth = 88.dp)
                is TeamDetailsUiState.Success -> TeamDetailsContent(state.content, onBack, appearance)
            }
        }
    }
}

@Composable
private fun TeamDetailsContent(model: TeamDetailsUiModel, onBack: () -> Unit, appearance: MatchDetailsAppearance) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        BasicText(
            stringResource(R.string.team_details_back),
            modifier = Modifier.clickable(onClick = onBack).padding(16.dp),
            style = matchTextStyle(14, colorResource(R.color.color_tab_selected_bg))
        )
        TeamHeader(model, appearance)
        TeamDetailsCard(appearance) {
            TeamCardHeading(R.string.team_details_club_info)
            model.infoRows.forEachIndexed { index, row ->
                if (index > 0) TeamRowDivider()
                TeamInfoRow(row)
            }
        }
        if (model.showSquad) {
            TeamDetailsCard(appearance) {
                TeamCardHeading(R.string.team_details_squad)
                model.squadGroups.forEach { group ->
                    BasicText(
                        stringResource(group.label),
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = viewPadding(12), end = viewPadding(12),
                                top = viewPadding(12), bottom = viewPadding(4)),
                        style = matchTextStyle(12, colorResource(R.color.color_text_secondary), FontWeight.Bold)
                    )
                    TeamRowDivider()
                    group.players.forEach { TeamPlayerRow(it) }
                }
            }
        }
    }
}

@Composable
private fun TeamDetailsCard(appearance: MatchDetailsAppearance, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        shape = RoundedCornerShape(8.dp),
        color = appearance.cardColor,
        border = BorderStroke(1.dp, appearance.colors.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Column { content() }
    }
}

@Composable
private fun TeamHeader(model: TeamDetailsUiModel, appearance: MatchDetailsAppearance) {
    val titleColor = appearance.titleColor
    TeamDetailsCard(appearance) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ClubCrest(
                crestUrl = model.team.crest,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 12.dp).size(80.dp),
                crossfade = true,
                placeholderColor = colorResource(R.color.color_status_finished_bg)
            )
            BasicText(model.team.name, style = matchTextStyle(20, titleColor, FontWeight.Bold, TextAlign.Center))
            if (model.shortLabel.isNotBlank()) {
                BasicText(
                    model.shortLabel,
                    modifier = Modifier.padding(top = 4.dp),
                    style = matchTextStyle(13, appearance.colors.onSurfaceVariant, alignment = TextAlign.Center)
                )
            }
        }
    }
}

@Composable
private fun TeamCardHeading(label: Int) {
    BasicText(
        stringResource(label),
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
        style = matchTextStyle(12, MaterialTheme.colorScheme.onSurfaceVariant, FontWeight.Bold)
    )
    HorizontalDivider(thickness = 1.dp, color = colorResource(R.color.color_divider))
}

@Composable
private fun TeamRowDivider() {
    // The hand-built XML rows used one physical pixel, rather than one dp.
    HorizontalDivider(thickness = with(LocalDensity.current) { 1.toDp() }, color = colorResource(R.color.color_divider))
}

@Composable
private fun TeamInfoRow(row: InfoRowUiModel) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = viewPadding(12), vertical = viewPadding(10)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            stringResource(row.label), Modifier.weight(1f),
            style = matchTextStyle(13, colorResource(R.color.color_text_hint))
        )
        BasicText(
            row.value, style = matchTextStyle(14, colorResource(R.color.color_text_primary), alignment = TextAlign.End)
        )
    }
}

@Composable
private fun TeamPlayerRow(player: SquadPlayerUiModel) {
    val hint = colorResource(R.color.color_text_hint)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = viewPadding(12), vertical = viewPadding(8)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(player.number, Modifier.width(32.dp), style = matchTextStyle(13, hint, alignment = TextAlign.Center))
        BasicText(
            player.name, Modifier.weight(1f), style = matchTextStyle(14, colorResource(R.color.color_text_primary))
        )
        BasicText(player.nationality, style = matchTextStyle(12, hint, alignment = TextAlign.End))
        player.age?.let { age ->
            BasicText(
                age, Modifier.padding(start = viewPadding(8)),
                style = matchTextStyle(13, hint, alignment = TextAlign.End)
            )
        }
    }
}

// Match the integer pixel padding used by the original programmatic rows.
@Composable
private fun viewPadding(value: Int) = with(LocalDensity.current) { (value * density).toInt().toDp() }
