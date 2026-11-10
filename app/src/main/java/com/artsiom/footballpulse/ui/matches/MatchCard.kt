package com.artsiom.footballpulse.ui.matches

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.ui.common.ClubCrest
import com.artsiom.footballpulse.ui.matchdetails.MatchDetailsAppearance
import com.artsiom.footballpulse.ui.matchdetails.matchTextStyle
import com.artsiom.footballpulse.ui.presentation.MatchStatusStyle

@Composable
internal fun MatchCard(card: MatchCardUiModel, appearance: MatchDetailsAppearance, onMatchClick: (Int) -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp), color = appearance.cardColor,
        border = BorderStroke(1.dp, appearance.colors.outlineVariant), shadowElevation = 2.dp
    ) {
        MatchCardContent(card, appearance, Modifier.clickable { onMatchClick(card.match.id) }.padding(12.dp))
    }
}

@Composable
private fun MatchCardContent(card: MatchCardUiModel, appearance: MatchDetailsAppearance, modifier: Modifier) {
    Layout(content = {
        BasicText(card.timeLabel, style = matchTextStyle(12, appearance.colors.onSurfaceVariant))
        MatchStatusBadge(card)
        MatchTeams(card, appearance.colors.onSurface)
    }, modifier = modifier) { children, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val badge = children[1].measure(loose)
        val time = children[0].measure(loose.copy(maxWidth = (constraints.maxWidth - badge.width - 8.dp.roundToPx())
            .coerceAtLeast(0)))
        val teams = children[2].measure(loose)
        // XML anchors the teams below the time label, rather than below the taller status badge.
        val teamsTop = time.height + 10.dp.roundToPx()
        layout(constraints.maxWidth, maxOf(badge.height, teamsTop + teams.height)) {
            time.placeRelative(0, 0)
            badge.placeRelative(constraints.maxWidth - badge.width, 0)
            teams.placeRelative(0, teamsTop)
        }
    }
}

@Composable
private fun MatchStatusBadge(card: MatchCardUiModel) {
    val background = when (card.statusStyle) {
        MatchStatusStyle.FINISHED -> colorResource(R.color.color_status_finished_bg)
        MatchStatusStyle.LIVE -> colorResource(R.color.color_status_live_bg)
        MatchStatusStyle.SCHEDULED -> colorResource(R.color.color_status_scheduled_bg)
    }
    val color = if (card.statusStyle == MatchStatusStyle.FINISHED) {
        colorResource(R.color.color_status_finished_text)
    } else Color.White
    BasicText(
        card.match.status,
        Modifier.background(background, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        style = matchTextStyle(10, color, FontWeight.Bold)
    )
}

@Composable
private fun MatchTeams(card: MatchCardUiModel, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MatchTeam(card.match.homeTeam, card.match.homeTeamCrest, true, color, Modifier.weight(1f))
        BasicText(
            card.scoreLabel, Modifier.widthIn(min = 80.dp).padding(horizontal = 8.dp),
            style = matchTextStyle(22, color, FontWeight.Bold, TextAlign.Center)
        )
        MatchTeam(card.match.awayTeam, card.match.awayTeamCrest, false, color, Modifier.weight(1f))
    }
}

@Composable
private fun MatchTeam(name: String, crest: String?, home: Boolean, color: Color, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (home) {
            MatchCrest(crest)
            Spacer(Modifier.width(6.dp))
        }
        BasicText(
            name, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = matchTextStyle(14, color, FontWeight.Bold, if (home) TextAlign.Start else TextAlign.End)
        )
        if (!home) {
            Spacer(Modifier.width(6.dp))
            MatchCrest(crest)
        }
    }
}

@Composable
private fun MatchCrest(crest: String?) {
    ClubCrest(
        crest, contentDescription = null, modifier = Modifier.size(24.dp), crossfade = true,
        placeholderColor = colorResource(R.color.color_status_finished_bg)
    )
}
