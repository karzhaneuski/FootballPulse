package com.artsiom.footballpulse.ui.matches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.League
import com.artsiom.footballpulse.ui.common.ErrorState
import com.artsiom.footballpulse.ui.common.LeagueSelector
import com.artsiom.footballpulse.ui.common.LoadingState
import com.artsiom.footballpulse.ui.matchdetails.matchDetailsAppearance
import com.artsiom.footballpulse.ui.matchdetails.matchTextStyle

@Composable
fun MatchesScreen(
    state: MatchesUiState,
    leagues: List<League>,
    selectedLeagueCode: String,
    navigation: MatchdayNavigation,
    actions: MatchesActions
) {
    val appearance = matchDetailsAppearance()
    val typography = MaterialTheme.typography.copy(
        bodyLarge = matchTextStyle(14, appearance.colors.onSurfaceVariant, alignment = TextAlign.Center),
        labelLarge = matchTextStyle(14, appearance.colors.onPrimary, FontWeight.Medium, TextAlign.Center)
            .copy(letterSpacing = 0.1.sp, lineHeight = 20.sp)
    )
    val listState = rememberLazyListState()
    MaterialTheme(colorScheme = appearance.colors, typography = typography) {
        Surface(Modifier.fillMaxSize(), color = appearance.colors.background) {
            Column {
                Surface(Modifier.zIndex(2f), color = appearance.colors.surface, shadowElevation = 4.dp) {
                    LeagueSelector(leagues, selectedLeagueCode, actions.onLeagueSelected, Modifier.fillMaxWidth())
                }
                Surface(Modifier.zIndex(1f), color = appearance.colors.surface, shadowElevation = 2.dp) {
                    MatchdaySelector(navigation, actions, appearance.titleColor)
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    when (state) {
                        MatchesUiState.Loading -> LoadingState()
                        is MatchesUiState.Error -> ErrorState(
                            state.message, actions.onRetry, retryButtonMinWidth = 88.dp, spacing = 12.dp
                        )
                        is MatchesUiState.Success -> LazyColumn(
                            state = listState, contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                        ) {
                            itemsIndexed(state.items, key = { index, item ->
                                when (item) {
                                    is MatchListItem.DateHeader -> "date:$index"
                                    is MatchListItem.MatchCard -> "match:${item.card.match.id}"
                                }
                            }, contentType = { _, item -> item is MatchListItem.DateHeader }) { _, item ->
                                when (item) {
                                    is MatchListItem.DateHeader -> MatchDateHeader(item)
                                    is MatchListItem.MatchCard -> MatchCard(item.card, appearance, actions.onMatchClick)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchdaySelector(navigation: MatchdayNavigation, actions: MatchesActions, textColor: Color) {
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        MatchdayArrow(true, navigation.previousEnabled, textColor, actions.onPreviousMatchday)
        BasicText(
            stringResource(R.string.matchday_format, navigation.matchday), Modifier.weight(1f),
            style = matchTextStyle(16, textColor, alignment = TextAlign.Center)
        )
        MatchdayArrow(false, navigation.nextEnabled, textColor, actions.onNextMatchday)
    }
}

@Composable
private fun MatchdayArrow(previous: Boolean, enabled: Boolean, color: Color, onClick: () -> Unit) {
    val description = stringResource(
        if (previous) R.string.matches_previous_matchday else R.string.matches_next_matchday
    )
    Box(
        Modifier.size(52.dp).alpha(if (enabled) 1f else 0.3f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            stringResource(if (previous) R.string.matches_previous_arrow else R.string.matches_next_arrow),
            style = matchTextStyle(28, color, alignment = TextAlign.Center)
        )
    }
}

@Composable
private fun MatchDateHeader(header: MatchListItem.DateHeader) {
    BasicText(
        header.dateLabel ?: stringResource(R.string.unknown_date),
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
        style = matchTextStyle(12, colorResource(R.color.color_date_header_text), FontWeight.Bold)
            .copy(letterSpacing = 0.08.em)
    )
}
