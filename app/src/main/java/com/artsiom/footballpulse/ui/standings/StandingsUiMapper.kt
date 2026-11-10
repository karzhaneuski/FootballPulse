package com.artsiom.footballpulse.ui.standings

import com.artsiom.footballpulse.domain.model.FootballSeason
import com.artsiom.footballpulse.domain.model.League
import com.artsiom.footballpulse.domain.model.LeagueZone
import com.artsiom.footballpulse.domain.model.Leagues
import com.artsiom.footballpulse.domain.model.Standing
import com.artsiom.footballpulse.ui.presentation.FormResult

private const val FORM_WINDOW = 5

data class StandingUiModel(
    val standing: Standing,
    val position: String,
    val playedGames: String,
    val won: String,
    val draw: String,
    val lost: String,
    val points: String,
    val goals: String,
    val goalDifference: String,
    val form: List<FormResult?>,
    val zone: LeagueZone
)

fun mapStandings(
    standings: List<Standing>,
    league: League = Leagues.DEFAULT,
    season: Int = FootballSeason.current()
): List<StandingUiModel> = standings.map { standing ->
    val form = standing.form.orEmpty().mapNotNull { character ->
        FormResult.entries.firstOrNull { it.label == character.toString() }
    }.takeLast(FORM_WINDOW)
    StandingUiModel(
        standing = standing,
        position = standing.position.toString(),
        playedGames = standing.playedGames.toString(),
        won = standing.won.toString(),
        draw = standing.draw.toString(),
        lost = standing.lost.toString(),
        points = standing.points.toString(),
        goals = "${standing.goalsFor}:${standing.goalsAgainst}",
        goalDifference = if (standing.goalDifference > 0) {
            "+${standing.goalDifference}"
        } else standing.goalDifference.toString(),
        form = List(FORM_WINDOW - form.size) { null } + form,
        zone = league.zoneAt(standing.position, season)
    )
}
