package com.artsiom.footballpulse.ui.matchdetails

import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.MatchDetail
import com.artsiom.footballpulse.ui.presentation.MatchStatusStyle
import com.artsiom.footballpulse.ui.presentation.UiDates
import com.artsiom.footballpulse.ui.presentation.matchStatusStyle
import java.time.ZoneId
import java.util.Locale

data class ScoreRowUiModel(val label: Int, val home: String, val away: String)

data class MatchDetailsUiModel(
    val match: MatchDetail,
    val dateLabel: String,
    val scoreOrTime: String,
    val statusStyle: MatchStatusStyle,
    val notStarted: Boolean,
    val scores: List<ScoreRowUiModel>
)

fun mapMatchDetails(match: MatchDetail, zone: ZoneId = ZoneId.systemDefault()): MatchDetailsUiModel {
    val dates = UiDates(zone)
    val notStarted = match.status.uppercase(Locale.ROOT) in setOf("SCHEDULED", "TIMED")
    val rows = buildList {
        if (!notStarted) {
            if (match.halfTimeHome != null && match.halfTimeAway != null) {
                add(ScoreRowUiModel(R.string.first_half, match.halfTimeHome.toString(), match.halfTimeAway.toString()))
            }
            add(ScoreRowUiModel(R.string.full_time, match.fullTimeHome?.toString() ?: "-",
                match.fullTimeAway?.toString() ?: "-"))
        }
    }
    return MatchDetailsUiModel(
        match = match,
        dateLabel = dates.dateLabel(match.date),
        scoreOrTime = if (match.homeScore != null && match.awayScore != null) {
            "${match.homeScore} - ${match.awayScore}"
        } else dates.timeLabel(match.date) ?: "- : -",
        statusStyle = matchStatusStyle(match.status),
        notStarted = notStarted,
        scores = rows
    )
}
