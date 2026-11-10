package com.artsiom.footballpulse.ui.standings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.ui.common.ClubCrest
import com.artsiom.footballpulse.ui.matchdetails.matchTextStyle
import com.artsiom.footballpulse.ui.presentation.FormResult

@Composable
internal fun StandingsTableHeader(mode: ViewMode) {
    StandingCells(null, mode, Modifier)
}

@Composable
internal fun StandingsTableRow(row: StandingUiModel, mode: ViewMode, onTeamClick: (Int) -> Unit) {
    StandingCells(row, mode, Modifier.clickable { onTeamClick(row.standing.teamId) })
}

@Composable
private fun StandingCells(row: StandingUiModel?, mode: ViewMode, modifier: Modifier) {
    val background = when {
        row == null -> R.color.color_standings_header_bg
        row.standing.position % 2 == 0 -> R.color.color_standings_row_odd
        else -> R.color.color_standings_row_even
    }
    val color = colorResource(if (row == null) R.color.color_date_header_text else R.color.color_text_primary)
    Row(
        modifier.fillMaxWidth().height(44.dp).background(colorResource(background)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(4.dp).fillMaxHeight()
            .background(row?.let { standingsZoneColor(it.zone) } ?: Color.Transparent))
        StandingCell(row?.position ?: stringResource(R.string.standings_rank), 28.dp, color)
        Box(Modifier.padding(horizontal = 4.dp).size(24.dp)) {
            if (row != null) ClubCrest(
                row.standing.crest, contentDescription = null, modifier = Modifier.size(24.dp),
                crossfade = true, placeholderColor = colorResource(R.color.color_status_finished_bg)
            )
        }
        BasicText(
            row?.standing?.teamName ?: stringResource(R.string.standings_team),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            style = matchTextStyle(12, color), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        StandingStatistics(row, mode, color)
        Box(Modifier.width(32.dp).padding(end = 4.dp), contentAlignment = Alignment.Center) {
            BasicText(
                row?.points ?: stringResource(R.string.standings_points),
                style = matchTextStyle(12, color, if (row == null) FontWeight.Normal else FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun StandingStatistics(row: StandingUiModel?, mode: ViewMode, color: Color) {
    when (mode) {
        ViewMode.FULL -> {
            StandingCell(row?.playedGames ?: stringResource(R.string.standings_played), 32.dp, color)
            StandingCell(row?.won ?: stringResource(R.string.standings_won), 28.dp, color)
            StandingCell(row?.draw ?: stringResource(R.string.standings_drawn), 28.dp, color)
            StandingCell(row?.lost ?: stringResource(R.string.standings_lost), 28.dp, color)
            StandingCell(row?.goals ?: stringResource(R.string.standings_goals), 54.dp, color, size = 11)
        }
        ViewMode.SHORT -> {
            StandingCell(row?.playedGames ?: stringResource(R.string.standings_played), 32.dp, color)
            StandingCell(row?.goalDifference ?: stringResource(R.string.standings_goal_difference), 36.dp, color)
        }
        ViewMode.FORM -> Box(Modifier.width(155.dp), contentAlignment = Alignment.Center) {
            if (row == null) BasicText(stringResource(R.string.standings_form), style = matchTextStyle(11, color))
            else StandingForm(row.form)
        }
    }
}

@Composable
private fun StandingCell(text: String, width: Dp, color: Color, size: Int = 12) {
    BasicText(text, Modifier.width(width), style = matchTextStyle(size, color, alignment = TextAlign.Center))
}

@Composable
private fun StandingForm(results: List<FormResult?>) {
    // The former hand-built badges rounded their size and gap down to physical pixels.
    val size = with(LocalDensity.current) { (28 * density).toInt().toDp() }
    val gap = with(LocalDensity.current) { (3 * density).toInt().toDp() }
    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
        results.forEach { result ->
            val background = when (result) {
                FormResult.WIN -> colorResource(R.color.color_form_win_bg)
                FormResult.DRAW -> colorResource(R.color.color_form_draw_bg)
                FormResult.LOSS -> colorResource(R.color.color_form_loss_bg)
                null -> Color.Transparent
            }
            val label = when (result) {
                FormResult.WIN -> stringResource(R.string.standings_form_win)
                FormResult.DRAW -> stringResource(R.string.standings_form_draw)
                FormResult.LOSS -> stringResource(R.string.standings_form_loss)
                null -> ""
            }
            Box(Modifier.size(size).background(background, CircleShape), contentAlignment = Alignment.Center) {
                BasicText(label, style = matchTextStyle(11, Color.White, FontWeight.Bold, TextAlign.Center))
            }
        }
    }
}
