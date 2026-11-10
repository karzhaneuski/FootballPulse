package com.artsiom.footballpulse.ui.matches

import com.artsiom.footballpulse.domain.model.Match
import com.artsiom.footballpulse.ui.presentation.MatchStatusStyle
import com.artsiom.footballpulse.ui.presentation.UiDates
import com.artsiom.footballpulse.ui.presentation.matchStatusStyle
import java.time.ZoneId

sealed class MatchListItem {
    data class DateHeader(val dateLabel: String?) : MatchListItem()
    data class MatchCard(val card: MatchCardUiModel) : MatchListItem()
}

data class MatchCardUiModel(
    val match: Match,
    val timeLabel: String,
    val scoreLabel: String,
    val statusStyle: MatchStatusStyle
)

fun mapMatches(matches: List<Match>, zone: ZoneId = ZoneId.systemDefault()): List<MatchListItem> {
    val dates = UiDates(zone)
    return matches.groupBy { dates.localDate(it.date) }
        .toSortedMap(nullsLast(naturalOrder()))
        .flatMap { (_, group) ->
            listOf(MatchListItem.DateHeader(dates.headerLabel(group.first().date))) + group.map { match ->
                MatchListItem.MatchCard(MatchCardUiModel(
                    match = match,
                    timeLabel = dates.timeLabel(match.date) ?: match.date,
                    scoreLabel = if (match.homeScore != null && match.awayScore != null) {
                        "${match.homeScore} - ${match.awayScore}"
                    } else "- : -",
                    statusStyle = matchStatusStyle(match.status)
                ))
            }
        }
}
