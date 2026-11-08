package com.artsiom.footballpulse.domain.repository

import com.artsiom.footballpulse.domain.model.Match
import com.artsiom.footballpulse.domain.model.MatchDetail
import com.artsiom.footballpulse.domain.model.MatchdayResult
import com.artsiom.footballpulse.domain.model.Standing
import com.artsiom.footballpulse.domain.model.TeamDetails

interface FootballRepository {
    suspend fun getCurrentMatchdayResult(leagueCode: String): MatchdayResult
    suspend fun getMatchesByMatchday(leagueCode: String, matchday: Int, season: Int): List<Match>
    suspend fun getStandings(leagueCode: String): List<Standing>
    suspend fun getMatchDetail(matchId: Int): MatchDetail
    suspend fun getTeamById(id: Int): TeamDetails
}
